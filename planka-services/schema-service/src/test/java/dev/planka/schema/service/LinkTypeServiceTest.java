package dev.planka.schema.service;

import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.api.schema.request.linktype.CreateLinkTypeRequest;
import dev.planka.api.schema.request.linktype.UpdateLinkTypeRequest;
import dev.planka.api.schema.vo.linktype.LinkTypeOptionVO;
import dev.planka.api.schema.vo.linktype.LinkTypeVO;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.link.LinkTypeId;
import dev.planka.domain.schema.EntityState;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.cardtype.AbstractCardType;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaCommonService;
import dev.planka.schema.service.common.SchemaQuery;
import dev.planka.schema.service.linktype.LinkTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LinkTypeService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class LinkTypeServiceTest {

    @Mock
    private SchemaRepository schemaRepository;

    @Mock
    private SchemaCommonService schemaCommonService;

    @Mock
    private SchemaQuery schemaQuery;

    private LinkTypeService linkTypeService;

    private static final String ORG_ID = "org-1";
    private static final String OPERATOR_ID = "operator-1";

    @BeforeEach
    void setUp() {
        linkTypeService = new LinkTypeService(schemaRepository, schemaCommonService, schemaQuery);
    }

    @Nested
    @DisplayName("listLinkTypes 测试")
    class ListLinkTypesTests {

        @Test
        @DisplayName("返回组织下所有关联类型")
        void shouldReturnAllLinkTypes() {
            // Given
            LinkTypeDefinition linkType1 = createLinkType("lt-1", "父子关联");
            LinkTypeDefinition linkType2 = createLinkType("lt-2", "依赖关联");

            when(schemaQuery.query(ORG_ID, SchemaType.LINK_TYPE))
                    .thenReturn(List.of(linkType1, linkType2));

            // When
            Result<List<LinkTypeVO>> result = linkTypeService.listLinkTypes(ORG_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(2);
            assertThat(result.getData().get(0).getName()).isEqualTo("源端->目标端");
            assertThat(result.getData().get(1).getName()).isEqualTo("源端->目标端");
        }

        @Test
        @DisplayName("空列表时返回空结果")
        void shouldReturnEmptyListWhenNoLinkTypes() {
            // Given
            when(schemaQuery.query(ORG_ID, SchemaType.LINK_TYPE))
                    .thenReturn(Collections.emptyList());

            // When
            Result<List<LinkTypeVO>> result = linkTypeService.listLinkTypes(ORG_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
        }
    }

    @Nested
    @DisplayName("listLinkTypeOptions 测试")
    class ListLinkTypeOptionsTests {

        @Test
        @DisplayName("只返回启用的关联类型选项")
        void shouldReturnOnlyEnabledLinkTypeOptions() {
            // Given
            LinkTypeDefinition enabledLinkType = createLinkType("lt-1", "启用关联");
            enabledLinkType.setEnabled(true);

            LinkTypeDefinition disabledLinkType = createLinkType("lt-2", "禁用关联");
            disabledLinkType.setEnabled(false);

            when(schemaQuery.query(ORG_ID, SchemaType.LINK_TYPE))
                    .thenReturn(List.of(enabledLinkType, disabledLinkType));

            // When
            Result<List<LinkTypeOptionVO>> result = linkTypeService.listLinkTypeOptions(ORG_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getName()).isEqualTo("源端->目标端");
        }
    }

    @Nested
    @DisplayName("getLinkTypeById 测试")
    class GetLinkTypeByIdTests {

        @Test
        @DisplayName("关联类型存在时返回详情")
        void shouldReturnLinkTypeWhenExists() {
            // Given
            LinkTypeDefinition linkType = createLinkType("lt-1", "父子关联");
            when(schemaRepository.findById("lt-1")).thenReturn(Optional.of(linkType));

            // When
            Result<LinkTypeVO> result = linkTypeService.getLinkTypeById("lt-1");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo("lt-1");
            assertThat(result.getData().getName()).isEqualTo("源端->目标端");
        }

        @Test
        @DisplayName("关联类型不存在时返回失败")
        void shouldReturnFailureWhenNotFound() {
            // Given
            when(schemaRepository.findById("not-exist")).thenReturn(Optional.empty());

            // When
            Result<LinkTypeVO> result = linkTypeService.getLinkTypeById("not-exist");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("createLinkType 测试")
    class CreateLinkTypeTests {

        @Test
        @DisplayName("成功创建关联类型")
        void shouldCreateLinkTypeSuccessfully() {
            // Given
            CreateLinkTypeRequest request = new CreateLinkTypeRequest();
            request.setSourceName("父卡片");
            request.setTargetName("子卡片");
            request.setSourceVisible(true);
            request.setTargetVisible(true);
            request.setSourceMultiSelect(true);
            request.setTargetMultiSelect(true);

            when(schemaCommonService.create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class)))
                    .thenAnswer(invocation -> {
                        CreateSchemaRequest schemaRequest = invocation.getArgument(2);
                        return Result.success(schemaRequest.getDefinition());
                    });

            // When
            Result<LinkTypeVO> result = linkTypeService.createLinkType(ORG_ID, OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getName()).isEqualTo("父卡片->子卡片");
            assertThat(result.getData().getSourceName()).isEqualTo("父卡片");
            assertThat(result.getData().getTargetName()).isEqualTo("子卡片");
            assertThat(result.getData().isSourceVisible()).isTrue();
            assertThat(result.getData().isTargetVisible()).isTrue();

            verify(schemaCommonService).create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class));
        }

        @Test
        @DisplayName("指定多个源端卡片类型时必须都是属性集")
        void shouldFailWhenMultipleSourceCardTypesNotAllAbstract() {
            // Given
            CreateLinkTypeRequest request = new CreateLinkTypeRequest();
            request.setSourceName("源端");
            request.setTargetName("目标端");
            request.setSourceCardTypeIds(List.of("ct-1", "ct-2"));

            AbstractCardType abstractType = createAbstractCardType("ct-1", "属性集");
            EntityCardType concreteType = createEntityCardType("ct-2", "实体类型");

            when(schemaRepository.findByIds(any())).thenReturn(List.of(abstractType, concreteType));

            // When
            Result<LinkTypeVO> result = linkTypeService.createLinkType(ORG_ID, OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR.getCode());
            assertThat(result.getMessage()).contains("属性集");
        }

        @Test
        @DisplayName("指定多个属性集时成功创建")
        void shouldSucceedWhenMultipleSourceCardTypesAllAbstract() {
            // Given
            CreateLinkTypeRequest request = new CreateLinkTypeRequest();
            request.setSourceName("源端");
            request.setTargetName("目标端");
            request.setSourceCardTypeIds(List.of("ct-1", "ct-2"));

            AbstractCardType abstractType1 = createAbstractCardType("ct-1", "属性集1");
            AbstractCardType abstractType2 = createAbstractCardType("ct-2", "属性集2");

            when(schemaRepository.findByIds(any())).thenReturn(List.of(abstractType1, abstractType2));
            when(schemaCommonService.create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class)))
                    .thenAnswer(invocation -> {
                        CreateSchemaRequest schemaRequest = invocation.getArgument(2);
                        return Result.success(schemaRequest.getDefinition());
                    });

            // When
            Result<LinkTypeVO> result = linkTypeService.createLinkType(ORG_ID, OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("单个卡片类型不验证是否抽象")
        void shouldNotValidateWhenSingleCardType() {
            // Given
            CreateLinkTypeRequest request = new CreateLinkTypeRequest();
            request.setSourceName("源端");
            request.setTargetName("目标端");
            request.setSourceCardTypeIds(List.of("ct-1"));

            when(schemaCommonService.create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class)))
                    .thenAnswer(invocation -> {
                        CreateSchemaRequest schemaRequest = invocation.getArgument(2);
                        return Result.success(schemaRequest.getDefinition());
                    });

            // When
            Result<LinkTypeVO> result = linkTypeService.createLinkType(ORG_ID, OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateLinkType 测试")
    class UpdateLinkTypeTests {

        @Test
        @DisplayName("成功更新关联类型")
        void shouldUpdateLinkTypeSuccessfully() {
            // Given
            LinkTypeDefinition existingLinkType = createLinkType("lt-1", "旧名称");
            existingLinkType.setContentVersion(1);

            when(schemaRepository.findById("lt-1")).thenReturn(Optional.of(existingLinkType));
            when(schemaCommonService.update(eq("lt-1"), eq(OPERATOR_ID), any(UpdateSchemaRequest.class)))
                    .thenAnswer(invocation -> {
                        UpdateSchemaRequest schemaRequest = invocation.getArgument(2);
                        return Result.success(schemaRequest.getDefinition());
                    });

            UpdateLinkTypeRequest request = new UpdateLinkTypeRequest();
            request.setSourceName("新源端");
            request.setTargetName("新目标端");
            request.setDescription("新描述");

            // When
            Result<LinkTypeVO> result = linkTypeService.updateLinkType("lt-1", OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getName()).isEqualTo("新源端->新目标端");
            assertThat(result.getData().getDescription()).isEqualTo("新描述");
        }

        @Test
        @DisplayName("关联类型不存在时返回失败")
        void shouldFailWhenLinkTypeNotFound() {
            // Given
            when(schemaRepository.findById("not-exist")).thenReturn(Optional.empty());

            UpdateLinkTypeRequest request = new UpdateLinkTypeRequest();
            request.setDescription("新描述");

            // When
            Result<LinkTypeVO> result = linkTypeService.updateLinkType("not-exist", OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("版本冲突时返回失败")
        void shouldFailWhenVersionConflict() {
            // Given
            LinkTypeDefinition existingLinkType = createLinkType("lt-1", "名称");
            existingLinkType.setContentVersion(5);

            when(schemaRepository.findById("lt-1")).thenReturn(Optional.of(existingLinkType));
            when(schemaCommonService.update(eq("lt-1"), eq(OPERATOR_ID), any(UpdateSchemaRequest.class)))
                    .thenReturn(Result.failure(CommonErrorCode.CONFLICT, "数据已被其他用户修改，请刷新后重试"));

            UpdateLinkTypeRequest request = new UpdateLinkTypeRequest();
            request.setDescription("新描述");
            request.setExpectedVersion(3);  // 期望版本与实际不符

            // When
            Result<LinkTypeVO> result = linkTypeService.updateLinkType("lt-1", OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.CONFLICT.getCode());
        }
    }

    @Nested
    @DisplayName("getAvailableLinkTypes 测试")
    class GetAvailableLinkTypesTests {

        @Test
        @DisplayName("返回卡片类型可用的关联类型（不限位置）")
        void shouldReturnAvailableLinkTypesForCardType() {
            // Given
            EntityCardType cardType = createEntityCardType("ct-1", "需求");

            LinkTypeDefinition linkType1 = createLinkType("lt-1", "父子关联");
            linkType1.setEnabled(true);

            LinkTypeDefinition linkType2 = createLinkType("lt-2", "限定关联");
            linkType2.setEnabled(true);
            linkType2.setSourceCardTypeIds(List.of(CardTypeId.of("ct-2")));  // 限定其他卡片类型
            linkType2.setTargetCardTypeIds(List.of(CardTypeId.of("ct-2"))); // 目标端也限定为其他卡片类型

            when(schemaRepository.findById("ct-1")).thenReturn(Optional.of(cardType));
            when(schemaQuery.queryAll(ORG_ID, SchemaType.LINK_TYPE))
                    .thenReturn(List.of(linkType1, linkType2));

            // When
            Result<List<LinkTypeOptionVO>> result = linkTypeService.getAvailableLinkTypes("ct-1", null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getName()).isEqualTo("源端->目标端");
        }

        @Test
        @DisplayName("按 SOURCE 位置筛选可用关联类型")
        void shouldFilterBySourcePosition() {
            // Given
            EntityCardType cardType = createEntityCardType("ct-1", "需求");

            LinkTypeDefinition linkType = createLinkType("lt-1", "父子关联");
            linkType.setEnabled(true);
            linkType.setSourceCardTypeIds(List.of(CardTypeId.of("ct-1")));
            linkType.setTargetCardTypeIds(List.of(CardTypeId.of("ct-2")));

            when(schemaRepository.findById("ct-1")).thenReturn(Optional.of(cardType));
            when(schemaQuery.queryAll(ORG_ID, SchemaType.LINK_TYPE))
                    .thenReturn(List.of(linkType));

            // When - 作为源端
            Result<List<LinkTypeOptionVO>> sourceResult =
                    linkTypeService.getAvailableLinkTypes("ct-1", LinkPosition.SOURCE);

            // Then
            assertThat(sourceResult.isSuccess()).isTrue();
            assertThat(sourceResult.getData()).hasSize(1);

            // When - 作为目标端
            Result<List<LinkTypeOptionVO>> targetResult =
                    linkTypeService.getAvailableLinkTypes("ct-1", LinkPosition.TARGET);

            // Then
            assertThat(targetResult.isSuccess()).isTrue();
            assertThat(targetResult.getData()).isEmpty();
        }

        @Test
        @DisplayName("卡片类型不存在时返回失败")
        void shouldFailWhenCardTypeNotFound() {
            // Given
            when(schemaRepository.findById("not-exist")).thenReturn(Optional.empty());

            // When
            Result<List<LinkTypeOptionVO>> result =
                    linkTypeService.getAvailableLinkTypes("not-exist", null);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("deleteLinkType 测试")
    class DeleteLinkTypeTests {

        @Test
        @DisplayName("成功删除关联类型")
        void shouldDeleteLinkTypeSuccessfully() {
            // Given
            when(schemaCommonService.delete(eq("lt-1"), eq(OPERATOR_ID))).thenReturn(Result.success());

            // When
            Result<Void> result = linkTypeService.deleteLinkType("lt-1", OPERATOR_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(schemaCommonService).delete(eq("lt-1"), eq(OPERATOR_ID));
        }
    }

    // ==================== 辅助方法 ====================

    private LinkTypeDefinition createLinkType(String id, String name) {
        // name参数已废弃，不再使用。name由sourceName和targetName拼接而成
        LinkTypeDefinition linkType = new LinkTypeDefinition(
                LinkTypeId.of(id), ORG_ID, "源端->目标端");
        linkType.setSourceName("源端");
        linkType.setTargetName("目标端");
        linkType.setSourceVisible(true);
        linkType.setTargetVisible(true);
        linkType.setSourceMultiSelect(true);
        linkType.setTargetMultiSelect(true);
        linkType.setState(EntityState.ACTIVE);
        linkType.setEnabled(true);
        linkType.setContentVersion(1);
        linkType.setCreatedAt(LocalDateTime.now());
        linkType.setUpdatedAt(LocalDateTime.now());
        return linkType;
    }

    private AbstractCardType createAbstractCardType(String id, String name) {
        return new AbstractCardType(CardTypeId.of(id), ORG_ID, name);
    }

    private EntityCardType createEntityCardType(String id, String name) {
        return new EntityCardType(CardTypeId.of(id), ORG_ID, name);
    }
}
