package dev.planka.domain.schema.definition.rule;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.BizRuleId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.rule.action.RuleAction;
import dev.planka.domain.schema.definition.rule.trigger.ScheduleConfig;
import dev.planka.domain.stream.StatusId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 业务规则定义
 * <p>
 * 定义一个业务规则，包括触发条件和执行动作。
 * belongTo: 所属的卡片类型ID
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BizRuleDefinition extends AbstractSchemaDefinition<BizRuleId> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.BIZ_RULE;
    }

    /** 所属卡片类型ID */
    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    /** 触发事件类型 */
    @JsonProperty("triggerEvent")
    private TriggerEvent triggerEvent;


    //监听的属性列表，当triggerEvent=ON_FIELD_CHANGE时有有效
    //包括自定义属性，关联属性和内置的卡片描述属性
    private List<String> listenFieldList;

    //目标卡片状态，当triggerEvent=ON_STATUS_MOVE或ON_STATUS_ROLLBACK时有有效
    private StatusId targetStatusId;

    @JsonProperty("condition")
    private Condition condition;

    /** 执行动作列表 */
    @JsonProperty("actions")
    private List<RuleAction> actions;

    /** 定时触发配置（triggerEvent=ON_SCHEDULE时使用） */
    @JsonProperty("scheduleConfig")
    private ScheduleConfig scheduleConfig;

    /** 规则描述 */
    @JsonProperty("description")
    private String description;

    /** 是否启用 */
    @JsonProperty("enabled")
    private boolean enabled = true;

    /** 执行失败重试配置 */
    @JsonProperty("retryConfig")
    private RetryConfig retryConfig;

    @JsonCreator
    public BizRuleDefinition(
            @JsonProperty("id") BizRuleId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.BIZ_RULE;
    }

    @Override
    public SchemaId belongTo() {
        return cardTypeId;  // 业务规则属于某个卡片类型
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of(cardTypeId);
    }

    @Override
    protected BizRuleId newId() {
        return BizRuleId.generate();
    }

    @Override
    public void validate() {
        super.validate();
        if (triggerEvent == null) {
            throw new IllegalArgumentException("触发事件不能为空");
        }
    }

    /**
     * 触发事件枚举
     */
    public enum TriggerEvent {
        /** 创建时 */
        ON_CREATE,
        /** 丢弃时 */
        ON_DISCARD,
        /** 归档时 */
        ON_ARCHIVE,
        /** 还原时 */
        ON_RESTORE,
        /** 状态向前移动时 */
        ON_STATUS_MOVE,
        /** 状态向后回滚时 */
        ON_STATUS_ROLLBACK,
        /** 字段变更时 */
        ON_FIELD_CHANGE,
        /** 定时触发 */
        ON_SCHEDULE,
    }

}
