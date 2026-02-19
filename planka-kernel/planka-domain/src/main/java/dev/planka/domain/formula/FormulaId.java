package dev.planka.domain.formula;

import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 计算公式ID值对象
 */
public record FormulaId(@JsonValue String value) implements SchemaId {

    private static final long serialVersionUID = 1L;

    public FormulaId {
        Objects.requireNonNull(value, "FormulaId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("FormulaId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.FORMULA_DEFINITION;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FormulaId of(String value) {
        // 允许空字符串或 null，用于创建新公式时前端可能发送空字符串
        if (value == null || value.isBlank()) {
            return null;
        }
        return new FormulaId(value);
    }

    /**
     * 使用雪花算法生成新的 FormulaId
     */
    public static FormulaId generate() {
        return new FormulaId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
