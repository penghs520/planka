package cn.planka.card.service.permission;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.Yield;
import cn.planka.card.repository.CardRepository;
import cn.planka.card.service.evaluator.ConditionEvaluator;
import cn.planka.card.service.permission.exception.PermissionDeniedException;
import cn.planka.card.service.permission.model.BatchPermissionCheckResult;
import cn.planka.domain.card.*;
import cn.planka.domain.card.CardCycle;
import cn.planka.domain.expression.TextExpressionTemplate;
import cn.planka.domain.field.FieldId;
import cn.planka.domain.field.FieldPermissionStatus;
import cn.planka.domain.field.TextFieldValue;
import cn.planka.domain.field.NumberFieldValue;
import cn.planka.domain.schema.PermissionConfigId;
import cn.planka.domain.schema.definition.condition.Condition;
import cn.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import cn.planka.domain.schema.definition.permission.PermissionConfig.CardOperationPermission;
import cn.planka.domain.schema.definition.permission.PermissionConfig.FieldPermission;
import cn.planka.domain.schema.definition.permission.PermissionConfig.FieldOperation;
import cn.planka.domain.schema.definition.linkconfig.LinkFieldConfig;
import cn.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import cn.planka.domain.field.FieldConfigId;
import cn.planka.infra.cache.card.CardCacheService;
import cn.planka.infra.cache.card.model.CardBasicInfo;
import cn.planka.infra.cache.schema.query.CardTypeCacheQuery;
import cn.planka.infra.expression.TextExpressionTemplateResolver;
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
    private PermissionConfigYieldBuilder permissionConfigYieldBuilder;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardTypeCacheQuery cardTypeCacheQuery;
    @Mock
    private TextExpressionTemplateResolver templateResolver;
    @Mock
    private cn.planka.infra.cache.schema.SchemaCacheService schemaCacheService;

    private CardPermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new CardPermissionServiceImpl(
                cardCacheService,
                permissionConfigCacheService,
                conditionEvaluator,
                permissionConfigYieldBuilder,
                cardRepository,
                templateResolver,
                cardTypeCacheQuery,
                schemaCacheService
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
                CardCycle.ACTIVE,
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
        opPermission.setOperations(List.of(operation));
        opPermission.setCardConditions(cardConditions);
        opPermission.setOperatorConditions(operatorConditions);
        opPermission.setAlertMessage(alertMessage != null ? new TextExpressionTemplate(alertMessage) : null);
        config.setCardOperations(List.of(opPermission));
        return config;
    }

    private LinkFieldConfig createLinkFieldConfig(String linkFieldId, String name) {
        LinkFieldConfig config = new LinkFieldConfig(
                FieldConfigId.of(linkFieldId),
                "test_org",
                name,
                CardTypeId.of("task"),
                FieldId.of(linkFieldId),
                false
        );
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
            when(permissionConfigCacheService.hasCardOperationPermissionConfig(
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
            when(permissionConfigCacheService.hasCardOperationPermissionConfig(
                    basicInfo.cardTypeId(), CardOperation.EDIT
            )).thenReturn(true);
            when(permissionConfigCacheService.getPermissionConfigs(
                    basicInfo.cardTypeId()
            )).thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildCardYield(anyList(), eq(CardOperation.EDIT))).thenReturn(new Yield());
            when(permissionConfigYieldBuilder.buildMemberYield(anyList(), eq(CardOperation.EDIT))).thenReturn(new Yield());
            when(cardRepository.findById(eq(targetCardId), any(), anyString())).thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(), anyString())).thenReturn(Optional.of(memberCard));
            // 卡片条件评估（3个参数）
            when(conditionEvaluator.evaluate(any(Condition.class), eq(targetCard), eq(memberCard))).thenReturn(true);
            // 操作人条件评估（2个参数）
            when(conditionEvaluator.evaluate(any(Condition.class), eq(memberCard), eq(memberCard))).thenReturn(true);

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
            when(permissionConfigCacheService.hasCardOperationPermissionConfig(
                    basicInfo.cardTypeId(), CardOperation.EDIT
            )).thenReturn(true);
            when(permissionConfigCacheService.getPermissionConfigs(
                    basicInfo.cardTypeId()
            )).thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildCardYield(anyList(), eq(CardOperation.EDIT))).thenReturn(new Yield());
            when(permissionConfigYieldBuilder.buildMemberYield(anyList(), eq(CardOperation.EDIT))).thenReturn(new Yield());
            when(cardRepository.findById(eq(targetCardId), any(), anyString())).thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(), anyString())).thenReturn(Optional.of(memberCard));
            // 卡片条件评估失败
            when(conditionEvaluator.evaluate(any(Condition.class), eq(targetCard), eq(memberCard))).thenReturn(false);
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
        @DisplayName("当无权限配置时直接允许创建")
        void checkCardOperationForCreate_allowsCreate_whenNoOrgLevelConfig() {
            CardTypeId cardTypeId = CardTypeId.of("task");
            CardId operatorId = CardId.of(2001L);

            when(permissionConfigCacheService.hasCardOperationPermissionConfig(cardTypeId, CardOperation.CREATE)).thenReturn(false);

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

            when(permissionConfigCacheService.hasCardOperationPermissionConfig(cardTypeId, CardOperation.CREATE)).thenReturn(true);
            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId)).thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYield(anyList(), eq(CardOperation.CREATE))).thenReturn(new Yield());
            when(cardRepository.findById(eq(operatorId), any(), anyString())).thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), eq(memberCard), eq(memberCard))).thenReturn(true);

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

            when(permissionConfigCacheService.hasCardOperationPermissionConfig(cardTypeId, CardOperation.CREATE)).thenReturn(true);
            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId)).thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYield(anyList(), eq(CardOperation.CREATE))).thenReturn(new Yield());
            when(cardRepository.findById(eq(operatorId), any(), anyString())).thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), eq(memberCard), eq(memberCard))).thenReturn(false);
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
            when(permissionConfigCacheService.hasCardOperationPermissionConfig(any(), eq(CardOperation.EDIT))).thenReturn(false);

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

    @Nested
    @DisplayName("applyFieldReadPermissions - 批量字段读权限应用")
    class ApplyFieldReadPermissionsTests {

        private final CardId operatorId = CardId.of(2001L);
        private final CardTypeId cardTypeId = CardTypeId.of("task");

        private CardDTO createCardWithFields(long cardId, Map<String, cn.planka.domain.field.FieldValue<?>> fieldValues) {
            CardDTO card = createCardDTO(cardId);
            card.setTypeId(cardTypeId);
            card.setFieldValues(new HashMap<>(fieldValues));
            return card;
        }

        private PermissionConfigDefinition createFieldPermissionConfig(
                FieldOperation operation,
                List<FieldId> fieldIds,
                List<Condition> cardConditions,
                List<Condition> operatorConditions) {
            PermissionConfigDefinition config = new PermissionConfigDefinition(
                    PermissionConfigId.of("test_field_perm"),
                    "test_org",
                    "Test Field Permission"
            );
            config.setCardTypeId(cardTypeId);
            FieldPermission fieldPerm = new FieldPermission();
            fieldPerm.setOperations(List.of(operation));
            fieldPerm.setFieldIds(fieldIds);
            fieldPerm.setCardConditions(cardConditions);
            fieldPerm.setOperatorConditions(operatorConditions);
            config.setFieldPermissions(List.of(fieldPerm));
            return config;
        }

        @Test
        @DisplayName("无字段权限配置时不修改任何字段")
        void noFieldPermissionConfig_fieldsUnchanged() {
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));

            PermissionConfigDefinition config = new PermissionConfigDefinition(
                    PermissionConfigId.of("test_perm"), "test_org", "Test");
            config.setFieldPermissions(null);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            assertThat(card.getFieldValue("name").getValue()).isEqualTo("张三");
            assertThat(card.getFieldValue("name").getPermissionStatus()).isNull();
        }

        @Test
        @DisplayName("空列表不报错")
        void emptyList_noError() {
            permissionService.applyFieldReadPermissions(List.of(), operatorId);
            permissionService.applyFieldReadPermissions(null, operatorId);
        }

        @Test
        @DisplayName("有 READ 权限且条件满足时保持 FULL_ACCESS")
        void readPermissionMet_fullAccess() {
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of("name")),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(true);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            assertThat(card.getFieldValue("name").getValue()).isEqualTo("张三");
            assertThat(card.getFieldValue("name").getPermissionStatus()).isEqualTo(FieldPermissionStatus.FULL_ACCESS);
        }

        @Test
        @DisplayName("无任何读权限时设置 NO_PERMISSION")
        void noReadPermission_noPermission() {
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            NumberFieldValue scoreField = new NumberFieldValue("score", 95.0);
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField, "score", scoreField));

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of("name"), FieldId.of("score")),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            assertThat(card.getFieldValue("name").getPermissionStatus()).isEqualTo(FieldPermissionStatus.NO_PERMISSION);
            assertThat(card.getFieldValue("name").getValue()).isNull();
            assertThat(card.getFieldValue("score").getPermissionStatus()).isEqualTo(FieldPermissionStatus.NO_PERMISSION);
            assertThat(card.getFieldValue("score").getValue()).isNull();
        }

        @Test
        @DisplayName("不在受控集合中的字段不受影响")
        void uncontrolledField_unchanged() {
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            TextFieldValue descField = new TextFieldValue("desc", "描述内容");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField, "desc", descField));

            Condition condition = new Condition();
            // 只控制 name 字段
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of("name")),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            // name 被限制
            assertThat(card.getFieldValue("name").getPermissionStatus()).isEqualTo(FieldPermissionStatus.NO_PERMISSION);
            // desc 不受影响
            assertThat(card.getFieldValue("desc").getValue()).isEqualTo("描述内容");
            assertThat(card.getFieldValue("desc").getPermissionStatus()).isNull();
        }

        @Test
        @DisplayName("操作人卡片不存在时所有受控字段设为 NO_PERMISSION")
        void operatorNotFound_allControlledFieldsNoPermission() {
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of("name")),
                    List.of(condition),
                    List.of(condition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.empty());

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            assertThat(card.getFieldValue("name").getPermissionStatus()).isEqualTo(FieldPermissionStatus.NO_PERMISSION);
            assertThat(card.getFieldValue("name").getValue()).isNull();
        }

        @Test
        @DisplayName("批量多卡片类型分组处理")
        void batchMultipleCardTypes_groupedCorrectly() {
            CardTypeId typeA = CardTypeId.of("typeA");
            CardTypeId typeB = CardTypeId.of("typeB");

            TextFieldValue fieldA = new TextFieldValue("name", "卡片A");
            CardDTO cardA = createCardDTO(1001L);
            cardA.setTypeId(typeA);
            cardA.setFieldValues(new HashMap<>(Map.of("name", fieldA)));

            TextFieldValue fieldB = new TextFieldValue("name", "卡片B");
            CardDTO cardB = createCardDTO(1002L);
            cardB.setTypeId(typeB);
            cardB.setFieldValues(new HashMap<>(Map.of("name", fieldB)));

            // typeA 有字段权限配置，typeB 没有
            Condition condition = new Condition();
            PermissionConfigDefinition configA = new PermissionConfigDefinition(
                    PermissionConfigId.of("perm_a"), "test_org", "Perm A");
            configA.setCardTypeId(typeA);
            FieldPermission fieldPermA = new FieldPermission();
            fieldPermA.setOperations(List.of(FieldOperation.READ));
            fieldPermA.setFieldIds(List.of(FieldId.of("name")));
            fieldPermA.setCardConditions(List.of(condition));
            fieldPermA.setOperatorConditions(List.of(condition));
            configA.setFieldPermissions(List.of(fieldPermA));

            PermissionConfigDefinition configB = new PermissionConfigDefinition(
                    PermissionConfigId.of("perm_b"), "test_org", "Perm B");
            configB.setFieldPermissions(null);

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(typeA)).thenReturn(List.of(configA));
            when(permissionConfigCacheService.getPermissionConfigs(typeB)).thenReturn(List.of(configB));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(cardA, cardB), operatorId);

            // typeA 的 name 被限制
            assertThat(cardA.getFieldValue("name").getPermissionStatus()).isEqualTo(FieldPermissionStatus.NO_PERMISSION);
            // typeB 的 name 不受影响
            assertThat(cardB.getFieldValue("name").getValue()).isEqualTo("卡片B");
            assertThat(cardB.getFieldValue("name").getPermissionStatus()).isNull();
        }

        @Test
        @DisplayName("关联属性无权限时设置 NO_PERMISSION 并清空 linkedCards")
        void linkField_noPermission_clearsLinkedCards() {
            String linkFieldId = "100001:SOURCE";
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));

            // 设置关联卡片
            CardDTO linkedCard = createCardDTO(3001L);
            linkedCard.setTypeId(CardTypeId.of("bug"));
            card.setLinkedCards(new HashMap<>(Map.of(linkFieldId, new HashSet<>(Set.of(linkedCard)))));

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of(linkFieldId)),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            assertThat(card.getLinkedCardPermissions()).containsEntry(linkFieldId, FieldPermissionStatus.NO_PERMISSION);
            assertThat(card.getLinkedCards().get(linkFieldId)).isNull();
        }

        @Test
        @DisplayName("关联属性有权限时 linkedCards 保持不变")
        void linkField_hasPermission_linkedCardsUnchanged() {
            String linkFieldId = "100001:SOURCE";
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));

            CardDTO linkedCard = createCardDTO(3001L);
            linkedCard.setTypeId(CardTypeId.of("bug"));
            Set<CardDTO> linkedSet = new HashSet<>(Set.of(linkedCard));
            card.setLinkedCards(new HashMap<>(Map.of(linkFieldId, linkedSet)));

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of(linkFieldId)),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            // bug 类型无权限配置
            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("bug")))
                    .thenReturn(List.of());
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(true);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            assertThat(card.getLinkedCardPermissions()).isNull();
            assertThat(card.getLinkedCards().get(linkFieldId)).hasSize(1);
        }

        @Test
        @DisplayName("关联属性不在受控集合中时不受影响")
        void linkField_notControlled_unchanged() {
            String linkFieldId = "100001:SOURCE";
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));

            CardDTO linkedCard = createCardDTO(3001L);
            linkedCard.setTypeId(CardTypeId.of("bug"));
            card.setLinkedCards(new HashMap<>(Map.of(linkFieldId, new HashSet<>(Set.of(linkedCard)))));

            Condition condition = new Condition();
            // 只控制 name 字段，不控制关联属性
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of("name")),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            // bug 类型无权限配置
            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("bug")))
                    .thenReturn(List.of());
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            // 关联属性不受影响
            assertThat(card.getLinkedCardPermissions()).isNull();
            assertThat(card.getLinkedCards().get(linkFieldId)).hasSize(1);
        }

        @Test
        @DisplayName("操作人卡片不存在时关联属性也设为 NO_PERMISSION")
        void operatorNotFound_linkFieldNoPermission() {
            String linkFieldId = "100001:SOURCE";
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));

            CardDTO linkedCard = createCardDTO(3001L);
            linkedCard.setTypeId(CardTypeId.of("bug"));
            card.setLinkedCards(new HashMap<>(Map.of(linkFieldId, new HashSet<>(Set.of(linkedCard)))));

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of("name"), FieldId.of(linkFieldId)),
                    List.of(condition),
                    List.of(condition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.empty());

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            // 普通属性
            assertThat(card.getFieldValue("name").getPermissionStatus()).isEqualTo(FieldPermissionStatus.NO_PERMISSION);
            // 关联属性
            assertThat(card.getLinkedCardPermissions()).containsEntry(linkFieldId, FieldPermissionStatus.NO_PERMISSION);
            assertThat(card.getLinkedCards().get(linkFieldId)).isNull();
        }

        @Test
        @DisplayName("递归处理：一级关联有权限，二级关联卡片按自身类型权限配置评估")
        void recursive_nestedLinkedCards_evaluatedByOwnCardType() {
            String linkFieldId1 = "100001:SOURCE";
            String linkFieldId2 = "100002:TARGET";
            CardTypeId bugTypeId = CardTypeId.of("bug");

            // 一级卡片（task 类型）
            TextFieldValue nameField = new TextFieldValue("name", "需求卡片");
            CardDTO topCard = createCardWithFields(1001L, Map.of("name", nameField));

            // 二级卡片（bug 类型），有自己的关联属性
            CardDTO nestedCard = createCardDTO(3001L);
            nestedCard.setTypeId(bugTypeId);
            TextFieldValue bugNameField = new TextFieldValue("bugName", "缺陷名称");
            nestedCard.setFieldValues(new HashMap<>(Map.of("bugName", bugNameField)));

            // 三级卡片
            CardDTO deepCard = createCardDTO(4001L);
            deepCard.setTypeId(CardTypeId.of("story"));
            nestedCard.setLinkedCards(new HashMap<>(Map.of(linkFieldId2, new HashSet<>(Set.of(deepCard)))));

            // 一级卡片关联二级卡片
            topCard.setLinkedCards(new HashMap<>(Map.of(linkFieldId1, new HashSet<>(Set.of(nestedCard)))));

            Condition condition = new Condition();

            // task 类型：控制 linkFieldId1（一级关联），条件满足
            PermissionConfigDefinition taskConfig = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of(linkFieldId1)),
                    List.of(condition),
                    List.of(condition)
            );

            // bug 类型：控制 linkFieldId2（二级关联），条件不满足
            PermissionConfigDefinition bugConfig = new PermissionConfigDefinition(
                    PermissionConfigId.of("bug_perm"), "test_org", "Bug Perm");
            bugConfig.setCardTypeId(bugTypeId);
            FieldPermission bugFieldPerm = new FieldPermission();
            bugFieldPerm.setOperations(List.of(FieldOperation.READ));
            bugFieldPerm.setFieldIds(List.of(FieldId.of(linkFieldId2)));
            bugFieldPerm.setCardConditions(List.of(condition));
            bugFieldPerm.setOperatorConditions(List.of(condition));
            bugConfig.setFieldPermissions(List.of(bugFieldPerm));

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(taskConfig));
            when(permissionConfigCacheService.getPermissionConfigs(bugTypeId))
                    .thenReturn(List.of(bugConfig));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));

            // task 类型条件满足（一级关联有权限），bug 类型条件不满足（二级关联无权限）
            when(conditionEvaluator.evaluate(any(), eq(topCard), any())).thenReturn(true);
            when(conditionEvaluator.evaluate(any(), eq(memberCard), eq(memberCard))).thenReturn(true);
            when(conditionEvaluator.evaluate(any(), eq(nestedCard), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(topCard), operatorId);

            // 一级关联有权限，linkedCards 保持
            assertThat(topCard.getLinkedCardPermissions()).isNull();
            assertThat(topCard.getLinkedCards().get(linkFieldId1)).hasSize(1);

            // 二级关联无权限，linkedCards 被清空
            assertThat(nestedCard.getLinkedCardPermissions())
                    .containsEntry(linkFieldId2, FieldPermissionStatus.NO_PERMISSION);
            assertThat(nestedCard.getLinkedCards().get(linkFieldId2)).isNull();
        }

        @Test
        @DisplayName("同一卡片同时有普通属性和关联属性受控，分别独立评估")
        void mixedFieldAndLinkField_evaluatedIndependently() {
            String linkFieldId = "100001:SOURCE";
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));

            CardDTO linkedCard = createCardDTO(3001L);
            linkedCard.setTypeId(CardTypeId.of("bug"));
            card.setLinkedCards(new HashMap<>(Map.of(linkFieldId, new HashSet<>(Set.of(linkedCard)))));

            // 同时控制普通属性 name 和关联属性 linkFieldId
            Condition cardCondition = new Condition();
            Condition operatorCondition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of("name"), FieldId.of(linkFieldId)),
                    List.of(cardCondition),
                    List.of(operatorCondition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            // bug 类型无权限配置
            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("bug")))
                    .thenReturn(List.of());
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            // 条件全部满足
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(true);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            // 普通属性有权限
            assertThat(card.getFieldValue("name").getPermissionStatus()).isEqualTo(FieldPermissionStatus.FULL_ACCESS);
            assertThat(card.getFieldValue("name").getValue()).isEqualTo("张三");
            // 关联属性有权限
            assertThat(card.getLinkedCardPermissions()).isNull();
            assertThat(card.getLinkedCards().get(linkFieldId)).hasSize(1);
        }

        @Test
        @DisplayName("同一卡片有多个关联属性，部分有权限部分无权限")
        void multipleLinkFields_partialPermission() {
            String linkFieldId1 = "100001:SOURCE";
            String linkFieldId2 = "100002:TARGET";

            CardDTO card = createCardWithFields(1001L, Map.of());

            CardDTO linkedCard1 = createCardDTO(3001L);
            linkedCard1.setTypeId(CardTypeId.of("bug"));
            CardDTO linkedCard2 = createCardDTO(3002L);
            linkedCard2.setTypeId(CardTypeId.of("story"));
            card.setLinkedCards(new HashMap<>(Map.of(
                    linkFieldId1, new HashSet<>(Set.of(linkedCard1)),
                    linkFieldId2, new HashSet<>(Set.of(linkedCard2))
            )));

            Condition condition = new Condition();

            // 创建两条权限规则：linkFieldId1 有权限，linkFieldId2 无权限
            PermissionConfigDefinition config = new PermissionConfigDefinition(
                    PermissionConfigId.of("test_perm"), "test_org", "Test");
            config.setCardTypeId(cardTypeId);

            FieldPermission perm1 = new FieldPermission();
            perm1.setOperations(List.of(FieldOperation.READ));
            perm1.setFieldIds(List.of(FieldId.of(linkFieldId1)));
            perm1.setCardConditions(null); // 无条件，默认满足
            perm1.setOperatorConditions(null);

            FieldPermission perm2 = new FieldPermission();
            perm2.setOperations(List.of(FieldOperation.READ));
            perm2.setFieldIds(List.of(FieldId.of(linkFieldId2)));
            perm2.setCardConditions(List.of(condition));
            perm2.setOperatorConditions(List.of(condition));

            config.setFieldPermissions(List.of(perm1, perm2));

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            // bug 和 story 类型无权限配置
            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("bug")))
                    .thenReturn(List.of());
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            // perm2 的条件不满足
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            // linkFieldId1 有权限（无条件限制，默认满足）
            assertThat(card.getLinkedCards().get(linkFieldId1)).hasSize(1);
            // linkFieldId2 无权限
            assertThat(card.getLinkedCardPermissions()).containsEntry(linkFieldId2, FieldPermissionStatus.NO_PERMISSION);
            assertThat(card.getLinkedCards().get(linkFieldId2)).isNull();
            // linkFieldId1 不在 permissions 中
            assertThat(card.getLinkedCardPermissions()).doesNotContainKey(linkFieldId1);
        }

        @Test
        @DisplayName("卡片无 linkedCards 时不报错")
        void cardWithoutLinkedCards_noError() {
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));
            // linkedCards 为 null

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of("name"), FieldId.of("100001:SOURCE")),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            // 普通属性正常受控
            assertThat(card.getFieldValue("name").getPermissionStatus()).isEqualTo(FieldPermissionStatus.NO_PERMISSION);
            // 不会因为 linkedCards 为 null 而报错
            assertThat(card.getLinkedCardPermissions()).isNull();
        }

        @Test
        @DisplayName("卡片有空 linkedCards map 时不报错")
        void cardWithEmptyLinkedCards_noError() {
            TextFieldValue nameField = new TextFieldValue("name", "张三");
            CardDTO card = createCardWithFields(1001L, Map.of("name", nameField));
            card.setLinkedCards(new HashMap<>());

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of("name")),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(true);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            assertThat(card.getFieldValue("name").getPermissionStatus()).isEqualTo(FieldPermissionStatus.FULL_ACCESS);
            assertThat(card.getLinkedCardPermissions()).isNull();
        }

        @Test
        @DisplayName("卡片仅有 linkedCards 无 fieldValues 时关联属性权限正常应用")
        void cardWithOnlyLinkedCards_permissionApplied() {
            String linkFieldId = "100001:SOURCE";
            CardDTO card = createCardDTO(1001L);
            card.setTypeId(cardTypeId);
            // fieldValues 为 null

            CardDTO linkedCard = createCardDTO(3001L);
            linkedCard.setTypeId(CardTypeId.of("bug"));
            card.setLinkedCards(new HashMap<>(Map.of(linkFieldId, new HashSet<>(Set.of(linkedCard)))));

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of(linkFieldId)),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            assertThat(card.getLinkedCardPermissions()).containsEntry(linkFieldId, FieldPermissionStatus.NO_PERMISSION);
            assertThat(card.getLinkedCards().get(linkFieldId)).isNull();
        }

        @Test
        @DisplayName("批量卡片不同类型各自的关联属性独立评估")
        void batchMultipleCardTypes_linkFieldsEvaluatedByOwnType() {
            CardTypeId typeA = CardTypeId.of("typeA");
            CardTypeId typeB = CardTypeId.of("typeB");
            String linkFieldIdA = "200001:SOURCE";
            String linkFieldIdB = "200002:TARGET";

            // typeA 卡片有关联属性
            CardDTO cardA = createCardDTO(1001L);
            cardA.setTypeId(typeA);
            cardA.setFieldValues(new HashMap<>());
            CardDTO linkedCardA = createCardDTO(3001L);
            linkedCardA.setTypeId(CardTypeId.of("sub"));
            cardA.setLinkedCards(new HashMap<>(Map.of(linkFieldIdA, new HashSet<>(Set.of(linkedCardA)))));

            // typeB 卡片有关联属性
            CardDTO cardB = createCardDTO(1002L);
            cardB.setTypeId(typeB);
            cardB.setFieldValues(new HashMap<>());
            CardDTO linkedCardB = createCardDTO(3002L);
            linkedCardB.setTypeId(CardTypeId.of("sub"));
            cardB.setLinkedCards(new HashMap<>(Map.of(linkFieldIdB, new HashSet<>(Set.of(linkedCardB)))));

            Condition condition = new Condition();

            // typeA 控制 linkFieldIdA
            PermissionConfigDefinition configA = new PermissionConfigDefinition(
                    PermissionConfigId.of("perm_a"), "test_org", "Perm A");
            configA.setCardTypeId(typeA);
            FieldPermission fieldPermA = new FieldPermission();
            fieldPermA.setOperations(List.of(FieldOperation.READ));
            fieldPermA.setFieldIds(List.of(FieldId.of(linkFieldIdA)));
            fieldPermA.setCardConditions(List.of(condition));
            fieldPermA.setOperatorConditions(List.of(condition));
            configA.setFieldPermissions(List.of(fieldPermA));

            // typeB 无字段权限配置
            PermissionConfigDefinition configB = new PermissionConfigDefinition(
                    PermissionConfigId.of("perm_b"), "test_org", "Perm B");
            configB.setFieldPermissions(null);

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(typeA)).thenReturn(List.of(configA));
            when(permissionConfigCacheService.getPermissionConfigs(typeB)).thenReturn(List.of(configB));
            // sub 类型无权限配置
            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("sub")))
                    .thenReturn(List.of());
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(cardA, cardB), operatorId);

            // typeA 的关联属性被限制
            assertThat(cardA.getLinkedCardPermissions()).containsEntry(linkFieldIdA, FieldPermissionStatus.NO_PERMISSION);
            assertThat(cardA.getLinkedCards().get(linkFieldIdA)).isNull();
            // typeB 的关联属性不受影响（无权限配置）
            assertThat(cardB.getLinkedCardPermissions()).isNull();
            assertThat(cardB.getLinkedCards().get(linkFieldIdB)).hasSize(1);
        }

        @Test
        @DisplayName("递归处理：嵌套关联卡片的普通属性也按自身类型权限配置评估")
        void recursive_nestedLinkedCards_fieldValuesAlsoEvaluated() {
            String linkFieldId = "100001:SOURCE";
            CardTypeId bugTypeId = CardTypeId.of("bug");

            // 一级卡片
            CardDTO topCard = createCardWithFields(1001L, Map.of());
            CardDTO nestedCard = createCardDTO(3001L);
            nestedCard.setTypeId(bugTypeId);
            TextFieldValue bugField = new TextFieldValue("severity", "高");
            nestedCard.setFieldValues(new HashMap<>(Map.of("severity", bugField)));

            topCard.setLinkedCards(new HashMap<>(Map.of(linkFieldId, new HashSet<>(Set.of(nestedCard)))));

            // task 类型无字段权限配置（关联属性不受控）
            PermissionConfigDefinition taskConfig = new PermissionConfigDefinition(
                    PermissionConfigId.of("task_perm"), "test_org", "Task Perm");
            taskConfig.setFieldPermissions(null);

            // bug 类型控制 severity 字段
            Condition condition = new Condition();
            PermissionConfigDefinition bugConfig = new PermissionConfigDefinition(
                    PermissionConfigId.of("bug_perm"), "test_org", "Bug Perm");
            bugConfig.setCardTypeId(bugTypeId);
            FieldPermission bugFieldPerm = new FieldPermission();
            bugFieldPerm.setOperations(List.of(FieldOperation.READ));
            bugFieldPerm.setFieldIds(List.of(FieldId.of("severity")));
            bugFieldPerm.setCardConditions(List.of(condition));
            bugFieldPerm.setOperatorConditions(List.of(condition));
            bugConfig.setFieldPermissions(List.of(bugFieldPerm));

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(taskConfig));
            when(permissionConfigCacheService.getPermissionConfigs(bugTypeId))
                    .thenReturn(List.of(bugConfig));
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(topCard), operatorId);

            // 一级卡片不受影响
            assertThat(topCard.getLinkedCardPermissions()).isNull();
            assertThat(topCard.getLinkedCards().get(linkFieldId)).hasSize(1);
            // 嵌套的 bug 卡片的 severity 字段被限制
            assertThat(nestedCard.getFieldValue("severity").getPermissionStatus())
                    .isEqualTo(FieldPermissionStatus.NO_PERMISSION);
            assertThat(nestedCard.getFieldValue("severity").getValue()).isNull();
        }

        @Test
        @DisplayName("递归处理：一级关联无权限时不会递归处理被清空的嵌套卡片")
        void recursive_noPermissionAtFirstLevel_nestedNotProcessed() {
            String linkFieldId = "100001:SOURCE";
            CardTypeId bugTypeId = CardTypeId.of("bug");

            CardDTO topCard = createCardWithFields(1001L, Map.of());

            // 嵌套卡片有自己的普通属性
            CardDTO nestedCard = createCardDTO(3001L);
            nestedCard.setTypeId(bugTypeId);
            TextFieldValue bugField = new TextFieldValue("severity", "高");
            nestedCard.setFieldValues(new HashMap<>(Map.of("severity", bugField)));

            topCard.setLinkedCards(new HashMap<>(Map.of(linkFieldId, new HashSet<>(Set.of(nestedCard)))));

            Condition condition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.READ,
                    List.of(FieldId.of(linkFieldId)),
                    List.of(condition),
                    List.of(condition)
            );

            CardDTO memberCard = createCardDTO(2001L);

            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));
            // 注意：bug 类型的 getPermissionConfigs 不应被调用，因为 nestedCard 已被清空
            when(permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(any()))
                    .thenReturn(Yield.basic());
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));
            when(conditionEvaluator.evaluate(any(), any(), any())).thenReturn(false);

            permissionService.applyFieldReadPermissions(List.of(topCard), operatorId);

            // 一级关联被清空
            assertThat(topCard.getLinkedCardPermissions()).containsEntry(linkFieldId, FieldPermissionStatus.NO_PERMISSION);
            assertThat(topCard.getLinkedCards().get(linkFieldId)).isNull();
            // 嵌套卡片的 severity 不受影响（因为整个关联被清空，递归时不会收集到它）
            assertThat(nestedCard.getFieldValue("severity").getValue()).isEqualTo("高");
            assertThat(nestedCard.getFieldValue("severity").getPermissionStatus()).isNull();
        }

        @Test
        @DisplayName("关联属性权限仅控制 READ 操作，EDIT 操作的字段权限不影响关联属性")
        void onlyEditPermission_linkFieldNotAffected() {
            String linkFieldId = "100001:SOURCE";
            CardDTO card = createCardWithFields(1001L, Map.of());

            CardDTO linkedCard = createCardDTO(3001L);
            linkedCard.setTypeId(CardTypeId.of("bug"));
            card.setLinkedCards(new HashMap<>(Map.of(linkFieldId, new HashSet<>(Set.of(linkedCard)))));

            // 只配置 EDIT 操作
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of(linkFieldId)),
                    List.of(new Condition()),
                    List.of(new Condition())
            );

            // bug 类型无权限配置
            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("bug")))
                    .thenReturn(List.of());
            when(permissionConfigCacheService.getPermissionConfigs(cardTypeId))
                    .thenReturn(List.of(config));

            permissionService.applyFieldReadPermissions(List.of(card), operatorId);

            // EDIT 配置不影响 READ 权限，关联属性不受影响
            assertThat(card.getLinkedCardPermissions()).isNull();
            assertThat(card.getLinkedCards().get(linkFieldId)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("checkFieldEditPermission (含关联属性双向检查) - 关联属性权限校验")
    class CheckLinkFieldEditPermissionTests {

        private final CardId operatorId = CardId.of(3001L);

        private PermissionConfigDefinition createFieldPermissionConfig(
                FieldOperation operation,
                List<FieldId> fieldIds,
                List<Condition> cardConditions,
                List<Condition> operatorConditions) {
            PermissionConfigDefinition config = new PermissionConfigDefinition(
                    PermissionConfigId.of("test_field_perm"),
                    "test_org",
                    "Test Field Permission"
            );
            FieldPermission fieldPerm = new FieldPermission();
            fieldPerm.setOperations(List.of(operation));
            fieldPerm.setFieldIds(fieldIds);
            fieldPerm.setCardConditions(cardConditions);
            fieldPerm.setOperatorConditions(operatorConditions);
            config.setFieldPermissions(List.of(fieldPerm));
            return config;
        }

        @Test
        @DisplayName("关联属性无权限配置时允许")
        void linkFieldNoPermissionConfig_allowed() {
            CardId targetCardId = CardId.of(1001L);
            Set<FieldId> changedFieldIds = Set.of();
            Set<String> linkFieldIds = Set.of("263671031548350464:SOURCE");
            Map<String, List<String>> targetCardIdsByLinkField = Map.of(
                    "263671031548350464:SOURCE", List.of("2001")
            );

            when(cardCacheService.getBasicInfoById(targetCardId))
                    .thenReturn(Optional.of(createBasicInfo(1001L, "story")));
            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("story")))
                    .thenReturn(List.of());

            // 不应抛出异常
            permissionService.checkFieldEditPermission(
                    targetCardId, operatorId, changedFieldIds, linkFieldIds, targetCardIdsByLinkField);
        }

        @Test
        @DisplayName("当前侧配置了关联属性权限且满足时允许")
        void currentSideLinkFieldPermission_satisfied_allowed() {
            CardId targetCardId = CardId.of(1001L);
            Set<FieldId> changedFieldIds = Set.of();
            Set<String> linkFieldIds = Set.of("263671031548350464:SOURCE");
            Map<String, List<String>> targetCardIdsByLinkField = Map.of(
                    "263671031548350464:SOURCE", List.of("2001")
            );

            CardBasicInfo basicInfo = createBasicInfo(1001L, "story");
            when(cardCacheService.getBasicInfoById(targetCardId))
                    .thenReturn(Optional.of(basicInfo));

            Condition satisfiedCondition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of("263671031548350464:SOURCE")),
                    List.of(satisfiedCondition),
                    List.of(satisfiedCondition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("story")))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildCardYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());
            when(permissionConfigYieldBuilder.buildMemberYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());

            CardDTO targetCard = createCardDTO(1001L);
            CardDTO memberCard = createCardDTO(3001L);
            when(cardRepository.findById(eq(targetCardId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));

            // 卡片条件评估
            when(conditionEvaluator.evaluate(eq(satisfiedCondition), eq(targetCard), eq(memberCard)))
                    .thenReturn(true);
            // 操作人条件评估（memberCard, memberCard）
            when(conditionEvaluator.evaluate(eq(satisfiedCondition), eq(memberCard), eq(memberCard)))
                    .thenReturn(true);

            // 不应抛出异常
            permissionService.checkFieldEditPermission(
                    targetCardId, operatorId, changedFieldIds, linkFieldIds, targetCardIdsByLinkField);
        }

        @Test
        @DisplayName("当前侧配置了关联属性权限但不满足时拒绝")
        void currentSideLinkFieldPermission_notSatisfied_denied() {
            CardId targetCardId = CardId.of(1001L);
            Set<FieldId> changedFieldIds = Set.of();
            Set<String> linkFieldIds = Set.of("263671031548350464:SOURCE");
            Map<String, List<String>> targetCardIdsByLinkField = Map.of(
                    "263671031548350464:SOURCE", List.of("2001")
            );

            CardBasicInfo basicInfo = createBasicInfo(1001L, "story");
            when(cardCacheService.getBasicInfoById(targetCardId))
                    .thenReturn(Optional.of(basicInfo));

            Condition unsatisfiedCondition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of("263671031548350464:SOURCE")),
                    List.of(unsatisfiedCondition),
                    List.of(unsatisfiedCondition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("story")))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildCardYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());
            when(permissionConfigYieldBuilder.buildMemberYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());

            CardDTO targetCard = createCardDTO(1001L);
            CardDTO memberCard = createCardDTO(3001L);
            when(cardRepository.findById(eq(targetCardId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));

            // 条件不满足
            when(conditionEvaluator.evaluate(eq(unsatisfiedCondition), any(CardDTO.class), any(CardDTO.class)))
                    .thenReturn(false);

            // 应抛出正常的 fieldEdit 异常
            assertThatThrownBy(() -> permissionService.checkFieldEditPermission(
                    targetCardId, operatorId, changedFieldIds, linkFieldIds, targetCardIdsByLinkField))
                    .isInstanceOf(PermissionDeniedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PERMISSION_DENIED_FIELD_EDIT");
        }

        @Test
        @DisplayName("对侧配置了权限且满足时允许")
        void oppositeSidePermission_satisfied_allowed() {
            CardId targetCardId = CardId.of(1001L);
            Set<FieldId> changedFieldIds = Set.of();
            Set<String> linkFieldIds = Set.of("263671031548350464:SOURCE");
            Map<String, List<String>> targetCardIdsByLinkField = Map.of(
                    "263671031548350464:SOURCE", List.of("2001")
            );

            // 当前侧无权限配置
            when(cardCacheService.getBasicInfoById(targetCardId))
                    .thenReturn(Optional.of(createBasicInfo(1001L, "story")));
            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("story")))
                    .thenReturn(List.of());

            // 对侧卡片 - 批量获取基本信息
            CardId oppositeCardId = CardId.of(2001L);
            when(cardCacheService.getBasicInfoByIds(eq(Set.of(oppositeCardId))))
                    .thenReturn(Map.of(oppositeCardId, createBasicInfo(2001L, "task")));

            // 对侧配置了 TARGET 权限
            Condition satisfiedCondition = new Condition();
            PermissionConfigDefinition oppositeConfig = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of("263671031548350464:TARGET")),
                    List.of(satisfiedCondition),
                    List.of(satisfiedCondition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("task")))
                    .thenReturn(List.of(oppositeConfig));
            when(permissionConfigYieldBuilder.buildCardYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());
            when(permissionConfigYieldBuilder.buildMemberYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());

            CardDTO oppositeCard = createCardDTO(2001L);
            CardDTO memberCard = createCardDTO(3001L);
            when(cardRepository.findByIds(eq(List.of(oppositeCardId)), any(Yield.class), eq("system")))
                    .thenReturn(List.of(oppositeCard));
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));

            // 卡片条件评估
            when(conditionEvaluator.evaluate(eq(satisfiedCondition), eq(oppositeCard), eq(memberCard)))
                    .thenReturn(true);
            // 操作人条件评估（memberCard, memberCard）
            when(conditionEvaluator.evaluate(eq(satisfiedCondition), eq(memberCard), eq(memberCard)))
                    .thenReturn(true);

            // 不应抛出异常
            permissionService.checkFieldEditPermission(
                    targetCardId, operatorId, changedFieldIds, linkFieldIds, targetCardIdsByLinkField);
        }

        @Test
        @DisplayName("对侧配置了权限但不满足时拒绝（对侧提示）")
        void oppositeSidePermission_notSatisfied_deniedWithOppositeSideMessage() {
            CardId targetCardId = CardId.of(1001L);
            Set<FieldId> changedFieldIds = Set.of();
            Set<String> linkFieldIds = Set.of("263671031548350464:SOURCE");
            Map<String, List<String>> targetCardIdsByLinkField = Map.of(
                    "263671031548350464:SOURCE", List.of("2001")
            );

            // 当前侧无权限配置
            when(cardCacheService.getBasicInfoById(targetCardId))
                    .thenReturn(Optional.of(createBasicInfo(1001L, "story")));
            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("story")))
                    .thenReturn(List.of());

            // 对侧卡片 - 批量获取基本信息
            CardId oppositeCardId = CardId.of(2001L);
            when(cardCacheService.getBasicInfoByIds(eq(Set.of(oppositeCardId))))
                    .thenReturn(Map.of(oppositeCardId, createBasicInfo(2001L, "task")));

            // 对侧配置了 TARGET 权限
            Condition unsatisfiedCondition = new Condition();
            PermissionConfigDefinition oppositeConfig = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of("263671031548350464:TARGET")),
                    List.of(unsatisfiedCondition),
                    List.of(unsatisfiedCondition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("task")))
                    .thenReturn(List.of(oppositeConfig));
            when(permissionConfigYieldBuilder.buildCardYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());
            when(permissionConfigYieldBuilder.buildMemberYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());

            CardDTO memberCard = createCardDTO(3001L);
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));

            // 条件不满足（操作人条件不满足，会提前抛异常，不会查询对端卡片）
            when(conditionEvaluator.evaluate(eq(unsatisfiedCondition), any(CardDTO.class), any(CardDTO.class)))
                    .thenReturn(false);

            // mock LinkFieldConfig 返回字段名称
            LinkFieldConfig linkFieldConfig = createLinkFieldConfig("263671031548350464:SOURCE", "父卡片");
            when(schemaCacheService.getById("263671031548350464:SOURCE"))
                    .thenReturn(Optional.of(linkFieldConfig));

            // 应抛出对侧权限拒绝异常
            assertThatThrownBy(() -> permissionService.checkFieldEditPermission(
                    targetCardId, operatorId, changedFieldIds, linkFieldIds, targetCardIdsByLinkField))
                    .isInstanceOf(PermissionDeniedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PERMISSION_DENIED_OPPOSITE_FIELD_EDIT")
                    .hasMessageContaining("父卡片");
        }

        @Test
        @DisplayName("双侧都配置权限，当前侧不满足时拒绝（当前侧提示优先）")
        void bothSidesConfigured_currentSideNotSatisfied_deniedWithCurrentSideMessage() {
            CardId targetCardId = CardId.of(1001L);
            Set<FieldId> changedFieldIds = Set.of();
            Set<String> linkFieldIds = Set.of("263671031548350464:SOURCE");
            Map<String, List<String>> targetCardIdsByLinkField = Map.of(
                    "263671031548350464:SOURCE", List.of("2001")
            );

            // 当前侧配置了权限
            CardBasicInfo basicInfo = createBasicInfo(1001L, "story");
            when(cardCacheService.getBasicInfoById(targetCardId))
                    .thenReturn(Optional.of(basicInfo));

            Condition unsatisfiedCondition = new Condition();
            PermissionConfigDefinition currentConfig = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of("263671031548350464:SOURCE")),
                    List.of(unsatisfiedCondition),
                    List.of(unsatisfiedCondition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("story")))
                    .thenReturn(List.of(currentConfig));
            when(permissionConfigYieldBuilder.buildCardYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());
            when(permissionConfigYieldBuilder.buildMemberYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());

            CardDTO targetCard = createCardDTO(1001L);
            CardDTO memberCard = createCardDTO(3001L);
            when(cardRepository.findById(eq(targetCardId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));

            // 条件不满足
            when(conditionEvaluator.evaluate(eq(unsatisfiedCondition), any(CardDTO.class), any(CardDTO.class)))
                    .thenReturn(false);

            // 应抛出当前侧权限拒绝异常（优先）
            assertThatThrownBy(() -> permissionService.checkFieldEditPermission(
                    targetCardId, operatorId, changedFieldIds, linkFieldIds, targetCardIdsByLinkField))
                    .isInstanceOf(PermissionDeniedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PERMISSION_DENIED_FIELD_EDIT");
        }

        @Test
        @DisplayName("双侧都配置权限，当前侧满足但对侧不满足时拒绝（对侧提示）")
        void bothSidesConfigured_currentSideSatisfied_oppositeSideNotSatisfied_deniedWithOppositeSideMessage() {
            CardId targetCardId = CardId.of(1001L);
            Set<FieldId> changedFieldIds = Set.of();
            Set<String> linkFieldIds = Set.of("263671031548350464:SOURCE");
            Map<String, List<String>> targetCardIdsByLinkField = Map.of(
                    "263671031548350464:SOURCE", List.of("2001")
            );

            // 当前侧配置了权限且满足
            CardBasicInfo basicInfo = createBasicInfo(1001L, "story");
            when(cardCacheService.getBasicInfoById(targetCardId))
                    .thenReturn(Optional.of(basicInfo));

            Condition satisfiedCondition = new Condition();
            PermissionConfigDefinition currentConfig = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of("263671031548350464:SOURCE")),
                    List.of(satisfiedCondition),
                    List.of(satisfiedCondition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("story")))
                    .thenReturn(List.of(currentConfig));

            // 对侧卡片 - 批量获取基本信息
            CardId oppositeCardId = CardId.of(2001L);
            when(cardCacheService.getBasicInfoByIds(eq(Set.of(oppositeCardId))))
                    .thenReturn(Map.of(oppositeCardId, createBasicInfo(2001L, "task")));

            // 对侧配置了权限但不满足
            Condition unsatisfiedCondition = new Condition();
            PermissionConfigDefinition oppositeConfig = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of("263671031548350464:TARGET")),
                    List.of(unsatisfiedCondition),
                    List.of(unsatisfiedCondition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("task")))
                    .thenReturn(List.of(oppositeConfig));
            when(permissionConfigYieldBuilder.buildCardYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());
            when(permissionConfigYieldBuilder.buildMemberYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());

            CardDTO targetCard = createCardDTO(1001L);
            CardDTO memberCard = createCardDTO(3001L);

            when(cardRepository.findById(eq(targetCardId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));

            // 当前侧条件满足
            when(conditionEvaluator.evaluate(eq(satisfiedCondition), eq(targetCard), eq(memberCard)))
                    .thenReturn(true);
            when(conditionEvaluator.evaluate(eq(satisfiedCondition), eq(memberCard), eq(memberCard)))
                    .thenReturn(true);
            // 对侧操作人条件不满足（会提前抛异常，不会查询对端卡片）
            when(conditionEvaluator.evaluate(eq(unsatisfiedCondition), any(CardDTO.class), any(CardDTO.class)))
                    .thenReturn(false);

            // mock LinkFieldConfig 返回字段名称
            LinkFieldConfig linkFieldConfig = createLinkFieldConfig("263671031548350464:SOURCE", "父卡片");
            when(schemaCacheService.getById("263671031548350464:SOURCE"))
                    .thenReturn(Optional.of(linkFieldConfig));

            // 应抛出对侧权限拒绝异常
            assertThatThrownBy(() -> permissionService.checkFieldEditPermission(
                    targetCardId, operatorId, changedFieldIds, linkFieldIds, targetCardIdsByLinkField))
                    .isInstanceOf(PermissionDeniedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PERMISSION_DENIED_OPPOSITE_FIELD_EDIT")
                    .hasMessageContaining("父卡片");
        }

        @Test
        @DisplayName("部分字段有配置权限，更新无配置的字段时允许")
        void someFieldsHaveConfig_updateFieldWithoutConfig_allowed() {
            CardId targetCardId = CardId.of(1001L);
            // 更新两个字段：一个有配置，一个没有配置
            Set<FieldId> changedFieldIds = Set.of(
                    FieldId.of("title"),      // 无配置
                    FieldId.of("priority")    // 有配置
            );
            Set<String> linkFieldIds = Set.of();
            Map<String, List<String>> targetCardIdsByLinkField = Map.of();

            CardBasicInfo basicInfo = createBasicInfo(1001L, "story");
            when(cardCacheService.getBasicInfoById(targetCardId))
                    .thenReturn(Optional.of(basicInfo));

            // 只配置了 priority 字段的权限
            Condition unsatisfiedCondition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of("priority")),  // 只有 priority 有配置
                    List.of(unsatisfiedCondition),
                    List.of(unsatisfiedCondition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("story")))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildCardYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());
            when(permissionConfigYieldBuilder.buildMemberYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());

            CardDTO targetCard = createCardDTO(1001L);
            CardDTO memberCard = createCardDTO(3001L);
            when(cardRepository.findById(eq(targetCardId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));

            // priority 字段条件不满足
            when(conditionEvaluator.evaluate(eq(unsatisfiedCondition), eq(targetCard), eq(memberCard)))
                    .thenReturn(false);
            when(conditionEvaluator.evaluate(eq(unsatisfiedCondition), eq(memberCard), eq(memberCard)))
                    .thenReturn(false);

            // title 字段没有配置，应该允许更新
            // priority 字段有配置但条件不满足，应该被拒绝
            assertThatThrownBy(() -> permissionService.checkFieldEditPermission(
                    targetCardId, operatorId, changedFieldIds, linkFieldIds, targetCardIdsByLinkField))
                    .isInstanceOf(PermissionDeniedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PERMISSION_DENIED_FIELD_EDIT");
        }

        @Test
        @DisplayName("部分字段有配置权限，只更新无配置的字段时允许")
        void someFieldsHaveConfig_updateOnlyFieldWithoutConfig_allowed() {
            CardId targetCardId = CardId.of(1001L);
            // 只更新没有配置的字段
            Set<FieldId> changedFieldIds = Set.of(FieldId.of("title"));  // 无配置
            Set<String> linkFieldIds = Set.of();
            Map<String, List<String>> targetCardIdsByLinkField = Map.of();

            CardBasicInfo basicInfo = createBasicInfo(1001L, "story");
            when(cardCacheService.getBasicInfoById(targetCardId))
                    .thenReturn(Optional.of(basicInfo));

            // 配置了 priority 字段的权限，但 title 没有配置
            Condition unsatisfiedCondition = new Condition();
            PermissionConfigDefinition config = createFieldPermissionConfig(
                    FieldOperation.EDIT,
                    List.of(FieldId.of("priority")),  // priority 有配置
                    List.of(unsatisfiedCondition),
                    List.of(unsatisfiedCondition)
            );

            when(permissionConfigCacheService.getPermissionConfigs(CardTypeId.of("story")))
                    .thenReturn(List.of(config));
            when(permissionConfigYieldBuilder.buildCardYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());
            when(permissionConfigYieldBuilder.buildMemberYield(any(), eq(CardOperation.EDIT)))
                    .thenReturn(Yield.basic());

            CardDTO targetCard = createCardDTO(1001L);
            CardDTO memberCard = createCardDTO(3001L);
            when(cardRepository.findById(eq(targetCardId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(targetCard));
            when(cardRepository.findById(eq(operatorId), any(Yield.class), eq("system")))
                    .thenReturn(Optional.of(memberCard));

            // title 字段没有配置任何权限，应该直接允许

            // 不应抛出异常
            permissionService.checkFieldEditPermission(
                    targetCardId, operatorId, changedFieldIds, linkFieldIds, targetCardIdsByLinkField);
        }
    }
}
