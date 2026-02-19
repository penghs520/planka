package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 权限配置ID值对象
 */
public record PermissionConfigId(@JsonValue String value) implements SchemaId {

    public PermissionConfigId {
        Objects.requireNonNull(value, "PermissionConfigId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PermissionConfigId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.CARD_PERMISSION;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PermissionConfigId of(String value) {
        return new PermissionConfigId(value);
    }

    /**
     * 使用雪花算法生成新的 PermissionConfigId
     */
    public static PermissionConfigId generate() {
        return new PermissionConfigId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
