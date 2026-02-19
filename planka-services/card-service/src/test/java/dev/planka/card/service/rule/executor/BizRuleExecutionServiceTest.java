package dev.planka.card.service.rule.executor;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.permission.ConditionEvaluator;
import dev.planka.card.service.rule.log.RuleExecutionLogService;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.BizRuleId;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.domain.schema.definition.rule.RetryConfig;
import dev.planka.domain.schema.definition.rule.RuleExecutionLog;
import dev.planka.domain.schema.definition.rule.action.ActionTargetSelector;
import dev.planka.domain.schema.definition.rule.action.DiscardCardAction;
import dev.planka.domain.schema.definition.rule.action.RuleAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BizRuleExecutionService 单元测试")
class BizRuleExecutionServiceTest {

    @Mock
    private RuleActionExecutorRegistry executorRegistry;
    @Mock
    private ConditionEvaluator conditionEvaluator;
    @Mock
    private RuleExecutionLogService logService;

    private BizRuleExecutionService service;

    private static final BizRuleId RULE_ID = BizRuleId.of("rule-001");
    private static final CardId CARD_ID = CardId.of("card-001");
    private static final CardTypeId CARD_TYPE_ID = CardTypeId.of("ct-001");

    @BeforeEach
    void setUp() {
        service = new BizRuleExecutionService(executorRegistry, conditionEvaluator, logService);
    }

    private BizRuleDefinition createRule(boolean enabled) {
        BizRuleDefinition rule = new BizRuleDefinition(RULE_ID, "org-001", "测试规则");
        rule.setCardTypeId(CARD_TYPE_ID);
        rule.setTriggerEvent(BizRuleDefinition.TriggerEvent.ON_CREATE);
        rule.setEnabled(enabled);
        return rule;
    }

    private RuleExecutionContext createContext() {
        CardDTO card = new CardDTO();
        card.setId(CARD_ID);
        card.setTypeId(CARD_TYPE_ID);
        return RuleExecutionContext.builder()
                .triggerCard(card)
                .cardId(CARD_ID)
                .cardTypeId(CARD_TYPE_ID)
                .operatorId("user-001")
                .triggerEvent(BizRuleDefinition.TriggerEvent.ON_CREATE)
                .build();
    }

    private RuleAction createAction() {
        ActionTargetSelector target = new ActionTargetSelector(
                ActionTargetSelector.TargetType.CURRENT_CARD, null, null);
        return new DiscardCardAction(target, null, 0);
    }

    @Nested
    @DisplayName("基本执行流程")
    class BasicExecutionTests {

        @Test
        @DisplayName("禁用规则应跳过执行，不记录执行日志")
        void shouldSkip_whenRuleDisabled() {
            BizRuleDefinition rule = createRule(false);
            RuleExecutionContext context = createContext();

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.isSkipped()).isTrue();
            verify(logService, never()).save(any());
        }

        @Test
        @DisplayName("条件不满足应跳过执行，不记录执行日志")
        void shouldSkip_whenConditionNotMet() {
            BizRuleDefinition rule = createRule(true);
            Condition condition = mock(Condition.class);
            rule.setCondition(condition);
            RuleExecutionContext context = createContext();

            when(conditionEvaluator.evaluate(eq(condition), any(CardDTO.class))).thenReturn(false);

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.isSkipped()).isTrue();
            verify(logService, never()).save(any());
        }

        @Test
        @DisplayName("null条件应视为满足")
        void shouldTreatNullConditionAsMet() {
            BizRuleDefinition rule = createRule(true);
            rule.setCondition(null);
            rule.setActions(List.of());
            RuleExecutionContext context = createContext();

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("空动作列表应返回成功")
        void shouldSucceed_whenNoActions() {
            BizRuleDefinition rule = createRule(true);
            rule.setActions(List.of());
            RuleExecutionContext context = createContext();

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getActionResults()).isEmpty();
        }

        @Test
        @DisplayName("条件满足应执行动作并返回成功")
        void shouldExecuteActions_whenConditionMet() {
            BizRuleDefinition rule = createRule(true);
            RuleAction action = createAction();
            rule.setActions(List.of(action));
            RuleExecutionContext context = createContext();

            RuleExecutionResult.ActionExecutionResult actionResult =
                    RuleExecutionResult.ActionExecutionResult.success("DISCARD_CARD", 0, 10, List.of());
            when(executorRegistry.executeAction(any(RuleAction.class), any(RuleExecutionContext.class)))
                    .thenReturn(actionResult);

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getActionResults()).hasSize(1);
        }

        @Test
        @DisplayName("全部动作失败应返回失败")
        void shouldReturnFailed_whenAllActionsFail() {
            BizRuleDefinition rule = createRule(true);
            RuleAction action = createAction();
            rule.setActions(List.of(action));
            RuleExecutionContext context = createContext();

            RuleExecutionResult.ActionExecutionResult actionResult =
                    RuleExecutionResult.ActionExecutionResult.failed("DISCARD_CARD", 0, 10, "执行失败");
            when(executorRegistry.executeAction(any(RuleAction.class), any(RuleExecutionContext.class)))
                    .thenReturn(actionResult);

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.getStatus()).isEqualTo(RuleExecutionResult.ExecutionStatus.FAILED);
        }

        @Test
        @DisplayName("部分动作失败应返回部分成功")
        void shouldReturnPartialSuccess_whenSomeActionsFail() {
            BizRuleDefinition rule = createRule(true);
            // 创建两个动作
            RuleAction action1 = createAction();
            RuleAction action2 = createAction();
            rule.setActions(List.of(action1, action2));
            RuleExecutionContext context = createContext();

            // 第一个成功，第二个失败
            RuleExecutionResult.ActionExecutionResult successResult =
                    RuleExecutionResult.ActionExecutionResult.success("DISCARD_CARD", 0, 10, List.of());
            RuleExecutionResult.ActionExecutionResult failedResult =
                    RuleExecutionResult.ActionExecutionResult.failed("DISCARD_CARD", 1, 10, "执行失败");

            when(executorRegistry.executeAction(any(RuleAction.class), any(RuleExecutionContext.class)))
                    .thenReturn(successResult)
                    .thenReturn(failedResult);

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.getStatus()).isEqualTo(RuleExecutionResult.ExecutionStatus.PARTIAL_SUCCESS);
        }

        @Test
        @DisplayName("异常应返回失败")
        void shouldReturnFailed_whenExceptionThrown() {
            BizRuleDefinition rule = createRule(true);
            RuleAction action = createAction();
            rule.setActions(List.of(action));
            RuleExecutionContext context = createContext();

            when(executorRegistry.executeAction(any(RuleAction.class), any(RuleExecutionContext.class)))
                    .thenThrow(new RuntimeException("意外错误"));

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.getStatus()).isEqualTo(RuleExecutionResult.ExecutionStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("重试逻辑")
    class RetryTests {

        @Test
        @DisplayName("无 retryConfig 不重试")
        void shouldNotRetry_whenNoRetryConfig() {
            BizRuleDefinition rule = createRule(true);
            rule.setRetryConfig(null);
            RuleAction action = createAction();
            rule.setActions(List.of(action));
            RuleExecutionContext context = createContext();

            RuleExecutionResult.ActionExecutionResult failedResult =
                    RuleExecutionResult.ActionExecutionResult.failed("DISCARD_CARD", 0, 10, "失败");
            when(executorRegistry.executeAction(any(RuleAction.class), any(RuleExecutionContext.class)))
                    .thenReturn(failedResult);

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.getRetryCount()).isZero();
            verify(executorRegistry, times(1)).executeAction(any(), any());
        }

        @Test
        @DisplayName("maxRetries=0 不重试")
        void shouldNotRetry_whenMaxRetriesIsZero() {
            BizRuleDefinition rule = createRule(true);
            rule.setRetryConfig(RetryConfig.noRetry());
            RuleAction action = createAction();
            rule.setActions(List.of(action));
            RuleExecutionContext context = createContext();

            RuleExecutionResult.ActionExecutionResult failedResult =
                    RuleExecutionResult.ActionExecutionResult.failed("DISCARD_CARD", 0, 10, "失败");
            when(executorRegistry.executeAction(any(RuleAction.class), any(RuleExecutionContext.class)))
                    .thenReturn(failedResult);

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.getRetryCount()).isZero();
            verify(executorRegistry, times(1)).executeAction(any(), any());
        }

        @Test
        @DisplayName("第一次失败重试成功")
        void shouldRetryAndSucceed() {
            BizRuleDefinition rule = createRule(true);
            RetryConfig retryConfig = new RetryConfig();
            retryConfig.setMaxRetries(3);
            retryConfig.setRetryIntervalMs(1); // 最小延迟以加速测试
            retryConfig.setExponentialBackoff(false);
            rule.setRetryConfig(retryConfig);
            RuleAction action = createAction();
            rule.setActions(List.of(action));
            RuleExecutionContext context = createContext();

            RuleExecutionResult.ActionExecutionResult failedResult =
                    RuleExecutionResult.ActionExecutionResult.failed("DISCARD_CARD", 0, 10, "失败");
            RuleExecutionResult.ActionExecutionResult successResult =
                    RuleExecutionResult.ActionExecutionResult.success("DISCARD_CARD", 0, 10, List.of());

            when(executorRegistry.executeAction(any(RuleAction.class), any(RuleExecutionContext.class)))
                    .thenReturn(failedResult)
                    .thenReturn(successResult);

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getRetryCount()).isEqualTo(1);
            verify(executorRegistry, times(2)).executeAction(any(), any());
        }

        @Test
        @DisplayName("重试耗尽返回失败")
        void shouldExhaustRetries() {
            BizRuleDefinition rule = createRule(true);
            RetryConfig retryConfig = new RetryConfig();
            retryConfig.setMaxRetries(2);
            retryConfig.setRetryIntervalMs(1);
            retryConfig.setExponentialBackoff(false);
            rule.setRetryConfig(retryConfig);
            RuleAction action = createAction();
            rule.setActions(List.of(action));
            RuleExecutionContext context = createContext();

            RuleExecutionResult.ActionExecutionResult failedResult =
                    RuleExecutionResult.ActionExecutionResult.failed("DISCARD_CARD", 0, 10, "失败");
            when(executorRegistry.executeAction(any(RuleAction.class), any(RuleExecutionContext.class)))
                    .thenReturn(failedResult);

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.getStatus()).isEqualTo(RuleExecutionResult.ExecutionStatus.FAILED);
            assertThat(result.getRetryCount()).isEqualTo(2);
            // 1 initial + 2 retries = 3 total
            verify(executorRegistry, times(3)).executeAction(any(), any());
        }
    }

    @Nested
    @DisplayName("日志记录")
    class LogTests {

        @Test
        @DisplayName("成功执行应记录日志")
        void shouldRecordLog_onSuccess() {
            BizRuleDefinition rule = createRule(true);
            rule.setActions(List.of());
            RuleExecutionContext context = createContext();

            service.execute(rule, context);

            verify(logService).save(any(RuleExecutionLog.class));
        }

        @Test
        @DisplayName("失败执行应记录日志")
        void shouldRecordLog_onFailure() {
            BizRuleDefinition rule = createRule(true);
            RuleAction action = createAction();
            rule.setActions(List.of(action));
            RuleExecutionContext context = createContext();

            when(executorRegistry.executeAction(any(RuleAction.class), any(RuleExecutionContext.class)))
                    .thenThrow(new RuntimeException("错误"));

            service.execute(rule, context);

            verify(logService).save(any(RuleExecutionLog.class));
        }

        @Test
        @DisplayName("日志记录异常不影响返回结果")
        void shouldReturnResult_evenWhenLogFails() {
            BizRuleDefinition rule = createRule(true);
            rule.setActions(List.of());
            RuleExecutionContext context = createContext();

            doThrow(new RuntimeException("日志保存失败")).when(logService).save(any());

            RuleExecutionResult result = service.execute(rule, context);

            assertThat(result.isSuccess()).isTrue();
        }
    }
}