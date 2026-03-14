package cn.agilean.kanban.domain.stream;

import cn.agilean.kanban.common.util.SnowflakeIdGenerator;
import cn.agilean.kanban.domain.schema.SchemaId;
import cn.agilean.kanban.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 价值流状态ID值对象
 */
public record StatusId(@JsonValue String value) implements SchemaId {

    public StatusId {
        Objects.requireNonNull(value, "StatusId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("StatusId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.VALUE_STREAM;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StatusId of(String value) {
        return new StatusId(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public static StatusId generate() {
        return new StatusId(SnowflakeIdGenerator.generateStr());
    }

}
