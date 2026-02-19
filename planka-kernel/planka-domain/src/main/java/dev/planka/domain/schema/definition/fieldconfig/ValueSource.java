package dev.planka.domain.schema.definition.fieldconfig;

/**
 * 属性值来源类型枚举
 */
public enum ValueSource {
    /**
     * 系统更新
     */
    SYSTEM,

    /**
     * 手动输入
     */
    MANUAL,

    /**
     * 计算公式
     */
    FORMULA,

    /**
     * 引用
     */
    REFERENCE
}
