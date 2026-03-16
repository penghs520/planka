package cn.planka.domain.notification;

/**
 * 定义参数类型
 */
public enum DefinitionParameterType {
    /**
     * 卡片类型 - 需要选择具体的 CardTypeId
     */
    CARD_TYPE,

    /**
     * 日期 - 只需要输入名称作为标识
     */
    DATE,

    /**
     * 文本 - 只需要输入名称作为标识
     */
    TEXT,

    /**
     * 多行文本 - 只需要输入名称作为标识
     */
    MULTILINE_TEXT,

    /**
     * 链接 - 只需要输入名称作为标识
     */
    LINK,

    /**
     * 数字 - 只需要输入名称作为标识
     */
    NUMBER
}
