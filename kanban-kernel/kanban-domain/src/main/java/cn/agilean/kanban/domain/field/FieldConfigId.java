package cn.agilean.kanban.domain.field;

import cn.agilean.kanban.common.util.SnowflakeIdGenerator;
import cn.agilean.kanban.domain.schema.SchemaId;
import cn.agilean.kanban.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 属性配置ID值对象
 */
public record FieldConfigId(@JsonValue String value) implements SchemaId {

    public FieldConfigId {
        Objects.requireNonNull(value, "FieldConfigId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("FieldConfigId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.FIELD_CONFIG;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FieldConfigId of(String value) {
        return new FieldConfigId(value);
    }

    /**
     * 使用雪花算法生成新的 FieldConfigId
     */
    public static FieldConfigId generate() {
        return new FieldConfigId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
