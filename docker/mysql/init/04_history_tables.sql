-- ============================================================
-- history-service 数据库表结构
-- ============================================================

CREATE DATABASE IF NOT EXISTS planka_history
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE planka_history;

-- card_history_meta：记录每个 __PLANKA_EINST__ 对应的历史表名，用于动态分表
CREATE TABLE IF NOT EXISTS card_history_meta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    card_type_id VARCHAR(64) NOT NULL COMMENT '__PLANKA_EINST__ID',
    table_name VARCHAR(100) NOT NULL COMMENT '历史表名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_card_type_id (card_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡片历史表元数据';
