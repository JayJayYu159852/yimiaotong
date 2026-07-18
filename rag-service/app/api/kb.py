"""知识库管理 API（仅管理员）"""
import math
import uuid
from pathlib import Path

from fastapi import APIRouter, BackgroundTasks, Depends, UploadFile
from pydantic import BaseModel, Field
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.result import BusinessException, success
from app.core.security import UserContext, require_admin
from app.db.models import AiKbDocument, DocStatus
from app.db.mysql import get_db
from app.rag.ingest import ingest_document
from app.rag.loader import SUPPORTED_TYPES
from app.rag.retriever import hybrid_retrieve, invalidate_index
from app.rag.vectorstore import UPLOAD_DIR, delete_doc_chunks, get_doc_chunks

router = APIRouter(prefix="/api/kb", tags=["知识库管理"], dependencies=[Depends(require_admin)])

MAX_FILE_SIZE = 10 * 1024 * 1024


def _doc_vo(doc: AiKbDocument) -> dict:
    return {
        "id": doc.id,
        "docName": doc.doc_name,
        "fileSize": doc.file_size,
        "fileType": doc.file_type,
        "status": doc.status,
        "failReason": doc.fail_reason,
        "chunkCount": doc.chunk_count,
        "isEnable": doc.is_enable,
        "uploadBy": doc.upload_by,
        "createTime": doc.create_time.strftime("%Y-%m-%d %H:%M:%S") if doc.create_time else None,
    }


@router.post("/doc/upload")
async def upload_doc(
    file: UploadFile,
    background_tasks: BackgroundTasks,
    user: UserContext = Depends(require_admin),
    db: Session = Depends(get_db),
):
    """上传文档，异步切分向量化（前端轮询 status：0解析中/1已生效/2失败）"""
    filename = Path(file.filename or "").name
    file_type = filename.rsplit(".", 1)[-1].lower() if "." in filename else ""
    if file_type == "doc":
        raise BusinessException("暂不支持 .doc 旧格式，请用 Word 另存为 .docx 后上传")
    if file_type not in SUPPORTED_TYPES:
        raise BusinessException(f"仅支持 {'/'.join(sorted(SUPPORTED_TYPES))} 格式")

    content = await file.read()
    if len(content) == 0:
        raise BusinessException("文件内容为空")
    if len(content) > MAX_FILE_SIZE:
        raise BusinessException("文件大小超过 10MB 限制")

    save_path = UPLOAD_DIR / f"{uuid.uuid4().hex}.{file_type}"
    save_path.write_bytes(content)

    doc = AiKbDocument(
        doc_name=filename,
        file_path=str(save_path),
        file_size=len(content),
        file_type=file_type,
        status=DocStatus.PARSING,
        upload_by=user.user_name,
    )
    db.add(doc)
    db.commit()

    background_tasks.add_task(ingest_document, doc.id)
    return success(_doc_vo(doc), "上传成功，正在解析")


@router.get("/doc/list")
def list_docs(
    pageNum: int = 1,
    pageSize: int = 10,
    keyword: str = "",
    db: Session = Depends(get_db),
):
    """文档分页列表（结构对齐 Java 端 CommonPage）"""
    cond = [AiKbDocument.is_deleted == 0]
    if keyword.strip():
        cond.append(AiKbDocument.doc_name.like(f"%{keyword.strip()}%"))

    total = db.scalar(select(func.count()).select_from(AiKbDocument).where(*cond)) or 0
    rows = db.scalars(
        select(AiKbDocument)
        .where(*cond)
        .order_by(AiKbDocument.id.desc())
        .offset((pageNum - 1) * pageSize)
        .limit(pageSize)
    ).all()
    return success({
        "pageNum": pageNum,
        "pageSize": pageSize,
        "totalPage": math.ceil(total / pageSize) if pageSize else 0,
        "total": total,
        "list": [_doc_vo(d) for d in rows],
    })


def _get_doc_or_raise(doc_id: int, db: Session) -> AiKbDocument:
    doc = db.get(AiKbDocument, doc_id)
    if doc is None or doc.is_deleted == 1:
        raise BusinessException("文档不存在")
    return doc


@router.delete("/doc/{doc_id}")
def delete_doc(doc_id: int, db: Session = Depends(get_db)):
    """删除文档：逻辑删除记录 + 物理清理向量分块与磁盘文件"""
    doc = _get_doc_or_raise(doc_id, db)
    doc.is_deleted = 1
    delete_doc_chunks(doc_id)
    Path(doc.file_path).unlink(missing_ok=True)
    db.commit()
    invalidate_index()
    return success(message="删除成功")


class EnableParam(BaseModel):
    isEnable: int = Field(ge=0, le=1)


@router.put("/doc/{doc_id}/enable")
def toggle_enable(doc_id: int, param: EnableParam, db: Session = Depends(get_db)):
    """启用/停用：停用后该文档分块不参与检索（数据保留）"""
    doc = _get_doc_or_raise(doc_id, db)
    doc.is_enable = param.isEnable
    db.commit()
    invalidate_index()
    return success(message="已启用" if param.isEnable == 1 else "已停用")


@router.get("/doc/{doc_id}/chunks")
def preview_chunks(doc_id: int, db: Session = Depends(get_db)):
    """分块预览：调试切分效果"""
    doc = _get_doc_or_raise(doc_id, db)
    return success({"docName": doc.doc_name, "chunks": get_doc_chunks(doc_id)})


class RetrieveTestParam(BaseModel):
    question: str = Field(min_length=1, max_length=200)
    topK: int = Field(default=5, ge=1, le=20)


@router.post("/retrieve/test")
def retrieve_test(param: RetrieveTestParam):
    """召回测试控制台：走线上同款全链路（混合检索->重排->阈值过滤），所见即所得"""
    results = hybrid_retrieve(param.question)
    return success([
        {
            "content": c["content"],
            "docName": c["docName"],
            "chunkIndex": c["chunkIndex"],
            "score": c.get("score"),
            "recall": c.get("recall"),
        }
        for c in results[: param.topK]
    ])
