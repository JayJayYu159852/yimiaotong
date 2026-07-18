"""批量导入 kb_docs/ 下的示例知识文档（同步执行入库流水线）
用法：python import_docs.py [--replace]
--replace：先逻辑删除库中已存在的同名文档（含早期测试文档），再导入，避免重复分块
"""
import shutil
import sys
import uuid
from pathlib import Path

from dotenv import load_dotenv

BASE = Path(__file__).parent
load_dotenv(BASE / ".env")

from sqlalchemy import select

from app.db.models import AiKbDocument, DocStatus
from app.db.mysql import SessionLocal
from app.rag.ingest import ingest_document
from app.rag.vectorstore import UPLOAD_DIR, delete_doc_chunks

KB_DIR = BASE / "kb_docs"
# 早期联调测试文档，导入正式文档集时一并清理
LEGACY_TEST_DOCS = {"test_kb_doc.md"}


def remove_existing(names: set[str]) -> None:
    with SessionLocal() as db:
        rows = db.scalars(
            select(AiKbDocument).where(
                AiKbDocument.is_deleted == 0, AiKbDocument.doc_name.in_(list(names))
            )
        ).all()
        for doc in rows:
            doc.is_deleted = 1
            delete_doc_chunks(doc.id)
            Path(doc.file_path).unlink(missing_ok=True)
            print(f"已清理旧文档: id={doc.id} {doc.doc_name}")
        db.commit()


def import_all() -> None:
    files = sorted(KB_DIR.glob("*.md"))
    if not files:
        print("kb_docs/ 下没有待导入的 md 文档")
        return
    if "--replace" in sys.argv:
        remove_existing({f.name for f in files} | LEGACY_TEST_DOCS)

    for f in files:
        save_path = UPLOAD_DIR / f"{uuid.uuid4().hex}.md"
        shutil.copyfile(f, save_path)
        with SessionLocal() as db:
            doc = AiKbDocument(
                doc_name=f.name,
                file_path=str(save_path),
                file_size=f.stat().st_size,
                file_type="md",
                status=DocStatus.PARSING,
                upload_by="system",
            )
            db.add(doc)
            db.commit()
            doc_id = doc.id
        ingest_document(doc_id)  # 同步执行，便于脚本内看到结果
        with SessionLocal() as db:
            doc = db.get(AiKbDocument, doc_id)
            flag = "OK " if doc.status == DocStatus.ACTIVE else "FAIL"
            print(f"[{flag}] {doc.doc_name}: status={doc.status}, chunks={doc.chunk_count}, {doc.fail_reason or ''}")


if __name__ == "__main__":
    import_all()
