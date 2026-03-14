-- ============================================================
-- Extension服务数据库表结构
-- 描述: Extension服务所有表的完整结构
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS kanban_extension
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE kanban_extension;

-- ============================================================
-- 1. card_history_meta 卡片历史表元数据
-- ============================================================
-- 记录每个卡片类型对应的历史表名，用于动态分表
CREATE TABLE IF NOT EXISTS card_history_meta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    card_type_id VARCHAR(64) NOT NULL COMMENT '卡片类型ID',
    table_name VARCHAR(100) NOT NULL COMMENT '历史表名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_card_type_id (card_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡片历史表元数据';




-- 2. 评论主表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT PRIMARY KEY COMMENT '评论ID',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    card_id VARCHAR(64) NOT NULL COMMENT '卡片ID',
    card_type_id VARCHAR(64) NOT NULL COMMENT '卡片类型ID',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论ID（用于回复）',
    root_id BIGINT DEFAULT NULL COMMENT '根评论ID（用于回复链）',
    reply_to_member_id VARCHAR(64) DEFAULT NULL COMMENT '回复的成员ID',
    content TEXT NOT NULL COMMENT '评论内容（Markdown格式）',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/WITHDRAWN/DELETED',
    edit_count INT NOT NULL DEFAULT 0 COMMENT '编辑次数',
    last_edited_at DATETIME DEFAULT NULL COMMENT '最后编辑时间',
    author_id VARCHAR(64) NOT NULL COMMENT '作者成员ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_org_card (org_id, card_id),
    INDEX idx_card_created (card_id, created_at),
    INDEX idx_parent (parent_id),
    INDEX idx_author (org_id, author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 3. @提及表
CREATE TABLE IF NOT EXISTS comment_mention (
    id BIGINT PRIMARY KEY COMMENT '提及ID',
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    mentioned_member_id VARCHAR(64) NOT NULL COMMENT '被提及的成员ID',
    start_offset INT NOT NULL COMMENT '在评论内容中的起始位置',
    end_offset INT NOT NULL COMMENT '在评论内容中的结束位置',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_comment (comment_id),
    INDEX idx_mentioned (org_id, mentioned_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论@提及表';

-- 4. #卡片引用表
CREATE TABLE IF NOT EXISTS comment_card_ref (
    id BIGINT PRIMARY KEY COMMENT '引用ID',
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    ref_card_id VARCHAR(64) NOT NULL COMMENT '引用的卡片ID',
    ref_card_code VARCHAR(50) NOT NULL COMMENT '引用的卡片编号',
    start_offset INT NOT NULL COMMENT '在评论内容中的起始位置',
    end_offset INT NOT NULL COMMENT '在评论内容中的结束位置',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_comment (comment_id),
    INDEX idx_ref_card (org_id, ref_card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论#卡片引用表';
