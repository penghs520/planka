package dev.planka.card.service.structure;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.UpdateLinkRequest;
import dev.planka.api.card.request.Yield;
import dev.planka.card.repository.CardRepository;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.card.OrgId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.field.StructureFieldValue;
import dev.planka.domain.field.StructureItem;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.StructureId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.schema.definition.fieldconfig.StructureFieldConfig;
import dev.planka.domain.schema.definition.structure.StructureDefinition;
import dev.planka.domain.schema.definition.structure.StructureLevel;
import dev.planka.domain.schema.definition.structure.StructureLevelBinding;
import dev.planka.event.publisher.EventPublisher;
import dev.planka.infra.cache.schema.SchemaCacheService;
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
import planka.graph.driver.PlankaGraphWriteClient;
import planka.graph.driver.proto.write.BatchCreateLinkRequest;
import planka.graph.driver.proto.write.BatchLinkCommonResponse;

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
@DisplayName("StructureLinkSyncService 单元测试")
class StructureLinkSyncServiceTest {

    @Mock
    private SchemaCacheService schemaCacheService;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private PlankaGraphWriteClient writeClient;
    @Mock
    private EventPublisher eventPublisher;

    private StructureLinkSyncService service;

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

    // 架构线层级
    private static final String TRIBE_LINK_FIELD_ID = "lt_tribe:SOURCE";      // 用户故事 → 部落
    private static final String SQUAD_LINK_FIELD_ID = "lt_squad:SOURCE";      // 用户故事 → 小队
    private static final String PARENT_LINK_FIELD_ID = "lt_parent:TARGET";    // 小队 → 部落

    // 架构属性
    private static final String STRUCTURE_FIELD_ID = "sf_001";
    private static final String STRUCTURE_ID = "struct_001";

    // 第二个架构属性（用于测试多架构属性绑定同一关联属性的场景）
    private static final String STRUCTURE_FIELD_ID_2 = "sf_002";
    private static final String STRUCTURE_ID_2 = "struct_002";

    @BeforeEach
    void setUp() {
        service = new StructureLinkSyncService(
                schemaCacheService, cardRepository, writeClient, eventPublisher);
    }

    @Nested
    @DisplayName("findStructureBinding - 架构绑定查找")
    class FindStructureBindingTests {

        @Test
        @DisplayName("当关联属性属于架构绑定时返回匹配信息")
        void returnsMatch_whenLinkFieldBelongsToStructureBinding() {
            // 准备架构属性定义
            StructureFieldConfig structureDef = createStructureFieldConfig();
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(structureDef));

            // 执行
            Optional<StructureBindingMatch> result = service.findStructureBinding(CARD_TYPE_ID, SQUAD_LINK_FIELD_ID);

            // 验证
            assertThat(result).isPresent();
            assertThat(result.get().levelIndex()).isEqualTo(1); // 小队是第二层（index=1）
            assertThat(result.get().structureFieldDef().getId().value()).isEqualTo(STRUCTURE_FIELD_ID);
        }

        @Test
        @DisplayName("当关联属性不属于任何架构绑定时返回空")
        void returnsEmpty_whenLinkFieldNotInAnyStructureBinding() {
            // 准备架构属性定义（不包含测试的关联属性）
            StructureFieldConfig structureDef = createStructureFieldConfig();
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(structureDef));

            // 执行 - 使用不在绑定中的关联属性
            Optional<StructureBindingMatch> result = service.findStructureBinding(CARD_TYPE_ID, "unknown_link:SOURCE");

            // 验证
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("当卡片类型没有架构属性时返回空")
        void returnsEmpty_whenNoStructureFieldDefinition() {
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of());

            Optional<StructureBindingMatch> result = service.findStructureBinding(CARD_TYPE_ID, SQUAD_LINK_FIELD_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllStructureBindings - 多架构绑定查找")
    class FindAllStructureBindingsTests {

        @Test
        @DisplayName("当关联属性被多个架构属性绑定时返回所有匹配")
        void returnsAllMatches_whenLinkFieldBoundToMultipleStructures() {
            // 准备两个架构属性定义，都绑定了同一个关联属性
            StructureFieldConfig structureDef1 = createStructureFieldConfig();
            StructureFieldConfig structureDef2 = createSecondStructureFieldConfig();

            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(structureDef1, structureDef2));

            // 执行 - 查找绑定了 TRIBE_LINK_FIELD_ID 的所有架构属性
            List<StructureBindingMatch> result = service.findAllStructureBindings(CARD_TYPE_ID, TRIBE_LINK_FIELD_ID);

            // 验证返回两个匹配
            assertThat(result).hasSize(2);
            assertThat(result.stream().map(m -> m.structureFieldDef().getId().value()))
                    .containsExactlyInAnyOrder(STRUCTURE_FIELD_ID, STRUCTURE_FIELD_ID_2);
        }

        @Test
        @DisplayName("当关联属性只被一个架构属性绑定时返回单个匹配")
        void returnsSingleMatch_whenLinkFieldBoundToOneStructure() {
            StructureFieldConfig structureDef = createStructureFieldConfig();
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(structureDef));

            List<StructureBindingMatch> result = service.findAllStructureBindings(CARD_TYPE_ID, TRIBE_LINK_FIELD_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).structureFieldDef().getId().value()).isEqualTo(STRUCTURE_FIELD_ID);
        }

        @Test
        @DisplayName("当关联属性不属于任何架构绑定时返回空列表")
        void returnsEmptyList_whenNoBindingFound() {
            StructureFieldConfig structureDef = createStructureFieldConfig();
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(structureDef));

            List<StructureBindingMatch> result = service.findAllStructureBindings(CARD_TYPE_ID, "unknown_link:SOURCE");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStructureValuesBeforeUpdate - 多架构属性旧值获取")
    class GetStructureValuesBeforeUpdateTests {

        @Test
        @DisplayName("当关联属性被多个架构属性绑定时返回所有旧值")
        void returnsAllOldValues_whenMultipleStructuresBound() {
            // 准备两个架构属性定义
            StructureFieldConfig structureDef1 = createStructureFieldConfig();
            StructureFieldConfig structureDef2 = createSecondStructureFieldConfig();

            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(structureDef1, structureDef2));

            // 准备卡片查询
            setupEmptyCardQueryMock();

            // 执行
            Map<String, StructureFieldValue> result = service.getStructureValuesBeforeUpdate(
                    CARD_ID, CARD_TYPE_ID, TRIBE_LINK_FIELD_ID, OPERATOR_ID);

            // 验证返回两个架构属性的旧值
            assertThat(result).hasSize(2);
            assertThat(result).containsKeys(STRUCTURE_FIELD_ID, STRUCTURE_FIELD_ID_2);
        }

        @Test
        @DisplayName("当关联属性不属于任何架构绑定时返回空Map")
        void returnsEmptyMap_whenNoBindingFound() {
            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of());

            Map<String, StructureFieldValue> result = service.getStructureValuesBeforeUpdate(
                    CARD_ID, CARD_TYPE_ID, "unknown_link:SOURCE", OPERATOR_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("syncStructureLinks - 入口1：关联属性更新时的联动同步")
    class SyncStructureLinksTests {

        @Test
        @DisplayName("当关联属性不属于架构绑定时不执行同步")
        void noSync_whenLinkFieldNotInStructureBinding() {
            when(schemaCacheService.getBySecondaryIndex(anyString(), anyString(), any()))
                    .thenReturn(List.of());

            StructureSyncResult result = service.syncStructureLinks(
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
            StructureSyncResult result = service.syncStructureLinks(
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
            StructureSyncResult result = service.syncStructureLinks(
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
            StructureSyncResult result = service.syncStructureLinks(
                    CARD_ID, CARD_TYPE_ID, TRIBE_LINK_FIELD_ID,
                    List.of(), Map.of(), ORG_ID, OPERATOR_ID, null);  // 空列表表示清空

            // 验证：下级关联被清除
            assertThat(result.synced()).isTrue();
        }

        @Test
        @DisplayName("当关联属性被多个架构属性绑定时同步所有架构属性")
        void syncsAllStructures_whenLinkFieldBoundToMultiple() {
            // 准备两个架构属性定义
            StructureFieldConfig structureDef1 = createStructureFieldConfig();
            StructureFieldConfig structureDef2 = createSecondStructureFieldConfig();
            StructureDefinition structureLineDef1 = createStructureDefinition();
            StructureDefinition structureLineDef2 = createSecondStructureDefinition();

            when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                    .thenReturn(List.of(structureDef1, structureDef2));
            when(schemaCacheService.getById(STRUCTURE_ID))
                    .thenReturn(Optional.of(structureLineDef1));
            when(schemaCacheService.getById(STRUCTURE_ID_2))
                    .thenReturn(Optional.of(structureLineDef2));

            setupCardWithExistingSquadLink();
            setupWriteClientMocks();

            // 执行 - 更新部落关联
            StructureSyncResult result = service.syncStructureLinks(
                    CARD_ID, CARD_TYPE_ID, TRIBE_LINK_FIELD_ID,
                    List.of(TRIBE_CARD_ID), Map.of(), ORG_ID, OPERATOR_ID, null);

            // 验证同步结果
            assertThat(result.synced()).isTrue();

            // 验证发布了事件：2个架构属性变更事件 + 可能的关联同步事件
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
            StructureItem oldItem = new StructureItem(TRIBE_X_CARD_ID, "部落X",
                    new StructureItem(SQUAD_EXISTING_CARD_ID, "小队旧", null));
            StructureFieldValue oldValue = new StructureFieldValue(STRUCTURE_FIELD_ID, oldItem);
            Map<String, StructureFieldValue> oldValues = Map.of(STRUCTURE_FIELD_ID, oldValue);

            // 执行
            service.syncStructureLinks(
                    CARD_ID, CARD_TYPE_ID, TRIBE_LINK_FIELD_ID,
                    List.of(TRIBE_CARD_ID), oldValues, ORG_ID, OPERATOR_ID, null);

            // 验证事件发布
            verify(eventPublisher, atLeastOnce()).publish(any());
        }
    }

    @Nested
    @DisplayName("applyStructureFieldValue - 入口2：架构属性更新转关联")
    class ApplyStructureFieldValueTests {

        @Test
        @DisplayName("将完整架构路径转换为多个关联更新")
        void convertsFullPathToMultipleLinkUpdates() {
            // 准备架构属性定义
            StructureFieldConfig structureDef = createStructureFieldConfig();
            when(schemaCacheService.getById(STRUCTURE_FIELD_ID))
                    .thenReturn(Optional.of(structureDef));

            // 准备架构属性值（部落A/小队A）
            StructureItem tribeItem = new StructureItem(TRIBE_CARD_ID, "部落A",
                    new StructureItem(SQUAD_CARD_ID, "小队A", null));
            StructureFieldValue structureValue = new StructureFieldValue(STRUCTURE_FIELD_ID, tribeItem);

            // 准备 Mock 回调
            StructureLinkSyncService.LinkUpdater mockUpdater = mock(StructureLinkSyncService.LinkUpdater.class);

            // 准备卡片查询（用于构建旧值）
            setupEmptyCardQueryMock();

            // 执行
            service.applyStructureFieldValue(
                    CARD_ID, CARD_TYPE_ID, structureValue,
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
            // 准备架构属性定义
            StructureFieldConfig structureDef = createStructureFieldConfig();
            when(schemaCacheService.getById(STRUCTURE_FIELD_ID))
                    .thenReturn(Optional.of(structureDef));

            // 准备架构属性值（只有部落A）
            StructureItem tribeItem = new StructureItem(TRIBE_CARD_ID, "部落A", null);
            StructureFieldValue structureValue = new StructureFieldValue(STRUCTURE_FIELD_ID, tribeItem);

            // 准备 Mock 回调
            StructureLinkSyncService.LinkUpdater mockUpdater = mock(StructureLinkSyncService.LinkUpdater.class);

            // 准备卡片查询
            setupEmptyCardQueryMock();

            // 执行
            service.applyStructureFieldValue(
                    CARD_ID, CARD_TYPE_ID, structureValue,
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

    private StructureFieldConfig createStructureFieldConfig() {
        StructureFieldConfig def = new StructureFieldConfig(
                FieldConfigId.of(STRUCTURE_FIELD_ID),
                ORG_ID,
                "测试架构属性",
                null,
                FieldId.of(STRUCTURE_FIELD_ID),
                false
        );
        def.setStructureId(StructureId.of(STRUCTURE_ID));
        def.setLevelBindings(List.of(
                new StructureLevelBinding(0, LinkFieldId.of("lt_tribe", LinkPosition.SOURCE), true),
                new StructureLevelBinding(1, LinkFieldId.of("lt_squad", LinkPosition.SOURCE), false)
        ));
        return def;
    }

    private StructureDefinition createStructureDefinition() {
        StructureDefinition def = new StructureDefinition(
                StructureId.of(STRUCTURE_ID),
                ORG_ID,
                "测试架构线"
        );
        def.setLevels(List.of(
                new StructureLevel(0, "部落", List.of(CardTypeId.of("ct_tribe")), null, null, null, null),
                new StructureLevel(1, "小队", List.of(CardTypeId.of("ct_squad")),
                        LinkFieldId.of("lt_parent", LinkPosition.TARGET), null, null, null)
        ));
        return def;
    }

    /**
     * 创建第二个架构属性定义（用于测试多架构属性绑定同一关联属性的场景）
     * 这个架构属性也绑定了 TRIBE_LINK_FIELD_ID
     */
    private StructureFieldConfig createSecondStructureFieldConfig() {
        StructureFieldConfig def = new StructureFieldConfig(
                FieldConfigId.of(STRUCTURE_FIELD_ID_2),
                ORG_ID,
                "测试架构属性2",
                null,
                FieldId.of(STRUCTURE_FIELD_ID_2),
                false
        );
        def.setStructureId(StructureId.of(STRUCTURE_ID_2));
        // 只绑定部落层级，与第一个架构属性共享同一个关联属性
        def.setLevelBindings(List.of(
                new StructureLevelBinding(0, LinkFieldId.of("lt_tribe", LinkPosition.SOURCE), true)
        ));
        return def;
    }

    private StructureDefinition createSecondStructureDefinition() {
        StructureDefinition def = new StructureDefinition(
                StructureId.of(STRUCTURE_ID_2),
                ORG_ID,
                "测试架构线2"
        );
        def.setLevels(List.of(
                new StructureLevel(0, "部落", List.of(CardTypeId.of("ct_tribe")), null, null, null, null)
        ));
        return def;
    }

    private void setupStructureBindingMocks() {
        StructureFieldConfig structureDef = createStructureFieldConfig();
        StructureDefinition structureLineDef = createStructureDefinition();

        when(schemaCacheService.getBySecondaryIndex(eq("CARD_TYPE"), eq(CARD_TYPE_ID), eq(SchemaType.FIELD_CONFIG)))
                .thenReturn(List.of(structureDef));
        when(schemaCacheService.getById(STRUCTURE_ID))
                .thenReturn(Optional.of(structureLineDef));
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
