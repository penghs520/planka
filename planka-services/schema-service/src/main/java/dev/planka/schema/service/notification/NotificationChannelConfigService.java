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
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaCommonService;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通知渠道配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationChannelConfigService {

    private final SchemaRepository schemaRepository;
    private final SchemaCommonService schemaCommonService;
    private final SchemaQuery schemaQuery;

    /**
     * 查询通知渠道配置列表
     */
    public Result<List<NotificationChannelConfigVO>> list(String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.NOTIFICATION_CHANNEL_CONFIG);

        List<NotificationChannelConfigVO> voList = schemas.stream()
                .filter(s -> s instanceof NotificationChannelConfigDefinition)
                .map(s -> toVO((NotificationChannelConfigDefinition) s))
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 根据 ID 获取通知渠道配置详情
     */
    public Result<NotificationChannelConfigVO> getById(String id) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(id);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "通知渠道配置不存在: " + id);
        }

        SchemaDefinition<?> schema = schemaOpt.get();
        if (!(schema instanceof NotificationChannelConfigDefinition config)) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "非通知渠道配置: " + id);
        }

        return Result.success(toVO(config));
    }

    /**
     * 创建通知渠道配置
     */
    @Transactional
    public Result<NotificationChannelConfigVO> create(String orgId, String operatorId, CreateNotificationChannelRequest request) {
        NotificationChannelConfigDefinition config = new NotificationChannelConfigDefinition(
                NotificationChannelConfigId.generate(),
                orgId,
                request.getName()
        );

        config.setChannelId(request.getChannelId());
        config.setConfig(request.getConfig());
        config.setDefault(request.isDefault());
        config.setPriority(request.getPriority());
        config.setEnabled(true);

        CreateSchemaRequest schemaRequest = new CreateSchemaRequest();
        schemaRequest.setDefinition(config);
        Result<SchemaDefinition<?>> createResult = schemaCommonService.create(orgId, operatorId, schemaRequest);

        if (!createResult.isSuccess()) {
            return Result.failure(createResult.getCode(), createResult.getMessage());
        }

        NotificationChannelConfigDefinition savedConfig = (NotificationChannelConfigDefinition) createResult.getData();
        log.info("NotificationChannelConfig created: id={}, name={}", savedConfig.getId().value(), savedConfig.getName());
        return Result.success(toVO(savedConfig));
    }

    /**
     * 更新通知渠道配置
     */
    @Transactional
    public Result<NotificationChannelConfigVO> update(String id, String operatorId, UpdateNotificationChannelRequest request) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(id);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "通知渠道配置不存在");
        }

        if (!(schemaOpt.get() instanceof NotificationChannelConfigDefinition config)) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "非通知渠道配置");
        }

        config.setName(request.getName());
        config.setConfig(request.getConfig());
        config.setDefault(request.isDefault());
        config.setPriority(request.getPriority());

        UpdateSchemaRequest schemaRequest = new UpdateSchemaRequest();
        schemaRequest.setDefinition(config);
        schemaRequest.setExpectedVersion(request.getExpectedVersion());
        Result<SchemaDefinition<?>> updateResult = schemaCommonService.update(id, operatorId, schemaRequest);

        if (!updateResult.isSuccess()) {
            return Result.failure(updateResult.getCode(), updateResult.getMessage());
        }

        NotificationChannelConfigDefinition savedConfig = (NotificationChannelConfigDefinition) updateResult.getData();
        log.info("NotificationChannelConfig updated: id={}, version={}", savedConfig.getId().value(), savedConfig.getContentVersion());
        return Result.success(toVO(savedConfig));
    }

    /**
     * 删除通知渠道配置
     */
    @Transactional
    public Result<Void> delete(String id, String operatorId) {
        return schemaCommonService.delete(id, operatorId);
    }

    /**
     * 启用通知渠道配置
     */
    @Transactional
    public Result<Void> activate(String id, String operatorId) {
        return schemaCommonService.activate(id, operatorId);
    }

    /**
     * 停用通知渠道配置
     */
    @Transactional
    public Result<Void> disable(String id, String operatorId) {
        return schemaCommonService.disable(id, operatorId);
    }

    /**
     * 将通知渠道配置转换为 VO
     */
    private NotificationChannelConfigVO toVO(NotificationChannelConfigDefinition config) {
        return NotificationChannelConfigVO.builder()
                .id(config.getId().value())
                .orgId(config.getOrgId())
                .name(config.getName())
                .channelId(config.getChannelId())
                .config(config.getConfig())
                .isDefault(config.isDefault())
                .priority(config.getPriority())
                .enabled(config.isEnabled())
                .contentVersion(config.getContentVersion())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
