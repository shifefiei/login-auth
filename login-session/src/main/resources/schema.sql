-- login-jwt 与 login-session 共用同一个数据库、同一张 user 表
-- MyBatis Plus 不会自动建表，请先手动执行本脚本

CREATE DATABASE IF NOT EXISTS login_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE login_auth;

CREATE TABLE IF NOT EXISTS user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号（敏感信息）',
    email       VARCHAR(128) DEFAULT NULL COMMENT '邮箱（敏感信息）',
    created_at  DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户表';
