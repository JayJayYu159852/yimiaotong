"""SQLAlchemy ORM 模型：对应 sql/ai_tables.sql 三张表"""
from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Integer, String, Text
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class DocStatus:
    """文档状态：与表注释一致"""

    PARSING = 0
    ACTIVE = 1
    FAILED = 2


class AiKbDocument(Base):
    __tablename__ = "ai_kb_document"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    doc_name: Mapped[str] = mapped_column(String(255))
    file_path: Mapped[str] = mapped_column(String(512))
    file_size: Mapped[int] = mapped_column(BigInteger, default=0)
    file_type: Mapped[str] = mapped_column(String(20))
    status: Mapped[int] = mapped_column(Integer, default=DocStatus.PARSING)
    fail_reason: Mapped[str | None] = mapped_column(String(500), nullable=True)
    chunk_count: Mapped[int] = mapped_column(Integer, default=0)
    is_enable: Mapped[int] = mapped_column(Integer, default=1)
    upload_by: Mapped[str] = mapped_column(String(64))
    create_time: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
    update_time: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, onupdate=datetime.now)
    is_deleted: Mapped[int] = mapped_column(Integer, default=0)


class AiConversation(Base):
    __tablename__ = "ai_conversation"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_name: Mapped[str] = mapped_column(String(64))
    title: Mapped[str] = mapped_column(String(100), default="新会话")
    create_time: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
    update_time: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, onupdate=datetime.now)
    is_deleted: Mapped[int] = mapped_column(Integer, default=0)


class AiMessage(Base):
    __tablename__ = "ai_message"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    conversation_id: Mapped[int] = mapped_column(BigInteger, index=True)
    role: Mapped[str] = mapped_column(String(16))
    content: Mapped[str] = mapped_column(Text)
    sources_json: Mapped[str | None] = mapped_column(Text, nullable=True)
    feedback: Mapped[int] = mapped_column(Integer, default=0)
    token_usage: Mapped[int] = mapped_column(Integer, default=0)
    create_time: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
    update_time: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, onupdate=datetime.now)
    is_deleted: Mapped[int] = mapped_column(Integer, default=0)
