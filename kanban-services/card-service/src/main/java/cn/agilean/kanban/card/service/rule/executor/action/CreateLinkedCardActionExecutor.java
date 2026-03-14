package cn.agilean.kanban.card.service.rule.executor.action;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.api.card.request.CreateCardRequest;
import cn.agilean.kanban.api.card.request.UpdateLinkRequest;
import cn.agilean.kanban.card.service.core.CardService;
import cn.agilean.kanban.card.service.core.LinkCardService;
import cn.agilean.kanban.card.service.rule.executor.ActionTargetResolver;
import cn.agilean.kanban.card.service.rule.executor.RuleExecutionContext;
import cn.agilean.kanban.infra.expression.TextExpressionTemplateResolver;
import cn.agilean.kanban.card.service.rule.executor.RuleExecutionResult;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.card.CardTitle;
import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.card.OrgId;
import cn.agilean.kanban.domain.field.*;
import cn.agilean.kanban.domain.schema.definition.action.assignment.DateValue;
import cn.agilean.kanban.domain.schema.definition.action.assignment.EnumValue;
import cn.agilean.kanban.domain.schema.definition.action.assignment.FieldAssignment;
import cn.agilean.kanban.domain.schema.definition.action.assignment.FixedValue;
import cn.agilean.kanban.domain.schema.definition.action.assignment.FixedValueAssignment;
import cn.agilean.kanban.domain.schema.definition.action.assignment.NumberValue;
import cn.agilean.kanban.domain.schema.definition.action.assignment.TextValue;
import cn.agilean.kanban.domain.schema.definition.rule.action.CreateLinkedCardAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建关联卡片动作执行器
 * <p>
 * 在当前卡片上创建一个关联的新卡片。
 */
@Slf4j
@Component
public class CreateLinkedCardActionExecutor extends AbstractRuleActionExecutor<CreateLinkedCardAction> {

    private final CardService cardService;
    private final LinkCardService linkCardService;
    private final TextExpressionTemplateResolver templateResolver;

    public CreateLinkedCardActionExecutor(ActionTargetResolver targetResolver, CardService cardService,
                                           LinkCardService linkCardService,
                                           TextExpressionTemplateResolver templateResolver) {
        super(targetResolver);
        this.cardService = cardService;
        this.linkCardService = linkCardService;
        this.templateResolver = templateResolver;
    }

    @Override
    public String getActionType() {
        return "CREATE_LINKED_CARD";
    }

    @Override
    public RuleExecutionResult.ActionExecutionResult execute(CreateLinkedCardAction action, RuleExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            CardDTO triggerCard = context.getTriggerCard();
            if (triggerCard == null) {
                throw new IllegalArgumentException("触发卡片不能为空");
            }

            CardId newCardId = executeOnCard(action, triggerCard, context);

            long duration = System.currentTimeMillis() - startTime;
            return RuleExecutionResult.ActionExecutionResult.success(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    newCardId != null ? List.of(newCardId) : List.of()
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("创建关联卡片动作执行失败: error={}", e.getMessage(), e);
            return RuleExecutionResult.ActionExecutionResult.failed(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    e.getMessage()
            );
        }
    }

    @Override
    protected CardId executeOnCard(CreateLinkedCardAction action, CardDTO targetCard, RuleExecutionContext context) {
        // 解析标题模板
        CardId memberCardId = context.getOperatorId() != null ? CardId.of(context.getOperatorId()) : null;
        String title = templateResolver.resolve(action.getTitleTemplate(), context.getCardId(), memberCardId);

        // 确定卡片类型
        CardTypeId cardTypeId = action.getCardTypeId();
        if (cardTypeId == null) {
            throw new IllegalArgumentException("目标卡片类型不能为空");
        }

        // 处理字段赋值
        Map<String, FieldValue<?>> fieldValues = processFieldAssignments(action.getFieldAssignments(), context);

        // 构建创建请求
        CreateCardRequest createRequest = new CreateCardRequest(
                OrgId.of(context.getOrgId()),
                cardTypeId,
                CardTitle.pure(title),
                null,  // description
                fieldValues.isEmpty() ? null : fieldValues
        );

        Result<CardId> createResult = cardService.create(createRequest, CardId.of(context.getOperatorId()));

        if (!createResult.isSuccess()) {
            throw new RuntimeException("创建卡片失败: " + createResult.getMessage());
        }

        CardId newCardId = createResult.getData();

        // 创建关联关系
        String linkFieldId = action.getLinkFieldId();
        if (linkFieldId != null && !linkFieldId.isEmpty()) {
            UpdateLinkRequest linkRequest = UpdateLinkRequest.builder()
                    .cardId(targetCard.getId().value())
                    .linkFieldId(linkFieldId)
                    .targetCardIds(List.of(newCardId.value()))
                    .build();

            Result<Void> linkResult = linkCardService.updateLink(
                    linkRequest,
                    context.getOrgId(),
                    context.getOperatorId(),
                    context.getSourceIp()
            );

            if (!linkResult.isSuccess()) {
                log.warn("创建关联关系失败: sourceCardId={}, targetCardId={}, error={}",
                        targetCard.getId(), newCardId, linkResult.getMessage());
            }
        }

        return newCardId;
    }

    private Map<String, FieldValue<?>> processFieldAssignments(List<FieldAssignment> assignments,
                                                                RuleExecutionContext context) {
        Map<String, FieldValue<?>> fieldValues = new HashMap<>();
        if (assignments == null || assignments.isEmpty()) {
            return fieldValues;
        }

        for (FieldAssignment assignment : assignments) {
            if (assignment instanceof FixedValueAssignment fixedValue) {
                FieldValue<?> fieldValue = convertToFieldValue(assignment.getFieldId(), fixedValue.getValue());
                if (fieldValue != null) {
                    fieldValues.put(assignment.getFieldId(), fieldValue);
                }
            }
        }

        return fieldValues;
    }

    private FieldValue<?> convertToFieldValue(String fieldId, FixedValue value) {
        if (value instanceof TextValue textValue) {
            return new TextFieldValue(fieldId, textValue.getText());
        } else if (value instanceof NumberValue numberValue) {
            return new NumberFieldValue(fieldId,
                    numberValue.getNumber() != null ? numberValue.getNumber().doubleValue() : null);
        } else if (value instanceof DateValue dateValue) {
            long timestamp = resolveDateValue(dateValue);
            return new DateFieldValue(fieldId, timestamp);
        } else if (value instanceof EnumValue enumValue) {
            return new EnumFieldValue(fieldId, enumValue.getEnumValueIds());
        }
        return null;
    }

    private long resolveDateValue(DateValue dateValue) {
        if (dateValue.getMode() == DateValue.DateMode.ABSOLUTE && dateValue.getAbsoluteDate() != null) {
            return dateValue.getAbsoluteDate().atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
        } else if (dateValue.getOffsetDays() != null) {
            return System.currentTimeMillis() + dateValue.getOffsetDays() * 24L * 60 * 60 * 1000;
        }
        return System.currentTimeMillis();
    }
}
