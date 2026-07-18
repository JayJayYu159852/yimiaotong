"""
环境校验脚本：百炼API（对话/向量化/重排）、MySQL、Redis 连通性实测
用法：python check_env.py
"""
import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).parent / ".env")

import pymysql
import redis
import requests

API_KEY = os.getenv("DASHSCOPE_API_KEY")
BASE_URL = os.getenv("DASHSCOPE_BASE_URL")

results = []


def check(name, fn):
    try:
        detail = fn()
        results.append((name, True, detail))
    except Exception as e:
        results.append((name, False, f"{type(e).__name__}: {str(e)[:300]}"))


def test_chat():
    resp = requests.post(
        f"{BASE_URL}/chat/completions",
        headers={"Authorization": f"Bearer {API_KEY}"},
        json={
            "model": os.getenv("LLM_MODEL"),
            "messages": [{"role": "user", "content": "只回复两个字：正常"}],
            "max_tokens": 8,
        },
        timeout=60,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"HTTP {resp.status_code}: {resp.text[:300]}")
    return "qwen-plus 回复: " + resp.json()["choices"][0]["message"]["content"].strip()


def test_embedding():
    resp = requests.post(
        f"{BASE_URL}/embeddings",
        headers={"Authorization": f"Bearer {API_KEY}"},
        json={"model": os.getenv("EMBEDDING_MODEL"), "input": "医院挂号流程"},
        timeout=60,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"HTTP {resp.status_code}: {resp.text[:300]}")
    dim = len(resp.json()["data"][0]["embedding"])
    return f"text-embedding-v3 向量维度: {dim}"


def test_rerank():
    # 重排走百炼原生接口（OpenAI 兼容模式不含 rerank）
    resp = requests.post(
        "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank",
        headers={"Authorization": f"Bearer {API_KEY}"},
        json={
            "model": os.getenv("RERANK_MODEL"),
            "input": {"query": "怎么挂号", "documents": ["预约挂号流程说明", "食堂今日菜单"]},
            "parameters": {"top_n": 1},
        },
        timeout=60,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"HTTP {resp.status_code}: {resp.text[:300]}")
    top = resp.json()["output"]["results"][0]
    return f"gte-rerank top1 命中文档下标: {top['index']}，分数: {top['relevance_score']:.4f}"


def test_mysql():
    conn = pymysql.connect(
        host=os.getenv("MYSQL_HOST"),
        port=int(os.getenv("MYSQL_PORT")),
        user=os.getenv("MYSQL_USER"),
        password=os.getenv("MYSQL_PASSWORD"),
        database=os.getenv("MYSQL_DB"),
        charset="utf8mb4",
        connect_timeout=5,
    )
    try:
        with conn.cursor() as cursor:
            # 顺带验证鉴权与号源Agent依赖的表都在
            cursor.execute("SELECT COUNT(*) FROM power_account")
            admins = cursor.fetchone()[0]
            cursor.execute("SELECT COUNT(*) FROM user_basic_info")
            users = cursor.fetchone()[0]
            cursor.execute("SELECT COUNT(*) FROM visit_plan")
            plans = cursor.fetchone()[0]
        return f"管理员账号 {admins} 个，用户 {users} 个，号源计划 {plans} 条"
    finally:
        conn.close()


def test_redis():
    client = redis.Redis(
        host=os.getenv("REDIS_HOST"),
        port=int(os.getenv("REDIS_PORT")),
        password=os.getenv("REDIS_PASSWORD"),
        db=int(os.getenv("REDIS_DB")),
        socket_timeout=5,
    )
    client.set("hospital:ai:ping", "pong", ex=60)
    value = client.get("hospital:ai:ping").decode()
    return f"db{os.getenv('REDIS_DB')} 读写正常: {value}"


if __name__ == "__main__":
    check("百炼-对话(qwen-plus)", test_chat)
    check("百炼-向量化(text-embedding-v3)", test_embedding)
    check("百炼-重排(gte-rerank)", test_rerank)
    check("MySQL(hospital库)", test_mysql)
    check("Redis(192.168.100.128)", test_redis)

    print("\n" + "=" * 60)
    all_ok = True
    for name, ok, detail in results:
        mark = "[OK] " if ok else "[FAIL]"
        print(f"{mark} {name}\n       {detail}")
        all_ok = all_ok and ok
    print("=" * 60)
    print("环境校验全部通过" if all_ok else "存在失败项，见上方明细")
    raise SystemExit(0 if all_ok else 1)
