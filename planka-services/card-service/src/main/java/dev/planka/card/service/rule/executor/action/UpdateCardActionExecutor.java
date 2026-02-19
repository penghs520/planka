package dev.planka.card.service.rule.executor.action;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.LinkFieldUpdate;
import dev.planka.api.card.request.UpdateCardRequest;
import dev.planka.card.service.core.CardService;
import dev.planka.card.service.rule.executor.ActionTargetResolver;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.field.*;
import dev.planka.domain.schema.definition.action.assignment.*;
import dev.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import dev.planka.domain.schema.definition.rule.action.UpdateCardAction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 修改卡片属性动作执行器
 */
@Component
public class UpdateCardActionExecutor extends AbstractRuleActionExecutor<UpdateCardAction> {

    private final CardService cardService;

    public UpdateCardActionExecutor(ActionTargetResolver targetResolver, CardService cardService) {
        super(targetResolver);
        this.cardService = cardService;
    }

    @Override
    public String getActionType() {
        return "UPDATE_CARD";
    }

    @Override
    protected ActionTargetSelector getTargetSelector(UpdateCardAction action) {
        return action.getTarget();
    }

    @Override
    protected CardId executeOnCard(UpdateCardAction action, CardDTO targetCard, RuleExecutionContext context) {
        if (action.getFieldAssignments() == null || action.getFieldAssignments().isEmpty()) {
            return null;
        }

        Map<String, FieldValue<?>> fieldValues = new HashMap<>();
        List<LinkFieldUpdate> linkUpdates = new ArrayList<>();

        for (FieldAssignment assignment : action.getFieldAssignments()) {
            processAssignment(assignment, targetCard, context, fieldValues, linkUpdates);
        }

        if (fieldValues.isEmpty() && linkUpdates.isEmpty()) {
            return null;
        }

        UpdateCardRequest updateRequest = new UpdateCardRequest(
                targetCard.getId(),
                null,
                null,
                fieldValues.isEmpty() ? null : fieldValues,
                linkUpdates.isEmpty() ? null : linkUpdates
        );

        Result<Void> result = cardService.update(updateRequest, CardId.of(context.getOperatorId()));

        if (!result.isSuccess()) {
            throw new RuntimeException("更新卡片失败: " + result.getMessage());
        }

        return targetCard.getId();
    }

    private void processAssignment(FieldAssignment assignment, CardDTO card, RuleExecutionContext context,
                                    Map<String, FieldValue<?>> fieldValues, List<LinkFieldUpdate> linkUpdates) {
        String fieldId = assignment.getFieldId();

        if (assignment instanceof FixedValueAssignment fixedValue) {
            processFixedValue(fieldId, fixedValue.getValue(), fieldValues, linkUpdates);
        } else if (assignment instanceof CurrentTimeAssignment currentTime) {
            long timestamp = System.currentTimeMillis();
            if (currentTime.getOffsetDays() != 0) {
                timestamp += currentTime.getOffsetDays() * 24L * 60 * 60 * 1000;
            }
            fieldValues.put(fieldId, new DateFieldValue(fieldId, timestamp));
        } else if (assignment instanceof ClearValueAssignment) {
            fieldValues.put(fieldId, new TextFieldValue(fieldId, null));
        } else if (assignment instanceof IncrementAssignment increment) {
            Double currentValue = getCurrentNumberValue(card, fieldId);
            double newValue = currentValue + (increment.getIncrementValue() != null
                    ? increment.getIncrementValue().doubleValue() : 0.0);
            if (!increment.isAllowNegative() && newValue < 0) {
                newValue = 0;
            }
            fieldValues.put(fieldId, new NumberFieldValue(fieldId, newValue));
        } else if (assignment instanceof ReferenceFieldAssignment refAssignment) {
            // 引用字段赋值，从当前卡片或关联卡片获取值
            FieldValue<?> refValue = resolveReferenceValue(refAssignment, card, context);
            if (refValue != null) {
                fieldValues.put(fieldId, refValue);
            }
        }
    }

    private void processFixedValue(String fieldId, FixedValue value,
                                    Map<String, FieldValue<?>> fieldValues, List<LinkFieldUpdate> linkUpdates) {
        if (value instanceof TextValue textValue) {
            fieldValues.put(fieldId, new TextFieldValue(fieldId, textValue.getText()));
        } else if (value instanceof NumberValue numberValue) {
            fieldValues.put(fieldId, new NumberFieldValue(fieldId,
                    numberValue.getNumber() != null ? numberValue.getNumber().doubleValue() : null));
        } else if (value instanceof DateValue dateValue) {
            long timestamp = resolveDateValue(dateValue);
            fieldValues.put(fieldId, new DateFieldValue(fieldId, timestamp));
        } else if (value instanceof EnumValue enumValue) {
            fieldValues.put(fieldId, new EnumFieldValue(fieldId, enumValue.getEnumValueIds()));
        } else if (value instanceof LinkValue linkValue) {
            if (linkValue.getIds() != null) {
                linkUpdates.add(new LinkFieldUpdate(fieldId, linkValue.getIds()));
            }
        }
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

    private Double getCurrentNumberValue(CardDTO card, String fieldId) {
        if (card.getFieldValues() == null || !card.getFieldValues().containsKey(fieldId)) {
            return 0.0;
        }
        FieldValue<?> fieldValue = card.getFieldValues().get(fieldId);
        if (fieldValue instanceof NumberFieldValue numberField) {
            return numberField.getValue() != null ? numberField.getValue() : 0.0;
        }
        return 0.0;
    }

    private FieldValue<?> resolveReferenceValue(ReferenceFieldAssignment refAssignment,
                                                 CardDTO card, RuleExecutionContext context) {
        // 目前只支持从当前卡片获取引用值
        String sourceFieldId = refAssignment.getSourceFieldId();
        if (sourceFieldId != null && card.getFieldValues() != null) {
            FieldValue<?> sourceValue = card.getFieldValues().get(sourceFieldId);
            if (sourceValue != null) {
                // 创建目标字段的值副本
                return copyFieldValue(refAssignment.getFieldId(), sourceValue);
            }
        }
        return null;
    }

    private FieldValue<?> copyFieldValue(String targetFieldId, FieldValue<?> source) {
        if (source instanceof TextFieldValue textValue) {
            return new TextFieldValue(targetFieldId, textValue.getValue());
        } else if (source instanceof NumberFieldValue numberValue) {
            return new NumberFieldValue(targetFieldId, numberValue.getValue());
        } else if (source instanceof DateFieldValue dateValue) {
            return new DateFieldValue(targetFieldId, dateValue.getValue());
        } else if (source instanceof EnumFieldValue enumValue) {
            return new EnumFieldValue(targetFieldId, enumValue.getValue());
        }
        return null;
    }
}
