package cn.planka.domain.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 简单定义参数
 * <p>
 * 用于 DATE/TEXT/MULTILINE_TEXT/LINK/NUMBER 类型，只需要输入名称作为标识。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SimpleDefinitionParameter implements DefinitionParameter {

    /**
     * 参数类型
     */
    @JsonProperty("type")
    private final DefinitionParameterType type;

    /**
     * 参数名称（标识）
     */
    @JsonProperty("name")
    private final String name;

    @JsonCreator
    public SimpleDefinitionParameter(
            @JsonProperty("type") DefinitionParameterType type,
            @JsonProperty("name") String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public DefinitionParameterType getType() {
        return type;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    /**
     * 工厂方法
     */
    public static SimpleDefinitionParameter ofDate(String name) {
        return new SimpleDefinitionParameter(DefinitionParameterType.DATE, name);
    }

    public static SimpleDefinitionParameter ofText(String name) {
        return new SimpleDefinitionParameter(DefinitionParameterType.TEXT, name);
    }

    public static SimpleDefinitionParameter ofMultilineText(String name) {
        return new SimpleDefinitionParameter(DefinitionParameterType.MULTILINE_TEXT, name);
    }

    public static SimpleDefinitionParameter ofLink(String name) {
        return new SimpleDefinitionParameter(DefinitionParameterType.LINK, name);
    }

    public static SimpleDefinitionParameter ofNumber(String name) {
        return new SimpleDefinitionParameter(DefinitionParameterType.NUMBER, name);
    }
}
