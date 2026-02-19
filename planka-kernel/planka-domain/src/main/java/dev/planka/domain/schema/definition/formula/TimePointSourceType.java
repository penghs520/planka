package dev.planka.domain.schema.definition.formula;

/**
 * 时间点数据源类型枚举
 */
public enum TimePointSourceType {
    /**
     * 卡片创建时间
     */
    CARD_CREATED_TIME,

    /**
     * 卡片更新时间
     */
    CARD_UPDATED_TIME,

    /**
     * 自定义日期字段的值
     */
    CUSTOM_DATE_FIELD,

    /**
     * 卡片进入某个具体价值流状态的时间
     */
    STATUS_ENTER_TIME,

    /**
     * 卡片离开某个具体价值流状态的时间
     */
    STATUS_EXIT_TIME,

    /**
     * 卡片进入当前价值流状态的时间
     */
    CURRENT_STATUS_ENTER_TIME,

    /**
     * 当前时间（仅用于时间段公式的结束时间）
     */
    CURRENT_TIME
}
