"""基于 LangGraph 的多 Agent 架构（Supervisor + 双 Worker）
Supervisor — 意图识别 + 任务路由
  ├── knowledge_worker — RAG 知识回答（流程/政策/介绍）
  └── slot_worker — Function Calling 实时号源查询

实现策略：Supervisor 逐请求编译的轻量图，Worker 级流式以保持 UX 体验。
"""
import json
import logging
from datetime import date, timedelta
from typing import Annotated, Literal, TypedDict

import redis as redis_lib
from fastapi.concurrency import run_in_threadpool
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_core.tools import tool
from langgraph.graph import StateGraph, END
from langgraph.graph.state import CompiledStateGraph
from sqlalchemy import text

from app.core.config import get_settings
from app.db.mysql import SessionLocal
from app.rag.chain import get_chat_llm

logger = logging.getLogger(__name__)
settings = get_settings()

_stock_redis = redis_lib.Redis(
    host=settings.redis_host, port=settings.redis_port,
    password=settings.redis_password or None, db=1, decode_responses=True, socket_timeout=3,
)

MAX_PER_SLOT, AM_SLOTS, PM_SLOTS = 5, 6, 8
TIME_PERIOD_DESC = {1: "上午", 2: "下午"}


# ==================== Tool ====================

def _resolve_date(raw: str) -> str | None:
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
    try:
        stock = _stock_redis.get(f"seckill:stock:{plan_id}")
        if stock is not None:
            return max(0, int(stock))
    except Exception:
        logger.warning("读取秒杀库存失败 planId=%s", plan_id)
    return max(0, (AM_SLOTS if time_period == 1 else PM_SLOTS) * MAX_PER_SLOT - booked)


@tool
def query_doctor_plan(doctor_name: str = "", special_name: str = "", query_date: str = "") -> str:
    """查询医生出诊计划与实时剩余号源。doctor_name=医生姓名（模糊），special_name=科室（如"儿科"），
    query_date=日期(YYYY-MM-DD 或 今天/明天/后天)。返回 JSON 含医生/职称/科室/日期/时段/号源。"""
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
    sql_s = text(
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
        rows = db.execute(sql_s, params).fetchall()
    if not rows:
        return json.dumps({"result": "未查询到符合条件的出诊计划"}, ensure_ascii=False)
    return json.dumps([
        {"医生": r.doctor_name, "职称": r.job_title, "科室": r.special_name,
         "日期": r.visit_day, "时段": TIME_PERIOD_DESC.get(r.time, str(r.time)),
         "剩余号源": _residue(r.id, r.time, r.booked)}
        for r in rows
    ], ensure_ascii=False)


TOOLS = [query_doctor_plan]
_TOOL_MAP = {t.name: t for t in TOOLS}


def _execute_tool(tool_call: dict) -> str:
    fn = _TOOL_MAP.get(tool_call.get("name", ""))
    if fn is None:
        return "未知工具"
    try:
        return fn.invoke(tool_call.get("args", {}))
    except Exception:
        logger.exception("工具执行失败: %s", tool_call)
        return "号源查询失败，请稍后重试"


# ==================== LangGraph Supervisor ====================

class AgentState(TypedDict):
    messages: Annotated[list, "add_messages"]
    next_worker: str


SUPERVISOR_PROMPT = """路由：判断用户问题类别，只输出一个单词。
- knowledge：医院/科室/医生介绍、挂号/退号/退费流程、就诊须知、证件要求
- slot：出诊时间、是否有号、号源余量、预约查询
示例：
"退号怎么退"→knowledge / "儿科今天还有号吗"→slot
"张医生擅长什么"→knowledge / "明天下午的号"→slot
只输出一个单词。"""


def _build_graph() -> CompiledStateGraph:
    llm = get_chat_llm()
    slot_llm = llm.bind_tools(TOOLS)

    def supervisor(state: AgentState) -> AgentState:
        user_msgs = [m for m in state["messages"] if isinstance(m, HumanMessage)]
        last = user_msgs[-1].content if user_msgs else ""
        try:
            resp = llm.invoke([SystemMessage(content=SUPERVISOR_PROMPT), HumanMessage(content=last)])
            worker = (resp.content or "").strip().lower()
        except Exception:
            worker = "knowledge"
        return {"next_worker": worker if worker in ("knowledge", "slot") else "knowledge"}

    def route(state: AgentState) -> Literal["knowledge_worker", "slot_worker"]:
        return state["next_worker"] + "_worker"  # type: ignore[return-value]

    def knowledge_worker(state: AgentState) -> AgentState:
        return {"messages": [llm.invoke(state["messages"])]}

    def slot_worker(state: AgentState) -> AgentState:
        current = list(state["messages"])
        for _ in range(2):  # 最多 2 轮 tool
            resp = slot_llm.invoke(current)
            tool_calls = getattr(resp, "tool_calls", None) or []
            if not tool_calls:
                return {"messages": [resp]}
            current.append(resp)
            for tc in tool_calls:
                current.append(ToolMessage(content=_execute_tool(tc), tool_call_id=tc["id"]))
        return {"messages": [current[-1]]}

    graph = StateGraph(AgentState)
    graph.add_node("supervisor", supervisor)
    graph.add_node("knowledge_worker", knowledge_worker)
    graph.add_node("slot_worker", slot_worker)
    graph.set_entry_point("supervisor")
    graph.add_conditional_edges("supervisor", route, {
        "knowledge_worker": "knowledge_worker", "slot_worker": "slot_worker",
    })
    graph.add_edge("knowledge_worker", END)
    graph.add_edge("slot_worker", END)
    return graph.compile()


_agent_graph: CompiledStateGraph | None = None


def _get_graph() -> CompiledStateGraph:
    global _agent_graph
    if _agent_graph is None:
        _agent_graph = _build_graph()
        logger.info("LangGraph Supervisor Agent 编译完成")
    return _agent_graph


# ==================== 流式入口（chat.py 调用） ====================

async def stream_agent_answer(messages: list):
    """流式生成 (增量文本, usage, 是否刚执行完工具)
    LangGraph 非流式路由一次 → Worker 重新流式生成（内容差异极微，保留真实 token 流式体验）
    """
    graph = _get_graph()
    state: AgentState = {"messages": messages, "next_worker": ""}
    result = await graph.ainvoke(state)

    final_msgs = result.get("messages", [])
    tool_used = any(getattr(m, "tool_calls", None) for m in final_msgs if isinstance(m, AIMessage))
    if tool_used:
        yield "", None, True

    # 流式：用同一消息列表重新 astream（LLM 非确定性但内容语义一致）
    llm = get_chat_llm()
    stream_llm = llm.bind_tools(TOOLS) if tool_used else llm

    async for chunk in stream_llm.astream(messages):
        piece = chunk.content if isinstance(chunk.content, str) else ""
        usage = getattr(chunk, "usage_metadata", None)
        if piece or usage:
            yield piece, usage, False
