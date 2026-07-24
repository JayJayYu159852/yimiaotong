"""智能问答与会话管理 API（SSE 流式 + 引用溯源 + 多轮上下文 + 缓存限流 + Langfuse 可观测）"""
import json
import logging
import time

from fastapi import APIRouter, Depends
from fastapi.concurrency import run_in_threadpool
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.result import BusinessException, success
from app.core.security import UserContext, get_current_user
from app.db.models import AiConversation, AiMessage
from app.db.mysql import get_db
from app.rag.agent import stream_agent_answer
from app.rag.chain import build_messages, rewrite_question
from app.rag.retriever import hybrid_retrieve
from app.rag.tracing import get_langfuse_handler, trace_answer, trace_retrieval
from app.services.chat_service import (
    add_token_usage,
    check_daily_budget,
    check_rate_limit,
    clear_history_cache,
    get_or_create_conversation,
    load_history,
    qa_cache_get,
    qa_cache_set,
    save_round,
)

router = APIRouter(prefix="/api/chat", tags=["智能问答"])
logger = logging.getLogger(__name__)


class ChatParam(BaseModel):
    question: str = Field(min_length=1, max_length=500)
    conversationId: int | None = None


def _sse(payload: dict) -> str:
    return "data: " + json.dumps(payload, ensure_ascii=False) + "\n\n"


@router.post("/completions")
async def completions(param: ChatParam, user: UserContext = Depends(get_current_user)):
    """SSE 事件流：sources -> delta*N -> done{conversationId, assistantMsgId, tokenUsage}
    流程：限流/额度 -> 会话归属 -> 热点缓存 -> 多轮改写 -> 混合检索 -> 流式生成 -> 持久化
    """
    # 前置校验在流开始前执行，异常走全局处理器返回 JSON（前端按 content-type 区分）
    check_rate_limit(user.user_name)
    check_daily_budget(user.user_name)
    conv_id, created = get_or_create_conversation(user.user_name, param.conversationId, param.question)

    async def event_stream():
        try:
            history = [] if created else await run_in_threadpool(load_history, conv_id)

            # 热点问答缓存：仅无上下文的首轮问题，命中则免检索免生成
            if not history:
                cached = await run_in_threadpool(qa_cache_get, param.question)
                if cached:
                    yield _sse({"type": "sources", "sources": cached["sources"]})
                    yield _sse({"type": "delta", "content": cached["answer"]})
                    _, ai_id = await run_in_threadpool(
                        save_round, conv_id, param.question, cached["answer"], cached["sources"], 0
                    )
                    yield _sse({
                        "type": "done", "conversationId": conv_id,
                        "assistantMsgId": ai_id, "tokenUsage": 0, "cached": True,
                    })
                    return

            # 多轮改写用于"检索"，原问题+历史用于"生成"（标准 RAG 实践）
            rewritten = await run_in_threadpool(rewrite_question, param.question, history)
            t0 = time.perf_counter()
            chunks = await run_in_threadpool(hybrid_retrieve, rewritten)
            retrieval_ms = (time.perf_counter() - t0) * 1000
            sources = [
                {
                    "docName": c["docName"],
                    "chunkIndex": c["chunkIndex"],
                    "content": c["content"],
                    "score": c.get("score"),
                }
                for c in chunks
            ]
            yield _sse({"type": "sources", "sources": sources})

            # Langfuse 检索 Trace（异步，不阻塞主流）
            await run_in_threadpool(trace_retrieval, param.question, sources, retrieval_ms)

            # Langfuse Callback（如配置了 KEY 则注入，控制 LLM 调用自动上报）
            langfuse_handler = await run_in_threadpool(get_langfuse_handler)
            callbacks = [langfuse_handler] if langfuse_handler else None

            answer, token_usage, tool_used, worker = "", 0, False, ""
            async for delta, usage, tool_flag in stream_agent_answer(
                build_messages(param.question, chunks, history)
            ):
                if tool_flag:
                    tool_used = True
                if delta:
                    answer += delta
                    yield _sse({"type": "delta", "content": delta})
                if usage:
                    token_usage += usage.get("total_tokens", 0)

            _, ai_id = await run_in_threadpool(
                save_round, conv_id, param.question, answer, sources, token_usage
            )
            await run_in_threadpool(add_token_usage, user.user_name, token_usage)
            # 知识库有命中且未走实时工具的首轮问答才进缓存（号源数据实时变化，禁止缓存）
            if not history and sources and not tool_used:
                await run_in_threadpool(qa_cache_set, param.question, answer, sources)

            # Langfuse 问答 Trace（Token/引用/路由，异步不阻塞）
            await run_in_threadpool(
                trace_answer, conv_id, param.question, answer, token_usage, sources, worker
            )

            yield _sse({
                "type": "done", "conversationId": conv_id,
                "assistantMsgId": ai_id, "tokenUsage": token_usage, "toolUsed": tool_used,
            })
        except Exception:
            logger.exception("问答流程异常: user=%s", user.user_name)
            yield _sse({"type": "error", "message": "AI服务繁忙，请稍后再试"})

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


# ==================== 会话管理 ====================

@router.get("/conversations")
def list_conversations(user: UserContext = Depends(get_current_user), db: Session = Depends(get_db)):
    """我的会话列表（按最近活跃排序）"""
    rows = db.scalars(
        select(AiConversation)
        .where(AiConversation.user_name == user.user_name, AiConversation.is_deleted == 0)
        .order_by(AiConversation.update_time.desc())
        .limit(50)
    ).all()
    return success([
        {
            "id": c.id,
            "title": c.title,
            "updateTime": c.update_time.strftime("%m-%d %H:%M") if c.update_time else "",
        }
        for c in rows
    ])


def _get_owned_conversation(conv_id: int, user: UserContext, db: Session) -> AiConversation:
    conv = db.get(AiConversation, conv_id)
    if conv is None or conv.is_deleted == 1 or conv.user_name != user.user_name:
        raise BusinessException("会话不存在")
    return conv


@router.delete("/conversation/{conv_id}")
def delete_conversation(conv_id: int, user: UserContext = Depends(get_current_user), db: Session = Depends(get_db)):
    conv = _get_owned_conversation(conv_id, user, db)
    conv.is_deleted = 1
    db.commit()
    clear_history_cache(conv_id)
    return success(message="删除成功")


@router.get("/messages/{conv_id}")
def list_messages(conv_id: int, user: UserContext = Depends(get_current_user), db: Session = Depends(get_db)):
    """历史消息（换设备/隔天登录找回完整对话）"""
    _get_owned_conversation(conv_id, user, db)
    rows = db.scalars(
        select(AiMessage)
        .where(AiMessage.conversation_id == conv_id, AiMessage.is_deleted == 0)
        .order_by(AiMessage.id.asc())
    ).all()
    return success([
        {
            "id": m.id,
            "role": m.role,
            "content": m.content,
            "sources": json.loads(m.sources_json) if m.sources_json else [],
            "feedback": m.feedback,
        }
        for m in rows
    ])


class FeedbackParam(BaseModel):
    messageId: int
    feedback: int = Field(ge=0, le=2)


@router.post("/feedback")
def feedback(param: FeedbackParam, user: UserContext = Depends(get_current_user), db: Session = Depends(get_db)):
    """点赞/点踩（0取消 1赞 2踩），为检索与回答调优积累数据"""
    msg = db.get(AiMessage, param.messageId)
    if msg is None or msg.is_deleted == 1:
        raise BusinessException("消息不存在")
    _get_owned_conversation(msg.conversation_id, user, db)
    msg.feedback = param.feedback
    db.commit()
    return success(message="感谢反馈")
