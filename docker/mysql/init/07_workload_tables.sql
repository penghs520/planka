-- 考勤导入记录表
CREATE TABLE IF NOT EXISTS attendance_import_record (
    id VARCHAR(64) PRIMARY KEY COMMENT '记录ID',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    batch VARCHAR(64) NOT NULL COMMENT '批次号',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(512) NOT NULL COMMENT '文件路径',
    total_count INT NOT NULL DEFAULT 0 COMMENT '总记录数',
    success_count INT NOT NULL DEFAULT 0 COMMENT '成功数',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '失败数',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-导入中，1-成功，2-失败',
    error_message TEXT COMMENT '错误信息',
    operator_id VARCHAR(64) NOT NULL COMMENT '操作人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_org_id (org_id),
    INDEX idx_batch (batch),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤导入记录表';

-- 窗口期配置表
CREATE TABLE IF NOT EXISTS window_config (
    id VARCHAR(64) PRIMARY KEY COMMENT '配置ID',
    org_id VARCHAR(64) NOT NULL UNIQUE COMMENT '组织ID',
    create_window INT NOT NULL DEFAULT 7 COMMENT '创建窗口期（天）',
    update_window INT NOT NULL DEFAULT 3 COMMENT '修改窗口期（天）',
    delete_window INT NOT NULL DEFAULT 1 COMMENT '删除窗口期（天）',
    lock_day INT NOT NULL DEFAULT 5 COMMENT '锁定日期（每月几号）',
    lock_hour INT NOT NULL DEFAULT 23 COMMENT '锁定时间（几点）',
    manager_whitelist_enable BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用管理员白名单',
    manager_whitelist TEXT COMMENT '管理员白名单（JSON数组）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='窗口期配置表';

-- 结算记录表
CREATE TABLE IF NOT EXISTS settlement_record (
    id VARCHAR(64) PRIMARY KEY COMMENT '记录ID',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    month VARCHAR(7) NOT NULL COMMENT '结算月份（YYYY-MM）',
    member_id VARCHAR(64) NOT NULL COMMENT '成员ID',
    total_work_days INT NOT NULL DEFAULT 0 COMMENT '总工作天数',
    total_work_hours DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总工作时长（小时）',
    total_overtime_hours DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总加班时长（小时）',
    total_leave_days INT NOT NULL DEFAULT 0 COMMENT '总请假天数',
    total_absent_days INT NOT NULL DEFAULT 0 COMMENT '总旷工天数',
    service_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '服务费（元）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待结算，COMPLETED-已完成，REVOKED-已撤销',
    settlement_time DATETIME COMMENT '结算时间',
    operator_id VARCHAR(64) NOT NULL COMMENT '操作人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_org_month (org_id, month),
    INDEX idx_member_id (member_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算记录表';
