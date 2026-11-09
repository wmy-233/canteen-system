-- ============================================================
-- 骑手模块数据库补丁 patch_rider.sql（组长 A 于阶段 3 生成）
-- 依赖：sql/schema.sql 与 sql/init_data.sql 已执行
-- 作用：A11 骑手管理功能的表结构与基础数据
--  ⊕ rider 表 / ⊕ order_info 配送三列 / ⊕ RIDER 角色与权限点 / 演示骑手与待分配配送单
-- 注意：
--   1. ALTER TABLE 不防重复：重复执行会报 Duplicate column，忽略即可（本补丁一次性）
--   2. 组员 C 需把本文件合入 schema.sql / init_data.sql（组规：结构变更须同步），
--      并把挂载加入 docker-compose.yml（如 03-patch_rider.sql）
-- 演示数据：骑手 rider1（在岗）/ rider2（停用），密码 123456；2 笔待分配配送单挂在学生账号下
-- ============================================================
USE canteen;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. rider 表：骑手档案（管理员端维护）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rider` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL COMMENT '绑定的登录账号(user表)',
    `name`        VARCHAR(30) NOT NULL COMMENT '骑手姓名',
    `phone`       VARCHAR(20) COMMENT '手机号',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1在岗',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='骑手表';

-- 2. order_info 增加配送三列（默认值 0 保证存量自取单不受影响）
ALTER TABLE order_info
    ADD COLUMN delivery_type TINYINT NOT NULL DEFAULT 0 COMMENT '0自取 1配送' AFTER pickup_code,
    ADD COLUMN rider_id      BIGINT NULL COMMENT '配送骑手(自取单为NULL)' AFTER delivery_type,
    ADD COLUMN delivery_status TINYINT NULL COMMENT '0未分配 1配送中 2已送达(自取单为NULL)' AFTER rider_id;

-- 3. RIDER 角色（幂等：存在则跳过）
INSERT INTO `role` (`role_code`, `role_name`)
SELECT 'RIDER', '骑手'
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE role_code = 'RIDER');

-- 4. 权限点 + 仅授予 ADMIN（骑手功能在管理端）
INSERT IGNORE INTO menu (menu_code, menu_name, sort) VALUES ('rider:list', '骑手查询', 11);
INSERT IGNORE INTO menu (menu_code, menu_name, sort) VALUES ('rider:manage', '配送管理', 12);
INSERT INTO role_menu (role_id, menu_id)
SELECT r.id, m.id FROM `role` r JOIN menu m
  ON m.menu_code IN ('rider:list', 'rider:manage')
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- ------------------------------------------------------------
-- 5. 演示数据：骑手账号（密码 123456，BCrypt 密文与 init_data.sql 同源）
-- ------------------------------------------------------------
INSERT INTO `user` (username, password, nickname, phone, balance, status) VALUES
('rider1', '$2a$10$VELqKXzIE2tQToLUP28l3egX1kE.wBHM.c/JMK431GaaYMnHNL722', '骑手王小跑', '13900000001', 0.00, 1),
('rider2', '$2a$10$VELqKXzIE2tQToLUP28l3egX1kE.wBHM.c/JMK431GaaYMnHNL722', '骑手李慢递', '13900000002', 0.00, 0);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u JOIN `role` r
  ON r.role_code = 'RIDER'
WHERE u.username IN ('rider1', 'rider2')
  AND NOT EXISTS (
    SELECT 1 FROM user_role ur2 WHERE ur2.user_id = u.id AND ur2.role_id = r.id
  );

INSERT INTO rider (user_id, name, phone, status)
SELECT u.id, u.nickname, u.phone, 1 FROM `user` u
WHERE u.username = 'rider1'
  AND NOT EXISTS (SELECT 1 FROM rider r WHERE r.user_id = u.id);

INSERT INTO rider (user_id, name, phone, status)
SELECT u.id, u.nickname, u.phone, 0 FROM `user` u
WHERE u.username = 'rider2'
  AND NOT EXISTS (SELECT 1 FROM rider r WHERE r.user_id = u.id);

-- ------------------------------------------------------------
-- 6. 演示数据：2 笔“待分配”配送单（学生账号自下单，便于 B9 页面与答辩演示）
--    学生余额 50.00 扣两单共 26.00；钱包流水同步留痕
-- ------------------------------------------------------------
INSERT INTO order_info (order_no, user_id, stall_id, total_amount, discount_amount, pay_amount, pickup_code, status, delivery_type, rider_id, delivery_status)
VALUES ('CT20261109090001', (SELECT id FROM `user` WHERE username='student'), 2,
        13.00, 0.00, 13.00, '7777', 0, 1, NULL, 0);
SET @order1 = LAST_INSERT_ID();

INSERT INTO order_item (order_id, dish_id, dish_name, price, quantity)
VALUES (@order1, 8, '牛肉拉面', 13.00, 1);

INSERT INTO order_info (order_no, user_id, stall_id, total_amount, discount_amount, pay_amount, pickup_code, status, delivery_type, rider_id, delivery_status)
VALUES ('CT20261109090002', (SELECT id FROM `user` WHERE username='student'), 1,
        14.00, 0.00, 14.00, '7778', 0, 1, NULL, 0);
SET @order2 = LAST_INSERT_ID();

INSERT INTO order_item (order_id, dish_id, dish_name, price, quantity)
VALUES (@order2, 17, '蛋炒饭', 14.00, 1);

INSERT INTO wallet_record (user_id, type, amount, ref_order_no)
SELECT u.id, 2, 13.00, 'CT20261109090001' FROM `user` u WHERE u.username = 'student';
INSERT INTO wallet_record (user_id, type, amount, ref_order_no)
SELECT u.id, 2, 14.00, 'CT20261109090002' FROM `user` u WHERE u.username = 'student';

UPDATE `user` SET balance = balance - 27.00 WHERE username = 'student';
