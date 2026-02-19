package dev.planka.domain.schema.definition.action;

/**
 * 动作类别枚举
 * <p>
 * 定义卡片动作的分类。
 */
public enum ActionCategory {

    /**
     * 生命周期操作
     * <p>
     * 固定显示，控制卡片生命周期（丢弃、归档、还原）
     */
    LIFECYCLE,

    /**
     * 状态切换操作
     * <p>
     * 根据卡片状态自动决定显示文案和行为（阻塞/解阻、点亮/暂停）
     */
    STATE_TOGGLE,

    /**
     * 自定义操作
     * <p>
     * 用户配置的业务操作（更新字段、调用接口等）
     */
    CUSTOM
}
