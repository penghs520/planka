package dev.planka.domain.schema.definition.condition;

/**
 * 条件节点类型常量
 */
public class NodeType {

    public static final String GROUP = "GROUP";

    // 自定义字段条件类型
    public static final String TEXT = "TEXT";
    public static final String NUMBER = "NUMBER";
    public static final String DATE = "DATE";
    public static final String ENUM = "ENUM";
    public static final String WEB_URL = "WEB_URL";
    public static final String LINK = "LINK";

    // 价值流状态条件
    public static final String STATUS = "STATUS";

    // 卡片生命周期状态条件（ACTIVE/ARCHIVED/DISCARDED）
    public static final String CARD_CYCLE = "CARD_CYCLE";

    // 系统字段条件类型
    public static final String TITLE = "TITLE";
    public static final String CODE = "CODE";
    public static final String KEYWORD = "KEYWORD";

    // 注：系统时间字段（CREATED_AT, UPDATED_AT, DISCARDED_AT, ARCHIVED_AT）
    // 通过 DateConditionItem 的 DateSubject.SystemDateSubject 支持，不再作为独立的 NodeType

    // 系统用户字段条件类型
    public static final String CREATED_BY = "CREATED_BY";
    public static final String UPDATED_BY = "UPDATED_BY";

}
