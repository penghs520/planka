package dev.planka.common.constant;

/**
 * 系统全局常量定义
 */
public final class KanbanConstants {

    private KanbanConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // ==================== 系统标识 ====================

    /** 系统名称 */
    public static final String SYSTEM_NAME = "ValueKanban";

    /** 系统版本 */
    public static final String SYSTEM_VERSION = "1.0.0";

    // ==================== 分隔符 ====================

    /** 通用分隔符 */
    public static final String SEPARATOR = "_";

    /** 路径分隔符 */
    public static final String PATH_SEPARATOR = "/";

    /** 逗号分隔符 */
    public static final String COMMA = ",";

    // ==================== 默认值 ====================

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 0;

    /** 默认每页大小 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 最大每页大小 */
    public static final int MAX_PAGE_SIZE = 1000;

    // ==================== 时间相关 ====================

    /** 默认时区 */
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    /** 日期格式 */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** 日期时间格式 */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** ISO日期时间格式 */
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    // ==================== 缓存相关 ====================

    /** 缓存键前缀 */
    public static final String CACHE_KEY_PREFIX = "kanban:";

    /** 默认缓存过期时间（秒） */
    public static final long DEFAULT_CACHE_TTL = 3600L;

    // ==================== 消息相关 ====================

    /** Kafka主题前缀 */
    public static final String KAFKA_TOPIC_PREFIX = "kanban.";

    /** 卡片事件主题 */
    public static final String TOPIC_CARD_EVENT = KAFKA_TOPIC_PREFIX + "card.event";

    /** Schema事件主题 */
    public static final String TOPIC_SCHEMA_EVENT = KAFKA_TOPIC_PREFIX + "schema.event";
}
