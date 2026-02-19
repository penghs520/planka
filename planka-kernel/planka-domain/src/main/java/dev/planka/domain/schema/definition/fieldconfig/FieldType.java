package dev.planka.domain.schema.definition.fieldconfig;

/**
 * 字段类型枚举
 * <p>
 * 定义系统支持的所有字段类型，用于：
 * - FieldConfig 的类型标识
 * - 前端渲染配置的类型判断
 * - 属性值的类型校验
 */
public enum FieldType {

    /**
     * 单行文本
     */
    TEXT("TEXT", "单行文本"),

    /**
     * 多行文本
     */
    TEXTAREA("TEXTAREA", "多行文本"),

    /**
     * 数字
     */
    NUMBER("NUMBER", "数字"),

    /**
     * 日期
     */
    DATE("DATE", "日期"),

    /**
     * 枚举（单选/多选）
     */
    ENUM("ENUM", "枚举"),

    /**
     * 附件
     */
    ATTACHMENT("ATTACHMENT", "附件"),

    /**
     * 关联
     */
    LINK("LINK", "关联"),

    /**
     * 架构层级
     */
    STRUCTURE("STRUCTURE", "架构层级"),

    /**
     * 网页链接
     */
    WEB_URL("WEB_URL", "网页链接"),

    /**
     * Markdown 富文本
     */
    MARKDOWN("MARKDOWN", "Markdown");

    private final String code;
    private final String displayName;

    FieldType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 获取类型编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据编码查找枚举值
     *
     * @param code 类型编码
     * @return 对应的枚举值，未找到时返回 null
     */
    public static FieldType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FieldType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
