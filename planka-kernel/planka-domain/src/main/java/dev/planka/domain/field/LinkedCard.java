package dev.planka.domain.field;

import dev.planka.domain.card.CardTitle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 关联卡片摘要
 * <p>
 * 用于关联属性编辑器中显示可选卡片列表
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LinkedCard(
        @JsonProperty("cardId") String cardId,
        @JsonProperty("title") CardTitle title
) {
}
