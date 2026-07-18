"""身份接口：前端据此判断是否展示知识库管理入口"""
from fastapi import APIRouter, Depends

from app.core.result import success
from app.core.security import UserContext, get_current_user

router = APIRouter(prefix="/api/auth", tags=["身份"])


@router.get("/me")
def me(user: UserContext = Depends(get_current_user)):
    """返回当前登录身份与角色（admin/user）"""
    return success({"userName": user.user_name, "role": user.role})
