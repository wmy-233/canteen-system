-- ============================================================
-- 智慧校园食堂点餐系统 数据库初始化脚本 v0.1
-- 引擎统一 InnoDB，字符集 utf8mb4
-- 执行方式：内部使用 Navicat/DataGrip 执行本文件即可（schema.sql + init_data.sql）
-- ============================================================

CREATE DATABASE IF NOT EXISTS canteen DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE canteen;
-- 防止执行工具字符集不是 utf8mb4 时中文被双重编码污染（如 docker-entrypoint 默认 latin1 客户端）
SET NAMES utf8mb4;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname`    VARCHAR(50)  COMMENT '昵称',
    `phone`       VARCHAR(20)  COMMENT '手机号',
    `avatar`      VARCHAR(255) COMMENT '头像URL',
    `balance`     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '钱包余额',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1正常',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户表';

-- 角色表（RBAC）
CREATE TABLE IF NOT EXISTS `role` (
    `id`        BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_code` VARCHAR(30)  NOT NULL UNIQUE COMMENT 'ADMIN/MERCHANT/STUDENT',
    `role_name` VARCHAR(50)  NOT NULL COMMENT '角色名称'
) ENGINE=InnoDB COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `user_role` (
    `id`      BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB COMMENT='用户角色关联';

-- 权限/菜单资源表
CREATE TABLE IF NOT EXISTS `menu` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `menu_code`   VARCHAR(50)  NOT NULL UNIQUE COMMENT '权限编码 如 dish:add',
    `menu_name`   VARCHAR(50)  NOT NULL COMMENT '权限名称',
    `path`        VARCHAR(100) COMMENT '前端路由',
    `parent_id`   BIGINT       DEFAULT 0 COMMENT '父级菜单',
    `sort`        INT          DEFAULT 0
) ENGINE=InnoDB COMMENT='权限菜单表';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS `role_menu` (
    `id`      BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`)
) ENGINE=InnoDB COMMENT='角色权限关联';

-- 食堂表
CREATE TABLE IF NOT EXISTS `canteen` (
    `id`       BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name`     VARCHAR(50) NOT NULL COMMENT '食堂名称',
    `location` VARCHAR(100) COMMENT '位置'
) ENGINE=InnoDB COMMENT='食堂表';

-- 档口表
CREATE TABLE IF NOT EXISTS `stall` (
    `id`              BIGINT PRIMARY KEY AUTO_INCREMENT,
    `canteen_id`      BIGINT NOT NULL COMMENT '所属食堂',
    `merchant_user_id` BIGINT NOT NULL COMMENT '商家账号(外键user)',
    `name`            VARCHAR(50) NOT NULL COMMENT '档口名称',
    `status`          TINYINT NOT NULL DEFAULT 2 COMMENT '0停业 1营业 2待审核',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='档口表';

-- 菜品分类
CREATE TABLE IF NOT EXISTS `dish_category` (
    `id`   BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(30) NOT NULL COMMENT '分类名称',
    `sort` INT DEFAULT 0
) ENGINE=InnoDB COMMENT='菜品分类';

-- 菜品表
CREATE TABLE IF NOT EXISTS `dish` (
    `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
    `stall_id`     BIGINT NOT NULL COMMENT '所属档口',
    `category_id`  BIGINT NOT NULL COMMENT '分类',
    `name`         VARCHAR(50) NOT NULL COMMENT '菜品名称',
    `description`  VARCHAR(255) COMMENT '描述',
    `price`        DECIMAL(8,2) NOT NULL COMMENT '价格',
    `image`        VARCHAR(255) COMMENT '图片URL',
    `daily_stock`  INT NOT NULL DEFAULT 0 COMMENT '每日限量数(0不限量)',
    `sale_count`   INT NOT NULL DEFAULT 0 COMMENT '累计销量',
    `status`       TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='菜品表';

-- 购物车
CREATE TABLE IF NOT EXISTS `cart_item` (
    `id`      BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `dish_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_dish` (`user_id`,`dish_id`)
) ENGINE=InnoDB COMMENT='购物车';

-- 订单主表
CREATE TABLE IF NOT EXISTS `order_info` (
    `id`              BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_no`        VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    `user_id`         BIGINT NOT NULL,
    `stall_id`        BIGINT NOT NULL COMMENT '档口',
    `total_amount`    DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    `pay_amount`      DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `pickup_code`     VARCHAR(8) NOT NULL COMMENT '取餐码',
    `status`          TINYINT NOT NULL DEFAULT 0 COMMENT '0待取餐 1已完成 2已取消',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user` (`user_id`),
    KEY `idx_stall_status` (`stall_id`,`status`)
) ENGINE=InnoDB COMMENT='订单主表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS `order_item` (
    `id`        BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id`  BIGINT NOT NULL,
    `dish_id`   BIGINT NOT NULL,
    `dish_name` VARCHAR(50) NOT NULL COMMENT '菜名快照',
    `price`     DECIMAL(8,2) NOT NULL COMMENT '价格快照',
    `quantity`  INT NOT NULL DEFAULT 1
) ENGINE=InnoDB COMMENT='订单明细';

-- 优惠券
CREATE TABLE IF NOT EXISTS `coupon` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name`        VARCHAR(50) NOT NULL,
    `threshold`   DECIMAL(8,2) NOT NULL COMMENT '满减门槛',
    `amount`      DECIMAL(8,2) NOT NULL COMMENT '减免金额',
    `stock`       INT NOT NULL DEFAULT 0 COMMENT '发放总量',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '0停发 1发放中',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='优惠券';

-- 用户领券
CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id`        BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`   BIGINT NOT NULL,
    `coupon_id` BIGINT NOT NULL,
    `used`      TINYINT NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户领券';

-- 评价表
CREATE TABLE IF NOT EXISTS `review` (
    `id`         BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id`   BIGINT NOT NULL,
    `user_id`    BIGINT NOT NULL,
    `dish_id`    BIGINT NOT NULL COMMENT '被评价的菜品',
    `score`      TINYINT NOT NULL COMMENT '评分1-5',
    `content`    VARCHAR(255) COMMENT '评价内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='评价表';

-- 钱包流水
CREATE TABLE IF NOT EXISTS `wallet_record` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL,
    `type`        TINYINT NOT NULL COMMENT '1充值 2消费 3退款',
    `amount`      DECIMAL(10,2) NOT NULL,
    `ref_order_no` VARCHAR(32) COMMENT '关联订单号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='钱包流水';

-- ============================================================
-- 初始演示数据（答辩演示用三角色账号，密码统一 123456 的 BCrypt，需注意替换）
-- ============================================================
INSERT INTO `role` (`role_code`,`role_name`) VALUES ('ADMIN','管理员'),('MERCHANT','档口商家'),('STUDENT','学生');
-- 演示账号/食堂档口/菜品/权限/优惠券等初始数据见 sql/init_data.sql（组员 C 维护，可反复重置）
