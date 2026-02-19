package dev.planka.domain.notification;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.domain.expression.TextExpressionTemplate;
import dev.planka.domain.schema.definition.rule.action.RecipientSelector;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 通知模板定义
 * <p>
 * 定义通知的内容模板，绑定到特定的卡片类型和触发事件。
 * 支持短内容（用于 IM/系统通知）和长内容（用于邮件）。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NotificationTemplateDefinition
        extends AbstractSchemaDefinition<NotificationTemplateId> {

    /**
     * 所属卡片类型ID
     */
    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    /**
     * 触发事件类型
     * <p>
     * 与 BizRuleDefinition.TriggerEvent 对应
     */
    @JsonProperty("triggerEvent")
    private BizRuleDefinition.TriggerEvent triggerEvent;

    /**
     * 适用的通知渠道列表
     * <p>
     * 如 ["builtin", "email", "feishu"]
     */
    @JsonProperty("channels")
    private List<String> channels;

    /**
     * 通知标题模板
     * <p>
     * 支持表达式变量，如 ${当前卡.标题}
     */
    @JsonProperty("titleTemplate")
    private TextExpressionTemplate titleTemplate;

    /**
     * 短内容模板（用于 IM/系统通知）
     * <p>
     * 纯文本格式，支持表达式变量
     */
    @JsonProperty("shortContent")
    private String shortContent;

    /**
     * 长内容模板（用于邮件）
     * <p>
     * 富文本 HTML 格式，支持表达式变量
     */
    @JsonProperty("longContent")
    private String longContent;

    /**
     * 接收者选择器
     */
    @JsonProperty("recipientSelector")
    private RecipientSelector recipientSelector;

    /**
     * 是否启用
     */
    @JsonProperty("enabled")
    private boolean enabled = true;

    /**
     * 优先级（数字越小优先级越高）
     */
    @JsonProperty("priority")
    private int priority = 100;

    @JsonCreator
    public NotificationTemplateDefinition(
            @JsonProperty("id") NotificationTemplateId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.NOTIFICATION_TEMPLATE;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.NOTIFICATION_TEMPLATE;
    }

    /**
     * 通知模板属于某个卡片类型
     */
    @Override
    public SchemaId belongTo() {
        return cardTypeId;
    }

    @Override
    public Set<SchemaId> secondKeys() {
        if (cardTypeId != null) {
            return Set.of(cardTypeId);
        }
        return Set.of();
    }

    @Override
    protected NotificationTemplateId newId() {
        return NotificationTemplateId.generate();
    }

    @Override
    public void validate() {
        super.validate();
        if (cardTypeId == null) {
            throw new IllegalArgumentException("所属卡片类型不能为空");
        }
        if (triggerEvent == null) {
            throw new IllegalArgumentException("触发事件不能为空");
        }
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("通知渠道不能为空");
        }
    }
}
