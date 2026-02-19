package dev.planka.schema.service.common;

import dev.planka.api.schema.dto.SchemaChangelogDTO;
import dev.planka.api.schema.dto.SchemaReferenceDTO;
import dev.planka.domain.schema.changelog.ChangeDetail;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.schema.model.SchemaChangelogEntity;
import dev.planka.schema.model.SchemaReferenceEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Schema 序列化和转换器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaAssembler {

    private final ObjectMapper objectMapper;

    /**
     * 序列化SchemaDefinition为JSON字符串
     */
    public String serializeDefinition(AbstractSchemaDefinition<?> definition) {
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

    /**
     * 反序列化JSON字符串为SchemaDefinition
     */
    public AbstractSchemaDefinition<?> deserializeDefinition(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(content, AbstractSchemaDefinition.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize SchemaDefinition: {}", content, e);
            throw new RuntimeException("Failed to deserialize SchemaDefinition", e);
        }
    }

    /**
     * 引用关系实体转DTO
     */
    public SchemaReferenceDTO toReferenceDTO(SchemaReferenceEntity entity) {
        SchemaReferenceDTO dto = new SchemaReferenceDTO();
        dto.setId(entity.getId());
        dto.setSourceId(entity.getSourceId());
        dto.setSourceType(entity.getSourceType());
        dto.setTargetId(entity.getTargetId());
        dto.setTargetType(entity.getTargetType());
        dto.setReferenceType(entity.getReferenceType());
        return dto;
    }

    /**
     * 变更日志实体转DTO
     */
    public SchemaChangelogDTO toChangelogDTO(SchemaChangelogEntity entity) {
        SchemaChangelogDTO dto = new SchemaChangelogDTO();
        dto.setId(entity.getId());
        dto.setSchemaId(entity.getSchemaId());
        dto.setSchemaType(entity.getSchemaType());
        dto.setAction(entity.getAction());
        dto.setContentVersion(entity.getContentVersion());
        dto.setBeforeSnapshot(entity.getBeforeSnapshot());
        dto.setAfterSnapshot(entity.getAfterSnapshot());
        dto.setChangeSummary(entity.getChangeSummary());
        dto.setChangeDetail(deserializeChangeDetail(entity.getChangeDetail()));
        dto.setChangedAt(entity.getChangedAt());
        dto.setChangedBy(entity.getChangedBy());
        dto.setTraceId(entity.getTraceId());
        return dto;
    }

    /**
     * 序列化 ChangeDetail 为 JSON 字符串
     */
    public String serializeChangeDetail(ChangeDetail changeDetail) {
        if (changeDetail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(changeDetail);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ChangeDetail", e);
            throw new RuntimeException("Failed to serialize ChangeDetail", e);
        }
    }

    /**
     * 反序列化 JSON 字符串为 ChangeDetail
     */
    public ChangeDetail deserializeChangeDetail(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ChangeDetail.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ChangeDetail: {}", json, e);
            throw new RuntimeException("Failed to deserialize ChangeDetail", e);
        }
    }
}
