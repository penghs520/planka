package dev.planka.domain.schema.definition.link;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkTypeId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 关联类型定义
 * <p>
 * 定义卡片之间的关联类型，如父子关系、依赖关系等。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LinkTypeDefinition extends AbstractSchemaDefinition<LinkTypeId> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.LINK_TYPE;
    }

    /** 关联类型编码（唯一标识，用于数据同步等场景） */
    @Setter
    @JsonProperty("code")
    private String code;

    /** 源端名称（如"父卡片"） */
    @Setter
    @JsonProperty("sourceName")
    private String sourceName;

    /** 目标端名称（如"子卡片"） */
    @Setter
    @JsonProperty("targetName")
    private String targetName;

    /** 源端是否显示 */
    @Setter
    @JsonProperty("sourceVisible")
    private boolean sourceVisible = true;

    /** 目标端是否显示 */
    @Setter
    @JsonProperty("targetVisible")
    private boolean targetVisible = true;

    /**
     * 源端允许的卡片类型ID列表（空表示所有）
     * <p>
     * 仅当都为属性集时才支持指定多个，表示多个属性集的共同实体类型才有此关联关系
     */
    @Setter
    @JsonProperty("sourceCardTypeIds")
    private List<CardTypeId> sourceCardTypeIds;

    /**
     * 目标端允许的卡片类型ID列表（空表示所有）
     * <p>
     * 仅当都为属性集时才支持指定多个，表示多个属性集的共同实体类型才有此关联关系
     */
    @Setter
    @JsonProperty("targetCardTypeIds")
    private List<CardTypeId> targetCardTypeIds;

    /** 源端是否多选（false=单选，true=多选） */
    @Setter
    @JsonProperty("sourceMultiSelect")
    private boolean sourceMultiSelect = true;

    /** 目标端是否多选（false=单选，true=多选） */
    @Setter
    @JsonProperty("targetMultiSelect")
    private boolean targetMultiSelect = true;

    /** 是否系统内置关联类型（系统内置类型不可删除，不可以修改） */
    @Setter
    @JsonProperty("systemLinkType")
    private boolean systemLinkType = false;

    /**
     * 值来源是否为系统更新，例如创建人、丢弃人、归档人
     */
    @Setter
    @JsonProperty("systemInput")
    private boolean systemInput = false;

    @JsonCreator
    public LinkTypeDefinition(
            @JsonProperty("id") LinkTypeId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.LINK_TYPE;
    }

    @Override
    public SchemaId belongTo() {
        return null;  // 关联类型没有所属关系
    }

    @Override
    public Set<SchemaId> secondKeys() {
        Set<SchemaId> keys = new HashSet<>();
        if (sourceCardTypeIds != null) {
            keys.addAll(sourceCardTypeIds);
        }
        if (targetCardTypeIds != null) {
            keys.addAll(targetCardTypeIds);
        }
        return keys;
    }

    @Override
    protected LinkTypeId newId() {
        return LinkTypeId.generate();
    }

    @Override
    public void validate() {
        super.validate();
    }
}
