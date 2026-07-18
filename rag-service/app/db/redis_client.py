"""Redis 连接池（db2，与 Java 端 db1 数据隔离）"""
import redis

from app.core.config import get_settings

settings = get_settings()

_pool = redis.ConnectionPool(
    host=settings.redis_host,
    port=settings.redis_port,
    password=settings.redis_password or None,
    db=settings.redis_db,
    decode_responses=True,
    max_connections=50,
    socket_timeout=5,
    socket_connect_timeout=5,
)

redis_client = redis.Redis(connection_pool=_pool)

# 统一 key 前缀，避免与其他项目冲突
KEY_PREFIX = "hospital:ai:"
