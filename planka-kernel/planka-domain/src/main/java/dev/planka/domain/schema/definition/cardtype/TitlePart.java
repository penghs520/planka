package dev.planka.domain.schema.definition.cardtype;

import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 标题组成部分
 * <p>
 * 表示卡片标题的一个组成部分，由路径和字段ID组合定义。
 * 路径用于导航到目标卡片，字段ID用于提取该卡片上的字段值。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TitlePart(
        /**
         * 关联路径
         * <p>
         * 可选。如果为null，表示直接从当前卡片获取字段值。
         * 如果有值，表示通过关联路径导航到目标卡片。
         */
        @JsonProperty("path")
        Path path,

        /**
         * 字段ID
         * <p>
         * 要提取值的字段标识符。
         */
        @JsonProperty("fieldId")
        String fieldId
) {
    @JsonCreator
    public TitlePart {
        // path 可以为 null，表示当前卡片
        // fieldId 必须存在
        if (fieldId == null || fieldId.isBlank()) {
            throw new IllegalArgumentException("fieldId of TitlePart can't be null or blank");
        }
    }
}
