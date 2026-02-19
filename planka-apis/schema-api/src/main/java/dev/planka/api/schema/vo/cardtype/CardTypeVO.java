package dev.planka.api.schema.vo.cardtype;

import dev.planka.domain.schema.SchemaSubType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 卡片类型 VO 基类
 * <p>
 * 使用 Jackson 多态序列化，不同类型返回不同字段。
 */
@Data
@SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "schemaSubType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TraitCardTypeVO.class, name = SchemaSubType.TRAIT_CARD_TYPE),
        @JsonSubTypes.Type(value = EntityCardTypeVO.class, name = SchemaSubType.ENTITY_CARD_TYPE)
})
public abstract class CardTypeVO {

    /**
     * 获取 Schema 子类型标识
     */
    public abstract String getSchemaSubType();

    /**
     * 卡片类型 ID
     */
    private String id;

    /**
     * 组织 ID
     */
    private String orgId;

    /**
     * 卡片类型名称
     */
    private String name;

    /**
     * 卡片类型编码
     */
    private String code;

    /**
     * 是否为系统内置卡片类型
     */
    private boolean systemCardType;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 内容版本号（乐观锁）
     */
    private int contentVersion;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
