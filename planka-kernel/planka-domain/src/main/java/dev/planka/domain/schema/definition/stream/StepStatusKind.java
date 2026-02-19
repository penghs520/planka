package dev.planka.domain.schema.definition.stream;

/**
 * 状态类别枚举
 */
public enum StepStatusKind {
    /**
     * 待办
     */
    TODO,
    /**
     * 进行中
     */
    IN_PROGRESS,
    /**
     * 已完成
     */
    DONE,
    /**
     * 已取消
     */
    CANCELLED
}
