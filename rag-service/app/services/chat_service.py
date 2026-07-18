"""会话领域服务：会话/消息持久化、Redis 上下文缓存、热点问答缓存、限流与额度
Redis 工程化思路对齐黑马点评：滑动窗口限流(ZSET)、缓存旁路、TTL 兜底。
"""
import hashlib
import json
import re
import time
from datetime import datetime

from sqlalchemy import select

from app.core.result import BusinessException
from app.db.models import AiConversation, AiMessage
from app.db.mysql import SessionLocal
from app.db.redis_client import KEY_PREFIX, redis_client

# 会话上下文缓存：最近 N 条消息，30 分钟 TTL
HISTORY_KEY = KEY_PREFIX + "conv:{}:history"
HISTORY_LIMIT = 8
HISTORY_TTL = 30 * 60

# 热点问答缓存：仅缓存"无上下文的首轮问题"，1 小时 TTL
QA_CACHE_KEY = KEY_PREFIX + "qa:{}"
QA_CACHE_TTL = 60 * 60

# 滑动窗口限流：每用户每分钟提问上限
RATE_KEY = KEY_PREFIX + "limit:{}"
RATE_LIMIT_PER_MIN = 20

# 每用户每日 token 额度（防刷爆 API 账单）
TOKEN_KEY = KEY_PREFIX + "tokens:{}:{}"
DAILY_TOKEN_BUDGET = 200_000


# ==================== 限流与额度 ====================

def check_rate_limit(user_name: str) -> None:
    """Redis ZSET 滑动窗口限流"""
    key = RATE_KEY.format(user_name)
    now = time.time()
    pipe = redis_client.pipeline()
    pipe.zremrangebyscore(key, 0, now - 60)
    pipe.zadd(key, {f"{now}": now})
    pipe.zcard(key)
    pipe.expire(key, 61)
    count = pipe.execute()[2]
    if count > RATE_LIMIT_PER_MIN:
        raise BusinessException("提问太频繁，请1分钟后再试")


def check_daily_budget(user_name: str) -> None:
    key = TOKEN_KEY.format(user_name, datetime.now().strftime("%Y%m%d"))
    used = int(redis_client.get(key) or 0)
    if used >= DAILY_TOKEN_BUDGET:
        raise BusinessException("今日 AI 问答额度已用完，请明天再来")


def add_token_usage(user_name: str, tokens: int) -> None:
    if tokens <= 0:
        return
    key = TOKEN_KEY.format(user_name, datetime.now().strftime("%Y%m%d"))
    pipe = redis_client.pipeline()
    pipe.incrby(key, tokens)
    pipe.expire(key, 86400 * 2)
    pipe.execute()


# ==================== 会话与消息 ====================

def get_or_create_conversation(user_name: str, conversation_id: int | None, first_question: str) -> tuple[int, bool]:
    """返回 (会话id, 是否新建)；校验会话归属，防越权访问他人会话"""
    with SessionLocal() as db:
        if conversation_id:
            conv = db.get(AiConversation, conversation_id)
            if conv is None or conv.is_deleted == 1 or conv.user_name != user_name:
                raise BusinessException("会话不存在")
            return conv.id, False
        # 新会话：截取首问前20字作标题
        conv = AiConversation(user_name=user_name, title=first_question.strip()[:20] or "新会话")
        db.add(conv)
        db.commit()
        return conv.id, True


def load_history(conversation_id: int) -> list[dict]:
    """加载多轮上下文：Redis 缓存优先，未命中回源 MySQL 并回填"""
    key = HISTORY_KEY.format(conversation_id)
    cached = redis_client.lrange(key, 0, -1)
    if cached:
        return [json.loads(item) for item in cached]

    with SessionLocal() as db:
        rows = db.scalars(
            select(AiMessage)
            .where(AiMessage.conversation_id == conversation_id, AiMessage.is_deleted == 0)
            .order_by(AiMessage.id.desc())
            .limit(HISTORY_LIMIT)
        ).all()
    history = [{"role": r.role, "content": r.content} for r in reversed(rows)]
    if history:
        pipe = redis_client.pipeline()
        pipe.rpush(key, *[json.dumps(m, ensure_ascii=False) for m in history])
        pipe.expire(key, HISTORY_TTL)
        pipe.execute()
    return history


def save_round(conversation_id: int, question: str, answer: str, sources: list[dict], token_usage: int) -> tuple[int, int]:
    """一轮问答落库（用户消息+助手消息+引用），并同步 Redis 上下文缓存"""
    with SessionLocal() as db:
        user_msg = AiMessage(conversation_id=conversation_id, role="user", content=question)
        ai_msg = AiMessage(
            conversation_id=conversation_id,
            role="assistant",
            content=answer,
            sources_json=json.dumps(sources, ensure_ascii=False) if sources else None,
            token_usage=token_usage,
        )
        db.add_all([user_msg, ai_msg])
        conv = db.get(AiConversation, conversation_id)
        if conv:
            conv.update_time = datetime.now()
        db.commit()
        ids = (user_msg.id, ai_msg.id)

    key = HISTORY_KEY.format(conversation_id)
    pipe = redis_client.pipeline()
    pipe.rpush(
        key,
        json.dumps({"role": "user", "content": question}, ensure_ascii=False),
        json.dumps({"role": "assistant", "content": answer}, ensure_ascii=False),
    )
    pipe.ltrim(key, -HISTORY_LIMIT, -1)
    pipe.expire(key, HISTORY_TTL)
    pipe.execute()
    return ids


def clear_history_cache(conversation_id: int) -> None:
    redis_client.delete(HISTORY_KEY.format(conversation_id))


# ==================== 热点问答缓存 ====================

def _normalize(question: str) -> str:
    return re.sub(r"[\s，。？！、,.?!~～]", "", question).lower()


def _qa_key(question: str) -> str:
    digest = hashlib.md5(_normalize(question).encode("utf-8")).hexdigest()
    return QA_CACHE_KEY.format(digest)


def qa_cache_get(question: str) -> dict | None:
    value = redis_client.get(_qa_key(question))
    return json.loads(value) if value else None


def qa_cache_set(question: str, answer: str, sources: list[dict]) -> None:
    redis_client.setex(
        _qa_key(question), QA_CACHE_TTL,
        json.dumps({"answer": answer, "sources": sources}, ensure_ascii=False),
    )
