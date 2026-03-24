package cn.planka.domain.schema;

/**
 * Schema 子类型常量
 * <p>
 * 用于 Jackson 多态序列化的类型标识。
 */
public final class SchemaSubType {

    private SchemaSubType() {
        // 私有构造函数，防止实例化
    }

    // ==================== 实体类型 ====================
    /** 特征类型 */
    public static final String TRAIT_CARD_TYPE = "TRAIT_CARD_TYPE";
    /** 实体类型 */
    public static final String ENTITY_CARD_TYPE = "ENTITY_CARD_TYPE";

    // ==================== 属性配置 ====================
    /** 单行文本属性配置 */
    public static final String TEXT_FIELD = "TEXT_FIELD";
    /** 多行文本属性配置 */
    public static final String MULTI_LINE_TEXT_FIELD = "MULTI_LINE_TEXT_FIELD";
    /** Markdown属性配置 */
    public static final String MARKDOWN_FIELD = "MARKDOWN_FIELD";
    /** 数字属性配置 */
    public static final String NUMBER_FIELD = "NUMBER_FIELD";
    /** 日期属性配置 */
    public static final String DATE_FIELD = "DATE_FIELD";
    /** 枚举属性配置 */
    public static final String ENUM_FIELD = "ENUM_FIELD";
    /** 附件属性配置 */
    public static final String ATTACHMENT_FIELD = "ATTACHMENT_FIELD";
    /** 网页链接属性配置 */
    public static final String WEB_URL_FIELD = "WEB_URL_FIELD";
    /** 级联属性配置 */
    public static final String CASCADE_FIELD = "CASCADE_FIELD";
    /** 关联属性配置 */
    public static final String LINK_FIELD = "LINK_FIELD";

    // ==================== 级联关系定义 ====================
    public static final String CASCADE_RELATION_DEFINITION = "CASCADE_RELATION_DEFINITION";


    // ==================== 其他 Schema 定义 ====================
    /** 列表视图 */
    public static final String LIST_VIEW = "LIST_VIEW";
    /** 价值流定义 */
    public static final String VALUE_STREAM = "VALUE_STREAM";
    /** 关联类型 */
    public static final String LINK_TYPE = "LINK_TYPE";
    /** 卡片详情页模板 */
    public static final String CARD_DETAIL_TEMPLATE = "CARD_DETAIL_TEMPLATE";
    /** 卡片新建页模板 */
    public static final String CARD_CREATE_PAGE_TEMPLATE = "CARD_CREATE_PAGE_TEMPLATE";
    /** 卡面定义 */
    public static final String CARD_FACE = "CARD_FACE";
    /** 卡片权限定义 */
    public static final String CARD_PERMISSION = "CARD_PERMISSION";
    /** 业务规则 */
    public static final String BIZ_RULE = "BIZ_RULE";
    /** 流转策略 */
    public static final String FLOW_POLICY = "FLOW_POLICY";

    // ==================== 菜单定义 ====================
    /** 菜单分组 */
    public static final String MENU_GROUP = "MENU_GROUP";

    // ==================== 计算公式定义 ====================
    /** 时间点公式定义 */
    public static final String TIME_POINT_FORMULA_DEFINITION = "TIME_POINT_FORMULA_DEFINITION";
    /** 时间段公式定义 */
    public static final String TIME_RANGE_FORMULA_DEFINITION = "TIME_RANGE_FORMULA_DEFINITION";
    /** 日期汇集公式定义 */
    public static final String DATE_COLLECTION_FORMULA_DEFINITION = "DATE_COLLECTION_FORMULA_DEFINITION";
    /** 卡片汇集公式定义 */
    public static final String CARD_COLLECTION_FORMULA_DEFINITION = "CARD_COLLECTION_FORMULA_DEFINITION";
    /** 数值运算公式定义 */
    public static final String NUMBER_CALCULATION_FORMULA_DEFINITION = "NUMBER_CALCULATION_FORMULA_DEFINITION";

    // ==================== 卡片动作 ====================
    /** 卡片动作配置 */
    public static final String CARD_ACTION_CONFIG = "CARD_ACTION_CONFIG";

}
