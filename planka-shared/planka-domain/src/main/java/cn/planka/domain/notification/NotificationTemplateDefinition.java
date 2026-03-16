package cn.planka.domain.notification;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.expression.TextExpressionTemplate;
import cn.planka.domain.schema.SchemaId;
import cn.planka.domain.schema.SchemaSubType;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
import cn.planka.domain.schema.definition.rule.BizRuleDefinition;
import cn.planka.domain.schema.definition.rule.action.RecipientSelector;
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
 * 定义通知的内容模板，支持多种定义参数类型。
 * 通知模板只能选择短内容模板或长内容模板之一，不能同时选择：
 * <ul>
 *   <li>短内容模板：用于 IM/系统通知，只包含一个文本表达式模板</li>
 *   <li>长内容模板：用于邮件通知，包含抄送人和富文本表达式模板</li>
 * </ul>
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NotificationTemplateDefinition
        extends AbstractSchemaDefinition<NotificationTemplateId> {

    /**
     * 模板类型（内置/自定义）
     */
    @JsonProperty("templateType")
    private TemplateType templateType = TemplateType.CUSTOM;

    /**
     * 定义参数
     * <p>
     * 支持多种类型：卡片类型、日期、文本、多行文本、链接、数字
     */
    @JsonProperty("definitionParameter")
    private DefinitionParameter definitionParameter;

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
     * 通知内容
     * <p>
     * 只能选择短内容模板或长内容模板之一
     */
    @JsonProperty("content")
    private NotificationContent content;

    /**
     * 接收者选择器
     */
    @JsonProperty("recipientSelector")
    private RecipientSelector recipientSelector;

    /**
     * 通知对象类型：MEMBER-通知人, GROUP-通知群
     */
    @JsonProperty("recipientType")
    private RecipientType recipientType = RecipientType.MEMBER;

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
     * 通知模板属于某个卡片类型（仅当定义参数为卡片类型时）
     */
    @Override
    public SchemaId belongTo() {
        if (definitionParameter instanceof CardTypeDefinitionParameter cardTypeParam) {
            return cardTypeParam.getCardTypeId();
        }
        return null;
    }

    @Override
    public Set<SchemaId> secondKeys() {
        if (definitionParameter instanceof CardTypeDefinitionParameter cardTypeParam
                && cardTypeParam.getCardTypeId() != null) {
            return Set.of(cardTypeParam.getCardTypeId());
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
        if (definitionParameter == null) {
            throw new IllegalArgumentException("定义参数不能为空");
        }
        if (triggerEvent == null) {
            throw new IllegalArgumentException("触发事件不能为空");
        }
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("通知渠道不能为空");
        }
        if (content == null) {
            throw new IllegalArgumentException("通知内容不能为空");
        }
    }

    /**
     * 获取卡片类型ID（兼容方法，仅当定义参数为卡片类型时有效）
     */
    public CardTypeId getCardTypeId() {
        if (definitionParameter instanceof CardTypeDefinitionParameter cardTypeParam) {
            return cardTypeParam.getCardTypeId();
        }
        return null;
    }
}
