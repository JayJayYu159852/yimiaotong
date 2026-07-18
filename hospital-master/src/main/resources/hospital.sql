SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for hospital_doctor
-- ----------------------------
DROP TABLE IF EXISTS `hospital_doctor`;
CREATE TABLE `hospital_doctor`
(
    `id`            bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT '医生编号',
    `name`          varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '医生姓名',
    `gender`        int(11)                                                       NOT NULL DEFAULT 1 COMMENT '性别：1，男；2，女',
    `job_title`     varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '医生职称',
    `specialty`     varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '医生专长',
    `special_id`    bigint(20)                                                    NOT NULL COMMENT '所属专科',
    `gmt_create`    datetime(0)                                                   NOT NULL COMMENT '创建时间',
    `gmt_modified`  datetime(0)                                                   NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `hospital_doctor_hospital_special_id_fk` (`special_id`) USING BTREE,
    CONSTRAINT `hospital_doctor_hospital_special_id_fk` FOREIGN KEY (`special_id`) REFERENCES `hospital_special` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10018
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '医生信息表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hospital_info
-- ----------------------------
DROP TABLE IF EXISTS `hospital_info`;
CREATE TABLE `hospital_info`
(
    `id`           bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT '医院编号 从1001开始',
    `name`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '医院名称',
    `phone`        varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL COMMENT '医院电话',
    `address`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '医院地址',
    `description`  varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '医院简介',
    `picture`      varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '医院图片',
    `latitude`     double                                                       NULL DEFAULT NULL COMMENT '纬度（用于GEO附近搜索）',
    `longitude`    double                                                       NULL DEFAULT NULL COMMENT '经度（用于GEO附近搜索）',
    `gmt_create`   datetime(0)                                                   NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                   NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `hospital_info_phone_uindex` (`phone`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1008
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '医院信息表 '
  ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for hospital_special
-- ----------------------------
DROP TABLE IF EXISTS `hospital_special`;
CREATE TABLE `hospital_special`
(
    `id`           bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT '专科编号',
    `name`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '专科名称',
    `description`  varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '专科简介',
    `gmt_create`   datetime(0)                                                   NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                   NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `hospital_special_name_uindex` (`name`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10012
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '医院专科表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hospital_special_relation
-- ----------------------------
DROP TABLE IF EXISTS `hospital_special_relation`;
CREATE TABLE `hospital_special_relation`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '关系编号',
    `hospital_id`  bigint(20)  NOT NULL COMMENT '医院编号',
    `special_id`   bigint(20)  NOT NULL COMMENT '专科编号',
    `gmt_create`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `hospital_special_relation_hospital_info_id_fk` (`hospital_id`) USING BTREE,
    INDEX `hospital_special_relation_hospital_special_id_fk` (`special_id`) USING BTREE,
    CONSTRAINT `hospital_special_relation_hospital_info_id_fk` FOREIGN KEY (`hospital_id`) REFERENCES `hospital_info` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `hospital_special_relation_hospital_special_id_fk` FOREIGN KEY (`special_id`) REFERENCES `hospital_special` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 17
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = ' '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for log_account_login
-- ----------------------------
DROP TABLE IF EXISTS `log_account_login`;
CREATE TABLE `log_account_login`
(
    `id`           bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT '登录记录编号',
    `account_id`   bigint(20)                                                   NOT NULL COMMENT '账号编号',
    `account_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '账号名称',
    `ip_address`   varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT 'ip地址',
    `gmt_create`   datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `log_account_login_power_account_id_fk` (`account_id`) USING BTREE,
    CONSTRAINT `log_account_login_power_account_id_fk` FOREIGN KEY (`account_id`) REFERENCES `power_account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10059
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '账号登录记录表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for log_operation
-- ----------------------------
DROP TABLE IF EXISTS `log_operation`;
CREATE TABLE `log_operation`
(
    `id`           bigint(20)                                                     NOT NULL AUTO_INCREMENT COMMENT '编号',
    `account_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL     DEFAULT NULL COMMENT '账号名称',
    `start_time`   bigint(20)                                                     NULL     DEFAULT NULL COMMENT '开始时间',
    `spend_time`   int(11)                                                        NULL     DEFAULT NULL COMMENT '消耗时间',
    `description`  varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL     DEFAULT NULL COMMENT '操作描述',
    `base_path`    varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL     DEFAULT NULL COMMENT '根路径',
    `uri`          varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL     DEFAULT NULL COMMENT 'uri',
    `url`          varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL     DEFAULT NULL COMMENT 'url',
    `method`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL     DEFAULT NULL COMMENT '请求方法',
    `ip_address`   varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NULL     DEFAULT NULL COMMENT 'ip地址',
    `parameter`    varchar(3072) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '请求参数',
    `result`       text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          NULL COMMENT '请求结果',
    `gmt_create`   datetime(0)                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `log_operation_power_account_name_fk` (`account_name`) USING BTREE,
    CONSTRAINT `log_operation_power_account_name_fk` FOREIGN KEY (`account_name`) REFERENCES `power_account` (`name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 11924
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户操作记录表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for power_account
-- ----------------------------
DROP TABLE IF EXISTS `power_account`;
CREATE TABLE `power_account`
(
    `id`           bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '登录账号 唯一',
    `password`     varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录密码 使用md5加密',
    `status`       int(11)                                                       NOT NULL DEFAULT 1 COMMENT '账号状态 1：正常，0：锁定',
    `login_time`   datetime(0)                                                   NULL     DEFAULT NULL COMMENT '最后登录时间',
    `gmt_create`   datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `power_account_name_uindex` (`name`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000004
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '账号信息表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for power_account_role_relation
-- ----------------------------
DROP TABLE IF EXISTS `power_account_role_relation`;
CREATE TABLE `power_account_role_relation`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '账号角色关系编号',
    `account_id`   bigint(20)  NOT NULL COMMENT '账号编号',
    `role_id`      bigint(20)  NOT NULL COMMENT '角色编号',
    `gmt_create`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `power_account_role_relation_power_role_id_fk` (`role_id`) USING BTREE,
    INDEX `power_account_role_relation_power_account_id_fk` (`account_id`) USING BTREE,
    CONSTRAINT `power_account_role_relation_power_account_id_fk` FOREIGN KEY (`account_id`) REFERENCES `power_account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `power_account_role_relation_power_role_id_fk` FOREIGN KEY (`role_id`) REFERENCES `power_role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '账号角色关系表'
  ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for power_resource
-- ----------------------------
DROP TABLE IF EXISTS `power_resource`;
CREATE TABLE `power_resource`
(
    `id`           bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT '资源编号',
    `category_id`  bigint(20)                                                   NOT NULL COMMENT '资源分类编号',
    `name`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '资源名称',
    `url`          varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '资源URL',
    `description`  varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '资源描述',
    `gmt_create`   datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `power_resource_url_uindex` (`url`) USING BTREE,
    UNIQUE INDEX `power_resource_name_uindex` (`name`) USING BTREE,
    INDEX `power_resource_power_resource_category_id_fk` (`category_id`) USING BTREE,
    CONSTRAINT `power_resource_power_resource_category_id_fk` FOREIGN KEY (`category_id`) REFERENCES `power_resource_category` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '权限资源表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for power_resource_category
-- ----------------------------
DROP TABLE IF EXISTS `power_resource_category`;
CREATE TABLE `power_resource_category`
(
    `id`           bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT '分类编号',
    `name`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
    `sort`         int(11)                                                      NOT NULL DEFAULT 1 COMMENT '分类排序 数值越小，越靠前',
    `gmt_create`   datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `power_resource_category_name_uindex` (`name`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '权限资源分类表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for power_role
-- ----------------------------
DROP TABLE IF EXISTS `power_role`;
CREATE TABLE `power_role`
(
    `id`           bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT '角色编号',
    `name`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '英文名称',
    `chinese_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名称',
    `admin_count`  int(11)                                                      NOT NULL DEFAULT 0 COMMENT '用户数目',
    `sort`         int(11)                                                      NOT NULL DEFAULT 0 COMMENT '排序 越小越靠前',
    `status`       int(11)                                                      NOT NULL DEFAULT 1 COMMENT '角色状态 1：启用，0：禁用',
    `gmt_create`   datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `power_role_name_uindex` (`name`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '权限角色表'
  ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for power_role_resource_relation
-- ----------------------------
DROP TABLE IF EXISTS `power_role_resource_relation`;
CREATE TABLE `power_role_resource_relation`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '关系编号',
    `role_id`      bigint(20)  NOT NULL COMMENT '角色编号',
    `resource_id`  bigint(20)  NOT NULL COMMENT '资源编号',
    `gmt_create`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `power_role_resource_relation_power_resource_id_fk` (`resource_id`) USING BTREE,
    INDEX `power_role_resource_relation_power_role_id_fk` (`role_id`) USING BTREE,
    CONSTRAINT `power_role_resource_relation_power_resource_id_fk` FOREIGN KEY (`resource_id`) REFERENCES `power_resource` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `power_role_resource_relation_power_role_id_fk` FOREIGN KEY (`role_id`) REFERENCES `power_role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '角色资源关系表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_basic_info
-- ----------------------------
DROP TABLE IF EXISTS `user_basic_info`;
CREATE TABLE `user_basic_info`
(
    `id`           bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '姓名',
    `avatar_url`   varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户头像',
    `phone`        varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '手机号',
    `gmt_create`   datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `user_basic_info_phone_uindex` (`phone`) USING BTREE,
    CONSTRAINT `user_basic_info_power_account_name_fk` FOREIGN KEY (`phone`) REFERENCES `power_account` (`name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 4
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户基础信息表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_case
-- ----------------------------
DROP TABLE IF EXISTS `user_case`;
CREATE TABLE `user_case`
(
    `id`             bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    `card_id`        bigint(20)                                                    NOT NULL COMMENT '就诊卡编号',
    `appointment_id` bigint(20)                                                    NOT NULL COMMENT '预约编号',
    `doctor_id`      bigint(20)                                                    NOT NULL COMMENT '医生编号',
    `content`        varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '病例详情',
    `gmt_create`     datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified`   datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `user_case_hospital_doctor_id_fk` (`doctor_id`) USING BTREE,
    INDEX `user_case_user_medical_card_id_fk` (`card_id`) USING BTREE,
    CONSTRAINT `user_case_hospital_doctor_id_fk` FOREIGN KEY (`doctor_id`) REFERENCES `hospital_doctor` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `user_case_user_medical_card_id_fk` FOREIGN KEY (`card_id`) REFERENCES `user_medical_card` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户病例表 '
  ROW_FORMAT = Dynamic;
-- ----------------------------
-- Table structure for user_medical_card
-- ----------------------------
DROP TABLE IF EXISTS `user_medical_card`;
CREATE TABLE `user_medical_card`
(
    `id`                    bigint(20)                                                   NOT NULL AUTO_INCREMENT COMMENT '就诊卡号',
    `name`                  varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
    `gender`                int(11)                                                      NOT NULL DEFAULT 1 COMMENT '性别 男：1，女：2',
    `phone`                 varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号',
    `identification_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '证件号（注册时可选，完善信息后必填）',
    `birth_date`            datetime(0)                                                  NULL DEFAULT NULL COMMENT '出生日期',
    `gmt_create`            datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified`          datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `user_medical_card_identification_number_uindex` (`identification_number`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 7000003
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户就诊卡信息表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_medical_card_relation
-- ----------------------------
DROP TABLE IF EXISTS `user_medical_card_relation`;
CREATE TABLE `user_medical_card_relation`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '关系编号',
    `account_id`   bigint(20)  NOT NULL COMMENT '账号编号',
    `card_id`      bigint(20)  NOT NULL COMMENT '就诊卡编号',
    `gmt_create`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    -- 唯一索引强制一人一卡（1:1）；将来演进为一账号多就诊人时，仅需删除 uk_account_id
    UNIQUE INDEX `uk_account_id` (`account_id`) USING BTREE,
    UNIQUE INDEX `uk_card_id` (`card_id`) USING BTREE,
    CONSTRAINT `user_medical_card_relation_power_account_id_fk` FOREIGN KEY (`account_id`) REFERENCES `power_account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `user_medical_card_relation_user_medical_card_id_fk` FOREIGN KEY (`card_id`) REFERENCES `user_medical_card` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 6
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户就诊卡关系表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for visit_appointment
-- ----------------------------
DROP TABLE IF EXISTS `visit_appointment`;
CREATE TABLE `visit_appointment`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '预约编号',
    `plan_id`      bigint(20)  NOT NULL COMMENT '出诊编号',
    `card_id`      bigint(20)  NOT NULL COMMENT '就诊卡号',
    `account_id`   bigint(20)  NOT NULL COMMENT '账号编号',
    `time_period`  int(11)     NOT NULL COMMENT '1： 8点半~9点，2： 9点~9点半，3： 9点半~10点，4： 10点~10点半，5： 11点~11点半，6： 11点半~12点，7：2点~2点半，8： 2点半~3点，9： 3点~3点半，10： 3点半~4点，11： 4点~4点半，12： 4点半~5点，13： 5点~5点半，14：5点半~6点',
    `status`       int(11)     NOT NULL DEFAULT 0 COMMENT '预约状态 0：未开始，1：未按时就诊，2：取消预约挂号，3：已完成',
    `pay_status`   int(11)     NOT NULL DEFAULT 0 COMMENT '支付状态：0=未支付, 1=已支付, 2=已退款',
    `payment_id`   bigint(20)  DEFAULT NULL COMMENT '关联支付订单编号',
    `gmt_create`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `visit_order_user_medical_card_id_fk` (`card_id`) USING BTREE,
    INDEX `visit_order_visit_plan_id_fk` (`plan_id`) USING BTREE,
    INDEX `visit_order_power_account_id_fk` (`account_id`) USING BTREE,
    CONSTRAINT `visit_order_power_account_id_fk` FOREIGN KEY (`account_id`) REFERENCES `power_account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `visit_order_user_medical_card_id_fk` FOREIGN KEY (`card_id`) REFERENCES `user_medical_card` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `visit_order_visit_plan_id_fk` FOREIGN KEY (`plan_id`) REFERENCES `visit_plan` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '出诊预约表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for visit_blacklist
-- ----------------------------
DROP TABLE IF EXISTS `visit_blacklist`;
CREATE TABLE `visit_blacklist`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '名单编号',
    `card_id`      bigint(20)  NOT NULL COMMENT '就诊卡号',
    `status`       int(11)     NOT NULL DEFAULT 1 COMMENT '禁封状态 1：生效，2：已解封',
    `gmt_create`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `visit_blacklist_user_medical_card_id_fk` (`card_id`) USING BTREE,
    CONSTRAINT `visit_blacklist_user_medical_card_id_fk` FOREIGN KEY (`card_id`) REFERENCES `user_medical_card` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '出诊黑名单 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for visit_plan
-- ----------------------------
DROP TABLE IF EXISTS `visit_plan`;
CREATE TABLE `visit_plan`
(
    `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '出诊编号',
    `hospital_id`   bigint(20)  NOT NULL COMMENT '医院编号',
    `special_id`    bigint(20)  NOT NULL COMMENT '专科编号',
    `doctor_id`     bigint(20)  NOT NULL COMMENT '医生编号',
    `time`          int(11)     NOT NULL DEFAULT 1 COMMENT '时间段 1：上午，2：下午',
    `day`           datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '出诊日期',
    `gmt_create`    datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified`  datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `visit_plan_hospital_doctor_id_fk` (`doctor_id`) USING BTREE,
    INDEX `visit_plan_hospital_info_id_fk` (`hospital_id`) USING BTREE,
    INDEX `visit_plan_hospital_special_id_fk` (`special_id`) USING BTREE,
    CONSTRAINT `visit_plan_hospital_doctor_id_fk` FOREIGN KEY (`doctor_id`) REFERENCES `hospital_doctor` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `visit_plan_hospital_info_id_fk` FOREIGN KEY (`hospital_id`) REFERENCES `hospital_info` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `visit_plan_hospital_special_id_fk` FOREIGN KEY (`special_id`) REFERENCES `hospital_special` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10042
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '出诊信息表 '
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hospital_follow
-- ----------------------------
DROP TABLE IF EXISTS `hospital_follow`;
CREATE TABLE `hospital_follow`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '关注编号',
    `user_id`      bigint(20)  NOT NULL COMMENT '用户编号',
    `follow_user_id` bigint(20) NOT NULL COMMENT '关注的医院编号',
    `gmt_create`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_hospital` (`user_id`, `follow_user_id`) USING BTREE,
    INDEX `idx_hospital_id` (`follow_user_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '医院关注表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hospital_notice
-- ----------------------------
DROP TABLE IF EXISTS `hospital_notice`;
CREATE TABLE `hospital_notice`
(
    `id`           bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT '资讯编号',
    `hospital_id`  bigint(20)                                                    NOT NULL COMMENT '医院编号',
    `title`        varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '资讯标题',
    `content`      text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci         NOT NULL COMMENT '资讯内容',
    `picture`      varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配图URL',
    `gmt_create`   datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_hospital_id` (`hospital_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '医院健康资讯表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for payment_wallet
-- ----------------------------
DROP TABLE IF EXISTS `payment_wallet`;
CREATE TABLE `payment_wallet`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '钱包编号',
    `account_id`   bigint(20)  NOT NULL COMMENT '账号编号',
    `balance`      bigint(20)  NOT NULL DEFAULT 1000 COMMENT '余额（单位：分，默认1000分=10元）',
    `version`      int(11)     NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `gmt_create`   datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_account_id` (`account_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '账户钱包表（虚拟余额）'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for payment_order
-- ----------------------------
DROP TABLE IF EXISTS `payment_order`;
CREATE TABLE `payment_order`
(
    `id`             bigint(20)   NOT NULL COMMENT '支付订单编号（RedisIdWorker生成）',
    `payment_no`     varchar(64)  NOT NULL COMMENT '支付单号（PAY+时间戳+随机数）',
    `account_id`     bigint(20)   NOT NULL COMMENT '付款账号编号',
    `appointment_id` bigint(20)   DEFAULT NULL COMMENT '关联预约编号',
    `amount`         bigint(20)   NOT NULL COMMENT '支付金额（单位：分）',
    `status`         int(11)      NOT NULL DEFAULT 0 COMMENT '支付状态：0=待支付, 1=支付中, 2=支付成功, 3=支付失败, 4=已退款, 5=已过期',
    `pay_method`     varchar(32)  DEFAULT 'WALLET' COMMENT '支付方式：WALLET=钱包余额',
    `pay_time`       datetime(0)  DEFAULT NULL COMMENT '支付完成时间',
    `expire_time`    datetime(0)  NOT NULL COMMENT '支付过期时间（创建时间+15分钟）',
    `refund_amount`  bigint(20)   DEFAULT 0 COMMENT '退款金额（单位：分）',
    `refund_time`    datetime(0)  DEFAULT NULL COMMENT '退款时间',
    `refund_reason`  varchar(255) DEFAULT NULL COMMENT '退款原因',
    `gmt_create`     datetime(0)  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified`   datetime(0)  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_payment_no` (`payment_no`) USING BTREE,
    INDEX `idx_account_id` (`account_id`) USING BTREE,
    INDEX `idx_appointment_id` (`appointment_id`) USING BTREE,
    INDEX `idx_status_expire` (`status`, `expire_time`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '支付订单表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for payment_flow
-- ----------------------------
DROP TABLE IF EXISTS `payment_flow`;
CREATE TABLE `payment_flow`
(
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '流水编号',
    `payment_id`     bigint(20)   NOT NULL COMMENT '支付订单编号',
    `payment_no`     varchar(64)  NOT NULL COMMENT '支付单号（冗余方便查询）',
    `account_id`     bigint(20)   NOT NULL COMMENT '账号编号',
    `amount`         bigint(20)   NOT NULL COMMENT '变动金额（分，正数=扣款）',
    `balance_before` bigint(20)   NOT NULL COMMENT '变动前余额（分）',
    `balance_after`  bigint(20)   NOT NULL COMMENT '变动后余额（分）',
    `flow_type`      int(11)      NOT NULL COMMENT '流水类型：1=支付扣款, 2=退款入账, 3=过期退回',
    `flow_status`    int(11)      NOT NULL DEFAULT 1 COMMENT '流水状态：1=成功',
    `remark`         varchar(255) DEFAULT NULL COMMENT '备注',
    `gmt_create`     datetime(0)  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_payment_id` (`payment_id`) USING BTREE,
    INDEX `idx_account_id` (`account_id`) USING BTREE,
    INDEX `idx_gmt_create` (`gmt_create`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '支付流水表（交易明细）'
  ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `hospital`.`power_account`(`id`, `name`, `password`, `status`, `login_time`, `gmt_create`, `gmt_modified`)
VALUES (10000006, 'test', '$2a$10$im44.HgXDahyBbY2Wx2FA.ZSEmhDoycqYjMSzr.V8SuQkJlQR6XRS', 1, '2020-04-11 14:31:26',
        '2020-04-11 14:31:14', '2020-04-11 14:31:14');
INSERT INTO `hospital`.`power_account`(`id`, `name`, `password`, `status`, `login_time`, `gmt_create`, `gmt_modified`)
VALUES (10000001, 'doctor', '$2a$10$S.e7q.IBQtE3pqoZhfsRDexYFGwEXWxAhO4Wv09E9C6dUM2QX1Kvm', 1, '2020-04-19 15:37:08',
        '2020-03-27 02:46:21', '2020-03-29 03:04:56');
INSERT INTO `hospital`.`power_account`(`id`, `name`, `password`, `status`, `login_time`, `gmt_create`, `gmt_modified`)
VALUES (10000005, 'admin', '$2a$10$l0dSGURNp2Q9z2m3jea37.CV9.OH76qprHxIDklyJPK2EzEIg4m2W', 1, '2020-04-11 14:30:26',
        '2020-01-25 08:04:14', '2020-03-29 02:57:48');

INSERT INTO `hospital`.`hospital_info`(`id`, `name`, `phone`, `address`, `description`, `picture`, `gmt_create`,
                                       `gmt_modified`)
VALUES (1000, '广东省中医院', '020-123452', '广州大学城', '广东省中医院', '',
        '2020-02-05 13:06:55', '2020-03-16 09:57:16');
INSERT INTO `hospital`.`hospital_info`(`id`, `name`, `phone`, `address`, `description`, `picture`, `gmt_create`,
                                       `gmt_modified`)
VALUES (1001, '顺德分院', '020-1234567', '广东省佛山市顺德区', '位于广东省佛山市顺德区',
        '', '2020-03-16 08:00:03', '2020-03-16 08:00:03');
INSERT INTO `hospital`.`hospital_info`(`id`, `name`, `phone`, `address`, `description`, `picture`, `gmt_create`,
                                       `gmt_modified`)
VALUES (1002, '白云分院', '020-1234568', '广东省广州市白云区', '位于广东省广州市白云区',
        '', '2020-03-16 08:03:30', '2020-03-16 08:03:30');
INSERT INTO `hospital`.`hospital_info`(`id`, `name`, `phone`, `address`, `description`, `picture`, `gmt_create`,
                                       `gmt_modified`)
VALUES (1007, '越秀分院', '020-1234563', '广东省越秀区', '位于广东省越秀区', '',
        '2020-03-22 04:53:40', '2020-03-22 04:53:40');

INSERT INTO `hospital`.`hospital_special`(`id`, `name`, `description`, `gmt_create`, `gmt_modified`)
VALUES (10000, '妇科', '女性妇科相关智联', '2020-03-20 03:54:25', '2020-03-20 03:54:25');
INSERT INTO `hospital`.`hospital_special`(`id`, `name`, `description`, `gmt_create`, `gmt_modified`)
VALUES (10006, '皮肤科', '皮肤相关治疗', '2020-03-20 04:14:07', '2020-03-20 04:14:07');
INSERT INTO `hospital`.`hospital_special`(`id`, `name`, `description`, `gmt_create`, `gmt_modified`)
VALUES (10008, '儿科', '儿童相关治疗', '2020-03-20 04:17:00', '2020-03-20 04:17:00');
INSERT INTO `hospital`.`hospital_special`(`id`, `name`, `description`, `gmt_create`, `gmt_modified`)
VALUES (10009, '眼科', '眼部相关治疗', '2020-03-20 06:53:22', '2020-03-20 06:53:22');
INSERT INTO `hospital`.`hospital_special`(`id`, `name`, `description`, `gmt_create`, `gmt_modified`)
VALUES (10010, '内科', '甲状腺等内科治疗', '2020-03-20 06:55:01', '2020-03-20 06:55:01');
INSERT INTO `hospital`.`hospital_special`(`id`, `name`, `description`, `gmt_create`, `gmt_modified`)
VALUES (10011, '心血管内科', '心血管相关疾病治疗', '2020-03-20 06:55:02', '2020-03-20 06:55:02');

INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (4, 1000, 10008, '2020-03-20 08:38:59', '2020-03-20 08:38:59');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (5, 1000, 10000, '2020-03-20 09:01:10', '2020-03-20 09:01:10');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (6, 1001, 10000, '2020-03-20 09:04:41', '2020-03-20 09:04:41');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (8, 1000, 10006, '2020-03-29 04:42:18', '2020-03-29 04:42:18');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (9, 1001, 10009, '2020-03-29 04:43:33', '2020-03-29 04:43:33');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10, 1001, 10010, '2020-03-29 04:43:36', '2020-03-29 04:43:36');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (11, 1002, 10008, '2020-03-29 04:43:45', '2020-03-29 04:43:45');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (12, 1002, 10010, '2020-03-29 04:43:49', '2020-03-29 04:43:49');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (13, 1007, 10009, '2020-03-29 04:43:50', '2020-03-29 04:43:50');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (14, 1007, 10011, '2020-03-29 04:43:51', '2020-03-29 04:43:51');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (15, 1000, 10011, '2020-03-29 04:43:52', '2020-03-29 04:43:52');
INSERT INTO `hospital`.`hospital_special_relation`(`id`, `hospital_id`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (16, 1002, 10011, '2020-03-29 04:43:53', '2020-03-29 04:43:53');


INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10008, '周建华', 1, '主任医师', '甲状腺疾病、糖尿病、高血压等内科常见病', 10010, NOW(), NOW());
INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10009, '陈秀英', 2, '副主任医师', '冠心病、高血压、心律失常等心血管疾病', 10011, NOW(), NOW());
INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10010, '张丽', 2, '主任医师', '妇科常见病、多发病诊治', 10000, NOW(), NOW());
INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10011, '王芳', 2, '副主任医师', '妇科肿瘤、内分泌疾病', 10000, NOW(), NOW());
INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10012, '赵敏', 2, '主任医师', '皮肤病、性病诊治', 10006, NOW(), NOW());
INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10013, '刘洋', 1, '主治医师', '皮肤外科、美容皮肤科', 10006, NOW(), NOW());
INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10014, '李娜', 2, '主任医师', '小儿呼吸、消化系统疾病', 10008, NOW(), NOW());
INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10015, '陈明', 1, '主任医师', '白内障、青光眼等眼科疾病', 10009, NOW(), NOW());
INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10016, '周杰', 1, '副主任医师', '近视矫正、眼底疾病', 10009, NOW(), NOW());
INSERT INTO `hospital`.`hospital_doctor`(`id`, `name`, `gender`, `job_title`, `specialty`, `special_id`, `gmt_create`, `gmt_modified`)
VALUES (10017, '张伟', 1, '主任医师', '呼吸系统、消化系统等内科疾病', 10010, NOW(), NOW());
