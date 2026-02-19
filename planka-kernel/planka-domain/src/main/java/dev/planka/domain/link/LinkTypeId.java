package dev.planka.domain.link;

import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 关联类型定义ID
 */
public record LinkTypeId(@JsonValue String value) implements SchemaId {

    public LinkTypeId {
        Objects.requireNonNull(value, "LinkTypeId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("LinkTypeId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.LINK_TYPE;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static LinkTypeId of(String value) {
        return new LinkTypeId(value);
    }

    /**
     * 使用雪花算法生成新的 LinkTypeId
     */
    public static LinkTypeId generate() {
        return new LinkTypeId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
