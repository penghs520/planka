package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 视图ID值对象
 */
public record ViewId(@JsonValue String value) implements SchemaId {

    public ViewId {
        Objects.requireNonNull(value, "ViewId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ViewId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.VIEW;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ViewId of(String value) {
        return new ViewId(value);
    }

    /**
     * 使用雪花算法生成新的 ViewId
     */
    public static ViewId generate() {
        return new ViewId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
