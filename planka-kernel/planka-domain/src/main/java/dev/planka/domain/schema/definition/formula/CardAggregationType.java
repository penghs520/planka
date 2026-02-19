package dev.planka.domain.schema.definition.formula;

/**
 * 卡片汇集方式枚举
 */
public enum CardAggregationType {
    /**
     * 计数（统计关联卡片个数）
     */
    COUNT,

    /**
     * 去重计数（统计关联卡片个数，去重）
     */
    DISTINCT_COUNT,

    /**
     * 求和
     */
    SUM,

    /**
     * 平均值
     */
    AVG,

    /**
     * 最小值
     */
    MIN,

    /**
     * 最大值
     */
    MAX,

    /**
     * P85分位数
     */
    P85
}
