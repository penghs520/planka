package dev.planka.card.service.validation;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.permission.ConditionEvaluator;
import dev.planka.infra.expression.TextExpressionTemplateResolver;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.fieldconfig.ValidationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 属性值校验器
 * 复用 ConditionEvaluator 和 ExpressionTemplateResolver
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FieldValueValidator {

    private final ConditionEvaluator conditionEvaluator;
    private final TextExpressionTemplateResolver expressionResolver;

    /**
     * 校验卡片的所有属性值
     * 遇到第一个校验失败就停止并返回错误
     *
     * @param card          待校验的卡片
     * @param fieldConfigs  属性配置列表
     * @param operatorId    操作人ID
     * @return 校验结果（第一个错误或成功）
     */
    public ValidationResult validateCard(CardDTO card,
                                         List<FieldConfig> fieldConfigs,
                                         String operatorId) {
        for (FieldConfig fieldConfig : fieldConfigs) {
            if (fieldConfig.getValidationRules() == null || fieldConfig.getValidationRules().isEmpty()) {
                continue;
            }

            // 校验该属性，遇到第一个失败就返回
            String error = validateField(card, fieldConfig, operatorId);
            if (error != null) {
                return ValidationResult.failure(
                    fieldConfig.getFieldId().value(),
                    fieldConfig.getName(),
                    error
                );
            }
        }

        return ValidationResult.success();
    }

    /**
     * 校验单个属性的所有规则
     * 遇到第一个失败就停止
     */
    private String validateField(CardDTO card,
                                 FieldConfig fieldConfig,
                                 String operatorId) {
        for (ValidationRule rule : fieldConfig.getValidationRules()) {
            if (!rule.isEnabled() || rule.isEmpty()) {
                continue;
            }

            try {
                // 使用 ConditionEvaluator 评估条件
                // 条件满足表示校验通过，不满足表示校验失败
                boolean conditionMet = conditionEvaluator.evaluate(rule.getCondition(), card, null);

                if (!conditionMet) {
                    // 校验失败，解析错误消息并立即返回
                    String errorMessage = resolveErrorMessage(rule, card, operatorId);

                    log.debug("属性校验失败: fieldId={}, fieldName={}, error={}",
                        fieldConfig.getFieldId().value(),
                        fieldConfig.getName(),
                        errorMessage);

                    return errorMessage;
                }
            } catch (Exception e) {
                log.error("执行属性校验规则时发生异常: fieldId={}, error={}",
                    fieldConfig.getFieldId().value(), e.getMessage(), e);
                return "校验规则执行失败: " + e.getMessage();
            }
        }

        return null; // 所有规则都通过
    }

    /**
     * 解析错误消息模板
     */
    private String resolveErrorMessage(ValidationRule rule, CardDTO card, String operatorId) {
        if (rule.getErrorMessage() == null || rule.getErrorMessage().template() == null) {
            return "属性值不符合校验规则";
        }

        try {
            CardId memberCardId = operatorId != null ? CardId.of(Long.parseLong(operatorId)) : null;
            return expressionResolver.resolve(rule.getErrorMessage(), card.getId(), memberCardId);
        } catch (Exception e) {
            log.error("解析错误消息模板失败: template={}, error={}",
                rule.getErrorMessage().template(), e.getMessage());
            return rule.getErrorMessage().template();
        }
    }

    /**
     * 校验结果
     */
    public record ValidationResult(
        boolean valid,
        String fieldId,
        String fieldName,
        String errorMessage
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, null, null, null);
        }

        public static ValidationResult failure(String fieldId, String fieldName, String errorMessage) {
            return new ValidationResult(false, fieldId, fieldName, errorMessage);
        }

        public String getFormattedError() {
            if (valid) {
                return null;
            }
            return fieldName + ": " + errorMessage;
        }
    }
}
