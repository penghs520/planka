package dev.planka.card.model;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.card.*;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 卡片存储实体
 */
@Getter
@Setter
public class CardEntity {

    // ==================== 标识信息 ====================

    /**
     * 卡片ID
     */
    private final CardId id;

    /**
     * 卡片内置编号
     */
    private final long codeInOrg;

    /**
     * 卡片自定义编号
     */
    private final String customCode;

    /**
     * 组织ID
     */
    private final OrgId orgId;

    /**
     * 卡片类型ID
     */
    private final CardTypeId typeId;

    // ==================== 基本信息 ====================

    /**
     * 卡片标题
     */
    private final CardTitle title;

    /**
     * 卡片描述
     */
    private String description;

    // ==================== 状态信息 ====================

    /**
     * 生命周期状态
     */
    private final CardStyle cardStyle;

    /**
     * 价值流定义ID
     */
    private final StreamId streamId;

    /**
     * 价值流状态ID
     */
    private final StatusId statusId;

    // ==================== 属性值存储 ====================

    /**
     * 自定义属性值 - 使用 fieldId 作为 key，FieldValue 自身也包含 fieldId
     */
    private Map<String/* fieldId */, FieldValue<?>> fieldValues;

    // ==================== 审计信息 ====================

    /**
     * 创建时间
     */
    private final Long createdAt;


    /**
     * 更新时间
     */
    private Long updatedAt;

    /**
     * 丢弃时间
     */
    private LocalDateTime abandonedAt;

    /**
     * 归档时间
     */
    private LocalDateTime archivedAt;

    public CardEntity(CardId id, long codeInOrg, String customCode, OrgId orgId, CardTypeId typeId, CardTitle title, CardStyle cardStyle,
                      StreamId streamId, StatusId statusId, LocalDateTime createdAt) {
        this.id = AssertUtils.requireNotNull(id, "cardId can't be null");
        this.codeInOrg = codeInOrg;
        this.customCode = customCode;
        this.orgId = AssertUtils.requireNotNull(orgId, "orgId can't be null");
        this.typeId = AssertUtils.requireNotNull(typeId, "typeId can't be null");
        this.title = AssertUtils.requireNotNull(title, "title can't be null");
        this.cardStyle = AssertUtils.requireNotNull(cardStyle, "state can't be null");
        this.streamId = streamId; // 价值流ID可为空（卡片类型未配置价值流时）
        this.statusId = statusId; // 状态ID可为空（卡片类型未配置价值流时）
        LocalDateTime createdAtTime = AssertUtils.requireNotNull(createdAt, "createdAt can't be null");
        this.createdAt = createdAtTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
