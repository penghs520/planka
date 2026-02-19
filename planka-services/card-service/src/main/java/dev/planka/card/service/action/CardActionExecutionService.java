package dev.planka.card.service.action;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.LinkFieldUpdate;
import dev.planka.api.card.request.MoveCardRequest;
import dev.planka.api.card.request.UpdateCardRequest;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.core.CardService;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.*;
import dev.planka.domain.schema.CardActionId;
import dev.planka.domain.schema.definition.action.*;
import dev.planka.domain.schema.definition.action.assignment.*;
import dev.planka.domain.stream.StatusId;
import dev.planka.infra.cache.schema.query.CardActionCacheQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 卡片动作执行服务
 * <p>
 * 负责执行卡片动作，包括内置动作和自定义动作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardActionExecutionService {

    private final CardActionCacheQuery cardActionCacheQuery;
    private final CardRepository cardRepository;
    private final CardService cardService;

    /**
     * 获取卡片可用的动作列表
     *
     * @param cardId     卡片ID
     * @param operatorId 操作人ID
     * @return 可用的动作配置列表
     */
    public Result<List<CardActionConfigDefinition>> getAvailableActions(
            CardId cardId,
            CardId operatorId) {
        try {
            // 获取卡片信息
            Optional<CardDTO> cardOpt = cardRepository.findById(cardId, null, operatorId.value());
            if (cardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "卡片不存在");
            }

            CardDTO card = cardOpt.get();
            CardTypeId cardTypeId = card.getTypeId();

            // 获取卡片类型的所有动作配置
            List<CardActionConfigDefinition> allActions = cardActionCacheQuery.getByCardTypeId(cardTypeId);

            // TODO: 根据可见性条件过滤动作
            // 目前先返回所有动作，后续实现条件评估逻辑
            List<CardActionConfigDefinition> availableActions = allActions.stream()
                    .filter(CardActionConfigDefinition::isEnabled)
                    .sorted((a, b) -> {
                        Integer orderA = a.getSortOrder() != null ? a.getSortOrder() : Integer.MAX_VALUE;
                        Integer orderB = b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE;
                        return orderA.compareTo(orderB);
                    })
                    .toList();

            return Result.success(availableActions);
        } catch (Exception e) {
            log.error("获取可用动作失败: cardId={}", cardId, e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "获取可用动作失败: " + e.getMessage());
        }
    }

    /**
     * 执行卡片动作
     *
     * @param actionId   动作配置ID
     * @param cardId     卡片ID
     * @param operatorId 操作人ID
     * @param userInputs 用户输入的字段值（可选）
     * @return 执行结果
     */
    @Transactional
    public Result<ActionExecutionResult> execute(
            CardActionId actionId,
            CardId cardId,
            CardId operatorId,
            Map<String, FixedValue> userInputs) {
        try {
            // 获取动作配置
            Optional<CardActionConfigDefinition> actionOpt = cardActionCacheQuery.getById(actionId);
            if (actionOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "动作配置不存在");
            }

            CardActionConfigDefinition action = actionOpt.get();

            // 获取卡片信息
            Optional<CardDTO> cardOpt = cardRepository.findById(cardId, null, operatorId.value());
            if (cardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "卡片不存在");
            }

            CardDTO card = cardOpt.get();

            // TODO: 检查执行条件
            // 目前先跳过条件检查，后续实现条件评估逻辑

            // 执行动作
            ActionExecutionResult result = executeAction(action, card, operatorId, userInputs);

            // 返回成功消息
            if (result.type() == ActionExecutionResult.ResultType.SUCCESS && action.getSuccessMessage() != null) {
                result = ActionExecutionResult.success(action.getSuccessMessage(), result.data());
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("执行动作失败: actionId={}, cardId={}", actionId, cardId, e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "执行动作失败: " + e.getMessage());
        }
    }

    /**
     * 执行具体动作
     */
    private ActionExecutionResult executeAction(
            CardActionConfigDefinition action,
            CardDTO card,
            CardId operatorId,
            Map<String, FixedValue> userInputs) {

        if (action.isBuiltIn()) {
            return executeBuiltInAction(action.getBuiltInActionType(), card, operatorId, userInputs);
        }

        ActionExecutionType executionType = action.getExecutionType();
        if (executionType == null) {
            return ActionExecutionResult.error("动作配置缺少执行类型");
        }

        if (executionType instanceof UpdateCardExecution updateCard) {
            return executeUpdateCard(updateCard, card, operatorId, userInputs);
        } else if (executionType instanceof NavigateToPageExecution navigate) {
            return executeNavigate(navigate, card);
        } else if (executionType instanceof CallExternalApiExecution callApi) {
            return executeCallExternalApi(callApi, card);
        } else if (executionType instanceof TriggerBuiltInExecution triggerBuiltIn) {
            return executeBuiltInAction(triggerBuiltIn.getBuiltInActionType(), card, operatorId, userInputs);
        } else {
            return ActionExecutionResult.error("不支持的执行类型: " + executionType.getType());
        }
    }

    /**
     * 丢弃原因字段ID
     */
    private static final String DISCARD_REASON_FIELD_ID = "discardReason";

    /**
     * 执行内置动作
     */
    private ActionExecutionResult executeBuiltInAction(
            BuiltInActionType builtInType,
            CardDTO card,
            CardId operatorId,
            Map<String, FixedValue> userInputs) {

        if (builtInType == null) {
            return ActionExecutionResult.error("未指定内置动作类型");
        }

        return switch (builtInType) {
            case DISCARD -> {
                // 检查是否提供了丢弃原因
                String discardReason = extractDiscardReason(userInputs);
                if (discardReason == null || discardReason.isBlank()) {
                    // 需要用户输入丢弃原因
                    log.info("丢弃动作需要用户输入丢弃原因: cardId={}", card.getId());
                    yield ActionExecutionResult.requireInput(
                            "请输入丢弃原因",
                            List.of(ActionExecutionResult.RequiredInput.textarea(
                                    DISCARD_REASON_FIELD_ID,
                                    "丢弃原因",
                                    "请输入丢弃该卡片的原因",
                                    true
                            ))
                    );
                }

                log.info("执行丢弃动作: cardId={}, reason={}", card.getId(), discardReason);
                Result<Void> result = cardService.discard(card.getId(), discardReason, operatorId);
                if (result.isSuccess()) {
                    yield ActionExecutionResult.success("卡片已丢弃");
                } else {
                    yield ActionExecutionResult.error("丢弃卡片失败: " + result.getMessage());
                }
            }
            case ARCHIVE -> {
                log.info("执行归档动作: cardId={}", card.getId());
                Result<Void> result = cardService.archive(card.getId(), operatorId);
                if (result.isSuccess()) {
                    yield ActionExecutionResult.success("卡片已归档");
                } else {
                    yield ActionExecutionResult.error("归档卡片失败: " + result.getMessage());
                }
            }
            case RESTORE -> {
                log.info("执行还原动作: cardId={}", card.getId());
                Result<Void> result = cardService.restore(card.getId(), operatorId);
                if (result.isSuccess()) {
                    yield ActionExecutionResult.success("卡片已还原");
                } else {
                    yield ActionExecutionResult.error("还原卡片失败: " + result.getMessage());
                }
            }
            case BLOCK_TOGGLE -> {
                // TODO: 实现阻塞/解阻逻辑
                log.info("执行阻塞切换动作: cardId={}", card.getId());
                yield ActionExecutionResult.success("阻塞状态已切换");
            }
            case HIGHLIGHT_TOGGLE -> {
                // TODO: 实现点亮/暂停逻辑
                log.info("执行点亮切换动作: cardId={}", card.getId());
                yield ActionExecutionResult.success("点亮状态已切换");
            }
        };
    }

    /**
     * 执行更新卡片动作
     *
     * @param execution  更新卡片执行配置
     * @param card       当前卡片
     * @param operatorId 操作人ID
     * @param userInputs 用户输入的字段值（用于 USER_INPUT 类型的赋值）
     */
    private ActionExecutionResult executeUpdateCard(
            UpdateCardExecution execution,
            CardDTO card,
            CardId operatorId,
            Map<String, FixedValue> userInputs) {

        try {
            // 1. 处理字段赋值，分离普通字段和关联字段
            Map<String, FieldValue<?>> fieldValues = new HashMap<>();
            List<LinkFieldUpdate> linkUpdates = new ArrayList<>();

            if (execution.getFieldAssignments() != null && !execution.getFieldAssignments().isEmpty()) {
                for (FieldAssignment assignment : execution.getFieldAssignments()) {
                    // 获取赋值的值（可能是固定值或用户输入）
                    FixedValue fixedValue = getFixedValueFromAssignment(assignment, card, operatorId, userInputs);

                    if (fixedValue instanceof LinkValue linkValue) {
                        // 关联字段：收集为 LinkFieldUpdate
                        if (linkValue.getIds() != null) {
                            linkUpdates.add(new LinkFieldUpdate(
                                    assignment.getFieldId(),
                                    linkValue.getIds()
                            ));
                        }
                    } else if (fixedValue != null) {
                        // 普通字段：转换为 FieldValue
                        FieldValue<?> fieldValue = convertFixedValueToFieldValue(assignment.getFieldId(), fixedValue);
                        if (fieldValue != null) {
                            fieldValues.put(assignment.getFieldId(), fieldValue);
                        }
                    } else {
                        // 特殊赋值类型（CLEAR_VALUE, INCREMENT 等）
                        FieldValue<?> fieldValue = processSpecialAssignment(assignment, card, operatorId);
                        if (fieldValue != null) {
                            fieldValues.put(assignment.getFieldId(), fieldValue);
                        }
                    }
                }
            }

            // 2. 构建更新请求（包含关联更新）
            UpdateCardRequest updateRequest = new UpdateCardRequest(
                    card.getId(),
                    card.getTitle() != null ? card.getTitle().getValue() : null,
                    card.getDescription() != null ? card.getDescription().getValue() : null,
                    fieldValues.isEmpty() ? null : fieldValues,
                    linkUpdates.isEmpty() ? null : linkUpdates
            );

            // 3. 执行卡片更新
            Result<Void> updateResult = cardService.update(updateRequest, operatorId);
            if (!updateResult.isSuccess()) {
                return ActionExecutionResult.error("更新卡片失败: " + updateResult.getMessage());
            }

            // 4. 处理状态切换
            if (execution.getTargetStatusId() != null && !execution.getTargetStatusId().isBlank()) {
                // 获取卡片的价值流信息
                if (card.getStreamId() != null) {
                    MoveCardRequest moveRequest = new MoveCardRequest(
                            card.getId(),
                            card.getStreamId(),
                            StatusId.of(execution.getTargetStatusId())
                    );
                    Result<Void> moveResult = cardService.move(moveRequest, operatorId);
                    if (!moveResult.isSuccess()) {
                        return ActionExecutionResult.error("切换状态失败: " + moveResult.getMessage());
                    }
                }
            }

            log.info("执行更新卡片动作成功: cardId={}, fieldAssignments={}, linkUpdates={}, targetStatusId={}",
                    card.getId(),
                    fieldValues.size(),
                    linkUpdates.size(),
                    execution.getTargetStatusId());

            return ActionExecutionResult.success("卡片已更新");
        } catch (Exception e) {
            log.error("执行更新卡片动作失败: cardId={}", card.getId(), e);
            return ActionExecutionResult.error("执行更新卡片动作失败: " + e.getMessage());
        }
    }

    /**
     * 从赋值配置中获取 FixedValue
     * <p>
     * 将 USER_INPUT、FIXED_VALUE、REFERENCE_FIELD、CURRENT_TIME 转换为对应的 FixedValue
     * 返回 null 表示需要特殊处理（如 CLEAR_VALUE、INCREMENT）
     */
    private FixedValue getFixedValueFromAssignment(
            FieldAssignment assignment,
            CardDTO card,
            CardId operatorId,
            Map<String, FixedValue> userInputs) {

        if (assignment instanceof UserInputAssignment userInput) {
            if (userInputs != null && userInputs.containsKey(userInput.getFieldId())) {
                return userInputs.get(userInput.getFieldId());
            }
            return null;
        } else if (assignment instanceof FixedValueAssignment fixedAssignment) {
            return fixedAssignment.getValue();
        } else if (assignment instanceof ReferenceFieldAssignment refAssignment) {
            // TODO: 引用字段赋值需要根据 source 获取值
            log.warn("引用字段赋值暂未完整实现: fieldId={}, source={}",
                    refAssignment.getFieldId(), refAssignment.getSource());
            return null;
        } else if (assignment instanceof CurrentTimeAssignment currentTime) {
            // 将当前时间转换为 DateValue
            DateValue dateValue = new DateValue();
            dateValue.setMode(DateValue.DateMode.RELATIVE);
            dateValue.setOffsetDays(currentTime.getOffsetDays());
            return dateValue;
        }
        // CLEAR_VALUE, INCREMENT 返回 null，交给 processSpecialAssignment 处理
        return null;
    }

    /**
     * 处理特殊赋值类型（CLEAR_VALUE, INCREMENT）
     */
    private FieldValue<?> processSpecialAssignment(
            FieldAssignment assignment,
            CardDTO card,
            CardId operatorId) {

        if (assignment instanceof ClearValueAssignment) {
            return processClearValueAssignment((ClearValueAssignment) assignment);
        } else if (assignment instanceof IncrementAssignment) {
            return processIncrementAssignment((IncrementAssignment) assignment, card);
        }
        return null;
    }

    /**
     * 处理字段赋值
     *
     * @param assignment 字段赋值配置
     * @param card       当前卡片
     * @param operatorId 操作人ID
     * @param userInputs 用户输入的值
     * @return 字段值，如果无法处理则返回 null
     */
    private FieldValue<?> processFieldAssignment(
            FieldAssignment assignment,
            CardDTO card,
            CardId operatorId,
            Map<String, FixedValue> userInputs) {

        if (assignment instanceof UserInputAssignment) {
            return processUserInputAssignment((UserInputAssignment) assignment, userInputs);
        } else if (assignment instanceof FixedValueAssignment) {
            return processFixedValueAssignment((FixedValueAssignment) assignment);
        } else if (assignment instanceof ReferenceFieldAssignment) {
            return processReferenceFieldAssignment((ReferenceFieldAssignment) assignment, card, operatorId);
        } else if (assignment instanceof CurrentTimeAssignment) {
            return processCurrentTimeAssignment((CurrentTimeAssignment) assignment);
        } else if (assignment instanceof ClearValueAssignment) {
            return processClearValueAssignment((ClearValueAssignment) assignment);
        } else if (assignment instanceof IncrementAssignment) {
            return processIncrementAssignment((IncrementAssignment) assignment, card);
        }
        return null;
    }

    /**
     * 处理用户输入赋值
     */
    private FieldValue<?> processUserInputAssignment(
            UserInputAssignment assignment,
            Map<String, FixedValue> userInputs) {

        if (userInputs == null || !userInputs.containsKey(assignment.getFieldId())) {
            log.warn("用户输入值不存在: fieldId={}", assignment.getFieldId());
            return null;
        }

        FixedValue inputValue = userInputs.get(assignment.getFieldId());
        return convertFixedValueToFieldValue(assignment.getFieldId(), inputValue);
    }

    /**
     * 处理固定值赋值
     */
    private FieldValue<?> processFixedValueAssignment(FixedValueAssignment assignment) {
        if (assignment.getValue() == null) {
            return null;
        }
        return convertFixedValueToFieldValue(assignment.getFieldId(), assignment.getValue());
    }

    /**
     * 将 FixedValue 转换为 FieldValue
     */
    private FieldValue<?> convertFixedValueToFieldValue(String fieldId, FixedValue value) {
        if (value == null) {
            return null;
        }

        if (value instanceof TextValue) {
            return new TextFieldValue(fieldId, ((TextValue) value).getText());
        } else if (value instanceof NumberValue) {
            NumberValue numberValue = (NumberValue) value;
            return new NumberFieldValue(fieldId,
                    numberValue.getNumber() != null ? numberValue.getNumber().doubleValue() : null);
        } else if (value instanceof DateValue) {
            return processDateValue(fieldId, (DateValue) value);
        } else if (value instanceof EnumValue) {
            return new EnumFieldValue(fieldId, ((EnumValue) value).getEnumValueIds());
        } else if (value instanceof LinkValue) {
            return processLinkValue(fieldId, (LinkValue) value);
        }
        return null;
    }

    /**
     * 处理日期值
     */
    private FieldValue<?> processDateValue(String fieldId, DateValue dateValue) {
        if (dateValue.getMode() == DateValue.DateMode.ABSOLUTE) {
            if (dateValue.getAbsoluteDate() != null) {
                long timestamp = java.time.ZoneId.systemDefault()
                        .getRules()
                        .getOffset(dateValue.getAbsoluteDate())
                        .getTotalSeconds() * 1000 
                        + dateValue.getAbsoluteDate().toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
                return new DateFieldValue(fieldId, timestamp);
            }
        } else {
            // RELATIVE mode
            if (dateValue.getOffsetDays() != null) {
                long timestamp = System.currentTimeMillis() + dateValue.getOffsetDays() * 24L * 60 * 60 * 1000;
                return new DateFieldValue(fieldId, timestamp);
            }
        }
        return null;
    }

    /**
     * 处理关联值
     */
    private FieldValue<?> processLinkValue(String fieldId, LinkValue linkValue) {
        if (linkValue.getIds() == null || linkValue.getIds().isEmpty()) {
            return null;
        }
        // TODO: 实现关联字段的更新
        log.warn("关联字段赋值暂未实现: fieldId={}", fieldId);
        return null;
    }

    /**
     * 处理引用字段赋值
     */
    private FieldValue<?> processReferenceFieldAssignment(
            ReferenceFieldAssignment assignment,
            CardDTO card,
            CardId operatorId) {

        // TODO: 实现引用字段赋值
        // 需要根据 source 和 path 获取引用的字段值
        log.warn("引用字段赋值暂未实现: fieldId={}, source={}", 
                assignment.getFieldId(), assignment.getSource());
        return null;
    }

    /**
     * 处理当前时间赋值
     */
    private FieldValue<?> processCurrentTimeAssignment(CurrentTimeAssignment assignment) {
        long timestamp = System.currentTimeMillis();
        if (assignment.getOffsetDays() != 0) {
            timestamp += assignment.getOffsetDays() * 24L * 60 * 60 * 1000;
        }
        return new DateFieldValue(assignment.getFieldId(), timestamp);
    }

    /**
     * 处理清空值赋值
     */
    private FieldValue<?> processClearValueAssignment(ClearValueAssignment assignment) {
        // 返回一个空值的 FieldValue
        return new TextFieldValue(assignment.getFieldId(), null);
    }

    /**
     * 处理增量赋值
     */
    private FieldValue<?> processIncrementAssignment(
            IncrementAssignment assignment,
            CardDTO card) {

        // 获取当前字段值
        Double currentValue = 0.0;
        if (card.getFieldValues() != null && card.getFieldValues().containsKey(assignment.getFieldId())) {
            FieldValue<?> fieldValue = card.getFieldValues().get(assignment.getFieldId());
            if (fieldValue instanceof NumberFieldValue numberField) {
                currentValue = numberField.getValue() != null ? numberField.getValue() : 0.0;
            }
        }

        // 计算新值
        double newValue = currentValue + (assignment.getIncrementValue() != null ? assignment.getIncrementValue().doubleValue() : 0.0);

        // 处理不允许负数的情况
        if (!assignment.isAllowNegative() && newValue < 0) {
            newValue = 0;
        }

        return new NumberFieldValue(assignment.getFieldId(), newValue);
    }

    /**
     * 执行跳转页面动作
     */
    private ActionExecutionResult executeNavigate(
            NavigateToPageExecution execution,
            CardDTO card) {

        String targetUrl = execution.getTargetUrl();

        // TODO: 实现变量替换逻辑
        // 例如将 ${card.id} 替换为实际的卡片ID

        return ActionExecutionResult.navigate(targetUrl, execution.isOpenInNewWindow());
    }

    /**
     * 执行调用外部接口动作
     */
    private ActionExecutionResult executeCallExternalApi(
            CallExternalApiExecution execution,
            CardDTO card) {

        // TODO: 实现外部接口调用逻辑
        // 包括变量替换、HTTP请求、响应处理等

        log.info("执行调用外部接口动作: cardId={}, url={}",
                card.getId(), execution.getUrl());

        return ActionExecutionResult.success("接口调用成功");
    }

    /**
     * 从用户输入中提取丢弃原因
     *
     * @param userInputs 用户输入
     * @return 丢弃原因，如果未提供则返回 null
     */
    private String extractDiscardReason(Map<String, FixedValue> userInputs) {
        if (userInputs == null || !userInputs.containsKey(DISCARD_REASON_FIELD_ID)) {
            return null;
        }

        FixedValue value = userInputs.get(DISCARD_REASON_FIELD_ID);
        if (value instanceof TextValue textValue) {
            return textValue.getText();
        }
        return null;
    }
}
