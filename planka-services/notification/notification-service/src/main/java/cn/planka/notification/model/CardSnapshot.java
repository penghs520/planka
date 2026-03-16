package cn.planka.notification.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 卡片快照
 * 用于模板表达式解析
 */
@Data
@Builder
public class CardSnapshot {
    /**
     * 卡片ID
     */
    private String cardId;

    /**
     * 卡片类型ID
     */
    private String cardTypeId;

    /**
     * 卡片类型名称
     */
    private String cardTypeName;

    /**
     * 字段值映射
     * key: 字段ID 或 字段名称
     * value: 字段值
     */
    private Map<String, Object> fieldValues;

    /**
     * 快照时间
     */
    private Instant snapshotAt;
}
