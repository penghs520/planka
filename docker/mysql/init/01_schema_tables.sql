-- ============================================================
-- Schema服务数据库表结构
-- 描述: Schema服务所有表的完整结构
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS kanban_schema
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE kanban_schema;

-- ============================================================
-- 1. schema_definition 主表
-- ============================================================
CREATE TABLE IF NOT EXISTS schema_definition (
    id VARCHAR(64) NOT NULL COMMENT 'Schema唯一标识',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    schema_type VARCHAR(50) NOT NULL COMMENT 'Schema类型',
    name VARCHAR(255) NOT NULL COMMENT 'Schema名称',
    content JSON NOT NULL COMMENT 'Schema定义内容(JSON)',
    state VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/DISABLED/DELETED)',
    content_version INT NOT NULL DEFAULT 1 COMMENT '内容版本号（乐观锁）',
    structure_version VARCHAR(20) DEFAULT '1.0.0' COMMENT '结构版本号（语义版本）',
    belong_to VARCHAR(64) NULL COMMENT '所属Schema ID(组合关系)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by VARCHAR(64) NOT NULL COMMENT '创建人ID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by VARCHAR(64) NOT NULL COMMENT '更新人ID',
    deleted_at DATETIME NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    INDEX idx_org_type (org_id, schema_type),
    INDEX idx_org_state (org_id, state),
    INDEX idx_belong_to (belong_to),
    INDEX idx_updated_at (updated_at),
    INDEX idx_type_state (schema_type, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Schema定义表';

-- ============================================================
-- 2. schema_index 二级索引表
-- ============================================================
CREATE TABLE IF NOT EXISTS schema_index (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '自增主键',
    schema_id VARCHAR(64) NOT NULL COMMENT 'Schema ID',
    index_type VARCHAR(50) NOT NULL COMMENT '索引类型(SchemaType枚举值)',
    index_key VARCHAR(255) NOT NULL COMMENT '索引键值',
    PRIMARY KEY (id),
    UNIQUE KEY uk_schema_index (schema_id, index_type, index_key),
    INDEX idx_type_key (index_type, index_key),
    INDEX idx_schema_id (schema_id),
    CONSTRAINT fk_schema_index_schema FOREIGN KEY (schema_id)
        REFERENCES schema_definition(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Schema二级索引表';

-- ============================================================
-- 3. schema_reference 引用关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS schema_reference (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '自增主键',
    source_id VARCHAR(64) NOT NULL COMMENT '源Schema ID',
    source_type VARCHAR(50) NOT NULL COMMENT '源Schema类型',
    target_id VARCHAR(64) NOT NULL COMMENT '目标Schema ID',
    target_type VARCHAR(50) NOT NULL COMMENT '目标Schema类型',
    reference_type VARCHAR(20) NOT NULL COMMENT '引用类型(COMPOSITION/AGGREGATION)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_target (source_id, target_id),
    INDEX idx_source (source_id, source_type),
    INDEX idx_target (target_id, target_type),
    INDEX idx_ref_type (reference_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Schema引用关系表';

-- ============================================================
-- 4. schema_changelog 变更日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS schema_changelog (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '自增主键',
    schema_id VARCHAR(64) NOT NULL COMMENT 'Schema ID',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    schema_type VARCHAR(50) NOT NULL COMMENT 'Schema类型',
    action VARCHAR(20) NOT NULL COMMENT '操作类型(CREATE/UPDATE/DELETE)',
    content_version INT NOT NULL COMMENT '变更后的版本号',
    before_snapshot JSON NULL COMMENT '变更前完整快照',
    after_snapshot JSON NULL COMMENT '变更后完整快照',
    change_summary VARCHAR(2000) NULL COMMENT '变更摘要(人可读)',
    change_detail JSON NULL COMMENT '结构化变更详情(JSON)',
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    changed_by VARCHAR(64) NOT NULL COMMENT '变更人ID',
    trace_id VARCHAR(64) NULL COMMENT '分布式追踪ID',
    PRIMARY KEY (id),
    INDEX idx_schema_id (schema_id),
    INDEX idx_org_changed_at (org_id, changed_at DESC),
    INDEX idx_org_type_changed_at (org_id, schema_type, changed_at DESC),
    INDEX idx_changed_by (changed_by),
    INDEX idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Schema变更日志表';

