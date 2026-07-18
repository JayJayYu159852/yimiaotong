"""
执行 sql/ai_tables.sql 建表脚本（幂等，IF NOT EXISTS）
用法：python init_db.py
"""
import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).parent / ".env")

import pymysql

sql_text = (Path(__file__).parent / "sql" / "ai_tables.sql").read_text(encoding="utf-8")
# 先逐行剔除注释行，再按分号拆分执行（避免语句前的注释行导致整段被丢弃）
sql_no_comment = "\n".join(
    line for line in sql_text.splitlines() if not line.strip().startswith("--")
)
statements = [s.strip() for s in sql_no_comment.split(";") if s.strip()]

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
        for stmt in statements:
            cursor.execute(stmt)
        conn.commit()
        cursor.execute("SHOW TABLES LIKE 'ai\\_%'")
        tables = [row[0] for row in cursor.fetchall()]
    print("建表完成，当前 ai_ 前缀表：", tables)
finally:
    conn.close()
