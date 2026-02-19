package dev.planka.domain.card;

import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 卡片类型ID值对象
 * <p>
 * 对应旧系统的ValueUnitType (VUT)
 */
public record CardTypeId(@JsonValue String value) implements SchemaId {

    private static final long serialVersionUID = 1L;

    public CardTypeId {
        Objects.requireNonNull(value, "CardTypeId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CardTypeId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.CARD_TYPE;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CardTypeId of(String value) {
        return new CardTypeId(value);
    }

    /**
     * 使用雪花算法生成新的 CardTypeId
     */
    public static CardTypeId generate() {
        return new CardTypeId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
