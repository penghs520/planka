package dev.planka.domain.notification;

import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 通知模板ID值对象
 */
public record NotificationTemplateId(@JsonValue String value) implements SchemaId {

    public NotificationTemplateId {
        Objects.requireNonNull(value, "NotificationTemplateId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("NotificationTemplateId value cannot be blank");
        }
    }

    @Override
    public SchemaType schemaType() {
        return SchemaType.NOTIFICATION_TEMPLATE;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NotificationTemplateId of(String value) {
        return new NotificationTemplateId(value);
    }

    /**
     * 使用雪花算法生成新的 NotificationTemplateId
     */
    public static NotificationTemplateId generate() {
        return new NotificationTemplateId(SnowflakeIdGenerator.generateStr());
    }

    @Override
    public String toString() {
        return value;
    }
}
