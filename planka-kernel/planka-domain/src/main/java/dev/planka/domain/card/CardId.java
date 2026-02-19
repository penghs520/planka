package dev.planka.domain.card;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 卡片ID值对象
 * <p>
 * 不可变对象，用于唯一标识一张卡片。
 * 序列化时只输出 value 值。
 */
public record CardId(String value) {

    /**
     * 生成新的卡片ID
     */
    public static CardId generate() {
        return new CardId(String.valueOf(SnowflakeIdGenerator.generate()));
    }

    @JsonValue
    public String toJson() {
        return value;
    }

    /**
     * 从字符串创建卡片ID
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CardId of(String value) {
        return new CardId(value);
    }

    /**
     * 从长整型创建卡片ID（向后兼容方法）
     */
    public static CardId of(long value) {
        return new CardId(String.valueOf(value));
    }

    @Override
    public String toString() {
        return value;
    }

    public String asStr() {
        return value;
    }
}
