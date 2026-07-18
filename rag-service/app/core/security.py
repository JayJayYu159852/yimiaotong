"""JWT 鉴权与角色判定
复用 Java 端签发的 token（HS256 + 共享 secret），零侵入打通身份：
  - sub 命中 power_account.name   -> 管理员（可管理知识库）
  - sub 命中 user_basic_info.phone -> 普通用户（仅问答）
角色查询结果缓存 Redis 30 分钟，避免每次请求都查库。
"""
import logging
from dataclasses import dataclass

import jwt
from fastapi import Depends, Request
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.result import AuthException, ForbiddenException
from app.db.mysql import get_db
from app.db.redis_client import KEY_PREFIX, redis_client

logger = logging.getLogger(__name__)
settings = get_settings()

ROLE_ADMIN = "admin"
ROLE_USER = "user"

ROLE_CACHE_KEY = KEY_PREFIX + "role:{}"
ROLE_CACHE_TTL = 30 * 60


@dataclass
class UserContext:
    """当前请求用户上下文"""

    user_name: str
    role: str

    @property
    def is_admin(self) -> bool:
        return self.role == ROLE_ADMIN


def _extract_token(request: Request) -> str:
    """提取 token：优先 Authorization 头，其次 ?token= 参数（SSE 场景备用）"""
    auth_header = request.headers.get("Authorization", "")
    head = settings.jwt_token_head.strip()
    if auth_header.startswith(head):
        return auth_header[len(head):].strip()
    token = request.query_params.get("token", "")
    if token:
        return token.strip()
    raise AuthException()


def _decode_token(token: str) -> str:
    """解析 JWT，返回 sub（用户名/手机号）"""
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
    except jwt.ExpiredSignatureError:
        raise AuthException("token已过期，请重新登录")
    except jwt.InvalidTokenError:
        raise AuthException()
    sub = payload.get("sub")
    if not sub:
        raise AuthException()
    return sub


def _resolve_role(user_name: str, db: Session) -> str:
    """角色判定（Redis 缓存 -> 查库回源）"""
    cache_key = ROLE_CACHE_KEY.format(user_name)
    cached = redis_client.get(cache_key)
    if cached:
        return cached

    # 管理员：power_account 启用中，且绑定了管理端角色（ROLE_ADMIN/ROLE_DOCTOR 等）
    # 注：本项目 power_account 为统一账号表（患者注册也写入，name=手机号），
    #     患者账号无角色绑定，故以"是否绑定角色"区分管理端/用户端身份
    row = db.execute(
        text(
            "SELECT r.id FROM power_account a "
            "JOIN power_account_role_relation ar ON ar.account_id = a.id "
            "JOIN power_role r ON r.id = ar.role_id "
            "WHERE a.name = :name AND a.status = 1 LIMIT 1"
        ),
        {"name": user_name},
    ).fetchone()
    if row:
        role = ROLE_ADMIN
    else:
        # 普通用户：统一账号表或用户信息表中存在即可
        row = db.execute(
            text(
                "SELECT id FROM user_basic_info WHERE phone = :sub "
                "UNION SELECT id FROM power_account WHERE name = :sub AND status = 1 LIMIT 1"
            ),
            {"sub": user_name},
        ).fetchone()
        if not row:
            logger.warning("token有效但账号不存在, sub={}".format(user_name))
            raise AuthException("账号不存在或已被禁用")
        role = ROLE_USER

    redis_client.setex(cache_key, ROLE_CACHE_TTL, role)
    return role


def get_current_user(request: Request, db: Session = Depends(get_db)) -> UserContext:
    """FastAPI 依赖：解析 token 并返回用户上下文（所有登录接口的门槛）"""
    token = _extract_token(request)
    user_name = _decode_token(token)
    role = _resolve_role(user_name, db)
    return UserContext(user_name=user_name, role=role)


def require_admin(user: UserContext = Depends(get_current_user)) -> UserContext:
    """FastAPI 依赖：仅管理员可访问（知识库管理接口的门槛）"""
    if not user.is_admin:
        raise ForbiddenException("仅管理员可进行知识库管理")
    return user
