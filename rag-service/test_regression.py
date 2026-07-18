"""回归测试：多轮改写/持久化/缓存/限流/会话CRUD"""
import json
import os
import time

import jwt
import requests
from dotenv import load_dotenv

load_dotenv(".env")

BASE = "http://localhost:8000"
now = int(time.time())
TOKEN = jwt.encode(
    {"sub": "13812345678", "created": now * 1000, "exp": now + 43200},
    os.getenv("JWT_SECRET"), algorithm="HS256",
)
H = {"Authorization": f"Bearer {TOKEN}"}


def ask(question, conv_id=None):
    t0 = time.time()
    r = requests.post(f"{BASE}/api/chat/completions", headers=H,
                      json={"question": question, "conversationId": conv_id}, stream=True, timeout=120)
    if "application/json" in r.headers.get("content-type", ""):
        return {"json": r.json()}
    answer, sources, done = "", [], {}
    for line in r.iter_lines(decode_unicode=True):
        if not line or not line.startswith("data: "):
            continue
        ev = json.loads(line[6:])
        if ev["type"] == "sources":
            sources = ev["sources"]
        elif ev["type"] == "delta":
            answer += ev["content"]
        elif ev["type"] == "done":
            done = ev
    return {"answer": answer, "sources": sources, "done": done, "cost": round(time.time() - t0, 2)}


print("=== 1. 首轮提问（新建会话）===")
r1 = ask("退号怎么退费？")
conv_id = r1["done"]["conversationId"]
print(f"convId={conv_id}, 引用{len(r1['sources'])}条, tokens={r1['done']['tokenUsage']}, 耗时{r1['cost']}s")

print("\n=== 2. 多轮追问「那当天呢？」(验证 qwen-turbo 改写) ===")
r2 = ask("那当天呢？", conv_id)
print(f"耗时{r2['cost']}s, 回答前80字: {r2['answer'][:80]}...")
hit = "2小时" in r2["answer"] or "两小时" in r2["answer"]
print("多轮理解", "✅ 正确（提到当天提前2小时规则）" if hit else "❌ 未命中当天退号规则")

print("\n=== 3. 重复首轮问题（验证热点缓存命中）===")
r3 = ask("退号怎么退费？")
print(f"耗时{r3['cost']}s, cached={r3['done'].get('cached')}, tokens={r3['done']['tokenUsage']}")

print("\n=== 4. 会话列表 ===")
convs = requests.get(f"{BASE}/api/chat/conversations", headers=H, timeout=10).json()["data"]
print(f"共{len(convs)}个会话, 最新: {convs[0]}")

print("\n=== 5. 历史消息找回 ===")
msgs = requests.get(f"{BASE}/api/chat/messages/{conv_id}", headers=H, timeout=10).json()["data"]
print(f"会话{conv_id}共{len(msgs)}条消息, 角色序列: {[m['role'] for m in msgs]}")
ai_msg_id = next(m["id"] for m in msgs if m["role"] == "assistant")

print("\n=== 6. 点赞反馈 ===")
fb = requests.post(f"{BASE}/api/chat/feedback", headers=H,
                   json={"messageId": ai_msg_id, "feedback": 1}, timeout=10).json()
print(fb["message"], "->", requests.get(f"{BASE}/api/chat/messages/{conv_id}", headers=H, timeout=10).json()["data"][1]["feedback"])

print("\n=== 7. 越权访问他人会话（doctor 访问患者会话，应失败）===")
tok2 = jwt.encode({"sub": "doctor", "created": now * 1000, "exp": now + 43200}, os.getenv("JWT_SECRET"), algorithm="HS256")
r7 = requests.get(f"{BASE}/api/chat/messages/{conv_id}", headers={"Authorization": f"Bearer {tok2}"}, timeout=10).json()
print(r7)

print("\n=== 8. 限流（直接调用 check_rate_limit 25 次）===")
from app.services.chat_service import check_rate_limit
from app.core.result import BusinessException
blocked = 0
for i in range(25):
    try:
        check_rate_limit("limit_test_user")
    except BusinessException as e:
        blocked += 1
print(f"25次调用中被拦截 {blocked} 次（应为5次，上限20/分钟）")
