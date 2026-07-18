# 医秒通 · RAG 智能问答服务（rag-service）

基于 **LangChain + FastAPI + 阿里云百炼** 的企业级 RAG 知识库问答服务，与医院挂号系统（Spring Boot :8080）通过 JWT 共享密钥打通身份，**Java 源码零修改**。

## 架构速览
```
index.html/admin.html (:8080) ──CORS──▶ rag-service (:8000)
    │                                      ├─ 知识库管理（仅管理员）
    │                                      ├─ RAG问答：混合检索(向量+BM25) → gte-rerank → qwen-plus SSE流式
    │                                      ├─ 号源Agent：Function Calling 只读查库（Redis秒杀库存优先）
    │                                      └─ 会话/历史/缓存/限流
    └─ MySQL hospital（共享，新增 ai_ 三表） / Redis（java=db1, ai=db2） / Chroma（本地 data/chroma）
```

## 快速启动
前置：MySQL(localhost:3306/hospital)、虚拟机 Redis(192.168.100.128:6379) 已启动；`.env` 已配置（百炼 API-KEY、JWT secret 等）。

```bash
cd rag-service
# 首次：创建虚拟环境并安装依赖
python -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt
# 建表（幂等）
.venv\Scripts\python.exe init_db.py
# 导入示例知识文档（可选，--replace 清理同名旧文档）
.venv\Scripts\python.exe import_docs.py --replace
# 启动服务
.venv\Scripts\python.exe -m uvicorn app.main:app --port 8000
```
接口文档：http://localhost:8000/docs ｜ 健康检查：`GET /api/health?deep=true`

## 实用脚本
| 脚本 | 用途 |
|---|---|
| check_env.py | 百炼三模型/MySQL/Redis 连通性体检 |
| gen_token.py | 生成与 Java 端同构的测试 JWT（管理员+用户各一枚） |
| import_docs.py | 批量导入 kb_docs/ 示例文档 |
| test_regression.py | 会话/缓存/限流/越权 回归测试 |

## 关键设计与参数
- **鉴权**：解析 Java 端 JWT（HS256 共享 secret）；管理员 = `power_account ⋈ power_account_role_relation ⋈ power_role` 有角色绑定（注意：power_account 是统一账号表，患者注册也写入，不能只查表存在性）；角色 Redis 缓存 30 分钟
- **检索**：向量 top10 + BM25(jieba) top10 → 去重 → gte-rerank-v2 精排 top4；**阈值 0.10 仅做噪声地板**（rerank 分数非校准概率，相关内容常落 0.15~0.3，切勿调高误杀）
- **号源口径**：Redis db1 `seckill:stock:{planId}` 优先 → 回退 DB 计算（上午6段/下午8段 × 每段5号 - 非取消预约数），与 Java 端 VisitPlanServiceImpl 完全一致
- **缓存**：会话上下文（Redis List，8条/30min）、热点问答（md5 归一化，1h，号源类回答禁止缓存）
- **限流**：ZSET 滑动窗口 20 次/分钟/用户 + 每日 20 万 token 额度
- **大小模型分工**：qwen-plus 生成 / qwen-turbo 多轮改写 / text-embedding-v3(1024维) / gte-rerank-v2
- **SSE 协议**：`sources → delta*N → done{conversationId, assistantMsgId, tokenUsage, toolUsed}`；限流等前置失败返回 JSON（前端按 content-type 区分）

## 常见问题
- 前端 AI 功能报"网络异常" → rag-service 未启动或 8000 端口被占
- 百炼 401 → `.env` 的 DASHSCOPE_API_KEY 失效，去百炼控制台重新生成
- 旧模型名 `gte-rerank` 对新 key 返回 403，必须用 `gte-rerank-v2`
- 修改 `.env` 或代码后需重启 uvicorn（未开 --reload）
