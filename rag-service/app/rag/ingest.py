"""文档异步入库流水线：解析 -> 切分 -> 向量化 -> 写入 Chroma
由 FastAPI BackgroundTasks 在线程池中执行，上传接口秒回不阻塞。
"""
import logging

from app.db.models import AiKbDocument, DocStatus
from app.db.mysql import SessionLocal
from app.rag.loader import load_text
from app.rag.splitter import split_text
from app.rag.vectorstore import delete_doc_chunks, get_vectorstore

logger = logging.getLogger(__name__)


def ingest_document(doc_id: int) -> None:
    """后台任务：独立数据库会话，全程状态可追踪"""
    session = SessionLocal()
    try:
        doc = session.get(AiKbDocument, doc_id)
        if doc is None or doc.is_deleted == 1:
            return
        try:
            text = load_text(doc.file_path, doc.file_type)
            if not text.strip():
                raise ValueError("文档解析结果为空（可能是扫描版PDF，暂不支持OCR）")

            chunks = split_text(text)
            # 幂等：重传/重试场景先清理旧分块
            delete_doc_chunks(doc_id)
            ids = [f"{doc_id}-{i}" for i in range(len(chunks))]
            metadatas = [
                {"docId": doc_id, "docName": doc.doc_name, "chunkIndex": i}
                for i in range(len(chunks))
            ]
            # add_texts 内部按批调用百炼 embedding（单批10条）
            get_vectorstore().add_texts(texts=chunks, metadatas=metadatas, ids=ids)

            doc.status = DocStatus.ACTIVE
            doc.chunk_count = len(chunks)
            doc.fail_reason = None
            logger.info("文档入库成功: docId=%s, name=%s, chunks=%s", doc_id, doc.doc_name, len(chunks))
            # 通知检索层重建 BM25 索引
            from app.rag.retriever import invalidate_index

            invalidate_index()
        except Exception as e:
            doc.status = DocStatus.FAILED
            doc.fail_reason = str(e)[:490]
            logger.exception("文档入库失败: docId=%s", doc_id)
        session.commit()
    finally:
        session.close()
