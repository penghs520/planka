package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 业务规则ID值对象
 */
public record BizRuleId(@JsonValue String value) implements SchemaId {

    public BizRuleId {
        Objects.requireNonNull(value, "BizRuleId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("BizRuleId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.BIZ_RULE;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BizRuleId of(String value) {
        return new BizRuleId(value);
    }

    /**
     * 使用雪花算法生成新的 BizRuleId
     */
    public static BizRuleId generate() {
        return new BizRuleId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
