package cn.planka.card.service.cascadefield;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.UpdateLinkRequest;
import cn.planka.api.card.request.Yield;
import cn.planka.card.repository.CardRepository;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.card.OrgId;
import cn.planka.domain.field.FieldId;
import cn.planka.domain.field.CascadeFieldValue;
import cn.planka.domain.field.CascadeItem;
import cn.planka.domain.link.LinkFieldId;
import cn.planka.domain.link.LinkPosition;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.CascadeRelationId;
import cn.planka.domain.field.FieldConfigId;
import cn.planka.domain.schema.definition.fieldconfig.CascadeFieldConfig;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationDefinition;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationLevel;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationLevelBinding;
import cn.planka.event.publisher.EventPublisher;
import cn.planka.infra.cache.schema.SchemaCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import zgraph.driver.ZgraphWriteClient;
import zgraph.driver.proto.write.BatchCreateLinkRequest;
import zgraph.driver.proto.write.BatchLinkCommonResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CascadeFieldLinkSyncService 单元测试")
class CascadeFieldLinkSyncServiceTest {

    @Mock
    private SchemaCacheService schemaCacheService;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private ZgraphWriteClient writeClient;
    @Mock
    private EventPublisher eventPublisher;

    private CascadeFieldLinkSyncService service;

    // 测试数据常量
    private static final String ORG_ID = "org_001";
    private static final String OPERATOR_ID = "user_001";
    private static final String CARD_TYPE_ID = "ct_user_story";
    private static final String CARD_ID = "100";

    // 测试用卡片ID（数字）
    private static final String TRIBE_CARD_ID = "200";
    private static final String SQUAD_CARD_ID = "300";
    private static final String TRIBE_X_CARD_ID = "201";
    private static final String SQUAD_EXISTING_CARD_ID = "301";

    // 级联关系层级
    private static final String TRIBE_LINK_FIELD_ID = "lt_tribe:SOURCE";      // 用户故事 → 部落
    private static final String SQUAD_LINK_FIELD_ID = "lt_squad:SOURCE";      // 用户故事 → 小队
    private static final String PARENT_LINK_FIELD_ID = "lt_parent:TARGET";    // 小队 → 部落

    // 级联属性
    private static final String STRUCTURE_FIELD_ID = "sf_001";
    private static final String STRUCTURE_ID = "struct_001";

    // 第二个级联属性（用于测试多级联属性绑定同一关联属性的场景）
    private static final String STRUCTURE_FIELD_ID_2 = "sf_002";
    private static final String STRUCTURE_ID_2 = "struct_002";

    @BeforeEach
    void setUp() {
        service = new CascadeFieldLinkSyncService(
                schemaCacheService, cardRepository, writeClient, eventPublisher);
    }

    @Nested
    @DisplayName("findStructureBinding - 级联绑定查找")
    class FindStructureBindingTests {

        @Test
        @DisplayName("当关联属性属于级联绑定时返回匹配信息")
        void returnsMatch_whenLinkFieldBelongsToStructureBinding() {
            // 准备级联属性定义
            CascadeFieldConfig cascadeRelationDef = createCascadeFieldConfig();
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(cascadeRelationDef));

            // 执行
            Optional<CascadeFieldBindingMatch> result = service.findStructureBinding(CARD_TYPE_ID, SQUAD_LINK_FIELD_ID);

            // 验证
            assertThat(result).isPresent();
            assertThat(result.get().levelIndex()).isEqualTo(1); // 小队是第二层（index=1）
            assertThat(result.get().cascadeFieldDef().getId().value()).isEqualTo(STRUCTURE_FIELD_ID);
        }

        @Test
        @DisplayName("当关联属性不属于任何级联绑定时返回空")
        void returnsEmpty_whenLinkFieldNotInAnyStructureBinding() {
            // 准备级联属性定义（不包含测试的关联属性）
            CascadeFieldConfig cascadeRelationDef = createCascadeFieldConfig();
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(cascadeRelationDef));

            // 执行 - 使用不在绑定中的关联属性
            Optional<CascadeFieldBindingMatch> result = service.findStructureBinding(CARD_TYPE_ID, "unknown_link:SOURCE");

            // 验证
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("当实体类型没有级联属性时返回空")
        void returnsEmpty_whenNoStructureFieldDefinition() {
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of());

            Optional<CascadeFieldBindingMatch> result = service.findStructureBinding(CARD_TYPE_ID, SQUAD_LINK_FIELD_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllStructureBindings - 多级联绑定查找")
    class FindAllStructureBindingsTests {

        @Test
        @DisplayName("当关联属性被多个级联属性绑定时返回所有匹配")
        void returnsAllMatches_whenLinkFieldBoundToMultipleStructures() {
            // 准备两个级联属性定义，都绑定了同一个关联属性
            CascadeFieldConfig cascadeRelationDef1 = createCascadeFieldConfig();
            CascadeFieldConfig cascadeRelationDef2 = createSecondCascadeFieldConfig();

            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(cascadeRelationDef1, cascadeRelationDef2));

            // 执行 - 查找绑定了 TRIBE_LINK_FIELD_ID 的所有级联属性
            List<CascadeFieldBindingMatch> result = service.findAllStructureBindings(CARD_TYPE_ID, TRIBE_LINK_FIELD_ID);

            // 验证返回两个匹配
            assertThat(result).hasSize(2);
            assertThat(result.stream().map(m -> m.cascadeFieldDef().getId().value()))
                    .containsExactlyInAnyOrder(STRUCTURE_FIELD_ID, STRUCTURE_FIELD_ID_2);
        }

        @Test
        @DisplayName("当关联属性只被一个级联属性绑定时返回单个匹配")
        void returnsSingleMatch_whenLinkFieldBoundToOneStructure() {
            CascadeFieldConfig cascadeRelationDef = createCascadeFieldConfig();
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(cascadeRelationDef));

            List<CascadeFieldBindingMatch> result = service.findAllStructureBindings(CARD_TYPE_ID, TRIBE_LINK_FIELD_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).cascadeFieldDef().getId().value()).isEqualTo(STRUCTURE_FIELD_ID);
        }

        @Test
        @DisplayName("当关联属性不属于任何级联绑定时返回空列表")
        void returnsEmptyList_whenNoBindingFound() {
            CascadeFieldConfig cascadeRelationDef = createCascadeFieldConfig();
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(cascadeRelationDef));

            List<CascadeFieldBindingMatch> result = service.findAllStructureBindings(CARD_TYPE_ID, "unknown_link:SOURCE");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCascadeFieldValuesBeforeUpdate - 多级联属性旧值获取")
    class GetStructureValuesBeforeUpdateTests {

        @Test
        @DisplayName("当关联属性被多个级联属性绑定时返回所有旧值")
        void returnsAllOldValues_whenMultipleStructuresBound() {
            // 准备两个级联属性定义
            CascadeFieldConfig cascadeRelationDef1 = createCascadeFieldConfig();
            CascadeFieldConfig cascadeRelationDef2 = createSecondCascadeFieldConfig();

            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(cascadeRelationDef1, cascadeRelationDef2));

            // 准备卡片查询
            setupEmptyCardQueryMock();

            // 执行
            Map<String, CascadeFieldValue> result = service.getCascadeFieldValuesBeforeUpdate(
                    CARD_ID, CARD_TYPE_ID, TRIBE_LINK_FIELD_ID, OPERATOR_ID);

            // 验证返回两个级联属性的旧值
            assertThat(result).hasSize(2);
            assertThat(result).containsKeys(STRUCTURE_FIELD_ID, STRUCTURE_FIELD_ID_2);
        }

        @Test
        @DisplayName("当关联属性不属于任何级联绑定时返回空Map")
        void returnsEmptyMap_whenNoBindingFound() {
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of());

            Map<String, CascadeFieldValue> result = service.getCascadeFieldValuesBeforeUpdate(
                    CARD_ID, CARD_TYPE_ID, "unknown_link:SOURCE", OPERATOR_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("syncCascadeFieldLinks - 入口1：关联属性更新时的联动同步")
    class SyncStructureLinksTests {

        @Test
        @DisplayName("当关联属性不属于级联绑定时不执行同步")
        void noSync_whenLinkFieldNotInStructureBinding() {
            when(schemaCacheService.getBySecondaryIndex(anyString(), anyString(), any()))
                    .thenReturn(List.of());

            CascadeFieldSyncResult result = service.syncCascadeFieldLinks(
                    CARD_ID, CARD_TYPE_ID, "other_link:SOURCE",
                    List.of("target_001"), Map.of(), ORG_ID, OPERATOR_ID, null);

            assertThat(result.synced()).isFalse();
            verifyNoInteractions(writeClient);
        }

        @Test
        @DisplayName("关联下级（小队）时自动同步上级（部落）")
        void syncsUpperLevel_whenLinkingToLowerLevel() {
            // 准备测试数据
            setupStructureBindingMocks();
            setupCardQueryMocks();
            setupWriteClientMocks();

            // 执行 - 用户故事关联小队（小队属于部落X）
            CascadeFieldSyncResult result = service.syncCascadeFieldLinks(
                    CARD_ID, CARD_TYPE_ID, SQUAD_LINK_FIELD_ID,
                    List.of(SQUAD_CARD_ID), Map.of(), ORG_ID, OPERATOR_ID, null);

            // 验证同步结果
            assertThat(result.synced()).isTrue();
            assertThat(result.addedLinks()).containsKey(TRIBE_LINK_FIELD_ID);
        }

        @Test
        @DisplayName("关联根层级（部落）时清除下级（小队）")
        void clearsLowerLevels_whenLinkingToRootLevel() {
            // 准备测试数据
            setupStructureBindingMocks();
            setupCardWithExistingSquadLink();
            setupWriteClientMocks();

            // 执行 - 用户故事关联部落（应该清除小队关联）
            CascadeFieldSyncResult result = service.syncCascadeFieldLinks(
                    CARD_ID, CARD_TYPE_ID, TRIBE_LINK_FIELD_ID,
                    List.of(TRIBE_CARD_ID), Map.of(), ORG_ID, OPERATOR_ID, null);

            // 验证：下级关联被清除
            assertThat(result.synced()).isTrue();
            assertThat(result.removedLinks()).containsKey(SQUAD_LINK_FIELD_ID);
        }

        @Test
        @DisplayName("清空关联时清除所有下级")
        void clearsAllLowerLevels_whenClearingLink() {
            // 准备测试数据
            setupStructureBindingMocks();
            setupCardWithExistingSquadLink();
            setupWriteClientMocks();

            // 执行 - 清空部落关联
            CascadeFieldSyncResult result = service.syncCascadeFieldLinks(
                    CARD_ID, CARD_TYPE_ID, TRIBE_LINK_FIELD_ID,
                    List.of(), Map.of(), ORG_ID, OPERATOR_ID, null);  // 空列表表示清空

            // 验证：下级关联被清除
            assertThat(result.synced()).isTrue();
        }

        @Test
        @DisplayName("当关联属性被多个级联属性绑定时同步所有级联属性")
        void syncsAllStructures_whenLinkFieldBoundToMultiple() {
            // 准备两个级联属性定义及对应级联关系定义
            CascadeFieldConfig cascadeFieldConfig1 = createCascadeFieldConfig();
            CascadeFieldConfig cascadeFieldConfig2 = createSecondCascadeFieldConfig();
            CascadeRelationDefinition cascadeRelationDefinition1 = createCascadeRelationDefinition();
            CascadeRelationDefinition cascadeRelationDefinition2 = createSecondCascadeRelationDefinition();

            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(cascadeFieldConfig1, cascadeFieldConfig2));
            when(schemaCacheService.getById(STRUCTURE_ID))
                    .thenReturn(Optional.of(cascadeRelationDefinition1));
            when(schemaCacheService.getById(STRUCTURE_ID_2))
                    .thenReturn(Optional.of(cascadeRelationDefinition2));

            setupCardWithExistingSquadLink();
            setupWriteClientMocks();

            // 执行 - 更新部落关联
            CascadeFieldSyncResult result = service.syncCascadeFieldLinks(
                    CARD_ID, CARD_TYPE_ID, TRIBE_LINK_FIELD_ID,
                    List.of(TRIBE_CARD_ID), Map.of(), ORG_ID, OPERATOR_ID, null);

            // 验证同步结果
            assertThat(result.synced()).isTrue();

            // 验证发布了事件：2个级联属性变更事件 + 可能的关联同步事件
            verify(eventPublisher, atLeast(2)).publish(any());
        }

        @Test
        @DisplayName("使用预计算的旧值发布变更事件")
        void usesPrecomputedOldValues_whenPublishingEvents() {
            // 准备测试数据
            setupStructureBindingMocks();
            setupCardWithExistingSquadLink();
            setupWriteClientMocks();

            // 预计算的旧值
            CascadeItem oldItem = new CascadeItem(TRIBE_X_CARD_ID, "部落X",
                    new CascadeItem(SQUAD_EXISTING_CARD_ID, "小队旧", null));
            CascadeFieldValue oldValue = new CascadeFieldValue(STRUCTURE_FIELD_ID, oldItem);
            Map<String, CascadeFieldValue> oldValues = Map.of(STRUCTURE_FIELD_ID, oldValue);

            // 执行
            service.syncCascadeFieldLinks(
                    CARD_ID, CARD_TYPE_ID, TRIBE_LINK_FIELD_ID,
                    List.of(TRIBE_CARD_ID), oldValues, ORG_ID, OPERATOR_ID, null);

            // 验证事件发布
            verify(eventPublisher, atLeastOnce()).publish(any());
        }
    }

    @Nested
    @DisplayName("applyCascadeFieldValue - 入口2：级联属性更新转关联")
    class ApplyCascadeFieldValueTests {

        @Test
        @DisplayName("将完整级联路径转换为多个关联更新")
        void convertsFullPathToMultipleLinkUpdates() {
            // 准备级联属性定义
            CascadeFieldConfig cascadeRelationDef = createCascadeFieldConfig();
            when(schemaCacheService.getById(STRUCTURE_FIELD_ID))
                    .thenReturn(Optional.of(cascadeRelationDef));

            // 准备级联属性值（部落A/小队A）
            CascadeItem tribeItem = new CascadeItem(TRIBE_CARD_ID, "部落A",
                    new CascadeItem(SQUAD_CARD_ID, "小队A", null));
            CascadeFieldValue cascadeFieldValue = new CascadeFieldValue(STRUCTURE_FIELD_ID, tribeItem);

            // 准备 Mock 回调
            CascadeFieldLinkSyncService.LinkUpdater mockUpdater = mock(CascadeFieldLinkSyncService.LinkUpdater.class);

            // 准备卡片查询（用于构建旧值）
            setupEmptyCardQueryMock();

            // 执行
            service.applyCascadeFieldValue(
                    CARD_ID, CARD_TYPE_ID, cascadeFieldValue,
                    ORG_ID, OPERATOR_ID, null, mockUpdater);

            // 验证：调用了两次关联更新
            ArgumentCaptor<UpdateLinkRequest> requestCaptor = ArgumentCaptor.forClass(UpdateLinkRequest.class);
            verify(mockUpdater, times(2)).updateLink(
                    requestCaptor.capture(), eq(ORG_ID), eq(OPERATOR_ID), isNull(), eq(true));

            List<UpdateLinkRequest> requests = requestCaptor.getAllValues();
            assertThat(requests).hasSize(2);

            // 验证部落层级更新
            UpdateLinkRequest tribeRequest = requests.stream()
                    .filter(r -> r.getLinkFieldId().equals(TRIBE_LINK_FIELD_ID))
                    .findFirst().orElseThrow();
            assertThat(tribeRequest.getTargetCardIds()).containsExactly(TRIBE_CARD_ID);

            // 验证小队层级更新
            UpdateLinkRequest squadRequest = requests.stream()
                    .filter(r -> r.getLinkFieldId().equals(SQUAD_LINK_FIELD_ID))
                    .findFirst().orElseThrow();
            assertThat(squadRequest.getTargetCardIds()).containsExactly(SQUAD_CARD_ID);
        }

        @Test
        @DisplayName("仅设置根层级时清空下级关联")
        void clearsLowerLevels_whenOnlyRootLevelSet() {
            // 准备级联属性定义
            CascadeFieldConfig cascadeRelationDef = createCascadeFieldConfig();
            when(schemaCacheService.getById(STRUCTURE_FIELD_ID))
                    .thenReturn(Optional.of(cascadeRelationDef));

            // 准备级联属性值（只有部落A）
            CascadeItem tribeItem = new CascadeItem(TRIBE_CARD_ID, "部落A", null);
            CascadeFieldValue cascadeFieldValue = new CascadeFieldValue(STRUCTURE_FIELD_ID, tribeItem);

            // 准备 Mock 回调
            CascadeFieldLinkSyncService.LinkUpdater mockUpdater = mock(CascadeFieldLinkSyncService.LinkUpdater.class);

            // 准备卡片查询
            setupEmptyCardQueryMock();

            // 执行
            service.applyCascadeFieldValue(
                    CARD_ID, CARD_TYPE_ID, cascadeFieldValue,
                    ORG_ID, OPERATOR_ID, null, mockUpdater);

            // 验证
            ArgumentCaptor<UpdateLinkRequest> requestCaptor = ArgumentCaptor.forClass(UpdateLinkRequest.class);
            verify(mockUpdater, times(2)).updateLink(
                    requestCaptor.capture(), eq(ORG_ID), eq(OPERATOR_ID), isNull(), eq(true));

            // 验证小队层级被清空
            UpdateLinkRequest squadRequest = requestCaptor.getAllValues().stream()
                    .filter(r -> r.getLinkFieldId().equals(SQUAD_LINK_FIELD_ID))
                    .findFirst().orElseThrow();
            assertThat(squadRequest.getTargetCardIds()).isEmpty();
        }
    }

    // ==================== 辅助方法 ====================

    private CascadeFieldConfig createCascadeFieldConfig() {
        CascadeFieldConfig def = new CascadeFieldConfig(
                FieldConfigId.of(STRUCTURE_FIELD_ID),
                ORG_ID,
                "测试级联属性",
                null,
                FieldId.of(STRUCTURE_FIELD_ID),
                false
        );
        def.setCascadeRelationId(CascadeRelationId.of(STRUCTURE_ID));
        def.setLevelBindings(List.of(
                new CascadeRelationLevelBinding(0, LinkFieldId.of("lt_tribe", LinkPosition.SOURCE), true),
                new CascadeRelationLevelBinding(1, LinkFieldId.of("lt_squad", LinkPosition.SOURCE), false)
        ));
        return def;
    }

    private CascadeRelationDefinition createCascadeRelationDefinition() {
        CascadeRelationDefinition def = new CascadeRelationDefinition(
                CascadeRelationId.of(STRUCTURE_ID),
                ORG_ID,
                "测试级联关系"
        );
        def.setLevels(List.of(
                new CascadeRelationLevel(0, "部落", CardTypeId.of("ct_tribe"), null, null, null, null),
                new CascadeRelationLevel(1, "小队", CardTypeId.of("ct_squad"),
                        LinkFieldId.of("lt_parent", LinkPosition.TARGET), null, null, null)
        ));
        return def;
    }

    /**
     * 创建第二个级联属性定义（用于测试多级联属性绑定同一关联属性的场景）
     * 这个级联属性也绑定了 TRIBE_LINK_FIELD_ID
     */
    private CascadeFieldConfig createSecondCascadeFieldConfig() {
        CascadeFieldConfig def = new CascadeFieldConfig(
                FieldConfigId.of(STRUCTURE_FIELD_ID_2),
                ORG_ID,
                "测试级联属性2",
                null,
                FieldId.of(STRUCTURE_FIELD_ID_2),
                false
        );
        def.setCascadeRelationId(CascadeRelationId.of(STRUCTURE_ID_2));
        // 只绑定部落层级，与第一个级联属性共享同一个关联属性
        def.setLevelBindings(List.of(
                new CascadeRelationLevelBinding(0, LinkFieldId.of("lt_tribe", LinkPosition.SOURCE), true)
        ));
        return def;
    }

    private CascadeRelationDefinition createSecondCascadeRelationDefinition() {
        CascadeRelationDefinition def = new CascadeRelationDefinition(
                CascadeRelationId.of(STRUCTURE_ID_2),
                ORG_ID,
                "测试级联关系2"
        );
        def.setLevels(List.of(
                new CascadeRelationLevel(0, "部落", CardTypeId.of("ct_tribe"), null, null, null, null)
        ));
        return def;
    }

    private void setupStructureBindingMocks() {
        CascadeFieldConfig cascadeFieldConfig = createCascadeFieldConfig();
        CascadeRelationDefinition cascadeRelationDefinition = createCascadeRelationDefinition();

        when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                .thenReturn(List.of(cascadeFieldConfig));
        when(schemaCacheService.getById(STRUCTURE_ID))
                .thenReturn(Optional.of(cascadeRelationDefinition));
    }

    private void setupCardQueryMocks() {
        // 模拟当前卡片（用户故事）- 没有现有关联
        CardDTO currentCard = createCardDTO(CARD_ID, CARD_TYPE_ID, Map.of());
        when(cardRepository.findById(eq(CardId.of(CARD_ID)), any(Yield.class), eq(OPERATOR_ID)))
                .thenReturn(Optional.of(currentCard));

        // 模拟小队卡片（有上级部落）
        CardDTO tribeCard = createCardDTO(TRIBE_X_CARD_ID, "ct_tribe", Map.of());
        CardDTO squadCard = createCardDTO(SQUAD_CARD_ID, "ct_squad",
                Map.of(PARENT_LINK_FIELD_ID, Set.of(tribeCard)));
        when(cardRepository.findById(eq(CardId.of(SQUAD_CARD_ID)), any(Yield.class), eq(OPERATOR_ID)))
                .thenReturn(Optional.of(squadCard));
    }

    private void setupCardWithExistingSquadLink() {
        // 模拟当前卡片有现有的小队关联
        CardDTO squadLinkedCard = createCardDTO(SQUAD_EXISTING_CARD_ID, "ct_squad", Map.of());
        CardDTO currentCard = createCardDTO(CARD_ID, CARD_TYPE_ID,
                Map.of(SQUAD_LINK_FIELD_ID, Set.of(squadLinkedCard)));

        when(cardRepository.findById(eq(CardId.of(CARD_ID)), any(Yield.class), eq(OPERATOR_ID)))
                .thenReturn(Optional.of(currentCard));
    }

    private void setupEmptyCardQueryMock() {
        CardDTO emptyCard = createCardDTO(CARD_ID, CARD_TYPE_ID, Map.of());
        when(cardRepository.findById(any(CardId.class), any(Yield.class), anyString()))
                .thenReturn(Optional.of(emptyCard));
    }

    private void setupWriteClientMocks() {
        BatchLinkCommonResponse response = BatchLinkCommonResponse.newBuilder()
                .setSuccess(1)
                .build();
        when(writeClient.batchCreateLink(any(BatchCreateLinkRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(writeClient.batchDeleteLink(any()))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    private CardDTO createCardDTO(String cardId, String cardTypeId, Map<String, Set<CardDTO>> linkedCards) {
        CardDTO dto = new CardDTO();
        dto.setId(CardId.of(cardId));
        dto.setTypeId(CardTypeId.of(cardTypeId));
        dto.setOrgId(OrgId.of(ORG_ID));
        dto.setLinkedCards(linkedCards.isEmpty() ? null : linkedCards);
        return dto;
    }
}
