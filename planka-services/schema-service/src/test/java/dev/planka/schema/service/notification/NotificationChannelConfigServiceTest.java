package dev.planka.schema.service.notification;

import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.api.schema.request.notification.CreateNotificationChannelRequest;
import dev.planka.api.schema.request.notification.UpdateNotificationChannelRequest;
import dev.planka.api.schema.vo.notification.NotificationChannelConfigVO;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.notification.NotificationChannelConfigDefinition;
import dev.planka.domain.notification.NotificationChannelConfigId;
import dev.planka.domain.schema.EntityState;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaCommonService;
import dev.planka.schema.service.common.SchemaQuery;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationChannelConfigService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationChannelConfigService 测试")
class NotificationChannelConfigServiceTest {

    @Mock
    private SchemaRepository schemaRepository;

    @Mock
    private SchemaCommonService schemaCommonService;

    @Mock
    private SchemaQuery schemaQuery;

    private NotificationChannelConfigService notificationChannelConfigService;

    private static final String ORG_ID = "org-1";
    private static final String OPERATOR_ID = "operator-1";

    @BeforeEach
    void setUp() {
        notificationChannelConfigService = new NotificationChannelConfigService(
                schemaRepository, schemaCommonService, schemaQuery);
    }

    @Nested
    @DisplayName("list 测试")
    class ListTests {

        @Test
        @DisplayName("返回组织下所有通知渠道配置")
        void shouldReturnAllNotificationChannels() {
            // Given
            NotificationChannelConfigDefinition config1 = createConfig("nc-1", "邮件渠道", "email");
            NotificationChannelConfigDefinition config2 = createConfig("nc-2", "飞书渠道", "feishu");

            when(schemaQuery.query(ORG_ID, SchemaType.NOTIFICATION_CHANNEL_CONFIG))
                    .thenReturn(List.of(config1, config2));

            // When
            Result<List<NotificationChannelConfigVO>> result = notificationChannelConfigService.list(ORG_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(2);
            assertThat(result.getData().get(0).getName()).isEqualTo("邮件渠道");
            assertThat(result.getData().get(1).getName()).isEqualTo("飞书渠道");
        }

        @Test
        @DisplayName("空列表时返回空结果")
        void shouldReturnEmptyListWhenNoConfigs() {
            // Given
            when(schemaQuery.query(ORG_ID, SchemaType.NOTIFICATION_CHANNEL_CONFIG))
                    .thenReturn(Collections.emptyList());

            // When
            Result<List<NotificationChannelConfigVO>> result = notificationChannelConfigService.list(ORG_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("过滤非通知渠道类型的配置")
        void shouldFilterNonNotificationChannelConfigs() {
            // Given
            NotificationChannelConfigDefinition config = createConfig("nc-1", "邮件渠道", "email");
            SchemaDefinition<?> otherSchema = org.mockito.Mockito.mock(SchemaDefinition.class);
            // Note: 不需要设置 getSchemaType()，因为代码中使用 instanceof 检查

            when(schemaQuery.query(ORG_ID, SchemaType.NOTIFICATION_CHANNEL_CONFIG))
                    .thenReturn(List.of(config, otherSchema));

            // When
            Result<List<NotificationChannelConfigVO>> result = notificationChannelConfigService.list(ORG_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getId()).isEqualTo("nc-1");
        }
    }

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTests {

        @Test
        @DisplayName("配置存在时返回详情")
        void shouldReturnConfigWhenExists() {
            // Given
            NotificationChannelConfigDefinition config = createConfig("nc-1", "邮件渠道", "email");
            when(schemaRepository.findById("nc-1")).thenReturn(Optional.of(config));

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.getById("nc-1");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo("nc-1");
            assertThat(result.getData().getName()).isEqualTo("邮件渠道");
            assertThat(result.getData().getChannelId()).isEqualTo("email");
        }

        @Test
        @DisplayName("配置不存在时返回失败")
        void shouldReturnFailureWhenNotFound() {
            // Given
            when(schemaRepository.findById("not-exist")).thenReturn(Optional.empty());

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.getById("not-exist");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("配置类型不匹配时返回失败")
        void shouldReturnFailureWhenTypeMismatch() {
            // Given
            // 使用非 NotificationChannelConfigDefinition 的类型，instanceof 检查会自然失败
            SchemaDefinition<?> otherSchema = org.mockito.Mockito.mock(SchemaDefinition.class);
            when(schemaRepository.findById("ct-1")).thenReturn(Optional.of(otherSchema));

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.getById("ct-1");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.BAD_REQUEST.getCode());
        }
    }

    @Nested
    @DisplayName("create 测试")
    class CreateTests {

        @Test
        @DisplayName("成功创建邮件渠道配置")
        void shouldCreateEmailConfigSuccessfully() {
            // Given
            CreateNotificationChannelRequest request = new CreateNotificationChannelRequest();
            request.setName("公司邮件服务器");
            request.setChannelId("email");
            request.setConfig(Map.of(
                    "host", "smtp.company.com",
                    "port", 587,
                    "username", "notify@company.com",
                    "ssl", true
            ));
            request.setDefault(true);
            request.setPriority(1);

            when(schemaCommonService.create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class)))
                    .thenAnswer(invocation -> {
                        CreateSchemaRequest schemaRequest = invocation.getArgument(2);
                        return Result.success(schemaRequest.getDefinition());
                    });

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.create(
                    ORG_ID, OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getName()).isEqualTo("公司邮件服务器");
            assertThat(result.getData().getChannelId()).isEqualTo("email");
            assertThat(result.getData().getConfig()).containsEntry("host", "smtp.company.com");
            assertThat(result.getData().isDefault()).isTrue();
            assertThat(result.getData().getPriority()).isEqualTo(1);
            assertThat(result.getData().isEnabled()).isTrue();

            verify(schemaCommonService).create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class));
        }

        @Test
        @DisplayName("成功创建飞书渠道配置")
        void shouldCreateFeishuConfigSuccessfully() {
            // Given
            CreateNotificationChannelRequest request = new CreateNotificationChannelRequest();
            request.setName("飞书应用");
            request.setChannelId("feishu");
            request.setConfig(Map.of(
                    "appId", "cli_xxx",
                    "appSecret", "secret_xxx"
            ));
            request.setDefault(false);
            request.setPriority(2);

            when(schemaCommonService.create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class)))
                    .thenAnswer(invocation -> {
                        CreateSchemaRequest schemaRequest = invocation.getArgument(2);
                        return Result.success(schemaRequest.getDefinition());
                    });

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.create(
                    ORG_ID, OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getChannelId()).isEqualTo("feishu");
            assertThat(result.getData().getConfig()).containsEntry("appId", "cli_xxx");
        }

        @Test
        @DisplayName("创建失败时返回错误")
        void shouldReturnFailureWhenCreateFails() {
            // Given
            CreateNotificationChannelRequest request = new CreateNotificationChannelRequest();
            request.setName("邮件渠道");
            request.setChannelId("email");
            request.setConfig(Map.of());

            when(schemaCommonService.create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class)))
                    .thenReturn(Result.failure(CommonErrorCode.DATA_ALREADY_EXISTS, "配置名称已存在"));

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.create(
                    ORG_ID, OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_ALREADY_EXISTS.getCode());
        }
    }

    @Nested
    @DisplayName("update 测试")
    class UpdateTests {

        @Test
        @DisplayName("成功更新渠道配置")
        void shouldUpdateConfigSuccessfully() {
            // Given
            NotificationChannelConfigDefinition existingConfig = createConfig("nc-1", "旧名称", "email");
            existingConfig.setConfig(Map.of("host", "old.smtp.com"));
            existingConfig.setContentVersion(1);

            when(schemaRepository.findById("nc-1")).thenReturn(Optional.of(existingConfig));
            when(schemaCommonService.update(eq("nc-1"), eq(OPERATOR_ID), any(UpdateSchemaRequest.class)))
                    .thenAnswer(invocation -> {
                        UpdateSchemaRequest schemaRequest = invocation.getArgument(2);
                        return Result.success(schemaRequest.getDefinition());
                    });

            UpdateNotificationChannelRequest request = new UpdateNotificationChannelRequest();
            request.setName("新名称");
            request.setConfig(Map.of("host", "new.smtp.com"));
            request.setDefault(false);
            request.setPriority(2);

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.update(
                    "nc-1", OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getName()).isEqualTo("新名称");
            assertThat(result.getData().getConfig()).containsEntry("host", "new.smtp.com");
        }

        @Test
        @DisplayName("配置不存在时返回失败")
        void shouldFailWhenConfigNotFound() {
            // Given
            when(schemaRepository.findById("not-exist")).thenReturn(Optional.empty());

            UpdateNotificationChannelRequest request = new UpdateNotificationChannelRequest();
            request.setName("新名称");

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.update(
                    "not-exist", OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("配置类型不匹配时返回失败")
        void shouldFailWhenTypeMismatch() {
            // Given
            // 使用非 NotificationChannelConfigDefinition 的类型，instanceof 检查会自然失败
            SchemaDefinition<?> otherSchema = org.mockito.Mockito.mock(SchemaDefinition.class);
            when(schemaRepository.findById("ct-1")).thenReturn(Optional.of(otherSchema));

            UpdateNotificationChannelRequest request = new UpdateNotificationChannelRequest();
            request.setName("新名称");

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.update(
                    "ct-1", OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.BAD_REQUEST.getCode());
        }

        @Test
        @DisplayName("版本冲突时返回失败")
        void shouldFailWhenVersionConflict() {
            // Given
            NotificationChannelConfigDefinition existingConfig = createConfig("nc-1", "名称", "email");
            existingConfig.setContentVersion(5);

            when(schemaRepository.findById("nc-1")).thenReturn(Optional.of(existingConfig));
            when(schemaCommonService.update(eq("nc-1"), eq(OPERATOR_ID), any(UpdateSchemaRequest.class)))
                    .thenReturn(Result.failure(CommonErrorCode.CONFLICT, "数据已被其他用户修改"));

            UpdateNotificationChannelRequest request = new UpdateNotificationChannelRequest();
            request.setName("新名称");
            request.setExpectedVersion(3);

            // When
            Result<NotificationChannelConfigVO> result = notificationChannelConfigService.update(
                    "nc-1", OPERATOR_ID, request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.CONFLICT.getCode());
        }
    }

    @Nested
    @DisplayName("delete 测试")
    class DeleteTests {

        @Test
        @DisplayName("成功删除渠道配置")
        void shouldDeleteConfigSuccessfully() {
            // Given
            when(schemaCommonService.delete(eq("nc-1"), eq(OPERATOR_ID)))
                    .thenReturn(Result.success());

            // When
            Result<Void> result = notificationChannelConfigService.delete("nc-1", OPERATOR_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(schemaCommonService).delete(eq("nc-1"), eq(OPERATOR_ID));
        }

        @Test
        @DisplayName("删除失败时返回错误")
        void shouldReturnFailureWhenDeleteFails() {
            // Given
            when(schemaCommonService.delete(eq("nc-1"), eq(OPERATOR_ID)))
                    .thenReturn(Result.failure(CommonErrorCode.DATA_NOT_FOUND, "配置不存在"));

            // When
            Result<Void> result = notificationChannelConfigService.delete("nc-1", OPERATOR_ID);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("activate 测试")
    class ActivateTests {

        @Test
        @DisplayName("成功启用渠道配置")
        void shouldActivateConfigSuccessfully() {
            // Given
            when(schemaCommonService.activate(eq("nc-1"), eq(OPERATOR_ID)))
                    .thenReturn(Result.success());

            // When
            Result<Void> result = notificationChannelConfigService.activate("nc-1", OPERATOR_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(schemaCommonService).activate(eq("nc-1"), eq(OPERATOR_ID));
        }
    }

    @Nested
    @DisplayName("disable 测试")
    class DisableTests {

        @Test
        @DisplayName("成功停用渠道配置")
        void shouldDisableConfigSuccessfully() {
            // Given
            when(schemaCommonService.disable(eq("nc-1"), eq(OPERATOR_ID)))
                    .thenReturn(Result.success());

            // When
            Result<Void> result = notificationChannelConfigService.disable("nc-1", OPERATOR_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(schemaCommonService).disable(eq("nc-1"), eq(OPERATOR_ID));
        }
    }

    // ==================== 辅助方法 ====================

    private NotificationChannelConfigDefinition createConfig(String id, String name, String channelId) {
        NotificationChannelConfigDefinition config = new NotificationChannelConfigDefinition(
                NotificationChannelConfigId.of(id), ORG_ID, name);
        config.setChannelId(channelId);
        config.setConfig(Map.of());
        config.setDefault(false);
        config.setPriority(100);
        config.setEnabled(true);
        config.setState(EntityState.ACTIVE);
        config.setContentVersion(1);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }
}
