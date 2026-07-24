"""混合检索（向量语义 + BM25 关键词）+ RRF 融合 + gte-rerank 重排
两阶段检索：粗召回（双路各取 top_k + RRF 加权融合）-> 精排（rerank 取 top-n，低分丢弃）
BM25 索引常驻内存，知识库变更后置脏标记按需重建（分块量级小，重建秒级）。

RRF 公式：score(d) = Σ 1/(k + rank_i(d))，默认 k=60
"""
import logging
import threading

import jieba
import requests
from rank_bm25 import BM25Okapi
from sqlalchemy import select

from app.core.config import get_settings
from app.db.models import AiKbDocument, DocStatus
from app.db.mysql import SessionLocal
from app.rag.vectorstore import get_collection, get_vectorstore

logger = logging.getLogger(__name__)

TOP_K_VECTOR = 10
TOP_K_BM25 = 10
RERANK_TOP_N = 4
# RRF 融合常数 k（平滑处理，避免单路高排名主导）
RRF_K = 60
# 重排分低于该阈值视为不相关直接丢弃。
# 注意：gte-rerank-v2 分数非校准概率，相关内容也常落在 0.15~0.3 区间，
# 阈值只做"地板"过滤真噪声，把握不准时宁可放行、靠 Prompt 兜底（实测调优结论）
SCORE_THRESHOLD = 0.10

_lock = threading.Lock()
_bm25: BM25Okapi | None = None
_chunks: list[dict] = []
_dirty = True


def invalidate_index() -> None:
    """知识库变更（入库/删除/启停）后调用，标记 BM25 索引待重建"""
    global _dirty
    _dirty = True


def _load_enabled_chunks() -> list[dict]:
    """加载全部「已生效且启用」文档的分块"""
    with SessionLocal() as db:
        enabled_ids = list(
            db.scalars(
                select(AiKbDocument.id).where(
                    AiKbDocument.is_deleted == 0,
                    AiKbDocument.is_enable == 1,
                    AiKbDocument.status == DocStatus.ACTIVE,
                )
            )
        )
    if not enabled_ids:
        return []
    result = get_collection().get(
        where={"docId": {"$in": enabled_ids}}, include=["documents", "metadatas"]
    )
    return [
        {
            "key": f"{meta.get('docId')}-{meta.get('chunkIndex')}",
            "content": doc,
            "docId": meta.get("docId"),
            "docName": meta.get("docName"),
            "chunkIndex": meta.get("chunkIndex"),
        }
        for doc, meta in zip(result["documents"], result["metadatas"])
    ]


def _rebuild_if_dirty() -> tuple[BM25Okapi | None, list[dict]]:
    global _bm25, _chunks, _dirty
    with _lock:
        if _dirty:
            _chunks = _load_enabled_chunks()
            _bm25 = BM25Okapi([jieba.lcut(c["content"]) for c in _chunks]) if _chunks else None
            _dirty = False
            logger.info("BM25 索引重建完成: %s 个分块", len(_chunks))
        return _bm25, _chunks


def hybrid_retrieve(question: str) -> list[dict]:
    """混合检索 + RRF 融合 + gte-rerank 重排，返回 [{content, docId, docName, chunkIndex, score, recall}]"""
    bm25, chunks = _rebuild_if_dirty()
    if not chunks:
        return []
    doc_ids = list({c["docId"] for c in chunks})

    # 路1：向量语义召回（排序列表，位置=rank）
    vector_ranked: list[dict] = []
    try:
        vec_results = get_vectorstore().similarity_search_with_relevance_scores(
            question, k=TOP_K_VECTOR, filter={"docId": {"$in": doc_ids}}
        )
        for doc, _score in vec_results:
            key = f"{doc.metadata.get('docId')}-{doc.metadata.get('chunkIndex')}"
            vector_ranked.append({
                "key": key,
                "content": doc.page_content,
                "docId": doc.metadata.get("docId"),
                "docName": doc.metadata.get("docName"),
                "chunkIndex": doc.metadata.get("chunkIndex"),
            })
    except Exception:
        logger.exception("向量召回失败，降级仅用 BM25")

    # 路2：BM25 关键词召回（排序列表，位置=rank）
    bm25_ranked: list[dict] = []
    if bm25 is not None:
        scores = bm25.get_scores(jieba.lcut(question))
        sorted_idx = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)
        for i in sorted_idx[:TOP_K_BM25]:
            if scores[i] <= 0:
                continue
            bm25_ranked.append({**chunks[i]})

    # RRF 融合：两路各维护 key→rank 的映射，按 RRF 公式加权
    rrf_scores: dict[str, float] = {}
    candidates: dict[str, dict] = {}

    for rank, item in enumerate(vector_ranked, start=1):
        rrf_scores[item["key"]] = rrf_scores.get(item["key"], 0) + 1.0 / (RRF_K + rank)
        if item["key"] not in candidates:
            candidates[item["key"]] = {**item, "recall": "vector"}

    for rank, item in enumerate(bm25_ranked, start=1):
        rrf_scores[item["key"]] = rrf_scores.get(item["key"], 0) + 1.0 / (RRF_K + rank)
        if item["key"] in candidates:
            candidates[item["key"]]["recall"] = "both"
        else:
            candidates[item["key"]] = {**item, "recall": "bm25"}

    # 按 RRF 分降序取 top-n（两路融合后的候选，上限是两路总数）
    merged = sorted(rrf_scores.items(), key=lambda kv: kv[1], reverse=True)
    merged_candidates = [candidates[key] for key, _ in merged]
    logger.debug("RRF 融合: 向量%d + BM25%d → %d 候选", len(vector_ranked), len(bm25_ranked), len(merged_candidates))

    return _rerank(question, merged_candidates)


def _rerank(question: str, candidates: list[dict]) -> list[dict]:
    """gte-rerank 精排；接口异常时降级返回召回原序 top-n（可用性优先）"""
    if not candidates:
        return []
    settings = get_settings()
    try:
        resp = requests.post(
            "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank",
            headers={"Authorization": f"Bearer {settings.dashscope_api_key}"},
            json={
                "model": settings.rerank_model,
                "input": {"query": question, "documents": [c["content"] for c in candidates]},
                "parameters": {"top_n": RERANK_TOP_N, "return_documents": False},
            },
            timeout=30,
        )
        if resp.status_code != 200:
            raise RuntimeError(f"HTTP {resp.status_code}: {resp.text[:200]}")
        picked = []
        for r in resp.json()["output"]["results"]:
            if r["relevance_score"] < SCORE_THRESHOLD:
                continue
            picked.append({**candidates[r["index"]], "score": round(r["relevance_score"], 4)})
        return picked
    except Exception:
        logger.exception("重排失败，降级返回召回原序 top%s", RERANK_TOP_N)
        return [{**c, "score": None} for c in candidates[:RERANK_TOP_N]]
