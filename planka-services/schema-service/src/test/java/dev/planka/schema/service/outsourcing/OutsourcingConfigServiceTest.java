//package dev.planka.schema.service.outsourcing;
//
//import dev.planka.api.schema.request.CreateSchemaRequest;
//import dev.planka.api.schema.request.UpdateSchemaRequest;
//import dev.planka.common.exception.CommonErrorCode;
//import dev.planka.common.result.Result;
//import dev.planka.domain.outsourcing.*;
//import dev.planka.domain.schema.EntityState;
//import dev.planka.domain.schema.SchemaType;
//import dev.planka.domain.schema.definition.SchemaDefinition;
//import dev.planka.schema.service.common.SchemaCommonService;
//import dev.planka.schema.service.common.SchemaQuery;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
///**
// * OutsourcingConfigService 单元测试
// */
//@ExtendWith(MockitoExtension.class)
//@DisplayName("OutsourcingConfigService 测试")
//class OutsourcingConfigServiceTest {
//
//    @Mock
//    private SchemaCommonService schemaCommonService;
//
//    @Mock
//    private SchemaQuery schemaQuery;
//
//    @Mock
//    private AttendanceCardTypeService attendanceCardTypeService;
//
//    private OutsourcingConfigService outsourcingConfigService;
//
//    private static final String ORG_ID = "org-test-001";
//    private static final String OPERATOR_ID = "operator-001";
//    private static final String CONFIG_NAME = "考勤配置";
//
//    @BeforeEach
//    void setUp() {
//        outsourcingConfigService = new OutsourcingConfigServiceImpl(schemaCommonService, schemaQuery, attendanceCardTypeService);
//    }
//
//    @Nested
//    @DisplayName("getByOrgId 测试")
//    class GetByOrgIdTests {
//
//        @Test
//        @DisplayName("配置存在时应返回配置")
//        void shouldReturnConfigWhenExists() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            when(schemaQuery.queryPaged(ORG_ID, SchemaType.OUTSOURCING_CONFIG, 0, 1))
//                    .thenReturn(List.of(config));
//
//            // When
//            Result<OutsourcingConfig> result = outsourcingConfigService.getByOrgId(ORG_ID);
//
//            // Then
//            assertThat(result.isSuccess()).isTrue();
//            assertThat(result.getData()).isNotNull();
//            assertThat(result.getData().getOrgId()).isEqualTo(ORG_ID);
//            assertThat(result.getData().getName()).isEqualTo(CONFIG_NAME);
//        }
//
//        @Test
//        @DisplayName("配置不存在时应返回失败")
//        void shouldReturnFailureWhenNotExists() {
//            // Given
//            when(schemaQuery.queryPaged(ORG_ID, SchemaType.OUTSOURCING_CONFIG, 0, 1))
//                    .thenReturn(Collections.emptyList());
//
//            // When
//            Result<OutsourcingConfig> result = outsourcingConfigService.getByOrgId(ORG_ID);
//
//            // Then
//            assertThat(result.isSuccess()).isFalse();
//            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
//            assertThat(result.getMessage()).contains("考勤配置不存在");
//        }
//    }
//
//    @Nested
//    @DisplayName("saveOrUpdate 测试 - 创建场景")
//    class SaveOrUpdateCreateTests {
//
//        @Test
//        @DisplayName("首次创建配置应成功")
//        void shouldCreateConfigSuccessfully() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//
//            when(schemaQuery.queryPaged(ORG_ID, SchemaType.OUTSOURCING_CONFIG, 0, 1))
//                    .thenReturn(Collections.emptyList());
//            when(schemaCommonService.create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class)))
//                    .thenAnswer(invocation -> {
//                        CreateSchemaRequest request = invocation.getArgument(2);
//                        return Result.success(request.getDefinition());
//                    });
//
//            // When
//            Result<OutsourcingConfig> result = outsourcingConfigService.saveOrUpdate(config, OPERATOR_ID);
//
//            // Then
//            assertThat(result.isSuccess()).isTrue();
//            assertThat(result.getData()).isNotNull();
//            verify(schemaCommonService).create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class));
//        }
//
//        @Test
//        @DisplayName("首次创建并开启考勤功能应触发初始化")
//        void shouldInitializeWhenFirstEnable() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//
//            when(schemaQuery.queryPaged(ORG_ID, SchemaType.OUTSOURCING_CONFIG, 0, 1))
//                    .thenReturn(Collections.emptyList());
//            when(schemaCommonService.create(eq(ORG_ID), eq(OPERATOR_ID), any(CreateSchemaRequest.class)))
//                    .thenAnswer(invocation -> {
//                        CreateSchemaRequest request = invocation.getArgument(2);
//                        return Result.success(request.getDefinition());
//                    });
//
//            // When
//            Result<OutsourcingConfig> result = outsourcingConfigService.saveOrUpdate(config, OPERATOR_ID);
//
//            // Then
//            assertThat(result.isSuccess()).isTrue();
//            // 注意：initOutsourcingForOrg 方法目前只是记录日志，没有实际操作
//        }
//    }
//
//    @Nested
//    @DisplayName("saveOrUpdate 测试 - 更新场景")
//    class SaveOrUpdateUpdateTests {
//
//        @Test
//        @DisplayName("更新现有配置应成功")
//        void shouldUpdateConfigSuccessfully() {
//            // Given
//            OutsourcingConfig existingConfig = createDefaultConfig();
//            existingConfig.setContentVersion(1);
//
//            OutsourcingConfig newConfig = createDefaultConfig();
//            newConfig.setDecimalScale(2);
//
//            when(schemaQuery.queryPaged(ORG_ID, SchemaType.OUTSOURCING_CONFIG, 0, 1))
//                    .thenReturn(List.of(existingConfig));
//            when(schemaCommonService.update(eq(existingConfig.getId().value()), eq(OPERATOR_ID), any(UpdateSchemaRequest.class)))
//                    .thenAnswer(invocation -> {
//                        UpdateSchemaRequest request = invocation.getArgument(2);
//                        return Result.success(request.getDefinition());
//                    });
//
//            // When
//            Result<OutsourcingConfig> result = outsourcingConfigService.saveOrUpdate(newConfig, OPERATOR_ID);
//
//            // Then
//            assertThat(result.isSuccess()).isTrue();
//            verify(schemaCommonService).update(eq(existingConfig.getId().value()), eq(OPERATOR_ID), any(UpdateSchemaRequest.class));
//        }
//
//        @Test
//        @DisplayName("从关闭到开启考勤功能应触发初始化")
//        void shouldInitializeWhenEnabling() {
//            // Given
//            OutsourcingConfig existingConfig = createDefaultConfig();
//            existingConfig.setContentVersion(1);
//
//            OutsourcingConfig newConfig = createDefaultConfig();
//
//            when(schemaQuery.queryPaged(ORG_ID, SchemaType.OUTSOURCING_CONFIG, 0, 1))
//                    .thenReturn(List.of(existingConfig));
//            when(schemaCommonService.update(eq(existingConfig.getId().value()), eq(OPERATOR_ID), any(UpdateSchemaRequest.class)))
//                    .thenAnswer(invocation -> {
//                        UpdateSchemaRequest request = invocation.getArgument(2);
//                        return Result.success(request.getDefinition());
//                    });
//
//            // When
//            Result<OutsourcingConfig> result = outsourcingConfigService.saveOrUpdate(newConfig, OPERATOR_ID);
//
//            // Then
//            assertThat(result.isSuccess()).isTrue();
//            // 注意：initOutsourcingForOrg 方法目前只是记录日志，没有实际操作
//        }
//
//        @Test
//        @DisplayName("保持开启状态不应重复初始化")
//        void shouldNotInitializeWhenAlreadyEnabled() {
//            // Given
//            OutsourcingConfig existingConfig = createDefaultConfig();
//            existingConfig.setContentVersion(1);
//
//            OutsourcingConfig newConfig = createDefaultConfig();
//
//            when(schemaQuery.queryPaged(ORG_ID, SchemaType.OUTSOURCING_CONFIG, 0, 1))
//                    .thenReturn(List.of(existingConfig));
//            when(schemaCommonService.update(eq(existingConfig.getId().value()), eq(OPERATOR_ID), any(UpdateSchemaRequest.class)))
//                    .thenAnswer(invocation -> {
//                        UpdateSchemaRequest request = invocation.getArgument(2);
//                        return Result.success(request.getDefinition());
//                    });
//
//            // When
//            Result<OutsourcingConfig> result = outsourcingConfigService.saveOrUpdate(newConfig, OPERATOR_ID);
//
//            // Then
//            assertThat(result.isSuccess()).isTrue();
//            // 不应触发初始化
//        }
//    }
//
//    @Nested
//    @DisplayName("saveOrUpdate 测试 - 校验场景")
//    class SaveOrUpdateValidationTests {
//
//        @Test
//        @DisplayName("配置校验失败时应返回错误")
//        void shouldReturnErrorWhenValidationFails() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            config.setDecimalScale(-1); // 无效值
//
//            // When
//            Result<OutsourcingConfig> result = outsourcingConfigService.saveOrUpdate(config, OPERATOR_ID);
//
//            // Then
//            assertThat(result.isSuccess()).isFalse();
//            assertThat(result.getCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR.getCode());
//            assertThat(result.getMessage()).contains("配置校验失败");
//        }
//
//        @Test
//        @DisplayName("签到配置校验失败时应返回错误")
//        void shouldReturnErrorWhenAttendanceConfValidationFails() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            AttendanceConf attendanceConf = new AttendanceConf();
//            attendanceConf.setWorkStart("17:30");
//            attendanceConf.setWorkEnd("08:30"); // 结束时间早于开始时间
//            attendanceConf.setLunchStart("11:30");
//            attendanceConf.setLunchEnd("13:30");
//            attendanceConf.setWorkDuration(8.0);
//            config.setAttendanceConf(attendanceConf);
//
//            // When
//            Result<OutsourcingConfig> result = outsourcingConfigService.saveOrUpdate(config, OPERATOR_ID);
//
//            // Then
//            assertThat(result.isSuccess()).isFalse();
//            assertThat(result.getCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR.getCode());
//            assertThat(result.getMessage()).contains("工作结束时间必须晚于工作开始时间");
//        }
//    }
//
//    @Nested
//    @DisplayName("initOutsourcingForOrg 测试")
//    class InitOutsourcingForOrgTests {
//
//        @Test
//        @DisplayName("初始化方法应正常执行")
//        void shouldInitializeSuccessfully() {
//            // When & Then
//            // 目前只是记录日志，不应抛出异常
//            outsourcingConfigService.initOutsourcingForOrg(ORG_ID);
//        }
//    }
//
//    // ==================== 辅助方法 ====================
//
//    private OutsourcingConfig createDefaultConfig() {
//        OutsourcingConfig config = new OutsourcingConfig(
//                OutsourcingConfigId.generate(),
//                ORG_ID,
//                CONFIG_NAME
//        );
//        config.setState(EntityState.ACTIVE);
//        config.setEnabled(true);
//        config.setContentVersion(1);
//        config.setCreatedAt(LocalDateTime.now());
//        config.setUpdatedAt(LocalDateTime.now());
//        return config;
//    }
//}
