package cn.planka.domain.schema.definition.workflow;

/**
 * 自动执行失败策略
 */
public enum FailureStrategy {
    /**
     * 阻塞流程
     */
    BLOCK_WORKFLOW,

    /**
     * 继续执行
     */
    CONTINUE
}
