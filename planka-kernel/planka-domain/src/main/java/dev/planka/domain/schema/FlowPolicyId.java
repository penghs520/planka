package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 流转策略ID值对象
 */
public record FlowPolicyId(@JsonValue String value) implements SchemaId {

    public FlowPolicyId {
        Objects.requireNonNull(value, "FlowPolicyId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("FlowPolicyId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.FLOW_POLICY;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FlowPolicyId of(String value) {
        return new FlowPolicyId(value);
    }

    /**
     * 使用雪花算法生成新的 FlowPolicyId
     */
    public static FlowPolicyId generate() {
        return new FlowPolicyId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
