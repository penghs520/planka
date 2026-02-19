package dev.planka.domain.schema.definition.cardtype;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.planka.domain.schema.definition.SchemaDefinition;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 卡片类型定义抽象基类
 * <p>
 * 卡片类型分为两种：
 * <ul>
 *     <li>属性集（AbstractCardType）：只能被继承，不能直接创建卡片，用于定义共同的属性和关联关系</li>
 *     <li>实体类型（EntityCardType）：可以继承多个属性集，可以用来创建卡片</li>
 * </ul>
 * <p>
 * 卡片类型一旦创建后，其类型（抽象/具体）不可改变。
 * <p>
 * 注意：Jackson 多态注解已集中配置在 {@link SchemaDefinition} 接口上。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CardTypeDefinition extends AbstractSchemaDefinition<CardTypeId> {

    /** 卡片类型编码（唯一标识，可以为空，仅在数据同步场景需要使用）） */
    @JsonProperty("code")
    protected String code;

    /** 是否系统内置类型（系统内置类型不可删除，不可以修改） */
    @JsonProperty("systemType")
    protected boolean systemType = false;

    protected CardTypeDefinition(CardTypeId id, String orgId, String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.CARD_TYPE;
    }

    @Override
    public SchemaId belongTo() {
        return null;  // 卡片类型没有所属关系
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of();  // 卡片类型没有二级索引
    }

    @Override
    public void validate() {
        super.validate();
    }

    @Override
    protected CardTypeId newId() {
        return CardTypeId.generate();
    }

    /**
     * 工作流配置
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowConfig {
        /** 初始状态ID */
        @JsonProperty("initialStatusId")
        private String initialStatusId;

        /** 完成状态ID列表 */
        @JsonProperty("doneStatusIds")
        private List<String> doneStatusIds;

        /** 是否启用WIP限制 */
        @JsonProperty("wipLimitEnabled")
        private boolean wipLimitEnabled = false;
    }
}
