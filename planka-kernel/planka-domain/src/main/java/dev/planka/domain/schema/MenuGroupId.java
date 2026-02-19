package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 菜单分组ID值对象
 */
public record MenuGroupId(@JsonValue String value) implements SchemaId {

    public MenuGroupId {
        Objects.requireNonNull(value, "MenuGroupId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("MenuGroupId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.MENU;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static MenuGroupId of(String value) {
        return new MenuGroupId(value);
    }

    /**
     * 使用雪花算法生成新的 MenuGroupId
     */
    public static MenuGroupId generate() {
        return new MenuGroupId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
