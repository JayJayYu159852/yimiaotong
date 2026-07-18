"""开发调试工具：用与 Java 端相同的算法/密钥签发测试 token
从库里取一个真实管理员账号和一个真实用户手机号，各生成一枚 12 小时有效期的 token。
用法：python gen_token.py
"""
import os
import time
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).parent / ".env")

import jwt
import pymysql


def make_token(sub: str) -> str:
    now = int(time.time())
    # 负载结构对齐 Java 端 JwtTokenUtil：sub + created + exp
    payload = {"sub": sub, "created": now * 1000, "exp": now + 43200}
    return jwt.encode(payload, os.getenv("JWT_SECRET"), algorithm="HS256")


if __name__ == "__main__":
    conn = pymysql.connect(
        host=os.getenv("MYSQL_HOST"),
        port=int(os.getenv("MYSQL_PORT")),
        user=os.getenv("MYSQL_USER"),
        password=os.getenv("MYSQL_PASSWORD"),
        database=os.getenv("MYSQL_DB"),
        charset="utf8mb4",
    )
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT name FROM power_account WHERE status = 1 ORDER BY id LIMIT 1")
            admin_name = cursor.fetchone()[0]
            cursor.execute(
                "SELECT phone FROM user_basic_info WHERE phone REGEXP '^1[0-9]{10}$' ORDER BY id LIMIT 1"
            )
            user_phone = cursor.fetchone()[0]
    finally:
        conn.close()

    print(f"ADMIN({admin_name}):\n{make_token(admin_name)}\n")
    print(f"USER({user_phone}):\n{make_token(user_phone)}")
