-- ============================================================
-- 站内消息 + 投诉工单模块 数据库补丁 patch_message_complaint.sql
-- 依赖：sql/schema.sql + init_data.sql (+ patch_rider.sql) 已执行
-- 作用：
--   1. sys_message 站内消息表（三端铃铛消息中心）
--   2. complaint 投诉工单表（学生投诉→商家回应→管理员裁定）
-- 注意：组员 C 需把本文件合入 schema.sql（组规：结构变更须同步）
-- ============================================================
USE canteen;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. sys_message：站内消息（铃铛）
--   type: 0系统 1订单 2入驻 3投诉 4配送
--   is_read: 0未读 1已读
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_message` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL COMMENT '接收人 user.id',
    `title`       VARCHAR(60)  NOT NULL COMMENT '消息标题',
    `content`     VARCHAR(300) NOT NULL COMMENT '消息正文',
    `type`        TINYINT NOT NULL DEFAULT 0 COMMENT '0系统 1订单 2入驻 3投诉 4配送',
    `is_read`     TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_msg_user` (`user_id`, `is_read`)
) ENGINE=InnoDB COMMENT='站内消息';

-- ------------------------------------------------------------
-- 2. complaint：投诉工单
--   状态机：0待处理 →(商家回应)→ 1已回应 →(管理员裁定)→ 2已裁定
--   result: 1投诉成立 0投诉不成立（管理员裁定时写入）
--   refund_amount: 裁定成立时的退款金额（>0 时走钱包退给学生，写退款流水）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `complaint` (
    `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id`       BIGINT NOT NULL COMMENT '关联订单 order_info.id',
    `student_user_id` BIGINT NOT NULL COMMENT '投诉人',
    `stall_id`       BIGINT NOT NULL COMMENT '被投诉档口',
    `type`           TINYINT NOT NULL COMMENT '1菜品质量 2漏发缺件 3配送问题 4服务态度',
    `content`        VARCHAR(300) NOT NULL COMMENT '投诉内容',
    `status`         TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1商家已回应 2已裁定',
    `reply`          VARCHAR(300) COMMENT '商家回应说明',
    `reply_time`     DATETIME COMMENT '回应时间',
    `verdict`        VARCHAR(300) COMMENT '管理员裁定说明',
    `result`         TINYINT COMMENT '1成立 0不成立',
    `refund_amount`  DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '裁定退款金额',
    `admin_id`       BIGINT COMMENT '裁定管理员',
    `create_time`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_cmpl_order` (`order_id`),
    KEY `idx_cmpl_stall` (`stall_id`, `status`)
) ENGINE=InnoDB COMMENT='投诉工单';
