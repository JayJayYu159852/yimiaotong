# 医秒通 · 挂号核心服务（Java）

医秒通平台的核心后端：账号权限（RBAC）、预约挂号、秒杀抢号、支付、病例、排班、统计报表。

> 完整架构、亮点与整体启动说明见[根目录 README](../README.md)。

## 模块速览

| 包 | 职责 |
| --- | --- |
| `controller.admin` / `controller.user` | 管理端 / 用户端接口（Knife4j 按分组展示） |
| `service` | 业务层：预约、号源计划、秒杀、支付、病例、黑名单、信用分 |
| `component` | WebSocket 推送、Redis Stream 秒杀消费者、MQ 生产者、UV 统计、文件存储 |
| `config` | Spring Security、Redis、Redisson、RabbitMQ、MyBatis-Plus 配置 |
| `common` | 统一响应体 `CommonResult`、JWT 工具、动态权限组件 |

## 关键实现

- **秒杀抢号**：`resources/seckill.lua` 原子校验库存 + 一人一单，`AppointmentStreamConsumer` 从 Redis Stream 异步消费落库；
- **动态权限**：`common/security` 下基于路径匹配的 RBAC 动态鉴权，白名单见 `application.yml` 的 `secure.ignored.urls`；
- **预约提醒**：`WebSocketServer` 通过 `/ws/**` 握手推送。

## 单独启动

```bash
# 1. 执行 resources/hospital.sql 建库
# 2. 复制配置模板并填入真实配置
cp src/main/resources/application-example.yml src/main/resources/application.yml
# 3. 启动
mvn spring-boot:run
```

接口文档：http://localhost:8080/hospital/doc.html
