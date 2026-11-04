-- ============================================================
-- 初始演示数据 init_data.sql v0.1（组员 C 维护）
-- 用途：本地联调与答辩演示；可反复执行重置（先清空再插入）
-- 依赖：先执行 schema.sql（建库建表 + role 三角色：ADMIN=1 MERCHANT=2 STUDENT=3）
-- 演示账号密码统一 123456（BCrypt 密文，已用项目同版本
--   spring-security-crypto 5.7.11 BCryptPasswordEncoder 验证 matches 通过）
-- ============================================================
USE canteen;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 0. 清空旧数据（可反复重置；role 表由 schema.sql 管理，此处不删）
-- 注意：会清掉此前通过注册接口创建的用户，重置后需重新注册
-- ------------------------------------------------------------
TRUNCATE TABLE review;
TRUNCATE TABLE wallet_record;
TRUNCATE TABLE user_coupon;
TRUNCATE TABLE coupon;
TRUNCATE TABLE order_item;
TRUNCATE TABLE order_info;
TRUNCATE TABLE cart_item;
TRUNCATE TABLE dish;
TRUNCATE TABLE dish_category;
TRUNCATE TABLE stall;
TRUNCATE TABLE canteen;
TRUNCATE TABLE role_menu;
TRUNCATE TABLE menu;
TRUNCATE TABLE user_role;
TRUNCATE TABLE `user`;

-- ------------------------------------------------------------
-- 1. 演示账号（密码统一 123456）
--    admin     管理员    -> ADMIN
--    merchant  川湘档口  -> MERCHANT（经营档口 1 川湘家常菜）
--    merchant2 面食档口  -> MERCHANT（经营档口 2 兰州拉面）
--    student   张同学    -> STUDENT（预置 50 元余额）
-- ------------------------------------------------------------
INSERT INTO `user` (id, username, password, nickname, phone, balance, status) VALUES
(1, 'admin',     '$2a$10$VELqKXzIE2tQToLUP28l3egX1kE.wBHM.c/JMK431GaaYMnHNL722', '系统管理员',   '13800000001', 0.00,  1),
(2, 'merchant',  '$2a$10$VELqKXzIE2tQToLUP28l3egX1kE.wBHM.c/JMK431GaaYMnHNL722', '川湘档口老板', '13800000002', 0.00,  1),
(3, 'merchant2', '$2a$10$VELqKXzIE2tQToLUP28l3egX1kE.wBHM.c/JMK431GaaYMnHNL722', '面食档口老板', '13800000003', 0.00,  1),
(4, 'student',   '$2a$10$VELqKXzIE2tQToLUP28l3egX1kE.wBHM.c/JMK431GaaYMnHNL722', '张同学',       '13800000004', 50.00, 1);

INSERT INTO user_role (user_id, role_id) VALUES
(1, (SELECT id FROM `role` WHERE role_code = 'ADMIN')),
(2, (SELECT id FROM `role` WHERE role_code = 'MERCHANT')),
(3, (SELECT id FROM `role` WHERE role_code = 'MERCHANT')),
(4, (SELECT id FROM `role` WHERE role_code = 'STUDENT'));

-- ------------------------------------------------------------
-- 2. 食堂 / 档口
--    档口 3 状态=2 待审核：用于演示管理员"档口审核"流程
-- ------------------------------------------------------------
INSERT INTO canteen (id, name, location) VALUES
(1, '第一食堂', '教学楼 A 区北侧'),
(2, '第二食堂', '宿舍区南门');

INSERT INTO stall (id, canteen_id, merchant_user_id, name, status) VALUES
(1, 1, 2, '川湘家常菜', 1),  -- 营业
(2, 1, 3, '兰州拉面',   1),  -- 营业
(3, 2, 2, '麻辣香锅',   2);  -- 待审核（演示用）

-- ------------------------------------------------------------
-- 3. 菜品分类 + 20 道演示菜品
--    daily_stock > 0 为限量菜（防超卖演示）；sale_count 有梯度（热销榜演示）
-- ------------------------------------------------------------
INSERT INTO dish_category (id, name, sort) VALUES
(1, '荤菜',     1),
(2, '素菜',     2),
(3, '主食',     3),
(4, '汤粉面',   4),
(5, '小吃饮品', 5);

INSERT INTO dish (id, stall_id, category_id, name, description, price, daily_stock, sale_count, status) VALUES
(1,  1, 1, '宫保鸡丁',     '微辣，花生脆',     12.00, 0,  326, 1),
(2,  1, 1, '辣椒炒肉',     '下饭神器',         11.00, 0,  254, 1),
(3,  1, 1, '红烧排骨',     '每日限量',         16.00, 30, 189, 1),
(4,  1, 2, '干煸四季豆',   '—',                7.00,  0,  121, 1),
(5,  1, 2, '麻婆豆腐',     '微辣',             8.00,  0,  98,  1),
(6,  1, 3, '米饭',         '管饱',             2.00,  0,  500, 1),
(7,  1, 5, '酸梅汤',       '解辣',             4.00,  50, 66,  1),
(8,  2, 4, '牛肉拉面',     '汤浓面筋道',       13.00, 0,  310, 1),
(9,  2, 4, '羊肉泡馍',     '限量 20 份',       18.00, 20, 45,  1),
(10, 2, 3, '炒刀削',       '—',                12.00, 0,  143, 1),
(11, 2, 2, '凉拌黄瓜',     '—',                5.00,  0,  77,  1),
(12, 2, 5, '烤肠',         '—',                3.00,  0,  210, 1),
(13, 3, 1, '麻辣香锅(荤)', '自选两荤',         20.00, 25, 88,  1),
(14, 3, 2, '麻辣香锅(素)', '自选三素',         14.00, 40, 52,  1),
(15, 3, 5, '冰粉',         '—',                5.00,  0,  95,  1),
(16, 1, 1, '糖醋里脊',     '酸甜口',           14.00, 0,  176, 1),
(17, 1, 3, '蛋炒饭',       '—',                9.00,  0,  231, 1),
(18, 2, 4, '酸汤水饺',     '—',                12.00, 60, 64,  1),
(19, 2, 1, '卤牛肉',       '每日限量',         22.00, 15, 30,  1),
(20, 3, 5, '柠檬水',       '—',                4.00,  0,  158, 1);

-- ------------------------------------------------------------
-- 4. 权限菜单 + 角色权限（RBAC 证据：管理员全量，商家限业务权限，学生端"登录即可"）
-- ------------------------------------------------------------
INSERT INTO menu (id, menu_code, menu_name, sort) VALUES
(1,  'user:list',     '用户查询',   1),
(2,  'user:ban',      '用户禁用',   2),
(3,  'stall:audit',   '档口审核',   3),
(4,  'dish:add',      '菜品新增',   4),
(5,  'dish:update',   '菜品修改',   5),
(6,  'dish:delete',   '菜品删除',   6),
(7,  'order:list',    '订单查询',   7),
(8,  'order:deliver', '订单出餐',   8),
(9,  'stats:view',    '统计报表',   9),
(10, 'coupon:grant',  '优惠券发放', 10);

-- ADMIN：全部权限
INSERT INTO role_menu (role_id, menu_id)
SELECT r.id, m.id FROM `role` r CROSS JOIN menu m WHERE r.role_code = 'ADMIN';

-- MERCHANT：仅档口经营相关权限
INSERT INTO role_menu (role_id, menu_id)
SELECT r.id, m.id FROM `role` r JOIN menu m
  ON m.menu_code IN ('dish:add', 'dish:update', 'dish:delete', 'order:list', 'order:deliver', 'stats:view')
WHERE r.role_code = 'MERCHANT';

-- ------------------------------------------------------------
-- 5. 优惠券 + 学生预置一张未使用券（学生端演示用）
-- ------------------------------------------------------------
INSERT INTO coupon (id, name, threshold, amount, stock, expire_time, status) VALUES
(1, '满10减2', 10.00, 2.00, 100, '2026-12-31 23:59:59', 1),
(2, '满20减5', 20.00, 5.00, 50,  '2026-12-31 23:59:59', 1);

INSERT INTO user_coupon (user_id, coupon_id, used) VALUES (4, 1, 0);
