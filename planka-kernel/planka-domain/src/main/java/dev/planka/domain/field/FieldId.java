package dev.planka.domain.field;

import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 属性ID值对象
 * <p>
 * 用于唯一标识一个属性，在FieldConfig中使用。
 * 外部系统引用属性时，应使用此ID而非FieldConfigId。
 */
public record FieldId(@JsonValue String value) implements SchemaId {

    private static final long serialVersionUID = 1L;

    public FieldId {
        Objects.requireNonNull(value, "FieldId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("FieldId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.FIELD_CONFIG;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FieldId of(String value) {
        return new FieldId(value);
    }

    /**
     * 使用雪花算法生成新的 FieldId
     */
    public static FieldId generate() {
        return new FieldId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
