package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 卡片详情模板ID值对象
 */
public record CardDetailTemplateId(@JsonValue String value) implements SchemaId {

    public CardDetailTemplateId {
        Objects.requireNonNull(value, "CardDetailTemplateId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CardDetailTemplateId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.CARD_DETAIL_TEMPLATE;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CardDetailTemplateId of(String value) {
        return new CardDetailTemplateId(value);
    }

    /**
     * 使用雪花算法生成新的 CardDetailTemplateId
     */
    public static CardDetailTemplateId generate() {
        return new CardDetailTemplateId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
