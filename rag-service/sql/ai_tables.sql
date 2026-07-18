-- ============================================================
-- RAG 知识库问答系统 · 新增表（独立于现有业务表，ai_ 前缀）
-- 执行库：hospital
-- ============================================================

-- AI 知识库文档表
CREATE TABLE IF NOT EXISTS `ai_kb_document` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `doc_name`     VARCHAR(255) NOT NULL COMMENT '文档名称',
    `file_path`    VARCHAR(512) NOT NULL COMMENT '文件存储路径',
    `file_size`    BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `file_type`    VARCHAR(20)  NOT NULL COMMENT '文件类型：pdf/docx/txt/md',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0解析中 1已生效 2失败',
    `fail_reason`  VARCHAR(500)          DEFAULT NULL COMMENT '解析失败原因',
    `chunk_count`  INT          NOT NULL DEFAULT 0 COMMENT '切分分块数',
    `is_enable`    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `upload_by`    VARCHAR(64)  NOT NULL COMMENT '上传人（管理员账号名）',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI知识库文档表';

-- AI 会话表
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_name`    VARCHAR(64)  NOT NULL COMMENT '归属用户（JWT sub：用户为手机号，管理员为账号名）',
    `title`        VARCHAR(100) NOT NULL DEFAULT '新会话' COMMENT '会话标题（首轮问答后自动生成）',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_user_name` (`user_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI会话表';

-- AI 消息表
CREATE TABLE IF NOT EXISTS `ai_message` (
    `id`               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `conversation_id`  BIGINT      NOT NULL COMMENT '所属会话id',
    `role`             VARCHAR(16) NOT NULL COMMENT '角色：user/assistant',
    `content`          TEXT        NOT NULL COMMENT '消息内容',
    `sources_json`     TEXT                 DEFAULT NULL COMMENT '引用的知识库片段（JSON数组：文档名/片段原文/相关度）',
    `feedback`         TINYINT     NOT NULL DEFAULT 0 COMMENT '用户反馈：0无 1赞 2踩',
    `token_usage`      INT         NOT NULL DEFAULT 0 COMMENT '本条消耗token数',
    `create_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI消息表';
