package dev.planka.domain.schema.definition.action;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.CardActionId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.schema.definition.condition.Condition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 卡片动作配置定义
 * <p>
 * 定义在卡片类型级别配置的可执行操作，用户可在卡片详情页点击触发执行。
 * belongTo: 所属的卡片类型ID
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CardActionConfigDefinition extends AbstractSchemaDefinition<CardActionId> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CARD_ACTION_CONFIG;
    }

    /**
     * 所属卡片类型ID
     */
    @Setter
    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    /**
     * 动作类别
     */
    @Setter
    @JsonProperty("actionCategory")
    private ActionCategory actionCategory;

    /**
     * 是否内置动作
     */
    @Setter
    @JsonProperty("builtIn")
    private boolean builtIn = false;

    /**
     * 内置动作类型（builtIn=true 时有效）
     */
    @Setter
    @JsonProperty("builtInActionType")
    private BuiltInActionType builtInActionType;

    /**
     * 图标
     */
    @Setter
    @JsonProperty("icon")
    private String icon;

    /**
     * 颜色
     */
    @Setter
    @JsonProperty("color")
    private String color;

    /**
     * 执行类型（自定义动作时有效）
     */
    @Setter
    @JsonProperty("executionType")
    private ActionExecutionType executionType;

    /**
     * 可见性条件
     * <p>
     * 控制动作按钮何时显示
     */
    @Setter
    @JsonProperty("visibilityConditions")
    private List<Condition> visibilityConditions;

    /**
     * 执行条件
     * <p>
     * 控制动作何时可执行（不满足条件时按钮禁用）
     */
    @Setter
    @JsonProperty("executionConditions")
    private List<Condition> executionConditions;

    /**
     * 确认提示
     * <p>
     * 执行前显示的确认消息，为空则不显示确认弹窗
     */
    @Setter
    @JsonProperty("confirmMessage")
    private String confirmMessage;

    /**
     * 成功提示
     * <p>
     * 执行成功后显示的消息
     */
    @Setter
    @JsonProperty("successMessage")
    private String successMessage;

    @JsonCreator
    public CardActionConfigDefinition(
            @JsonProperty("id") CardActionId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.CARD_ACTION;
    }

    @Override
    public SchemaId belongTo() {
        return cardTypeId;
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of();
    }

    @Override
    protected CardActionId newId() {
        return CardActionId.generate();
    }

    @Override
    public void validate() {
        super.validate();
        if (actionCategory == null) {
            throw new IllegalArgumentException("动作类别不能为空");
        }
        if (builtIn && builtInActionType == null) {
            throw new IllegalArgumentException("内置动作必须指定内置动作类型");
        }
        if (!builtIn && executionType == null) {
            throw new IllegalArgumentException("自定义动作必须指定执行类型");
        }
    }
}
