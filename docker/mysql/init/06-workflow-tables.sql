-- 工作流数据库表初始化脚本

-- 流程实例表
CREATE TABLE IF NOT EXISTS `workflow_instance` (
    `id` BIGINT NOT NULL COMMENT '流程实例ID',
    `org_id` BIGINT NOT NULL COMMENT '组织ID',
    `workflow_id` VARCHAR(64) NOT NULL COMMENT '工作流定义ID',
    `card_id` BIGINT NOT NULL COMMENT '关联卡片ID',
    `card_type_id` VARCHAR(64) NOT NULL COMMENT '实体类型ID',
    `status` VARCHAR(20) NOT NULL COMMENT 'RUNNING/COMPLETED/CANCELLED/SUSPENDED',
    `trigger_type` VARCHAR(20) NOT NULL COMMENT '触发类型',
    `current_node_ids` JSON COMMENT '当前活跃节点ID列表',
    `context_data` JSON COMMENT '流程上下文数据',
    `definition_snapshot` JSON COMMENT '启动时的流程定义快照',
    `definition_version` INT COMMENT '启动时的流程定义版本号',
    `started_by` BIGINT COMMENT '发起人ID',
    `completed_by` BIGINT COMMENT '完成人ID',
    `cancel_reason` VARCHAR(500) COMMENT '取消原因',
    `timeout_at` DATETIME COMMENT '流程超时时间',
    `started_at` DATETIME COMMENT '开始时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_card_id` (`card_id`),
    KEY `idx_org_status` (`org_id`, `status`),
    KEY `idx_started_by` (`started_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例';

-- 节点实例表
CREATE TABLE IF NOT EXISTS `workflow_node_instance` (
    `id` BIGINT NOT NULL COMMENT '节点实例ID',
    `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
    `node_id` VARCHAR(64) NOT NULL COMMENT '节点定义ID',
    `node_type` VARCHAR(20) NOT NULL COMMENT '节点类型',
    `node_name` VARCHAR(255) NOT NULL COMMENT '节点名称',
    `status` VARCHAR(20) NOT NULL COMMENT 'PENDING/ACTIVE/COMPLETED/FAILED/SKIPPED',
    `input_data` JSON COMMENT '输入数据',
    `output_data` JSON COMMENT '输出数据',
    `error_message` VARCHAR(2000) COMMENT '错误信息',
    `started_at` DATETIME COMMENT '开始时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance_node` (`instance_id`, `node_id`),
    KEY `idx_instance_id` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点实例';

-- 审批任务表
CREATE TABLE IF NOT EXISTS `workflow_approval_task` (
    `id` BIGINT NOT NULL COMMENT '审批任务ID',
    `org_id` BIGINT NOT NULL COMMENT '组织ID',
    `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
    `node_id` VARCHAR(64) NOT NULL COMMENT '节点定义ID',
    `card_id` BIGINT NOT NULL COMMENT '关联卡片ID',
    `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
    `status` VARCHAR(20) NOT NULL COMMENT 'PENDING/APPROVED/REJECTED/TRANSFERRED/TIMEOUT',
    `approval_mode` VARCHAR(20) NOT NULL COMMENT '审批模式(ANY_ONE/ALL_REQUIRED)',
    `due_at` DATETIME COMMENT '到期时间',
    `approved_at` DATETIME COMMENT '审批时间',
    `comment` VARCHAR(2000) COMMENT '审批意见',
    `form_data` JSON COMMENT '审批表单数据',
    `transferred_to` BIGINT COMMENT '转交给谁',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_card_id` (`card_id`),
    KEY `idx_approver_status` (`approver_id`, `status`),
    KEY `idx_due_at` (`due_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务';

-- 流程事件日志表
CREATE TABLE IF NOT EXISTS `workflow_event_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型',
    `node_id` VARCHAR(64) COMMENT '节点ID',
    `task_id` BIGINT COMMENT '审批任务ID',
    `operator_id` BIGINT COMMENT '操作人ID',
    `event_data` JSON COMMENT '事件数据',
    `occurred_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_occurred_at` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程事件日志';

