-- 添加评论操作来源字段
-- 用于记录评论是由用户直接创建、业务规则触发还是第三方API调用

ALTER TABLE comment
    ADD COLUMN operation_source JSON DEFAULT NULL COMMENT '操作来源（JSON格式）：{"type":"BIZ_RULE","ruleId":"xxx","ruleName":"状态前进时触发"}';
