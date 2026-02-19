package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 卡片动作ID值对象
 */
public record CardActionId(@JsonValue String value) implements SchemaId {

    public CardActionId {
        Objects.requireNonNull(value, "CardActionId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CardActionId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.CARD_ACTION;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CardActionId of(String value) {
        return new CardActionId(value);
    }

    /**
     * 使用雪花算法生成新的 CardActionId
     */
    public static CardActionId generate() {
        return new CardActionId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
