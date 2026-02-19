package dev.planka.api.card.renderconfig;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 字段渲染配置基类
 * <p>
 * 使用 Jackson 多态序列化，根据 type 字段自动映射到对应的子类。
 * 前端可以根据 type 字段判断实体类型并解析对应的配置。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EnumRenderConfig.class, name = "ENUM"),
        @JsonSubTypes.Type(value = DateRenderConfig.class, name = "DATE"),
        @JsonSubTypes.Type(value = NumberRenderConfig.class, name = "NUMBER"),
        @JsonSubTypes.Type(value = TextRenderConfig.class, name = "TEXT"),
        @JsonSubTypes.Type(value = AttachmentRenderConfig.class, name = "ATTACHMENT"),
        @JsonSubTypes.Type(value = LinkRenderConfig.class, name = "LINK"),
        @JsonSubTypes.Type(value = StructureRenderConfig.class, name = "STRUCTURE"),
        @JsonSubTypes.Type(value = WebUrlRenderConfig.class, name = "WEB_URL"),
        @JsonSubTypes.Type(value = MarkdownRenderConfig.class, name = "MARKDOWN"),
})
public abstract class FieldRenderConfig {
    // 基类，无公共字段
}
