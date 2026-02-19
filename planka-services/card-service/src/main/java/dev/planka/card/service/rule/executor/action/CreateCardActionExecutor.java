package dev.planka.card.service.rule.executor.action;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.CreateCardRequest;
import dev.planka.card.service.core.CardService;
import dev.planka.card.service.rule.executor.ActionTargetResolver;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.domain.field.*;
import dev.planka.infra.expression.TextExpressionTemplateResolver;
import dev.planka.card.service.rule.executor.RuleExecutionResult;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.card.OrgId;
import dev.planka.domain.schema.definition.action.assignment.DateValue;
import dev.planka.domain.schema.definition.action.assignment.EnumValue;
import dev.planka.domain.schema.definition.action.assignment.FieldAssignment;
import dev.planka.domain.schema.definition.action.assignment.FixedValue;
import dev.planka.domain.schema.definition.action.assignment.FixedValueAssignment;
import dev.planka.domain.schema.definition.action.assignment.NumberValue;
import dev.planka.domain.schema.definition.action.assignment.TextValue;
import dev.planka.domain.schema.definition.rule.action.CreateCardAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建卡片动作执行器
 */
@Slf4j
@Component
public class CreateCardActionExecutor extends AbstractRuleActionExecutor<CreateCardAction> {

    private final CardService cardService;
    private final TextExpressionTemplateResolver templateResolver;

    public CreateCardActionExecutor(ActionTargetResolver targetResolver, CardService cardService,
                                     TextExpressionTemplateResolver templateResolver) {
        super(targetResolver);
        this.cardService = cardService;
        this.templateResolver = templateResolver;
    }

    @Override
    public String getActionType() {
        return "CREATE_CARD";
    }

    @Override
    public RuleExecutionResult.ActionExecutionResult execute(CreateCardAction action, RuleExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // 解析标题模板
            CardId memberCardId = context.getOperatorId() != null ? CardId.of(Long.parseLong(context.getOperatorId())) : null;
            String title = templateResolver.resolve(action.getTitleTemplate(), context.getCardId(), memberCardId);

            // 确定卡片类型
            CardTypeId cardTypeId = action.getCardTypeId() != null
                    ? action.getCardTypeId()
                    : context.getCardTypeId();

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

            Result<CardId> result = cardService.create(createRequest, CardId.of(context.getOperatorId()));

            if (!result.isSuccess()) {
                throw new RuntimeException("创建卡片失败: " + result.getMessage());
            }

            long duration = System.currentTimeMillis() - startTime;
            return RuleExecutionResult.ActionExecutionResult.success(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    List.of(result.getData())
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("创建卡片动作执行失败: error={}", e.getMessage(), e);
            return RuleExecutionResult.ActionExecutionResult.failed(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    e.getMessage()
            );
        }
    }

    @Override
    protected CardId executeOnCard(CreateCardAction action, CardDTO targetCard, RuleExecutionContext context) {
        // 此方法不使用，创建卡片不依赖目标卡片
        return null;
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
            // 其他类型的赋值可以扩展
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
