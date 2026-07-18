"""统一响应体与业务异常：完全对齐 Java 端 CommonResult / ResultCodeEnum"""
from typing import Any


class ResultCode:
    """结果码，与 Java 端 ResultCodeEnum 一致"""

    SUCCESS = 200
    FAILED = 500
    VALIDATE_FAILED = 404
    UNAUTHORIZED = 401
    FORBIDDEN = 403


def success(data: Any = None, message: str = "操作成功") -> dict:
    return {"code": ResultCode.SUCCESS, "message": message, "data": data}


def failed(message: str = "操作失败", code: int = ResultCode.FAILED) -> dict:
    return {"code": code, "message": message, "data": None}


class BusinessException(Exception):
    """业务异常：全局异常处理器统一转换为 CommonResult 格式"""

    def __init__(self, message: str, code: int = ResultCode.FAILED):
        super().__init__(message)
        self.message = message
        self.code = code


class AuthException(BusinessException):
    """鉴权异常（未登录/token过期）"""

    def __init__(self, message: str = "暂未登录或token已经过期"):
        super().__init__(message, ResultCode.UNAUTHORIZED)


class ForbiddenException(BusinessException):
    """权限不足异常"""

    def __init__(self, message: str = "没有相关权限"):
        super().__init__(message, ResultCode.FORBIDDEN)
