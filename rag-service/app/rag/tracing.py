"""Langfuse LLM 可观测模块（按需启用，不影响核心链路）
未配置 LANGFUSE_PUBLIC_KEY / LANGFUSE_SECRET_KEY 时自动降级为空操作。
指标采集：检索耗时、精排得分、Token 消耗、引用覆盖率、Agent 路由决策。
"""
import logging
from functools import lru_cache

from langfuse import Langfuse
from langfuse.langchain import CallbackHandler

from app.core.config import get_settings

logger = logging.getLogger(__name__)

# 全局单例（懒加载，避免启动期因网络问题崩溃）
_langfuse: Langfuse | None = None
_handler: CallbackHandler | None = None
_disabled = False


def _init() -> None:
    global _langfuse, _handler, _disabled
    settings = get_settings()
    if _disabled:
        return
    if not settings.langfuse_enabled:
        _disabled = True
        logger.info("Langfuse 未配置(public_key/secret_key为空)，关闭可观测")
        return
    try:
        _langfuse = Langfuse(
            public_key=settings.langfuse_public_key,
            secret_key=settings.langfuse_secret_key,
            host=settings.langfuse_host,
        )
        _handler = CallbackHandler()
        logger.info("Langfuse 已连接: %s", settings.langfuse_host)
    except Exception:
        _disabled = True
        logger.warning("Langfuse 初始化失败，关闭可观测", exc_info=True)


def get_langfuse_handler() -> CallbackHandler | None:
    """返回 LangChain CallbackHandler（配置了即返回，否则返回 None）"""
    if _handler is None and not _disabled:
        _init()
    return _handler


def trace_retrieval(question: str, sources: list[dict], latency_ms: float) -> None:
    """记录检索事件：查询词、召回数量、最高精排得分、耗时"""
    if _langfuse is None:
        return
    try:
        scores = [s.get("score") for s in sources if s.get("score") is not None]
        trace = _langfuse.trace(name="retrieval")
        trace.span(
            name="hybrid_retrieve",
            input={"question": question},
            output={
                "candidate_count": len(sources),
                "top_score": max(scores) if scores else None,
                "latency_ms": round(latency_ms, 1),
            },
        )
        _langfuse.flush()
    except Exception:
        logger.debug("Langfuse retrieval trace 失败", exc_info=True)


def trace_answer(conversation_id: int, question: str, answer: str,
                 token_usage: int, sources: list[dict], worker: str = "") -> None:
    """记录问答事件：Token、引用覆盖率、Agent 路由"""
    if _langfuse is None:
        return
    try:
        cited_docs = len({s.get("docName") for s in sources if s.get("docName")})
        trace = _langfuse.trace(
            name="chat",
            user_id=str(conversation_id),
            metadata={"worker": worker} if worker else None,
        )
        trace.span(
            name="generation",
            input={"question": question},
            output={
                "answer_preview": answer[:200],
                "token_usage": token_usage,
                "cited_docs": cited_docs,
                "source_count": len(sources),
            },
        )
        _langfuse.flush()
    except Exception:
        logger.debug("Langfuse answer trace 失败", exc_info=True)


def shutdown_langfuse() -> None:
    if _langfuse is not None:
        try:
            _langfuse.flush()
        except Exception:
            pass
