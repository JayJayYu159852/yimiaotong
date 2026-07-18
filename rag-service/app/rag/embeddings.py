"""向量化模型：百炼 text-embedding-v3（OpenAI 兼容模式接入）"""
from functools import lru_cache

from langchain_openai import OpenAIEmbeddings

from app.core.config import get_settings


@lru_cache
def get_embeddings() -> OpenAIEmbeddings:
    settings = get_settings()
    return OpenAIEmbeddings(
        model=settings.embedding_model,
        api_key=settings.dashscope_api_key,
        base_url=settings.dashscope_base_url,
        # 非 OpenAI 官方端点必须关闭 tiktoken 预分词，否则发送 token 数组导致百炼报错
        check_embedding_ctx_length=False,
        # 百炼 embedding 接口单次批量上限 10 条
        chunk_size=10,
    )
