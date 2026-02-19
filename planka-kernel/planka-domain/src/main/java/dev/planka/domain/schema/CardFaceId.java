package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 卡面定义ID值对象
 */
public record CardFaceId(@JsonValue String value) implements SchemaId {

    public CardFaceId {
        Objects.requireNonNull(value, "CardFaceId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CardFaceId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.CARD_FACE;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CardFaceId of(String value) {
        return new CardFaceId(value);
    }

    /**
     * 使用雪花算法生成新的 CardFaceId
     */
    public static CardFaceId generate() {
        return new CardFaceId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
