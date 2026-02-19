package dev.planka.domain.expression;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文本表达式模板
 * <p>
 * 支持 `${card.fieldId}`、`${member.fieldId}`、`${system.currentTime}` 等表达式
 */
public record TextExpressionTemplate(String template) {

    /**
     * 用于 JSON 序列化 - 输出为纯字符串
     */
    @JsonValue
    public String template() {
        return template;
    }

    /**
     * 用于 JSON 反序列化 - 从字符串创建实例
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TextExpressionTemplate of(String value) {
        return value == null ? null : new TextExpressionTemplate(value);
    }

}
