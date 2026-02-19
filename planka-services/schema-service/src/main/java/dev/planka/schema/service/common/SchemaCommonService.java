package dev.planka.schema.service.common;

import dev.planka.api.schema.dto.ReferenceNodeDTO;
import dev.planka.api.schema.dto.SchemaReferenceSummaryDTO;
import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.EntityState;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.changelog.ChangeDetail;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.event.schema.SchemaCreatedEvent;
import dev.planka.event.schema.SchemaDeletedEvent;
import dev.planka.event.schema.SchemaUpdatedEvent;
import dev.planka.schema.mapper.SchemaChangelogMapper;
import dev.planka.schema.mapper.SchemaReferenceMapper;
import dev.planka.schema.model.SchemaChangelogEntity;
import dev.planka.schema.model.SchemaReferenceEntity;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.diff.SchemaDiffService;
import dev.planka.schema.service.common.lifecycle.SchemaLifecycleHandler;
import dev.planka.schema.service.common.lifecycle.SchemaLifecycleHandlerRegistry;
import dev.planka.schema.service.common.reference.SchemaReferenceAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Schema 业务服务
 */
@Slf4j
@Service
public class SchemaCommonService {

    private static final String KAFKA_TOPIC = "planka-schema-events";

    private final SchemaRepository schemaRepository;
    private final SchemaReferenceMapper referenceMapper;
    private final SchemaChangelogMapper changelogMapper;
    private final SchemaAssembler assembler;
    private final SchemaReferenceAnalyzer referenceAnalyzer;
    private final SchemaDiffService diffService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SchemaLifecycleHandlerRegistry lifecycleHandlerRegistry;
    private final SchemaQuery schemaQuery;

    @Autowired
    public SchemaCommonService(
            SchemaRepository schemaRepository,
            SchemaReferenceMapper referenceMapper,
            SchemaChangelogMapper changelogMapper,
            SchemaAssembler assembler,
            SchemaReferenceAnalyzer referenceAnalyzer,
            SchemaDiffService diffService,
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            SchemaLifecycleHandlerRegistry lifecycleHandlerRegistry,
            SchemaQuery schemaQuery) {
        this.schemaRepository = schemaRepository;
        this.referenceMapper = referenceMapper;
        this.changelogMapper = changelogMapper;
        this.assembler = assembler;
        this.referenceAnalyzer = referenceAnalyzer;
        this.diffService = diffService;
        this.kafkaTemplate = kafkaTemplate;
        this.lifecycleHandlerRegistry = lifecycleHandlerRegistry;
        this.schemaQuery = schemaQuery;
    }

    // ==================== 基础 CRUD ====================

    public Result<SchemaDefinition<?>> getById(String schemaId) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(schemaId);
        return schemaOpt.<Result<SchemaDefinition<?>>>map(Result::success)
                .orElseGet(() -> Result.failure(CommonErrorCode.DATA_NOT_FOUND, "Schema不存在"));
    }

    public Result<List<SchemaDefinition<?>>> getByIds(List<String> schemaIds) {
        List<SchemaDefinition<?>> schemas = schemaRepository.findByIds(new HashSet<>(schemaIds));
        return Result.success(schemas.stream().map(s -> (SchemaDefinition<?>) s).collect(Collectors.toList()));
    }

    /**
     * 批量获取 Schema（包含已删除）
     * <p>
     * 主要用于操作历史等需要显示已删除 Schema 名称的场景
     */
    public Result<List<SchemaDefinition<?>>> getByIdsWithDeleted(List<String> schemaIds) {
        List<SchemaDefinition<?>> schemas = schemaRepository.findByIdsWithDeleted(new HashSet<>(schemaIds));
        return Result.success(schemas.stream().map(s -> (SchemaDefinition<?>) s).collect(Collectors.toList()));
    }

    @Transactional
    public Result<SchemaDefinition<?>> create(String orgId, String operatorId, CreateSchemaRequest request) {
        // 直接获取类型安全的定义
        AbstractSchemaDefinition<?> schema = request.getDefinition();
        SchemaType type = schema.getSchemaType();

        // 设置元数据
        schema.setState(EntityState.ACTIVE);
        schema.setContentVersion(1);
        schema.setStructureVersion("1.0.0");
        schema.setCreatedAt(LocalDateTime.now());
        schema.setCreatedBy(operatorId);
        schema.setUpdatedAt(LocalDateTime.now());
        schema.setUpdatedBy(operatorId);
        schema.setOrgId(orgId);

        // 验证定义有效性
        try {
            schema.validate();
        } catch (IllegalArgumentException e) {
            return Result.failure(CommonErrorCode.VALIDATION_ERROR, "Schema定义验证失败: " + e.getMessage());
        }

        // 生命周期处理器 - 创建前
        SchemaLifecycleHandler<SchemaDefinition<?>> handler = lifecycleHandlerRegistry.getHandler(schema.getSchemaSubType());
        try {
            handler.beforeCreate(schema);
        } catch (IllegalArgumentException e) {
            return Result.failure(CommonErrorCode.VALIDATION_ERROR, "Schema创建前校验失败: " + e.getMessage());
        }

        // 检查 belongTo 要求
        if (type.requiresBelongTo() && !schema.hasBelongTo()) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "Schema类型 " + type + " 需要指定belongTo");
        }

        // 保存Schema
        schema = schemaRepository.save(schema);

        // 自动分析并保存引用关系
        List<SchemaReferenceAnalyzer.ReferenceInfo> refs = referenceAnalyzer.analyze(schema);
        saveReferences(schema.getId().value(), type, refs);

        // 记录变更日志
        saveChangelog(schema, "CREATE", null);

        // 发送事件
        publishSchemaCreatedEvent(schema);

        // 生命周期处理器 - 创建后
        handler.afterCreate(schema);

        log.info("Schema created: id={}, type={}, name={}", schema.getId().value(), type, schema.getName());
        return Result.success(schema);
    }

    @Transactional
    public Result<SchemaDefinition<?>> update(String schemaId, String operatorId, UpdateSchemaRequest request) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(schemaId);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "Schema不存在");
        }

        AbstractSchemaDefinition<?> schema = (AbstractSchemaDefinition<?>) schemaOpt.get();
        AbstractSchemaDefinition<?> oldSchema = schema; // 保存原始定义用于生命周期处理器

        // 检查是否已删除
        if (schema.isDeleted()) {
            return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "无法修改已删除的Schema");
        }

        // 系统内置类型校验：检查是否修改了受保护的字段
        if (request.getDefinition() != null) {
            Result<Void> systemTypeCheckResult = validateSystemTypeModification(schema, request.getDefinition());
            if (!systemTypeCheckResult.isSuccess()) {
                return Result.failure(systemTypeCheckResult.getCode(), systemTypeCheckResult.getMessage());
            }
        }

        // 保存更新前的快照
        String beforeSnapshot = assembler.serializeDefinition(schema);

        // 乐观锁检查
        if (request.getExpectedVersion() != null && !request.getExpectedVersion().equals(schema.getContentVersion())) {
            return Result.failure(CommonErrorCode.CONFLICT, "数据已被其他用户修改，请刷新后重试");
        }

        // 更新定义内容
        if (request.getDefinition() != null) {
            AbstractSchemaDefinition<?> newDefinition = request.getDefinition();

            // 验证类型匹配
            if (newDefinition.getSchemaType() != schema.getSchemaType()) {
                return Result.failure(CommonErrorCode.BAD_REQUEST,
                        "Schema定义类型与原类型不匹配: 定义类型=" + newDefinition.getSchemaType() + ", 原类型=" + schema.getSchemaType());
            }

            // 验证定义有效性
            try {
                newDefinition.validate();
            } catch (IllegalArgumentException e) {
                return Result.failure(CommonErrorCode.VALIDATION_ERROR, "Schema定义验证失败: " + e.getMessage());
            }

            // 生命周期处理器 - 更新前
            SchemaLifecycleHandler<SchemaDefinition<?>> handler = lifecycleHandlerRegistry.getHandler(schema.getSchemaSubType());
            try {
                handler.beforeUpdate(oldSchema, newDefinition);
            } catch (IllegalArgumentException e) {
                return Result.failure(CommonErrorCode.VALIDATION_ERROR, "Schema更新前校验失败: " + e.getMessage());
            }

            // 复制元数据到新定义（保留原有的不可变字段）
            newDefinition.setState(schema.getState());
            newDefinition.setContentVersion(schema.getContentVersion() + 1);
            newDefinition.setStructureVersion(schema.getStructureVersion());
            newDefinition.setCreatedAt(schema.getCreatedAt());
            newDefinition.setCreatedBy(schema.getCreatedBy());
            newDefinition.setUpdatedAt(LocalDateTime.now());
            newDefinition.setUpdatedBy(operatorId);
            newDefinition.setDeletedAt(schema.getDeletedAt());

            schema = newDefinition;
        }

        // 保存Schema
        schema = schemaRepository.save(schema);

        // 重新分析并保存引用关系
        referenceMapper.deleteBySourceId(schemaId);
        List<SchemaReferenceAnalyzer.ReferenceInfo> refs = referenceAnalyzer.analyze(schema);
        saveReferences(schema.getId().value(), schema.getSchemaType(), refs);

        // 记录变更日志
        saveChangelog(schema, "UPDATE", beforeSnapshot);

        // 发送事件
        publishSchemaUpdatedEvent(schema, beforeSnapshot);

        // 生命周期处理器 - 更新后
        SchemaLifecycleHandler<SchemaDefinition<?>> updateHandler = lifecycleHandlerRegistry.getHandler(schema.getSchemaSubType());
        updateHandler.afterUpdate(oldSchema, schema);

        log.info("Schema updated: id={}, version={}", schema.getId().value(), schema.getContentVersion());
        return Result.success(schema);
    }

    @Transactional
    public Result<Void> delete(String schemaId, String operatorId) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(schemaId);
        if (schemaOpt.isEmpty()) {
            return Result.success();
        }

        AbstractSchemaDefinition<?> schema = (AbstractSchemaDefinition<?>) schemaOpt.get();

        // 检查是否为系统内置类型（系统内置类型不可删除）
        if (schema instanceof CardTypeDefinition cardTypeDef && cardTypeDef.isSystemType()) {
            return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "系统内置类型不可删除");
        }

        // 检查是否被聚合引用，返回详细引用信息
        List<SchemaReferenceEntity> aggregationRefs = referenceMapper.findByTargetIdAndType(schemaId, "AGGREGATION");
        if (!aggregationRefs.isEmpty()) {
            String refDetails = buildReferenceDetails(aggregationRefs);
            return Result.failure(CommonErrorCode.CONFLICT,
                    "Schema被其他Schema引用，无法删除。引用方: " + refDetails);
        }

        // 生命周期处理器 - 删除前
        SchemaLifecycleHandler<SchemaDefinition<?>> handler = lifecycleHandlerRegistry.getHandler(schema.getSchemaSubType());
        try {
            handler.beforeDelete(schema);
        } catch (IllegalArgumentException e) {
            return Result.failure(CommonErrorCode.VALIDATION_ERROR, "Schema删除前校验失败: " + e.getMessage());
        }

        // 级联删除组合子项
        List<SchemaDefinition<?>> children = schemaRepository.findCompositionChildren(schemaId);
        for (SchemaDefinition<?> child : children) {
            schemaRepository.deleteById(child.getId().value());
        }

        // 删除引用关系
        referenceMapper.deleteBySourceId(schemaId);

        // 删除Schema（软删除，业务逻辑在Service层）
        String beforeSnapshot = assembler.serializeDefinition(schema);
        schema.setState(EntityState.DELETED);
        schema.setDeletedAt(LocalDateTime.now());
        schema.setUpdatedAt(LocalDateTime.now());
        schema.setUpdatedBy(operatorId);
        schemaRepository.save(schema);

        // 记录变更日志
        saveChangelog(schema, "DELETE", beforeSnapshot);

        // 发送事件
        publishSchemaDeletedEvent(schema, beforeSnapshot);

        // 生命周期处理器 - 删除后
        handler.afterDelete(schema);

        log.info("Schema deleted: id={}", schemaId);
        return Result.success();
    }

    /**
     * 构建引用详情字符串
     * 格式：卡片类型[需求]、工作流[需求流程]
     */
    private String buildReferenceDetails(List<SchemaReferenceEntity> refs) {
        return refs.stream()
                .map(ref -> {
                    String typeName = SchemaType.valueOf(ref.getSourceType()).getDisplayName();
                    String schemaName = schemaRepository.findById(ref.getSourceId())
                            .map(SchemaDefinition::getName)
                            .orElse(ref.getSourceId());
                    return typeName + "[" + schemaName + "]";
                })
                .collect(Collectors.joining("、"));
    }

    // ==================== 状态变更 ====================

    @Transactional
    public Result<Void> activate(String schemaId, String operatorId) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(schemaId);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "Schema不存在");
        }

        AbstractSchemaDefinition<?> schema = (AbstractSchemaDefinition<?>) schemaOpt.get();

        if (schema.isDeleted()) {
            return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "无法启用已删除的Schema");
        }

        schema.setState(EntityState.ACTIVE);
        schema.setEnabled(true);
        schema.setUpdatedAt(LocalDateTime.now());
        schema.setUpdatedBy(operatorId);
        schemaRepository.save(schema);

        log.info("Schema activated: id={}", schemaId);
        return Result.success();
    }

    @Transactional
    public Result<Void> disable(String schemaId, String operatorId) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(schemaId);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "Schema不存在");
        }

        AbstractSchemaDefinition<?> schema = (AbstractSchemaDefinition<?>) schemaOpt.get();

        if (schema.isDeleted()) {
            return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "无法停用已删除的Schema");
        }

        schema.setState(EntityState.DISABLED);
        schema.setEnabled(false);
        schema.setUpdatedAt(LocalDateTime.now());
        schema.setUpdatedBy(operatorId);
        schemaRepository.save(schema);

        log.info("Schema disabled: id={}", schemaId);
        return Result.success();
    }

    // ==================== 按条件查询 ====================

    /**
     * 按组织和类型分页查询 Schema
     *
     * @param orgId      组织ID
     * @param schemaType Schema 类型
     * @param page       页码（从1开始）
     * @param size       每页数量
     * @return 分页结果
     */
    public Result<PageResult<SchemaDefinition<?>>> listByOrgAndType(String orgId, SchemaType schemaType, int page, int size) {
        long total = schemaQuery.count(orgId, schemaType);
        int offset = (page - 1) * size;
        List<SchemaDefinition<?>> schemas = schemaQuery.queryPaged(orgId, schemaType, offset, size);

        return Result.success(PageResult.of(schemas, page, size, total));
    }


    public Result<SchemaReferenceSummaryDTO> getReferenceSummary(String schemaId) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(schemaId);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "Schema不存在");
        }

        AbstractSchemaDefinition<?> schema = (AbstractSchemaDefinition<?>) schemaOpt.get();

        // 出向引用：该 Schema 引用了哪些 Schema
        List<SchemaReferenceEntity> outgoingRefs = referenceMapper.findBySourceId(schemaId);
        List<ReferenceNodeDTO> outgoing = outgoingRefs.stream()
                .map(this::toReferenceNode)
                .collect(Collectors.toList());

        // 入向引用：该 Schema 被哪些 Schema 引用
        List<SchemaReferenceEntity> incomingRefs = referenceMapper.findByTargetId(schemaId);
        List<ReferenceNodeDTO> incoming = incomingRefs.stream()
                .map(this::toIncomingReferenceNode)
                .collect(Collectors.toList());

        return Result.success(SchemaReferenceSummaryDTO.builder()
                .schemaId(schemaId)
                .schemaName(schema.getName())
                .schemaType(schema.getSchemaType().name())
                .outgoing(outgoing)
                .incoming(incoming)
                .build());
    }

    /**
     * 转换出向引用为 ReferenceNodeDTO
     */
    private ReferenceNodeDTO toReferenceNode(SchemaReferenceEntity ref) {
        String targetName = schemaRepository.findById(ref.getTargetId())
                .map(SchemaDefinition::getName)
                .orElse(ref.getTargetId());

        return ReferenceNodeDTO.builder()
                .schemaId(ref.getTargetId())
                .schemaName(targetName)
                .schemaType(ref.getTargetType())
                .referenceType(ref.getReferenceType())
                .build();
    }

    /**
     * 转换入向引用为 ReferenceNodeDTO
     */
    private ReferenceNodeDTO toIncomingReferenceNode(SchemaReferenceEntity ref) {
        String sourceName = schemaRepository.findById(ref.getSourceId())
                .map(SchemaDefinition::getName)
                .orElse(ref.getSourceId());

        return ReferenceNodeDTO.builder()
                .schemaId(ref.getSourceId())
                .schemaName(sourceName)
                .schemaType(ref.getSourceType())
                .referenceType(ref.getReferenceType())
                .build();
    }


    /**
     * 还原至指定版本
     *
     * @param schemaId    Schema ID
     * @param changelogId 变更日志 ID
     * @param operatorId  操作人 ID
     * @return 还原后的 Schema
     */
    @Transactional
    public Result<SchemaDefinition<?>> restoreToVersion(String schemaId, Long changelogId, String operatorId) {
        // 查找变更日志
        SchemaChangelogEntity changelog = changelogMapper.selectById(changelogId);
        if (changelog == null) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "变更日志不存在");
        }

        // 验证变更日志属于该 Schema
        if (!schemaId.equals(changelog.getSchemaId())) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "变更日志与 Schema 不匹配");
        }

        // 获取要还原的快照（使用 afterSnapshot，即该版本变更后的状态）
        String snapshotToRestore = changelog.getAfterSnapshot();
        if (snapshotToRestore == null || snapshotToRestore.isBlank()) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "该版本无可还原的快照数据");
        }

        // 反序列化快照
        AbstractSchemaDefinition<?> restoredDefinition;
        try {
            restoredDefinition = assembler.deserializeDefinition(snapshotToRestore);
        } catch (Exception e) {
            log.error("Failed to deserialize snapshot for restore: schemaId={}, changelogId={}", schemaId, changelogId, e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "快照数据解析失败");
        }

        // 构建更新请求并调用更新方法
        UpdateSchemaRequest updateRequest = new UpdateSchemaRequest();
        updateRequest.setDefinition(restoredDefinition);

        return update(schemaId, operatorId, updateRequest);
    }


    // ==================== 私有方法 ====================

    /**
     * 校验系统内置类型的修改
     * <p>
     * 系统内置类型的修改规则：
     * - 卡片类型：只允许修改 icon、color，其他字段不允许修改
     * - 关联类型：不允许修改
     * - 属性定义：不允许修改
     */
    private Result<Void> validateSystemTypeModification(AbstractSchemaDefinition<?> oldSchema, AbstractSchemaDefinition<?> newSchema) {
        // 卡片类型校验：只允许修改 icon 和 color
        if (oldSchema instanceof CardTypeDefinition oldCardType && oldCardType.isSystemType()) {
            CardTypeDefinition newCardType = (CardTypeDefinition) newSchema;
            if (!Objects.equals(oldCardType.getName(), newCardType.getName())) {
                return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "系统内置卡片类型不允许修改名称");
            }
            if (!Objects.equals(oldCardType.getCode(), newCardType.getCode())) {
                return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "系统内置卡片类型不允许修改编码");
            }
            if (!Objects.equals(oldCardType.getDescription(), newCardType.getDescription())) {
                return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "系统内置卡片类型不允许修改描述");
            }
        }

        // 关联类型校验：完全不允许修改
        if (oldSchema instanceof LinkTypeDefinition oldLinkType && oldLinkType.isSystemLinkType()) {
            return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "系统内置关联类型不允许修改");
        }

        // 字段配置校验：完全不允许修改
        if (oldSchema instanceof FieldConfig oldFieldConfig && oldFieldConfig.isSystemField()) {
            return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "系统内置字段配置不允许修改");
        }

        return Result.success();
    }

    private void saveReferences(String sourceId, SchemaType sourceType, List<SchemaReferenceAnalyzer.ReferenceInfo> references) {
        if (references == null || references.isEmpty()) {
            return;
        }

        for (SchemaReferenceAnalyzer.ReferenceInfo ref : references) {
            SchemaReferenceEntity entity = new SchemaReferenceEntity();
            entity.setSourceId(sourceId);
            entity.setSourceType(sourceType.name());
            entity.setTargetId(ref.targetId());
            entity.setTargetType(ref.targetType().name());
            entity.setReferenceType(ref.referenceType().name());
            referenceMapper.insert(entity);
        }
    }

    private void saveChangelog(AbstractSchemaDefinition<?> schema, String action, String beforeSnapshot) {
        String afterSnapshot = assembler.serializeDefinition(schema);

        // 使用差异服务生成结构化变更详情
        ChangeDetail changeDetail = diffService.diff(beforeSnapshot, afterSnapshot);

        // 生成变更摘要
        String changeSummary = generateChangeSummary(action, schema.getName(), changeDetail);

        SchemaChangelogEntity logEntity = new SchemaChangelogEntity();
        logEntity.setSchemaId(schema.getId().value());
        logEntity.setOrgId(schema.getOrgId());
        logEntity.setSchemaType(schema.getSchemaType().name());
        logEntity.setAction(action);
        logEntity.setContentVersion(schema.getContentVersion());
        logEntity.setBeforeSnapshot(beforeSnapshot);
        logEntity.setAfterSnapshot(afterSnapshot);
        logEntity.setChangeSummary(changeSummary);
        logEntity.setChangeDetail(diffService.serializeChangeDetail(changeDetail));
        logEntity.setChangedAt(LocalDateTime.now());
        logEntity.setChangedBy(schema.getUpdatedBy());
        changelogMapper.insert(logEntity);
    }

    private String generateChangeSummary(String action, String schemaName, ChangeDetail changeDetail) {
        String basicSummary = switch (action) {
            case "CREATE" -> "创建了 " + schemaName;
            case "UPDATE" -> "更新了 " + schemaName;
            case "DELETE" -> "删除了 " + schemaName;
            default -> action + " " + schemaName;
        };

        // 如果有详细变更信息，追加到摘要中
        if (changeDetail != null && "UPDATE".equals(action)) {
            String detailText = diffService.generateSummaryText(changeDetail);
            if (detailText != null && !detailText.isEmpty()) {
                return basicSummary + "：" + detailText;
            }
        }

        return basicSummary;
    }

    private void publishSchemaCreatedEvent(AbstractSchemaDefinition<?> schema) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled, skip publishing SchemaCreatedEvent for {}", schema.getId().value());
            return;
        }
        String content = assembler.serializeDefinition(schema);
        SchemaCreatedEvent event = new SchemaCreatedEvent(
                schema.getOrgId(),
                schema.getCreatedBy(),
                null, // sourceIp
                null, // traceId
                schema.getId().value(),
                schema.getName(),
                content);
        event.withSchemaType(schema.getSchemaType().name());
        kafkaTemplate.send(KAFKA_TOPIC, schema.getOrgId(), event);
    }

    private void publishSchemaUpdatedEvent(AbstractSchemaDefinition<?> schema, String beforeSnapshot) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled, skip publishing SchemaUpdatedEvent for {}", schema.getId().value());
            return;
        }
        String afterContent = assembler.serializeDefinition(schema);
        SchemaUpdatedEvent event = new SchemaUpdatedEvent(
                schema.getOrgId(),
                schema.getUpdatedBy(),
                null, // sourceIp
                null, // traceId
                schema.getId().value(),
                beforeSnapshot,
                afterContent,
                schema.getContentVersion());
        event.withSchemaType(schema.getSchemaType().name());
        kafkaTemplate.send(KAFKA_TOPIC, schema.getOrgId(), event);
    }

    private void publishSchemaDeletedEvent(AbstractSchemaDefinition<?> schema, String lastSnapshot) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled, skip publishing SchemaDeletedEvent for {}", schema.getId().value());
            return;
        }
        SchemaDeletedEvent event = new SchemaDeletedEvent(
                schema.getOrgId(),
                schema.getUpdatedBy(),
                null, // sourceIp
                null, // traceId
                schema.getId().value(),
                lastSnapshot,
                schema.getContentVersion());
        event.withSchemaType(schema.getSchemaType().name());
        kafkaTemplate.send(KAFKA_TOPIC, schema.getOrgId(), event);
    }
}
