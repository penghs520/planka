package dev.planka.domain.schema.definition.template;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.CardDetailTemplateId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.template.detail.DetailHeaderConfig;
import dev.planka.domain.schema.definition.template.detail.TabConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 卡片详情页模板定义
 * <p>
 * 定义卡片详情页的布局和展示内容，包括区域划分、属性排列等。
 * 实体类型通过引用模板ID来使用该模板。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CardDetailTemplateDefinition extends AbstractSchemaDefinition<CardDetailTemplateId> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CARD_DETAIL_TEMPLATE;
    }

    /** 所属卡片类型ID */
    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    /** 是否系统内置模板 */
    @JsonProperty("systemTemplate")
    private boolean systemTemplate = false;

    /** 生效条件（当卡片满足此条件时使用该模板） */
    @JsonProperty("effectiveCondition")
    private Condition effectiveCondition;

    /** 优先级（数字越小优先级越高，用于多模板匹配） */
    @JsonProperty("priority")
    private Integer priority = 100;

    /** 是否默认模板（每个卡片类型只能有一个默认模板） */
    @JsonProperty("isDefault")
    private boolean isDefault = false;

    /** 头部配置 */
    @JsonProperty("header")
    private DetailHeaderConfig header;

    /** 标签页配置列表 */
    @JsonProperty("tabs")
    private List<TabConfig> tabs = new ArrayList<>();

    @JsonCreator
    public CardDetailTemplateDefinition(
            @JsonProperty("id") CardDetailTemplateId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.CARD_DETAIL_TEMPLATE;
    }

    @Override
    public SchemaId belongTo() {
        return cardTypeId;  // 卡片详情页模板属于某个卡片类型
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of(cardTypeId);
    }

    @Override
    protected CardDetailTemplateId newId() {
        return CardDetailTemplateId.generate();
    }

}
