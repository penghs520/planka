package cn.planka.domain.schema.definition.template;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.CardCreatePageTemplateId;
import cn.planka.domain.schema.SchemaId;
import cn.planka.domain.schema.SchemaSubType;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
import cn.planka.domain.schema.definition.template.create.CreatePageFieldItemConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 卡片新建页模板定义
 * <p>
 * 定义卡片创建表单的布局和字段配置。
 * 使用简化的平铺布局（无标签页），支持可选的区域分组。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CardCreatePageTemplateDefinition extends AbstractSchemaDefinition<CardCreatePageTemplateId> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CARD_CREATE_PAGE_TEMPLATE;
    }

    /** 所属实体类型ID */
    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    /** 是否系统内置模板 */
    @JsonProperty("systemTemplate")
    private boolean systemTemplate = false;

    /** 是否默认模板（每个实体类型只能有一个默认模板） */
    @JsonProperty("isDefault")
    private boolean isDefault = false;

    /** 字段项配置列表（平铺布局，无区域分组） */
    @JsonProperty("fieldItems")
    private List<CreatePageFieldItemConfig> fieldItems = new ArrayList<>();

    @JsonCreator
    public CardCreatePageTemplateDefinition(
            @JsonProperty("id") CardCreatePageTemplateId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.CARD_CREATE_PAGE_TEMPLATE;
    }

    @Override
    public SchemaId belongTo() {
        return cardTypeId;  // 卡片新建页模板属于某个实体类型
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of(cardTypeId);
    }

    @Override
    protected CardCreatePageTemplateId newId() {
        return CardCreatePageTemplateId.generate();
    }
}
