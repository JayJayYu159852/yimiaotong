"""医秒通 · RAG 智能问答服务入口"""
import logging

import uvicorn
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api import auth, chat, health, kb
from app.core.config import get_settings
from app.core.result import BusinessException, ResultCode, failed

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)
settings = get_settings()

app = FastAPI(
    title="医秒通 · RAG 智能问答服务",
    description="独立 AI 服务：知识库管理 / RAG 问答 / 实时号源 Agent",
    version="1.0",
)

# 跨域：放行 Java 端页面来源（8080）
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(BusinessException)
async def business_exception_handler(request: Request, exc: BusinessException):
    """业务/鉴权异常 -> CommonResult 格式；401/403 同步到 HTTP 状态码"""
    http_status = exc.code if exc.code in (ResultCode.UNAUTHORIZED, ResultCode.FORBIDDEN) else 200
    return JSONResponse(status_code=http_status, content=failed(exc.message, exc.code))


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """参数校验失败 -> code 404（对齐 Java 端 VALIDATE_FAILED）"""
    first = exc.errors()[0] if exc.errors() else {}
    field = ".".join(str(x) for x in first.get("loc", [])[1:]) or "参数"
    return JSONResponse(
        status_code=200,
        content=failed(f"参数检验失败: {field} {first.get('msg', '')}", ResultCode.VALIDATE_FAILED),
    )


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """兜底系统异常"""
    logger.exception("系统异常: path=%s", request.url.path)
    return JSONResponse(status_code=200, content=failed("系统繁忙，请稍后重试"))


app.include_router(health.router)
app.include_router(auth.router)
app.include_router(kb.router)
app.include_router(chat.router)


@app.on_event("shutdown")
async def shutdown_event():
    """优雅关闭：Langfuse flush 未发送的观测数据"""
    from app.rag.tracing import shutdown_langfuse
    shutdown_langfuse()


if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=settings.service_port)
