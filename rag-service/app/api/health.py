"""健康检查接口"""
import requests
from fastapi import APIRouter
from sqlalchemy import text

from app.core.config import get_settings
from app.core.result import success
from app.db.mysql import SessionLocal
from app.db.redis_client import redis_client

router = APIRouter(prefix="/api/health", tags=["健康检查"])
settings = get_settings()


@router.get("")
def health(deep: bool = False):
    """基础检查：MySQL/Redis 实测连通；deep=true 时额外实调百炼 embedding 接口"""
    detail = {}

    try:
        with SessionLocal() as db:
            db.execute(text("SELECT 1"))
        detail["mysql"] = "UP"
    except Exception as e:
        detail["mysql"] = "DOWN: {}".format(str(e)[:100])

    try:
        redis_client.ping()
        detail["redis"] = "UP"
    except Exception as e:
        detail["redis"] = "DOWN: {}".format(str(e)[:100])

    if deep:
        try:
            resp = requests.post(
                f"{settings.dashscope_base_url}/embeddings",
                headers={"Authorization": f"Bearer {settings.dashscope_api_key}"},
                json={"model": settings.embedding_model, "input": "ping"},
                timeout=30,
            )
            detail["bailian"] = "UP" if resp.status_code == 200 else f"DOWN: HTTP {resp.status_code}"
        except Exception as e:
            detail["bailian"] = "DOWN: {}".format(str(e)[:100])
    else:
        detail["bailian"] = "CONFIGURED" if settings.dashscope_api_key else "NOT_CONFIGURED"

    detail["status"] = "UP" if all(v.startswith(("UP", "CONFIGURED")) for v in detail.values()) else "DOWN"
    return success(detail)
