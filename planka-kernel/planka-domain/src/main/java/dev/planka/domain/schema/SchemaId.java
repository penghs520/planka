package dev.planka.domain.schema;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Schema ID 接口
 * <p>
 * 所有 Schema 相关的 ID 类型都实现此接口，提供统一的访问方式。
 * 实现类包括：CardTypeId, FieldId, StreamId, LinkTypeId 等。
 * <p>
 * 序列化时只输出 value 值，如 "123" 而非 {"value": "123"}
 */
public interface SchemaId {

    /**
     * 获取 ID 值
     */
    @JsonValue
    String value();

    /**
     * 获取对应的 Schema 类型
     */
    SchemaType schemaType();
}
