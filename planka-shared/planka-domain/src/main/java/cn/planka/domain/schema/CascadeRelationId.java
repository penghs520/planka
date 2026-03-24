package cn.planka.domain.schema;

import cn.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 级联关系定义ID
 */
public record CascadeRelationId(@JsonValue String value) implements SchemaId {

    public CascadeRelationId {
        Objects.requireNonNull(value, "CascadeRelationId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CascadeRelationId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.CASCADE_RELATION_DEFINITION;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CascadeRelationId of(String value) {
        return new CascadeRelationId(value);
    }

    /**
     * 使用雪花算法生成新的 CascadeRelationId
     */
    public static CascadeRelationId generate() {
        return new CascadeRelationId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
