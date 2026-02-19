package dev.planka.domain.stream;

import dev.planka.common.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 流动记录ID值对象
 */
public record FlowRecordId(@JsonValue long value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FlowRecordId of(long value) {
        return new FlowRecordId(value);
    }

    /**
     * 使用雪花算法生成新的 FlowRecordId
     */
    public static FlowRecordId generate() {
        return new FlowRecordId(SnowflakeIdGenerator.generate());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
