package dev.planka.domain.notification;

import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/**
 * 通知渠道配置定义
 * <p>
 * 存储组织级别的通知渠道配置，如邮件服务器、飞书应用凭证等。
 * 每个组织可以配置多个渠道，每个渠道类型可以有多个配置实例。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NotificationChannelConfigDefinition
        extends AbstractSchemaDefinition<NotificationChannelConfigId> {

    /**
     * 渠道 ID
     * <p>
     * 标识渠道的唯一key，如 builtin, email, feishu, dingtalk, wecom
     */
    @JsonProperty("channelId")
    private String channelId;

    /**
     * 渠道配置参数
     * <p>
     * 存储各渠道特定的配置参数，如：
     * - email: host, port, username, password, from
     * - feishu: appId, appSecret
     * - dingtalk: appKey, appSecret, agentId
     * - wecom: corpId, agentId, secret
     * <p>
     * 敏感信息（如密码、密钥）应加密存储
     */
    @JsonProperty("config")
    private Map<String, Object> config;

    /**
     * 是否为该渠道类型的默认配置
     */
    @JsonProperty("isDefault")
    private boolean isDefault;

    /**
     * 优先级（数字越小优先级越高）
     */
    @JsonProperty("priority")
    private int priority = 100;

    @JsonCreator
    public NotificationChannelConfigDefinition(
            @JsonProperty("id") NotificationChannelConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.NOTIFICATION_CHANNEL_CONFIG;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.NOTIFICATION_CHANNEL_CONFIG;
    }

    /**
     * 渠道配置是组织级别的，不属于其他 Schema
     */
    @Override
    public SchemaId belongTo() {
        return null;
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of();
    }

    @Override
    protected NotificationChannelConfigId newId() {
        return NotificationChannelConfigId.generate();
    }

    @Override
    public void validate() {
        super.validate();
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("渠道ID不能为空");
        }
    }
}
