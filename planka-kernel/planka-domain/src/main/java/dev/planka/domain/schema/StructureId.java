package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 架构线定义ID
 */
public record StructureId(@JsonValue String value) implements SchemaId {

    public StructureId {
        Objects.requireNonNull(value, "StructureId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("StructureId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.STRUCTURE_DEFINITION;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StructureId of(String value) {
        return new StructureId(value);
    }

    /**
     * 使用雪花算法生成新的 StructureId
     */
    public static StructureId generate() {
        return new StructureId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
