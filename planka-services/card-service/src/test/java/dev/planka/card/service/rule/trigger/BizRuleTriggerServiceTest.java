package dev.planka.card.service.rule.trigger;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.flowrecord.ValueStreamHelper;
import dev.planka.card.service.rule.executor.BizRuleExecutionService;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.BizRuleId;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.domain.schema.definition.stream.StatusConfig;
import dev.planka.domain.schema.definition.stream.StepConfig;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StatusWorkType;
import dev.planka.domain.stream.StepId;
import dev.planka.domain.stream.StreamId;
import dev.planka.infra.cache.schema.query.BizRuleCacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BizRuleTriggerService 单元测试")
class BizRuleTriggerServiceTest {

    @Mock
    private BizRuleCacheQuery bizRuleCacheQuery;
    @Mock
    private BizRuleExecutionService executionService;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private ValueStreamHelper valueStreamHelper;

    private BizRuleTriggerService triggerService;

    private static final CardTypeId CARD_TYPE_ID = CardTypeId.of("ct-001");
    private static final CardId CARD_ID = CardId.of("card-001");

    // 价值流状态定义: A -> B -> C -> D
    private static final StatusId STATUS_A = StatusId.of("status-A");
    private static final StatusId STATUS_B = StatusId.of("status-B");
    private static final StatusId STATUS_C = StatusId.of("status-C");
    private static final StatusId STATUS_D = StatusId.of("status-D");

    private ValueStreamDefinition valueStream;

    @BeforeEach
    void setUp() {
        triggerService = new BizRuleTriggerService(bizRuleCacheQuery, executionService, cardRepository, valueStreamHelper);
        valueStream = buildValueStream();
    }

    private CardDTO createCard() {
        CardDTO card = new CardDTO();
        card.setId(CARD_ID);
        card.setTypeId(CARD_TYPE_ID);
        return card;
    }

    private BizRuleDefinition createRule(String id, BizRuleDefinition.TriggerEvent event, boolean enabled) {
        BizRuleDefinition rule = new BizRuleDefinition(BizRuleId.of(id), "org-001", "规则-" + id);
        rule.setCardTypeId(CARD_TYPE_ID);
        rule.setTriggerEvent(event);
        rule.setEnabled(enabled);
        return rule;
    }

    private ValueStreamDefinition buildValueStream() {
        StatusConfig sA = new StatusConfig();
        sA.setId(STATUS_A);
        sA.setName("A");
        sA.setWorkType(StatusWorkType.WAITING);
        sA.setSortOrder(0);

        StatusConfig sB = new StatusConfig();
        sB.setId(STATUS_B);
        sB.setName("B");
        sB.setWorkType(StatusWorkType.WORKING);
        sB.setSortOrder(1);

        StatusConfig sC = new StatusConfig();
        sC.setId(STATUS_C);
        sC.setName("C");
        sC.setWorkType(StatusWorkType.WORKING);
        sC.setSortOrder(2);

        StatusConfig sD = new StatusConfig();
        sD.setId(STATUS_D);
        sD.setName("D");
        sD.setWorkType(StatusWorkType.WORKING);
        sD.setSortOrder(3);

        StepConfig step1 = new StepConfig();
        step1.setId(StepId.of("step-1"));
        step1.setName("阶段1");
        step1.setSortOrder(0);
        step1.setStatusList(List.of(sA, sB));

        StepConfig step2 = new StepConfig();
        step2.setId(StepId.of("step-2"));
        step2.setName("阶段2");
        step2.setSortOrder(1);
        step2.setStatusList(List.of(sC, sD));

        return new ValueStreamDefinition(
                StreamId.of("stream-001"), "org-001", "测试价值流",
                CARD_TYPE_ID, List.of(step1, step2));
    }

    @Nested
    @DisplayName("通用触发")
    class GeneralTriggerTests {

        @Test
        @DisplayName("匹配规则时应执行")
        void shouldExecute_whenRulesMatch() {
            CardDTO card = createCard();
            BizRuleDefinition rule = createRule("r1", BizRuleDefinition.TriggerEvent.ON_CREATE, true);
            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(rule));

            triggerService.triggerOnCreate(card, "user-001");

            verify(executionService).executeAsync(eq(rule), any(RuleExecutionContext.class));
        }

        @Test
        @DisplayName("无匹配规则时不执行")
        void shouldNotExecute_whenNoRulesMatch() {
            CardDTO card = createCard();
            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of());

            triggerService.triggerOnCreate(card, "user-001");

            verify(executionService, never()).executeAsync(any(), any());
        }

        @Test
        @DisplayName("规则触发的事件应被跳过（循环防护）")
        void shouldSkip_whenTriggeredByRule() {
            CardDTO card = createCard();

            triggerService.trigger(card, BizRuleDefinition.TriggerEvent.ON_CREATE, "user-001", true);

            verify(executionService, never()).executeAsync(any(), any());
            verify(bizRuleCacheQuery, never()).getByCardTypeId(any());
        }

        @Test
        @DisplayName("禁用规则应被过滤")
        void shouldFilter_disabledRules() {
            CardDTO card = createCard();
            BizRuleDefinition disabledRule = createRule("r1", BizRuleDefinition.TriggerEvent.ON_CREATE, false);
            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(disabledRule));

            triggerService.triggerOnCreate(card, "user-001");

            verify(executionService, never()).executeAsync(any(), any());
        }
    }

    @Nested
    @DisplayName("状态移动触发")
    class StatusMoveTriggerTests {

        @Test
        @DisplayName("匹配目标状态应执行")
        void shouldExecute_whenTargetStatusMatches() {
            CardDTO card = createCard();
            BizRuleDefinition rule = createRule("r1", BizRuleDefinition.TriggerEvent.ON_STATUS_MOVE, true);
            rule.setTargetStatusId(STATUS_B);
            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(rule));
            // A -> B 路径: [A, B]，排除 A 后为 [B]
            when(valueStreamHelper.getStatusPath(valueStream, STATUS_A, STATUS_B))
                    .thenReturn(List.of(
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_A, StatusWorkType.WAITING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_B, StatusWorkType.WORKING)));

            triggerService.triggerOnStatusMove(card, STATUS_A, STATUS_B, "user-001", false, valueStream);

            verify(executionService).executeAsync(eq(rule), any(RuleExecutionContext.class));
        }

        @Test
        @DisplayName("不匹配目标状态应被过滤")
        void shouldFilter_whenTargetStatusNotMatch() {
            CardDTO card = createCard();
            BizRuleDefinition rule = createRule("r1", BizRuleDefinition.TriggerEvent.ON_STATUS_MOVE, true);
            rule.setTargetStatusId(STATUS_C);
            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(rule));
            // A -> B 路径: [A, B]，排除 A 后为 [B]，不包含 C
            when(valueStreamHelper.getStatusPath(valueStream, STATUS_A, STATUS_B))
                    .thenReturn(List.of(
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_A, StatusWorkType.WAITING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_B, StatusWorkType.WORKING)));

            triggerService.triggerOnStatusMove(card, STATUS_A, STATUS_B, "user-001", false, valueStream);

            verify(executionService, never()).executeAsync(any(), any());
        }

        @Test
        @DisplayName("规则未指定目标状态应匹配所有")
        void shouldMatchAll_whenNoTargetStatusSpecified() {
            CardDTO card = createCard();
            BizRuleDefinition rule = createRule("r1", BizRuleDefinition.TriggerEvent.ON_STATUS_MOVE, true);
            rule.setTargetStatusId(null);
            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(rule));
            when(valueStreamHelper.getStatusPath(valueStream, STATUS_A, STATUS_B))
                    .thenReturn(List.of(
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_A, StatusWorkType.WAITING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_B, StatusWorkType.WORKING)));

            triggerService.triggerOnStatusMove(card, STATUS_A, STATUS_B, "user-001", false, valueStream);

            verify(executionService).executeAsync(eq(rule), any(RuleExecutionContext.class));
        }

        @Test
        @DisplayName("跨状态移动时，中间状态的规则也应触发")
        void shouldTriggerIntermediateStatusRules_whenCrossStatusMove() {
            CardDTO card = createCard();
            // 规则分别配置在 B、C、D 上
            BizRuleDefinition ruleB = createRule("rB", BizRuleDefinition.TriggerEvent.ON_STATUS_MOVE, true);
            ruleB.setTargetStatusId(STATUS_B);
            BizRuleDefinition ruleC = createRule("rC", BizRuleDefinition.TriggerEvent.ON_STATUS_MOVE, true);
            ruleC.setTargetStatusId(STATUS_C);
            BizRuleDefinition ruleD = createRule("rD", BizRuleDefinition.TriggerEvent.ON_STATUS_MOVE, true);
            ruleD.setTargetStatusId(STATUS_D);

            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(ruleB, ruleC, ruleD));
            // A -> D 路径: [A, B, C, D]，排除 A 后为 [B, C, D]
            when(valueStreamHelper.getStatusPath(valueStream, STATUS_A, STATUS_D))
                    .thenReturn(List.of(
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_A, StatusWorkType.WAITING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_B, StatusWorkType.WORKING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-2"), STATUS_C, StatusWorkType.WORKING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-2"), STATUS_D, StatusWorkType.WORKING)));

            triggerService.triggerOnStatusMove(card, STATUS_A, STATUS_D, "user-001", false, valueStream);

            verify(executionService).executeAsync(eq(ruleB), any(RuleExecutionContext.class));
            verify(executionService).executeAsync(eq(ruleC), any(RuleExecutionContext.class));
            verify(executionService).executeAsync(eq(ruleD), any(RuleExecutionContext.class));
            verify(executionService, times(3)).executeAsync(any(), any());
        }

        @Test
        @DisplayName("跨状态回滚时，中间状态的规则也应触发")
        void shouldTriggerIntermediateStatusRules_whenCrossStatusRollback() {
            CardDTO card = createCard();
            BizRuleDefinition ruleB = createRule("rB", BizRuleDefinition.TriggerEvent.ON_STATUS_ROLLBACK, true);
            ruleB.setTargetStatusId(STATUS_B);
            BizRuleDefinition ruleA = createRule("rA", BizRuleDefinition.TriggerEvent.ON_STATUS_ROLLBACK, true);
            ruleA.setTargetStatusId(STATUS_A);

            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(ruleB, ruleA));
            // D -> A 回滚路径: [D, C, B, A]，排除 D 后为 [C, B, A]
            when(valueStreamHelper.getStatusPath(valueStream, STATUS_D, STATUS_A))
                    .thenReturn(List.of(
                            new ValueStreamHelper.StatusNode(StepId.of("step-2"), STATUS_D, StatusWorkType.WORKING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-2"), STATUS_C, StatusWorkType.WORKING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_B, StatusWorkType.WORKING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_A, StatusWorkType.WAITING)));

            triggerService.triggerOnStatusMove(card, STATUS_D, STATUS_A, "user-001", true, valueStream);

            verify(executionService).executeAsync(eq(ruleB), any(RuleExecutionContext.class));
            verify(executionService).executeAsync(eq(ruleA), any(RuleExecutionContext.class));
            verify(executionService, times(2)).executeAsync(any(), any());
        }

        @Test
        @DisplayName("targetStatusId=null 的规则在跨状态移动时只执行一次")
        void shouldExecuteOnce_whenRuleHasNoTargetStatus_andCrossStatusMove() {
            CardDTO card = createCard();
            BizRuleDefinition wildcardRule = createRule("rWild", BizRuleDefinition.TriggerEvent.ON_STATUS_MOVE, true);
            wildcardRule.setTargetStatusId(null); // 匹配所有状态

            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(wildcardRule));
            when(valueStreamHelper.getStatusPath(valueStream, STATUS_A, STATUS_D))
                    .thenReturn(List.of(
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_A, StatusWorkType.WAITING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-1"), STATUS_B, StatusWorkType.WORKING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-2"), STATUS_C, StatusWorkType.WORKING),
                            new ValueStreamHelper.StatusNode(StepId.of("step-2"), STATUS_D, StatusWorkType.WORKING)));

            triggerService.triggerOnStatusMove(card, STATUS_A, STATUS_D, "user-001", false, valueStream);

            // wildcardRule 只执行一次（去重）
            verify(executionService, times(1)).executeAsync(eq(wildcardRule), any(RuleExecutionContext.class));
        }
    }

    @Nested
    @DisplayName("字段变更触发")
    class FieldChangeTriggerTests {

        @Test
        @DisplayName("变更字段为空不触发")
        void shouldNotTrigger_whenChangedFieldsEmpty() {
            CardDTO card = createCard();

            triggerService.triggerOnFieldChange(card, List.of(), "user-001");

            verify(bizRuleCacheQuery, never()).getByCardTypeId(any());
        }

        @Test
        @DisplayName("匹配监听字段应执行")
        void shouldExecute_whenListenFieldMatches() {
            CardDTO card = createCard();
            BizRuleDefinition rule = createRule("r1", BizRuleDefinition.TriggerEvent.ON_FIELD_CHANGE, true);
            rule.setListenFieldList(List.of("field-a", "field-b"));
            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(rule));

            triggerService.triggerOnFieldChange(card, List.of("field-b"), "user-001");

            verify(executionService).executeAsync(eq(rule), any(RuleExecutionContext.class));
        }

        @Test
        @DisplayName("规则未指定监听字段应匹配所有")
        void shouldMatchAll_whenNoListenFieldsSpecified() {
            CardDTO card = createCard();
            BizRuleDefinition rule = createRule("r1", BizRuleDefinition.TriggerEvent.ON_FIELD_CHANGE, true);
            rule.setListenFieldList(null);
            when(bizRuleCacheQuery.getByCardTypeId(CARD_TYPE_ID)).thenReturn(List.of(rule));

            triggerService.triggerOnFieldChange(card, List.of("field-x"), "user-001");

            verify(executionService).executeAsync(eq(rule), any(RuleExecutionContext.class));
        }
    }
}
