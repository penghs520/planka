package dev.planka.domain.schema;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 卡片新建页模板ID值对象
 */
public record CardCreatePageTemplateId(@JsonValue String value) implements SchemaId {

    public CardCreatePageTemplateId {
        Objects.requireNonNull(value, "CardCreatePageTemplateId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CardCreatePageTemplateId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.CARD_CREATE_PAGE_TEMPLATE;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CardCreatePageTemplateId of(String value) {
        return new CardCreatePageTemplateId(value);
    }

    /**
     * 使用雪花算法生成新的 CardCreatePageTemplateId
     */
    public static CardCreatePageTemplateId generate() {
        return new CardCreatePageTemplateId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
