package dev.planka.domain.outsourcing;

import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 考勤配置ID值对象
 */
public record OutsourcingConfigId(@JsonValue String value) implements SchemaId {

    public OutsourcingConfigId {
        Objects.requireNonNull(value, "OutsourcingConfigId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("OutsourcingConfigId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.OUTSOURCING_CONFIG;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OutsourcingConfigId of(String value) {
        return new OutsourcingConfigId(value);
    }

    /**
     * 使用雪花算法生成新的 OutsourcingConfigId
     */
    public static OutsourcingConfigId generate() {
        return new OutsourcingConfigId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
