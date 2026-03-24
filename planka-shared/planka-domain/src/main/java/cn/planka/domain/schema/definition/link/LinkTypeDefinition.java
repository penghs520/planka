package cn.planka.domain.schema.definition.link;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.link.LinkTypeId;
import cn.planka.domain.schema.SchemaId;
import cn.planka.domain.schema.SchemaSubType;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
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

    /** 源端编码（如"parent"） */
    @Setter
    @JsonProperty("sourceCode")
    private String sourceCode;

    /** 目标端名称（如"子卡片"） */
    @Setter
    @JsonProperty("targetName")
    private String targetName;

    /** 目标端编码（如"children"） */
    @Setter
    @JsonProperty("targetCode")
    private String targetCode;

    /** 源端是否显示 */
    @Setter
    @JsonProperty("sourceVisible")
    private boolean sourceVisible = true;

    /** 目标端是否显示 */
    @Setter
    @JsonProperty("targetVisible")
    private boolean targetVisible = true;

    /**
     * 源端允许的实体类型ID（null 表示不限制）
     */
    @Setter
    @JsonProperty("sourceCardTypeId")
    private CardTypeId sourceCardTypeId;

    /**
     * 目标端允许的实体类型ID（null 表示不限制）
     */
    @Setter
    @JsonProperty("targetCardTypeId")
    private CardTypeId targetCardTypeId;

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
     * 值来源是否为系统更新，例如创建人、回收人、存档人
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
        if (sourceCardTypeId != null) {
            keys.add(sourceCardTypeId);
        }
        if (targetCardTypeId != null) {
            keys.add(targetCardTypeId);
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
