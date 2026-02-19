package dev.planka.schema.repository;

import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.schema.mapper.SchemaIndexMapper;
import dev.planka.schema.mapper.SchemaMapper;
import dev.planka.schema.mapper.SchemaReferenceMapper;
import dev.planka.schema.model.SchemaEntity;
import dev.planka.schema.model.SchemaIndexEntity;
import dev.planka.schema.model.SchemaReferenceEntity;
import dev.planka.schema.service.common.SchemaQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Schema Repository（防腐层）
 * <p>
 * 隔离数据访问细节，将数据库实体转换为 SchemaDefinition。
 * <p>
 * 职责范围：
 * <ul>
 *   <li>基础 CRUD 操作（save、findById、findByIds、deleteById、existsById）</li>
 *   <li>引用关系查询（findAggregationSources、findCompositionChildren）</li>
 * </ul>
 * <p>
 * 注意：所有按条件查询的方法（如按组织、类型、二级索引等）都应通过 {@link SchemaQuery} 进行
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SchemaRepository {

    private final SchemaMapper schemaMapper;
    private final SchemaIndexMapper indexMapper;
    private final SchemaReferenceMapper referenceMapper;
    private final ObjectMapper objectMapper;

    // ==================== 基础 CRUD ====================

    @Transactional
    public <T extends AbstractSchemaDefinition<?>> T save(T schema) {
        SchemaEntity entity = toEntity(schema);

        if (schemaMapper.selectById(entity.getId()) == null) {
            schemaMapper.insert(entity);
        } else {
            schemaMapper.updateById(entity);
        }

        // 保存二级索引
        saveIndexes(schema);

        return schema;
    }

    public Optional<SchemaDefinition<?>> findById(String id) {
        SchemaEntity entity = schemaMapper.selectById(id);
        if (entity == null || "DELETED".equals(entity.getState())) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    public List<SchemaDefinition<?>> findByIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<SchemaEntity> entities = schemaMapper.selectByIds(new ArrayList<>(ids));
        return entities.stream()
                .filter(e -> !"DELETED".equals(e.getState()))
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 批量查询（包含已删除）
     * <p>
     * 主要用于操作历史等需要显示已删除 Schema 名称的场景
     */
    public List<SchemaDefinition<?>> findByIdsWithDeleted(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<SchemaEntity> entities = schemaMapper.selectByIdsWithDeleted(new ArrayList<>(ids));
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteById(String id) {
        // 删除索引
        indexMapper.deleteBySchemaId(id);
        // 删除引用关系
        referenceMapper.deleteBySourceId(id);
        // 删除Schema
        schemaMapper.deleteById(id);
    }

    public boolean existsById(String id) {
        SchemaEntity entity = schemaMapper.selectById(id);
        return entity != null && !"DELETED".equals(entity.getState());
    }

    // ==================== 引用关系查询 ====================

    public List<String> findAggregationSources(String targetId) {
        return referenceMapper.findAggregationSourceIds(targetId);
    }

    public List<SchemaDefinition<?>> findCompositionChildren(String targetId) {
        List<SchemaReferenceEntity> refs = referenceMapper.findByTargetIdAndType(targetId, "COMPOSITION");
        if (refs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> sourceIds = refs.stream()
                .map(SchemaReferenceEntity::getSourceId)
                .collect(Collectors.toSet());
        return findByIds(sourceIds);
    }

    // ==================== 私有方法 ====================

    private void saveIndexes(AbstractSchemaDefinition<?> schema) {
        String schemaIdValue = schema.getId().value();
        // 删除旧索引
        indexMapper.deleteBySchemaId(schemaIdValue);

        // 保存二级键索引
        if (CollectionUtils.isNotEmpty(schema.secondKeys())) {
            for (SchemaId secondKey : schema.secondKeys()) {
                SchemaIndexEntity indexEntity = new SchemaIndexEntity();
                indexEntity.setSchemaId(schemaIdValue);
                indexEntity.setIndexType(secondKey.schemaType().name());
                indexEntity.setIndexKey(secondKey.value());
                indexMapper.insert(indexEntity);
            }
        }
    }

    private SchemaEntity toEntity(AbstractSchemaDefinition<?> schema) {
        SchemaEntity entity = new SchemaEntity();
        entity.setId(schema.getId().value());
        entity.setOrgId(schema.getOrgId());
        entity.setSchemaType(schema.getSchemaType().name());
        entity.setName(schema.getName());
        entity.setContent(serializeDefinition(schema));
        entity.setState(schema.getState().name());
        entity.setContentVersion(schema.getContentVersion());
        entity.setStructureVersion(schema.getStructureVersion());
        entity.setBelongTo(schema.belongTo() != null ? schema.belongTo().value() : null);
        entity.setCreatedAt(schema.getCreatedAt());
        entity.setCreatedBy(schema.getCreatedBy());
        entity.setUpdatedAt(schema.getUpdatedAt());
        entity.setUpdatedBy(schema.getUpdatedBy());
        entity.setDeletedAt(schema.getDeletedAt());
        return entity;
    }

    private String serializeDefinition(SchemaDefinition<?> definition) {
        if (definition == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SchemaDefinition: {}", definition.getName(), e);
            throw new RuntimeException("Failed to serialize SchemaDefinition", e);
        }
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

    private SchemaDefinition<?> toDomain(SchemaEntity entity) {
        // 通过Jackson多态反序列化获取具体的SchemaDefinition子类
        SchemaDefinition<?> schema = deserializeDefinition(entity.getContent());
        if (schema == null) {
            throw new IllegalStateException("Failed to deserialize schema content for id: " + entity.getId());
        }
        return schema;
    }
}
