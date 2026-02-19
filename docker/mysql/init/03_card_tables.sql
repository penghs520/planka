-- ============================================================
-- Card服务数据库表结构
-- 描述: Card服务所有表的完整结构
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS planka_card
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE planka_card;

-- ============================================================
-- 1. sequence_segment 号段分配表
-- ============================================================
CREATE TABLE IF NOT EXISTS sequence_segment (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '自增主键',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    segment_key VARCHAR(64) NOT NULL COMMENT '号段类型标识（如 CARD_CODE）',
    current_max BIGINT NOT NULL DEFAULT 0 COMMENT '当前已分配的最大值',
    step INT NOT NULL DEFAULT 1000 COMMENT '每次获取的号段步长',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_key (org_id, segment_key),
    INDEX idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='号段分配表';

-- ============================================================
-- 2. flow_record_meta 流动记录元数据表
-- ============================================================
-- 用于管理按价值流动态创建的流动记录表
CREATE TABLE IF NOT EXISTS flow_record_meta (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '自增主键',
    stream_id VARCHAR(64) NOT NULL COMMENT '价值流ID',
    table_name VARCHAR(128) NOT NULL COMMENT '流动记录表名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stream_id (stream_id),
    UNIQUE KEY uk_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流动记录元数据表';

-- ============================================================
-- 流动记录表模板（DDL，由 FlowRecordTableManager 动态创建）
-- 表名格式: flow_record_{stream_id}
-- ============================================================
-- CREATE TABLE flow_record_{stream_id} (
--     id BIGINT UNSIGNED NOT NULL COMMENT '唯一标识（雪花算法）',
--     card_id BIGINT NOT NULL COMMENT '卡片ID',
--     card_type_id VARCHAR(64) NOT NULL COMMENT '卡片类型ID',
--     stream_id VARCHAR(64) NOT NULL COMMENT '价值流ID',
--     step_id VARCHAR(64) NOT NULL COMMENT '阶段ID',
--     status_id VARCHAR(64) NOT NULL COMMENT '状态ID',
--     status_work_type TINYINT NOT NULL COMMENT '状态工作类型（0=等待，1=工作中）',
--     record_type VARCHAR(20) NOT NULL COMMENT '记录类型枚举（ENTER/LEAVE/ROLLBACK_ENTER/ROLLBACK_LEAVE）',
--     event_time DATETIME(3) NOT NULL COMMENT '事件发生时间（毫秒精度）',
--     operator_id VARCHAR(64) NULL COMMENT '操作人ID',
--     PRIMARY KEY (id),
--     INDEX idx_card_id_time (card_id, event_time),
--     INDEX idx_event_time (event_time),
--     INDEX idx_status_time (status_id, event_time),
--     INDEX idx_step_time (step_id, event_time)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流动记录表';

-- ============================================================
-- 3. biz_rule_execution_log_meta 规则执行日志元数据表
-- ============================================================
-- 用于管理按卡片类型动态创建的规则执行日志表
CREATE TABLE IF NOT EXISTS biz_rule_execution_log_meta (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '自增主键',
    card_type_id VARCHAR(64) NOT NULL COMMENT '卡片类型ID',
    table_name VARCHAR(128) NOT NULL COMMENT '执行日志表名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_card_type_id (card_type_id),
    UNIQUE KEY uk_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则执行日志元数据表';

-- ============================================================
-- 规则执行日志表模板（DDL，由 RuleExecutionLogTableManager 动态创建）
-- 表名格式: biz_rule_execution_log_{card_type_id}
-- ============================================================
-- CREATE TABLE biz_rule_execution_log_{card_type_id} (
--     id VARCHAR(32) NOT NULL COMMENT '日志ID',
--     rule_id VARCHAR(32) COMMENT '规则ID',
--     rule_name VARCHAR(200) COMMENT '规则名称',
--     card_id VARCHAR(32) COMMENT '触发卡片ID',
--     trigger_event VARCHAR(50) COMMENT '触发事件类型',
--     operator_id VARCHAR(32) COMMENT '操作人ID',
--     execution_time DATETIME(3) COMMENT '执行时间',
--     duration_ms INT COMMENT '执行耗时（毫秒）',
--     status VARCHAR(20) COMMENT '执行状态',
--     affected_card_ids TEXT COMMENT '受影响的卡片ID列表（JSON）',
--     action_results TEXT COMMENT '各动作执行结果（JSON）',
--     error_message TEXT COMMENT '错误信息',
--     trace_id VARCHAR(64) COMMENT '追踪ID',
--     created_at DATETIME COMMENT '创建时间',
--     PRIMARY KEY (id),
--     INDEX idx_rule_id (rule_id),
--     INDEX idx_card_id (card_id),
--     INDEX idx_execution_time (execution_time),
--     INDEX idx_status (status),
--     INDEX idx_trace_id (trace_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则执行日志表';
