"""Chroma 向量库封装（本地嵌入式，LangChain 抽象下可切换 Milvus 等实现）"""
from functools import lru_cache
from pathlib import Path

import chromadb
from langchain_chroma import Chroma

from app.rag.embeddings import get_embeddings

# 数据目录：rag-service/data/
DATA_DIR = Path(__file__).resolve().parents[2] / "data"
UPLOAD_DIR = DATA_DIR / "uploads"
CHROMA_DIR = DATA_DIR / "chroma"
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
CHROMA_DIR.mkdir(parents=True, exist_ok=True)

COLLECTION_NAME = "hospital_kb"


@lru_cache
def _get_client() -> chromadb.ClientAPI:
    return chromadb.PersistentClient(path=str(CHROMA_DIR))


@lru_cache
def get_vectorstore() -> Chroma:
    """LangChain VectorStore 实例（余弦相似度）"""
    return Chroma(
        client=_get_client(),
        collection_name=COLLECTION_NAME,
        embedding_function=get_embeddings(),
        collection_metadata={"hnsw:space": "cosine"},
    )


def _collection():
    return _get_client().get_or_create_collection(COLLECTION_NAME)


def get_collection():
    """暴露原生 collection（BM25 索引构建等场景直接读取分块）"""
    return _collection()


def delete_doc_chunks(doc_id: int) -> None:
    """删除指定文档的全部向量分块（文档删除/重建索引时调用）"""
    _collection().delete(where={"docId": {"$eq": doc_id}})


def get_doc_chunks(doc_id: int) -> list[dict]:
    """按分块序号返回指定文档的全部分块原文（管理端分块预览）"""
    result = _collection().get(
        where={"docId": {"$eq": doc_id}}, include=["documents", "metadatas"]
    )
    chunks = [
        {"chunkIndex": meta.get("chunkIndex", 0), "content": doc}
        for doc, meta in zip(result["documents"], result["metadatas"])
    ]
    return sorted(chunks, key=lambda c: c["chunkIndex"])


def count_chunks() -> int:
    return _collection().count()
