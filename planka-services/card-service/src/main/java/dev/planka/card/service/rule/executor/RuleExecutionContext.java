package dev.planka.card.service.rule.executor;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.BizRuleId;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 规则执行上下文
 * <p>
 * 包含规则执行过程中所需的所有信息。
 */
@Getter
@Setter
@Builder
public class RuleExecutionContext {

    /** 追踪ID，用于日志追踪 */
    @Builder.Default
    private String traceId = UUID.randomUUID().toString().replace("-", "");

    /** 触发卡片 */
    private CardDTO triggerCard;

    /** 触发卡片ID */
    private CardId cardId;

    /** 卡片类型ID */
    private CardTypeId cardTypeId;

    /** 操作人ID */
    private String operatorId;

    /** 组织ID */
    private String orgId;

    /** 触发事件类型 */
    private BizRuleDefinition.TriggerEvent triggerEvent;

    /** 当前正在执行的规则 */
    private BizRuleDefinition currentRule;

    /** 当前正在执行的规则ID */
    private BizRuleId currentRuleId;

    /** 是否由规则触发（用于循环检测） */
    @Builder.Default
    private boolean triggeredByRule = false;

    /** 执行开始时间（毫秒） */
    @Builder.Default
    private long startTime = System.currentTimeMillis();

    /** 变量缓存（用于表达式解析） */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    /** 来源IP */
    private String sourceIp;

    /** 变更的字段列表（ON_FIELD_CHANGE事件使用） */
    private java.util.List<String> changedFieldIds;

    /** 原始卡片数据（用于比较变更前后的值） */
    private CardDTO originalCard;

    /**
     * 获取执行耗时（毫秒）
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 设置变量值
     */
    public void setVariable(String key, Object value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put(key, value);
    }

    /**
     * 获取变量值
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        if (variables == null) {
            return null;
        }
        return (T) variables.get(key);
    }

    /**
     * 创建用于规则触发的子上下文
     */
    public RuleExecutionContext createRuleTriggeredContext() {
        return RuleExecutionContext.builder()
                .traceId(this.traceId)
                .triggerCard(this.triggerCard)
                .cardId(this.cardId)
                .cardTypeId(this.cardTypeId)
                .operatorId(this.operatorId)
                .orgId(this.orgId)
                .triggerEvent(this.triggerEvent)
                .triggeredByRule(true)  // 标记为规则触发
                .sourceIp(this.sourceIp)
                .variables(new HashMap<>(this.variables))
                .build();
    }

    /**
     * 创建静态工厂方法
     */
    public static RuleExecutionContext forCard(CardDTO card, String operatorId,
                                                BizRuleDefinition.TriggerEvent event) {
        return RuleExecutionContext.builder()
                .triggerCard(card)
                .cardId(card.getId())
                .cardTypeId(card.getTypeId())
                .operatorId(operatorId)
                .orgId(card.getOrgId() != null ? card.getOrgId().value() : null)
                .triggerEvent(event)
                .build();
    }
}
