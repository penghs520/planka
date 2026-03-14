-- ============================================================
-- Notification服务数据库表结构
-- 描述: Notification服务所有表的完整结构
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS kanban_notification
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE kanban_notification;

-- ============================================================
-- 1. system_notification 系统通知表（站内信）
-- ============================================================
CREATE TABLE IF NOT EXISTS system_notification (
    id VARCHAR(32) PRIMARY KEY COMMENT '通知ID',
    org_id VARCHAR(32) NOT NULL COMMENT '组织ID',
    user_id VARCHAR(32) NOT NULL COMMENT '接收者用户ID',
    title VARCHAR(500) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '纯文本内容',
    rich_content TEXT COMMENT '富文本内容',
    source_type VARCHAR(50) COMMENT '来源类型: RULE, MANUAL',
    source_id VARCHAR(32) COMMENT '来源ID（规则ID等）',
    card_id VARCHAR(32) COMMENT '关联卡片ID',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    read_at DATETIME COMMENT '阅读时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_user_unread (user_id, is_read, created_at),
    INDEX idx_org_created (org_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- ============================================================
-- 2. notification_record 通知发送记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_record (
    id VARCHAR(32) PRIMARY KEY COMMENT '记录ID',
    org_id VARCHAR(32) NOT NULL COMMENT '组织ID',
    channel_id VARCHAR(50) NOT NULL COMMENT '渠道ID',
    template_id VARCHAR(32) COMMENT '模板ID',
    rule_id VARCHAR(32) COMMENT '规则ID',
    card_id VARCHAR(32) COMMENT '触发卡片ID',
    title VARCHAR(500) COMMENT '通知标题',
    recipient_count INT COMMENT '接收者数量',
    success_count INT COMMENT '成功数量',
    status VARCHAR(20) COMMENT '状态: PENDING, SUCCESS, PARTIAL, FAILED',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_org_created (org_id, created_at),
    INDEX idx_channel_created (channel_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知发送记录表';
