package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.Yield;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.permission.exception.PermissionDeniedException;
import dev.planka.card.service.permission.model.BatchPermissionCheckResult;
import dev.planka.domain.card.*;
import dev.planka.domain.expression.TextExpressionTemplate;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.PermissionConfigId;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import dev.planka.domain.schema.definition.permission.PermissionConfig.CardOperationPermission;
import dev.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import dev.planka.infra.cache.card.CardCacheService;
import dev.planka.infra.cache.card.model.CardBasicInfo;
import dev.planka.infra.expression.TextExpressionTemplateResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardPermissionServiceImpl 单元测试")
class CardPermissionServiceImplTest {

    @Mock
    private CardCacheService cardCacheService;
    @Mock
    private PermissionConfigCacheService permissionConfigCacheService;
    @Mock
    private ConditionEvaluator conditionEvaluator;
    @Mock
    private YieldBuilder yieldBuilder;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private TextExpressionTemplateResolver templateResolver;

    private CardPermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new CardPermissionServiceImpl(
            cardCacheService,
            permissionConfigCacheService,
            conditionEvaluator,
            yieldBuilder,
            cardRepository,
            templateResolver
        );
    }

    private CardDTO createCardDTO(long cardId) {
        CardDTO card = new CardDTO();
        card.setId(CardId.of(cardId));
        return card;
    }

    private CardBasicInfo createBasicInfo(long cardId, String cardTypeId) {
        return new CardBasicInfo(
            CardId.of(cardId),
            OrgId.of("test_org"),
            CardTypeId.of(cardTypeId),
            CardTitle.pure("Test Card"),
            "TEST-001",
            CardStyle.ACTIVE,
            null,
            null
        );
    }

    private PermissionConfigDefinition createPermissionConfig(
            CardOperation operation,
            List<Condition> cardConditions,
            List<Condition> operatorConditions,
            String alertMessage) {
        PermissionConfigDefinition config = new PermissionConfigDefinition(
            PermissionConfigId.of("test_permission_config"),
            "test_org",
            "Test Permission Config"
        );
        CardOperationPermission opPermission = new CardOperationPermission();
        opPermission.setOperation(operation);
        opPermission.setCardConditions(cardConditions);
        opPermission.setOperatorConditions(operatorConditions);
        opPermission.setAlertMessage(alertMessage != null ? new TextExpressionTemplate(alertMessage) : null);
        config.setCardOperations(List.of(opPermission));
        return config;
    }

    @Nested
    @DisplayName("checkCardOperation - 单个卡片操作权限校验")
    class CheckCardOperationTests {

        @Test
        @DisplayName("当卡片不存在时抛出 IllegalArgumentException")
        void checkCardOperation_throwsException_whenCardNotFound() {
            CardId targetCardId = CardId.of(1001L);
            CardId operatorId = CardId.of(2001L);

            when(cardCacheService.getBasicInfoById(targetCardId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                permissionService.checkCardOperation(CardOperation.EDIT, targetCardId, operatorId)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("卡片不存在");
        }

        @Test
        @DisplayName("当无权限配置时直接允许操作")
        void checkCardOperation_allowsOperation_whenNoPermissionConfig() {
            CardId targetCardId = CardId.of(1001L);
            CardId operatorId = CardId.of(2001L);
            CardBasicInfo basicInfo = createBasicInfo(1001L, "task");

            when(cardCacheService.getBasicInfoById(targetCardId)).thenReturn(Optional.of(basicInfo));
            when(permissionConfigCacheService.hasPermissionConfig(
                basicInfo.cardTypeId(), CardOperation.EDIT
            )).thenReturn(false);

            // 不应该抛出异常
            permissionService.checkCardOperation(CardOperation.EDIT, targetCardId, operatorId);
        }

        @Test
        @DisplayName("当权限校验通过时允许操作")
        void checkCardOperation_allowsOperation_whenPermissionGranted() {
            CardId targetCardId = CardId.of(1001L);
            CardId operatorId = CardId.of(2001L);
            CardBasicInfo basicInfo = createBasicInfo(1001L, "task");
            CardDTO targetCard = createCardDTO(1001L);
            CardDTO memberCard = createCardDTO(2001L);

            PermissionConfigDefinition config = createPermissionConfig(
                CardOperation.EDIT,
                List.of(new Condition()), // 空条件，默认满足
                List.of(new Condition()),
                null
            );

            when(cardCacheService.getBasicInfoById(targetCardId)).thenReturn(Optional.of(basicInfo));
            when(permissionConfigCacheService.hasPermissionConfig(
                basicInfo.cardTypeId(), CardOperation.EDIT
            )).thenReturn(true);
            when(permissionConfigCacheService.getPermissionConfigs(
                basicInfo.cardTypeId()
            )).thenReturn(List.of(config));
            when(yieldBuilder.buildCardYield(anyList(), eq(CardOperation.EDIT))).thenReturn(new Yield());
            when(yieldBuilder.buildMemberYield(anyList(), eq(CardOperation.EDIT))).thenReturn(new Yield());
            when(cardRepository.findById(eq(targetCardId), any(), anyString())).thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(), anyString())).thenReturn(Optional.of(memberCard));
            // 卡片条件评估（3个参数）
            when(conditionEvaluator.evaluate(any(Condition.class), eq(targetCard), eq(memberCard))).thenReturn(true);
            // 操作人条件评估（2个参数）
            when(conditionEvaluator.evaluate(any(Condition.class), eq(memberCard))).thenReturn(true);

            // 不应该抛出异常
            permissionService.checkCardOperation(CardOperation.EDIT, targetCardId, operatorId);
        }

        @Test
        @DisplayName("当权限校验失败时抛出 PermissionDeniedException")
        void checkCardOperation_throwsException_whenPermissionDenied() {
            CardId targetCardId = CardId.of(1001L);
            CardId operatorId = CardId.of(2001L);
            CardBasicInfo basicInfo = createBasicInfo(1001L, "task");
            CardDTO targetCard = createCardDTO(1001L);
            CardDTO memberCard = createCardDTO(2001L);

            PermissionConfigDefinition config = createPermissionConfig(
                CardOperation.EDIT,
                List.of(new Condition()),
                List.of(new Condition()),
                "没有权限编辑"
            );

            when(cardCacheService.getBasicInfoById(targetCardId)).thenReturn(Optional.of(basicInfo));
            when(permissionConfigCacheService.hasPermissionConfig(
                basicInfo.cardTypeId(), CardOperation.EDIT
            )).thenReturn(true);
            when(permissionConfigCacheService.getPermissionConfigs(
                basicInfo.cardTypeId()
            )).thenReturn(List.of(config));
            when(yieldBuilder.buildCardYield(anyList(), eq(CardOperation.EDIT))).thenReturn(new Yield());
            when(yieldBuilder.buildMemberYield(anyList(), eq(CardOperation.EDIT))).thenReturn(new Yield());
            when(cardRepository.findById(eq(targetCardId), any(), anyString())).thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(), anyString())).thenReturn(Optional.of(memberCard));
            // 卡片条件评估失败
            when(conditionEvaluator.evaluate(any(Condition.class), eq(targetCard), eq(memberCard))).thenReturn(false);
            // 操作人条件评估也需要 mock（即使可能不会被调用，也确保行为一致）
            when(conditionEvaluator.evaluate(any(Condition.class), eq(memberCard))).thenReturn(false);
            when(templateResolver.resolve(any(TextExpressionTemplate.class), eq(targetCardId), eq(operatorId)))
                .thenReturn("没有权限编辑");

            assertThatThrownBy(() ->
                permissionService.checkCardOperation(CardOperation.EDIT, targetCardId, operatorId)
            ).isInstanceOf(PermissionDeniedException.class)
             .hasMessageContaining("没有权限编辑");
        }
    }

    @Nested
    @DisplayName("checkCardOperationForCreate - 创建卡片权限校验")
    class CheckCardOperationForCreateTests {

        @Test
        @DisplayName("当无组织级权限配置时直接允许创建")
        void checkCardOperationForCreate_allowsCreate_whenNoOrgLevelConfig() {
            CardTypeId cardTypeId = CardTypeId.of("task");
            CardId operatorId = CardId.of(2001L);

            when(permissionConfigCacheService.hasPermissionConfig(cardTypeId, CardOperation.CREATE)).thenReturn(false);

            // 不应该抛出异常
            permissionService.checkCardOperationForCreate(CardOperation.CREATE, cardTypeId, operatorId);
        }

        @Test
        @DisplayName("当操作人条件满足时允许创建")
        void checkCardOperationForCreate_allowsCreate_whenOperatorConditionMet() {
            CardTypeId cardTypeId = CardTypeId.of("task");
            CardId operatorId = CardId.of(2001L);
            CardDTO memberCard = createCardDTO(2001L);

            PermissionConfigDefinition config = createPermissionConfig(
                CardOperation.CREATE,
                null,
                List.of(new Condition()),
                null
            );

            when(permissionConfigCacheService.hasPermissionConfig(cardTypeId, CardOperation.CREATE)).thenReturn(true);
            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId)).thenReturn(List.of(config));
            when(yieldBuilder.buildMemberYield(anyList(), eq(CardOperation.CREATE))).thenReturn(new Yield());
            when(cardRepository.findById(eq(operatorId), any(), anyString())).thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), eq(memberCard))).thenReturn(true);

            // 不应该抛出异常
            permissionService.checkCardOperationForCreate(CardOperation.CREATE, cardTypeId, operatorId);
        }

        @Test
        @DisplayName("当操作人条件不满足时抛出 PermissionDeniedException")
        void checkCardOperationForCreate_throwsException_whenOperatorConditionNotMet() {
            CardTypeId cardTypeId = CardTypeId.of("task");
            CardId operatorId = CardId.of(2001L);
            CardDTO memberCard = createCardDTO(2001L);

            PermissionConfigDefinition config = createPermissionConfig(
                CardOperation.CREATE,
                null,
                List.of(new Condition()),
                "没有权限创建任务"
            );

            when(permissionConfigCacheService.hasPermissionConfig(cardTypeId, CardOperation.CREATE)).thenReturn(true);
            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId)).thenReturn(List.of(config));
            when(yieldBuilder.buildMemberYield(anyList(), eq(CardOperation.CREATE))).thenReturn(new Yield());
            when(cardRepository.findById(eq(operatorId), any(), anyString())).thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), eq(memberCard))).thenReturn(false);
            when(templateResolver.resolve(any(TextExpressionTemplate.class), isNull(), eq(operatorId)))
                .thenReturn("没有权限创建任务");

            assertThatThrownBy(() ->
                permissionService.checkCardOperationForCreate(CardOperation.CREATE, cardTypeId, operatorId)
            ).isInstanceOf(PermissionDeniedException.class)
             .hasMessageContaining("没有权限创建任务");
        }
    }

    @Nested
    @DisplayName("batchCheckCardOperation - 批量卡片操作权限校验")
    class BatchCheckCardOperationTests {

        @Test
        @DisplayName("当卡片列表为空时返回空结果")
        void batchCheckCardOperation_returnsEmpty_whenCardListEmpty() {
            CardId operatorId = CardId.of(2001L);

            BatchPermissionCheckResult result = permissionService.batchCheckCardOperation(
                CardOperation.EDIT,
                List.of(),
                operatorId
            );

            assertThat(result.getAllowed()).isEmpty();
            assertThat(result.getDenied()).isEmpty();
        }

        @Test
        @DisplayName("当无权限配置时全部允许")
        void batchCheckCardOperation_allowsAll_whenNoPermissionConfig() {
            CardId operatorId = CardId.of(2001L);
            List<CardId> cardIds = List.of(CardId.of(1001L), CardId.of(1002L));

            CardBasicInfo info1 = createBasicInfo(1001L, "task");
            CardBasicInfo info2 = createBasicInfo(1002L, "task");

            when(cardCacheService.getBasicInfoByIds(new HashSet<>(cardIds))).thenReturn(Map.of(
                CardId.of(1001L), info1,
                CardId.of(1002L), info2
            ));
            when(permissionConfigCacheService.hasPermissionConfig(any(), eq(CardOperation.EDIT))).thenReturn(false);

            BatchPermissionCheckResult result = permissionService.batchCheckCardOperation(
                CardOperation.EDIT,
                cardIds,
                operatorId
            );

            assertThat(result.getAllowed()).containsExactlyInAnyOrderElementsOf(cardIds);
            assertThat(result.getDenied()).isEmpty();
        }
    }

    @Nested
    @DisplayName("checkFieldEditPermission - 属性编辑权限校验")
    class CheckFieldEditPermissionTests {

        @Test
        @DisplayName("当变更属性为空时直接返回")
        void checkFieldEditPermission_returns_whenChangedFieldsEmpty() {
            CardId targetCardId = CardId.of(1001L);
            CardId operatorId = CardId.of(2001L);

            // 不应该抛出异常
            permissionService.checkFieldEditPermission(targetCardId, operatorId, Set.of());
        }

        @Test
        @DisplayName("当卡片不存在时抛出 IllegalArgumentException")
        void checkFieldEditPermission_throwsException_whenCardNotFound() {
            CardId targetCardId = CardId.of(1001L);
            CardId operatorId = CardId.of(2001L);
            Set<FieldId> fieldIds = Set.of(FieldId.of("status"));

            when(cardCacheService.getBasicInfoById(targetCardId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                permissionService.checkFieldEditPermission(targetCardId, operatorId, fieldIds)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("卡片不存在");
        }

        @Test
        @DisplayName("当无属性权限配置时直接允许")
        void checkFieldEditPermission_allows_whenNoFieldPermissionConfig() {
            CardId targetCardId = CardId.of(1001L);
            CardId operatorId = CardId.of(2001L);
            Set<FieldId> fieldIds = Set.of(FieldId.of("status"));
            CardBasicInfo basicInfo = createBasicInfo(1001L, "task");

            // 配置没有 fieldPermissions
            PermissionConfigDefinition config = new PermissionConfigDefinition(
                PermissionConfigId.of("test_permission_config"),
                "test_org",
                "Test Permission Config"
            );
            config.setFieldPermissions(null);

            when(cardCacheService.getBasicInfoById(targetCardId)).thenReturn(Optional.of(basicInfo));
            when(permissionConfigCacheService.getPermissionConfigs(
                basicInfo.cardTypeId()
            )).thenReturn(List.of(config));

            // 不应该抛出异常
            permissionService.checkFieldEditPermission(targetCardId, operatorId, fieldIds);
        }
    }
}
