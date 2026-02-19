package dev.planka.domain.history;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 操作历史ID值对象
 */
public record CardHistoryId(@JsonValue String value) {

    public CardHistoryId {
        Objects.requireNonNull(value, "CardHistoryId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CardHistoryId value cannot be blank");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CardHistoryId of(String value) {
        return new CardHistoryId(value);
    }

    /**
     * 使用雪花算法生成新的 CardHistoryId
     */
    public static CardHistoryId generate() {
        return new CardHistoryId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
