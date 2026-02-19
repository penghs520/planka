package dev.planka.domain.notification;

import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 通知渠道配置ID值对象
 */
public record NotificationChannelConfigId(@JsonValue String value) implements SchemaId {

    public NotificationChannelConfigId {
        Objects.requireNonNull(value, "NotificationChannelConfigId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("NotificationChannelConfigId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.NOTIFICATION_CHANNEL_CONFIG;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NotificationChannelConfigId of(String value) {
        return new NotificationChannelConfigId(value);
    }

    /**
     * 使用雪花算法生成新的 NotificationChannelConfigId
     */
    public static NotificationChannelConfigId generate() {
        return new NotificationChannelConfigId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
