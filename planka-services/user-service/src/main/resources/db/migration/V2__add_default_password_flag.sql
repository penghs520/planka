-- 添加默认密码标记字段
ALTER TABLE sys_user ADD COLUMN using_default_password BOOLEAN DEFAULT FALSE;

-- 更新现有数据：如果用户是 ACTIVE 状态且密码不为空，则认为是自定义密码
-- 如果用户是 PENDING_ACTIVATION 状态，则保持使用默认密码的标记
UPDATE sys_user SET using_default_password = TRUE WHERE status = 'PENDING_ACTIVATION';
