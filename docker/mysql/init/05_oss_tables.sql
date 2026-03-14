-- ============================================================
-- OSS服务数据库表结构
-- 描述: OSS服务所有表的完整结构
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS kanban_oss
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE kanban_oss;

-- ============================================================
-- 1. sys_file_meta 文件元数据表
-- ============================================================
-- 记录上传文件的元数据信息
CREATE TABLE IF NOT EXISTS sys_file_meta (
    id VARCHAR(36) PRIMARY KEY COMMENT '文件ID',
    org_id VARCHAR(36) NOT NULL COMMENT '组织ID',
    operator_id VARCHAR(36) NOT NULL COMMENT '操作者ID（当前用户在当前组织对应的成员卡ID，即当前成员ID，不是用户ID）',
    category VARCHAR(32) NOT NULL COMMENT '文件类别: ATTACHMENT, AVATAR, ORG_LOGO, COMMENT_IMAGE, DESCRIPTION_IMAGE',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    object_key VARCHAR(512) NOT NULL COMMENT '对象存储Key',
    url VARCHAR(1024) NOT NULL COMMENT '访问URL',
    size BIGINT NOT NULL COMMENT '文件大小(字节)',
    content_type VARCHAR(128) COMMENT '内容类型',
    storage_plugin VARCHAR(32) NOT NULL COMMENT '存储插件ID: local, minio, aliyun, aws-s3',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_org_id (org_id),
    INDEX idx_category (category),
    INDEX idx_operator_id (operator_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件元数据表';
