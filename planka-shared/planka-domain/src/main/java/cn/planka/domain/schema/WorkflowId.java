package cn.planka.domain.schema;

import cn.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 工作流定义ID值对象
 */
public record WorkflowId(@JsonValue String value) implements SchemaId {

    public WorkflowId {
        Objects.requireNonNull(value, "WorkflowId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WorkflowId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.WORKFLOW;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WorkflowId of(String value) {
        return new WorkflowId(value);
    }

    public static WorkflowId generate() {
        return new WorkflowId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
