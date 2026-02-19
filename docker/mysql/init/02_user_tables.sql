-- ============================================================
-- User服务数据库表结构
-- 描述: User服务所有表的完整结构
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS kanban_user
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE kanban_user;

-- ============================================================
-- 1. sys_user 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id VARCHAR(64) NOT NULL COMMENT '用户ID（雪花算法）',
    email VARCHAR(255) NOT NULL COMMENT '邮箱（登录账号）',
    password_hash VARCHAR(255) NULL COMMENT '密码哈希（待激活用户为空）',
    nickname VARCHAR(100) NOT NULL COMMENT '昵称',
    avatar VARCHAR(500) NULL COMMENT '头像URL',
    phone VARCHAR(20) NULL COMMENT '手机号',

    super_admin TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否超级管理员',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_ACTIVATION' COMMENT '状态(PENDING_ACTIVATION/ACTIVE/DISABLED/LOCKED)',

    activation_code VARCHAR(64) NULL COMMENT '激活码（首次设置密码用）',
    activation_expires_at DATETIME NULL COMMENT '激活码过期时间',

    last_login_at DATETIME NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) NULL COMMENT '最后登录IP',
    login_fail_count INT NOT NULL DEFAULT 0 COMMENT '登录失败次数',

    using_default_password TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用默认密码（邀请成员/超管初始时标记）',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '软删除时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email),
    INDEX idx_status (status),
    INDEX idx_super_admin (super_admin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. sys_organization 组织表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_organization (
    id VARCHAR(64) NOT NULL COMMENT '组织ID（同时作为orgId）',
    name VARCHAR(200) NOT NULL COMMENT '组织名称',
    description VARCHAR(1000) NULL COMMENT '组织描述',
    logo VARCHAR(500) NULL COMMENT '组织Logo URL',

    member_card_type_id VARCHAR(64) NULL COMMENT '成员卡片类型ID',
    attendance_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '考勤功能是否启用',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/DISABLED/DELETED)',

    created_by VARCHAR(64) NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '软删除时间',

    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织表';

-- ============================================================
-- 3. sys_user_organization 用户-组织关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_organization (
    id VARCHAR(64) NOT NULL COMMENT '关系ID（雪花算法）',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    member_card_id VARCHAR(64) NULL COMMENT '成员卡片ID',

    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '角色(OWNER/ADMIN/MEMBER)',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/DISABLED)',

    invited_by VARCHAR(64) NULL COMMENT '邀请人ID',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_org (user_id, org_id),
    INDEX idx_org_id (org_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-组织关系表';

-- ============================================================
-- 4. sys_refresh_token 刷新令牌表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_refresh_token (
    id VARCHAR(64) NOT NULL COMMENT '令牌ID（雪花算法）',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    token_hash VARCHAR(255) NOT NULL COMMENT '令牌哈希值',
    device_info VARCHAR(500) NULL COMMENT '设备信息',

    org_id VARCHAR(64) NULL COMMENT '组织ID（可空，仅当用户已切换到某个组织时有值）',
    member_card_id VARCHAR(64) NULL COMMENT '成员卡片ID（可空，仅当用户已切换到某个组织时有值）',
    role VARCHAR(20) NULL COMMENT '用户在组织中的角色（可空）',

    expires_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    revoked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已撤销',

    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_token_hash (token_hash),
    INDEX idx_expires_at (expires_at),
    INDEX idx_org_id (org_id),
    INDEX idx_member_card_id (member_card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌表';
