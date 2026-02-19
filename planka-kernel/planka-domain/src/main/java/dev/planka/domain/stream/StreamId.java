package dev.planka.domain.stream;

import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 价值流ID值对象
 */
public record StreamId(@JsonValue String value) implements SchemaId {

    public StreamId {
        Objects.requireNonNull(value, "StreamId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("StreamId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.VALUE_STREAM;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StreamId of(String value) {
        return new StreamId(value);
    }

    /**
     * 使用雪花算法生成新的 StreamId
     */
    public static StreamId generate() {
        return new StreamId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
