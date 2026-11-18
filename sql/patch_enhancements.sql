-- ============================================================
-- 高分增强补丁：精确退款回滚 / 批量下单 / 投诉超时升级 / 操作日志落库
-- 执行：docker exec -i canteen-mysql mysql -uroot -p123456 canteen < sql/patch_enhancements.sql
-- 可重复执行（先判列再 ALTER）
-- ============================================================

-- 1) 订单：记录使用的券记录ID + 批次号（批量下单），用于精确回滚与追踪
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'user_coupon_id');
SET @sql := IF(@col = 0,
    'ALTER TABLE `order_info` ADD COLUMN `user_coupon_id` BIGINT NULL COMMENT ''下单核销的 user_coupon 记录ID（精确退券）'' AFTER `pay_amount`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'batch_no');
SET @sql := IF(@col = 0,
    'ALTER TABLE `order_info` ADD COLUMN `batch_no` VARCHAR(64) NULL COMMENT ''批量下单批次号（跨档口原子下单）'' AFTER `user_coupon_id`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 订单明细：标记下单时该菜是否限量（0不限量→不回滚库存；1限量→退款补库存）
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item' AND COLUMN_NAME = 'limited');
SET @sql := IF(@col = 0,
    'ALTER TABLE `order_item` ADD COLUMN `limited` TINYINT NOT NULL DEFAULT 0 COMMENT ''下单时是否限量菜（决定退款是否补库存）'' AFTER `quantity`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 投诉：超时升级标记
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaint' AND COLUMN_NAME = 'escalated');
SET @sql := IF(@col = 0,
    'ALTER TABLE `complaint` ADD COLUMN `escalated` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否因商家超时未回应升级给管理员'' AFTER `status`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3b) 菜品：独立标记是否限量（区分"不限量"与"限量菜已售完"）
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish' AND COLUMN_NAME = 'is_limited');
SET @sql := IF(@col = 0,
    'ALTER TABLE `dish` ADD COLUMN `is_limited` TINYINT NOT NULL DEFAULT 0 COMMENT ''1限量菜 0不限量'' AFTER `daily_stock`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE dish SET is_limited = 1 WHERE daily_stock > 0 AND is_limited = 0;

-- 3c) 配送到寝室的跑腿费（担保交易：下单时收取，送达后结算给配送员）
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'delivery_fee');
SET @sql := IF(@col = 0,
    'ALTER TABLE `order_info` ADD COLUMN `delivery_fee` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT ''跑腿费(担保交易,送达后结算配送员)'' AFTER `pay_amount`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3c-2) 校内送达地点（宿舍楼/图书馆/教学楼等，配送单填写）
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'delivery_location');
SET @sql := IF(@col = 0,
    'ALTER TABLE `order_info` ADD COLUMN `delivery_location` VARCHAR(100) NULL COMMENT ''校内送达地点(宿舍楼/图书馆/教学楼等)'' AFTER `delivery_status`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3d) 配送单没有取餐码，pickup_code 必须允许 NULL（原 schema 写成 NOT NULL 是历史遗留 bug）
ALTER TABLE `order_info` MODIFY COLUMN `pickup_code` VARCHAR(8) NULL COMMENT '取餐码(自取单;配送单为NULL)';

-- 4) 操作日志落库表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `operator`    VARCHAR(64)  NOT NULL DEFAULT '匿名' COMMENT '操作人账号',
    `module`      VARCHAR(64)  NOT NULL COMMENT '业务模块',
    `action`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '操作名',
    `method`      VARCHAR(200) NOT NULL DEFAULT '' COMMENT '方法签名',
    `params`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '参数（截断）',
    `cost_ms`     INT NOT NULL DEFAULT 0 COMMENT '耗时毫秒',
    `success`     TINYINT NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    `error`       VARCHAR(300) NOT NULL DEFAULT '',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_oplog_time` (`create_time`),
    KEY `idx_oplog_operator` (`operator`)
) ENGINE=InnoDB COMMENT='AOP 操作日志（可追溯）';
