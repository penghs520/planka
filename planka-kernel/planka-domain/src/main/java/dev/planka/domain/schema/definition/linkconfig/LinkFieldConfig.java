package dev.planka.domain.schema.definition.linkconfig;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.link.LinkTypeId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.fieldconfig.FieldType;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * 关联属性配置
 * <p>
 * 继承 FieldConfig，fieldId 字段存储格式为 "{linkTypeId}:{SOURCE|TARGET}"。
 * 当关联类型应用到卡片类型时，产生此配置，支持差异化设置。
 * <p>
 * belongTo: 所属的卡片类型ID
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LinkFieldConfig extends FieldConfig {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.LINK_FIELD;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.LINK;
    }

    // ==================== 关联特有配置 ====================

    /** 显示名称（覆盖源端/目标端名称，为 null 时使用 LinkTypeDefinition 中的配置） */
    @JsonProperty("displayName")
    private String displayName;

    /** 是否多选（从 LinkTypeDefinition 中的 sourceMultiSelect/targetMultiSelect 获取） */
    @JsonProperty("multiple")
    private Boolean multiple;

    /** 目标卡片过滤条件（仅 SOURCE 端有效） */
    @JsonProperty("targetCardFilter")
    private Condition targetCardFilter;

    /**
     * 完整构造函数
     *
     * @param id          配置ID
     * @param orgId       组织ID
     * @param name        名称
     * @param cardTypeId  所属卡片类型ID
     * @param linkFieldId 关联属性ID，格式为 "{linkTypeId}:{SOURCE|TARGET}"
     * @param systemField 是否系统内置
     */
    @JsonCreator
    public LinkFieldConfig(
            @JsonProperty("id") FieldConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("fieldId") FieldId linkFieldId,
            @JsonProperty("systemField") boolean systemField) {
        super(id, orgId, name, cardTypeId, linkFieldId, systemField);
    }

    /**
     * 获取关联属性ID（即 fieldId）
     * <p>
     * 格式为 "{linkTypeId}:{SOURCE|TARGET}"
     */
    @JsonIgnore
    public String getLinkFieldId() {
        return fieldId.value();
    }

    /**
     * 获取关联类型ID（从 fieldId 解析）
     */
    @JsonIgnore
    public LinkTypeId getLinkTypeId() {
        return LinkFieldIdUtils.getLinkTypeIdObject(getLinkFieldId());
    }

    /**
     * 获取关联位置（从 fieldId 解析）
     */
    @JsonIgnore
    public LinkPosition getLinkPosition() {
        return LinkFieldIdUtils.getPosition(getLinkFieldId());
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of(getCardTypeId(), getLinkTypeId());
    }

    @Override
    public void validate() {
        super.validate();
        if (!LinkFieldIdUtils.isValidFormat(getLinkFieldId())) {
            throw new IllegalArgumentException("关联属性ID格式无效: " + getLinkFieldId());
        }
    }
}
