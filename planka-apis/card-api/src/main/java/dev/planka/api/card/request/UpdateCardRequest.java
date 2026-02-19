package dev.planka.api.card.request;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.card.CardId;
import dev.planka.domain.field.FieldValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 更新卡片请求
 *
 * @param cardId      卡片ID
 * @param title       卡片标题 (可选，只更新原始标题值，不影响拼接部分)
 * @param description 卡片描述 (可选)
 * @param fieldValues 属性值更新 (可选，增量更新，如果要清除某个属性值，则指定FieldValue中value填空)
 * @param linkUpdates 关联属性更新 (可选，覆盖式更新)
 */
public record UpdateCardRequest(CardId cardId, String title, String description,
                                Map<String, FieldValue<?>> fieldValues,
                                List<LinkFieldUpdate> linkUpdates) {

    @JsonCreator
    public UpdateCardRequest(@JsonProperty("cardId") CardId cardId,
                             @JsonProperty("title") String title,
                             @JsonProperty("description") String description,
                             @JsonProperty("fieldValues") Map<String, FieldValue<?>> fieldValues,
                             @JsonProperty("linkUpdates") List<LinkFieldUpdate> linkUpdates) {
        AssertUtils.notNull(cardId, "cardId can't be null");
        this.cardId = cardId;
        this.title = title;
        this.description = description;
        this.fieldValues = fieldValues;
        this.linkUpdates = linkUpdates;
    }

    /**
     * 兼容旧构造函数（不更新关联属性）
     */
    public UpdateCardRequest(CardId cardId, String title, String description,
                             Map<String, FieldValue<?>> fieldValues) {
        this(cardId, title, description, fieldValues, null);
    }
}
