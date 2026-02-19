package dev.planka.schema.service.common;

import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.schema.mapper.SchemaIndexMapper;
import dev.planka.schema.mapper.SchemaMapper;
import dev.planka.schema.model.SchemaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Schema 查询服务
 * <p>
 * 统一管理所有 Schema 查询操作。
 * <p>
 * 除了根据 ID 查询的基础方法（findById、findByIds）保留在 SchemaRepository 外，
 * 所有其他查询方法都应该通过本服务进行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaQuery {

    private final SchemaMapper schemaMapper;
    private final SchemaIndexMapper indexMapper;
    private final ObjectMapper objectMapper;

    // ==================== 按组织和类型查询 ====================

    /**
     * 根据组织ID和类型查询 Schema
     *
     * @param orgId 组织ID
     * @param type  Schema 类型
     * @return Schema 列表
     */
    public List<SchemaDefinition<?>> query(String orgId, SchemaType type) {
        log.debug("Querying schemas for orgId={}, type={}", orgId, type);
        return findAllByOrgIdAndType(orgId, type);
    }

    /**
     * 查询全部 Schema
     *
     * @param orgId 组织ID
     * @param type  Schema 类型
     * @return Schema 列表
     */
    public List<SchemaDefinition<?>> queryAll(String orgId, SchemaType type) {
        log.debug("Querying all schemas for orgId={}, type={}", orgId, type);
        return findAllByOrgIdAndType(orgId, type);
    }

    /**
     * 分页查询 Schema
     *
     * @param orgId  组织ID
     * @param type   Schema 类型
     * @param offset 偏移量
     * @param limit  限制数量
     * @return Schema 列表
     */
    public List<SchemaDefinition<?>> queryPaged(String orgId, SchemaType type, int offset, int limit) {
        log.debug("Querying schemas (paged) for orgId={}, type={}", orgId, type);
        return findByOrgIdAndTypePaged(orgId, type, offset, limit);
    }

    /**
     * 统计 Schema 数量
     *
     * @param orgId 组织ID
     * @param type  Schema 类型
     * @return 数量
     */
    public long count(String orgId, SchemaType type) {
        return schemaMapper.countByOrgIdAndType(orgId, type.name());
    }

    // ==================== 按二级索引查询 ====================

    /**
     * 按二级键查询 Schema
     *
     * @param secondKey        索引值
     * @param secondKeyType    索引类型（必填）
     * @param targetSchemaType 目标 Schema 类型（可选）
     * @return Schema 列表
     */
    public List<SchemaDefinition<?>> queryBySecondKey(
            String secondKey,
            @Nonnull SchemaType secondKeyType,
            @Nullable SchemaType targetSchemaType) {
        return findBySecondKeyAndType(secondKey, secondKeyType, targetSchemaType);
    }

    /**
     * 根据二级索引查询schema
     *
     * @param secondKey        二级索引键（直接使用SchemaId类型，无需传递secondKeyType参数）
     * @param targetSchemaType 目标schema类型
     */
    public List<SchemaDefinition<?>> queryBySecondKey(
            SchemaId secondKey,
            @Nullable SchemaType targetSchemaType) {
        return queryBySecondKey(secondKey.value(), secondKey.schemaType(), targetSchemaType);
    }


    /**
     * 按卡片类型ID查询 Schema
     *
     * @param cardTypeId 卡片类型ID
     * @param type       目标 Schema 类型
     * @return Schema 列表
     */
    public List<SchemaDefinition<?>> queryByCardTypeId(String cardTypeId, SchemaType type) {
        return findByCardTypeIdAndType(cardTypeId, type);
    }

    /**
     * 按所属 Schema ID 查询
     *
     * @param belongTo      所属 Schema ID
     * @param schemaSubType Schema 子类型（可选，用于类型过滤，如 "CARD_PERMISSION"）
     * @return Schema 列表
     */
    public List<SchemaDefinition<?>> queryByBelongTo(String belongTo, @Nullable String schemaSubType) {
        List<SchemaEntity> entities = schemaMapper.findByBelongTo(belongTo);
        List<SchemaDefinition<?>> schemas = entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        if (schemaSubType != null && !schemaSubType.isEmpty()) {
            schemas = schemas.stream()
                    .filter(s -> schemaSubType.equals(s.getSchemaSubType()))
                    .collect(Collectors.toList());
        }

        return schemas;
    }

    // ==================== 私有方法 - 数据访问 ====================

    private List<SchemaDefinition<?>> findAllByOrgIdAndType(String orgId, SchemaType type) {
        List<SchemaEntity> entities = schemaMapper.findAllByOrgIdAndType(orgId, type.name());
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private List<SchemaDefinition<?>> findByOrgIdAndTypePaged(String orgId, SchemaType type, int offset, int limit) {
        List<SchemaEntity> entities = schemaMapper.findByOrgIdAndTypePaged(orgId, type.name(), offset, limit);
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private List<SchemaDefinition<?>> findBySecondKeyAndType(String secondKey, SchemaType secondKeyType, SchemaType targetSchemaType) {
        List<String> schemaIds = indexMapper.findSchemaIdsByTypeAndKey(secondKeyType.name(), secondKey);
        if (schemaIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SchemaDefinition<?>> schemas = findByIds(new HashSet<>(schemaIds));
        if (targetSchemaType == null) {
            return schemas;
        }
        return schemas.stream()
                .filter(s -> s.getSchemaType() == targetSchemaType)
                .collect(Collectors.toList());
    }

    private List<SchemaDefinition<?>> findByCardTypeIdAndType(String cardTypeId, SchemaType type) {
        List<String> schemaIds = indexMapper.findSchemaIdsByTypeAndKey("CARD_TYPE", cardTypeId);
        if (schemaIds.isEmpty()) {
            return Collections.emptyList();
        }
        return findByIds(new HashSet<>(schemaIds))
                .stream()
                .filter(s -> s.getSchemaType() == type)
                .collect(Collectors.toList());
    }

    private List<SchemaDefinition<?>> findByIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<SchemaEntity> entities = schemaMapper.selectByIds(new ArrayList<>(ids));
        return entities.stream()
                .filter(e -> !"DELETED".equals(e.getState()))
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 - 序列化 ====================

    private SchemaDefinition<?> toDomain(SchemaEntity entity) {
        SchemaDefinition<?> schema = deserializeDefinition(entity.getContent());
        if (schema == null) {
            throw new IllegalStateException("Failed to deserialize schema content for id: " + entity.getId());
        }
        return schema;
    }

    private SchemaDefinition<?> deserializeDefinition(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(content, SchemaDefinition.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize SchemaDefinition from: {}", content, e);
            throw new RuntimeException("Failed to deserialize SchemaDefinition", e);
        }
    }
}
