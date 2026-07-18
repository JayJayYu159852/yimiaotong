"""实时号源 Agent：Function Calling 工具定义、执行与流式编排
路由决策交给 LLM 本身：绑定工具后，涉及出诊/号源的问题模型自动发起工具调用，
其余问题正常走 RAG 上下文回答——一次调用同时具备两种能力。
号源余量与 Java 端口径一致：Redis 秒杀库存(db1)优先，未命中回退 DB 计算。
"""
import json
import logging
from datetime import date, timedelta

import redis as redis_lib
from fastapi.concurrency import run_in_threadpool
from langchain_core.messages import ToolMessage
from langchain_core.tools import tool
from sqlalchemy import text

from app.core.config import get_settings
from app.db.mysql import SessionLocal
from app.rag.chain import get_chat_llm

logger = logging.getLogger(__name__)
settings = get_settings()

# Java 端秒杀库存存于 Redis db1（本服务业务数据在 db2），建独立只读客户端
_stock_redis = redis_lib.Redis(
    host=settings.redis_host,
    port=settings.redis_port,
    password=settings.redis_password or None,
    db=1,
    decode_responses=True,
    socket_timeout=3,
)

# 号源容量口径对齐 Java 端 VisitPlanServiceImpl：每半小时段 5 个号
# 上午 6 个时段(8:30~12:00)共 30 号，下午 8 个时段(14:00~18:00)共 40 号
MAX_PER_SLOT = 5
AM_SLOTS, PM_SLOTS = 6, 8

TIME_PERIOD_DESC = {1: "上午", 2: "下午"}


def _resolve_date(raw: str) -> str | None:
    """'今天'/'明天'/'后天'/YYYY-MM-DD -> YYYY-MM-DD；无法识别返回 None"""
    raw = (raw or "").strip()
    if not raw:
        return None
    offset = {"今天": 0, "明天": 1, "后天": 2}.get(raw)
    if offset is not None:
        return (date.today() + timedelta(days=offset)).isoformat()
    try:
        return date.fromisoformat(raw).isoformat()
    except ValueError:
        return None


def _residue(plan_id: int, time_period: int, booked: int) -> int:
    """剩余号源：Redis 秒杀库存优先（与秒杀页数据统一），未命中回退 DB 口径"""
    try:
        stock = _stock_redis.get(f"seckill:stock:{plan_id}")
        if stock is not None:
            return max(0, int(stock))
    except Exception:
        logger.warning("读取秒杀库存失败, planId=%s, 回退DB口径", plan_id)
    capacity = (AM_SLOTS if time_period == 1 else PM_SLOTS) * MAX_PER_SLOT
    return max(0, capacity - booked)


@tool
def query_doctor_plan(doctor_name: str = "", special_name: str = "", query_date: str = "") -> str:
    """查询医生出诊计划与实时剩余号源。三个参数均可选（至少提供一个）：
    doctor_name=医生姓名（支持模糊）；special_name=科室/专科名称（支持模糊，如"儿科"）；
    query_date=出诊日期，格式 YYYY-MM-DD，也支持"今天"/"明天"/"后天"。
    返回 JSON 数组，包含医生、职称、科室、日期、时段、剩余号源。"""
    conditions = ["p.day >= CURDATE()"]
    params: dict = {}
    if doctor_name.strip():
        conditions.append("d.name LIKE :dn")
        params["dn"] = f"%{doctor_name.strip().removesuffix('医生')}%"
    if special_name.strip():
        conditions.append("s.name LIKE :sn")
        params["sn"] = f"%{special_name.strip()}%"
    resolved = _resolve_date(query_date)
    if resolved:
        conditions.append("DATE(p.day) = :qd")
        params["qd"] = resolved

    # 明确字段 + 限制条数，只读查询不碰业务写路径
    sql = text(
        "SELECT p.id, d.name AS doctor_name, d.job_title, s.name AS special_name, "
        "DATE_FORMAT(p.day, '%Y-%m-%d') AS visit_day, p.time, "
        "(SELECT COUNT(*) FROM visit_appointment a "
        " WHERE a.plan_id = p.id AND a.status != 2) AS booked "
        "FROM visit_plan p "
        "JOIN hospital_doctor d ON d.id = p.doctor_id "
        "JOIN hospital_special s ON s.id = p.special_id "
        "WHERE " + " AND ".join(conditions) + " "
        "ORDER BY p.day, p.time, d.id LIMIT 15"
    )
    with SessionLocal() as db:
        rows = db.execute(sql, params).fetchall()

    if not rows:
        return json.dumps({"result": "未查询到符合条件的出诊计划（可能日期无排班或姓名/科室有误）"}, ensure_ascii=False)

    plans = [
        {
            "医生": r.doctor_name,
            "职称": r.job_title,
            "科室": r.special_name,
            "日期": r.visit_day,
            "时段": TIME_PERIOD_DESC.get(r.time, str(r.time)),
            "剩余号源": _residue(r.id, r.time, r.booked),
        }
        for r in rows
    ]
    return json.dumps(plans, ensure_ascii=False)


TOOLS = [query_doctor_plan]
_TOOL_MAP = {t.name: t for t in TOOLS}


def _execute_tool(tool_call: dict) -> str:
    fn = _TOOL_MAP.get(tool_call.get("name", ""))
    if fn is None:
        return "未知工具"
    try:
        result = fn.invoke(tool_call.get("args", {}))
        logger.info("工具调用: %s(%s) -> %s", tool_call["name"], tool_call.get("args"), result[:200])
        return result
    except Exception:
        logger.exception("工具执行失败: %s", tool_call)
        return "号源查询失败，请稍后重试或前往挂号页面查看"


async def stream_agent_answer(messages: list):
    """带工具的流式生成：逐帧产出 (增量文本, usage, 是否刚执行完工具)
    最多 2 轮工具调用，防止模型循环调用。
    """
    llm = get_chat_llm().bind_tools(TOOLS)
    rounds = 0
    while True:
        full = None
        async for chunk in llm.astream(messages):
            piece = chunk.content if isinstance(chunk.content, str) else ""
            usage = getattr(chunk, "usage_metadata", None)
            full = chunk if full is None else full + chunk
            if piece or usage:
                yield piece, usage, False

        tool_calls = getattr(full, "tool_calls", None) or []
        if not tool_calls or rounds >= 2:
            return
        rounds += 1
        messages = list(messages) + [full]
        for tc in tool_calls:
            result = await run_in_threadpool(_execute_tool, tc)
            messages.append(ToolMessage(content=result, tool_call_id=tc["id"]))
        yield "", None, True
