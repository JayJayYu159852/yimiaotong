"""MySQL 连接（SQLAlchemy），复用现有 hospital 库"""
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.core.config import get_settings

settings = get_settings()

# 连接池参数对齐 Java 端 Hikari 配置量级
engine = create_engine(
    settings.mysql_url,
    pool_size=10,
    max_overflow=20,
    pool_pre_ping=True,
    pool_recycle=1200,
    echo=False,
)

SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)


def get_db():
    """FastAPI 依赖：请求级会话，用完归还连接池"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
