package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 文本值
 * <p>
 * 适用于：单行文本、多行文本、Markdown、URL 字段
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TextValue implements FixedValue {

    /**
     * 文本内容
     */
    @JsonProperty("text")
    private String text;

    @Override
    public String getValueType() {
        return "TEXT";
    }
}
