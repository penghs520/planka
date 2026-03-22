package cn.planka.domain.schema.definition.cardtype;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.SchemaId;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * __PLANKA_EINST__定义抽象基类
 * <p>
 * __PLANKA_EINST__分为两种：
 * <ul>
 *     <li>特征类型（AbstractCardType）：只能被继承，不能直接创建卡片，用于定义共同的属性和关联关系</li>
 *     <li>__PLANKA_EINST__（EntityCardType）：可以继承多个特征类型，可以用来创建卡片</li>
 * </ul>
 * <p>
 * __PLANKA_EINST__一旦创建后，其类型（抽象/具体）不可改变。
 * <p>
 * 注意：Jackson 多态注解已集中配置在 {@link cn.planka.domain.schema.definition.SchemaDefinition} 接口上。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CardTypeDefinition extends AbstractSchemaDefinition<CardTypeId> {

    /** __PLANKA_EINST__编码（唯一标识，可以为空，仅在数据同步场景需要使用）） */
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
        return null;  // __PLANKA_EINST__没有所属关系
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of();  // __PLANKA_EINST__没有二级索引
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
