package cn.agilean.kanban.domain.notification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 定义参数接口
 * <p>
 * 通知模板的定义参数，支持多种类型：
 * <ul>
 *   <li>CARD_TYPE：卡片类型参数，需要选择具体的 CardTypeId</li>
 *   <li>DATE/TEXT/MULTILINE_TEXT/LINK/NUMBER：简单参数，只需要输入名称作为标识</li>
 * </ul>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardTypeDefinitionParameter.class, name = "CARD_TYPE"),
        @JsonSubTypes.Type(value = SimpleDefinitionParameter.class, name = "DATE"),
        @JsonSubTypes.Type(value = SimpleDefinitionParameter.class, name = "TEXT"),
        @JsonSubTypes.Type(value = SimpleDefinitionParameter.class, name = "MULTILINE_TEXT"),
        @JsonSubTypes.Type(value = SimpleDefinitionParameter.class, name = "LINK"),
        @JsonSubTypes.Type(value = SimpleDefinitionParameter.class, name = "NUMBER")
})
public sealed interface DefinitionParameter permits CardTypeDefinitionParameter, SimpleDefinitionParameter {

    /**
     * 获取参数类型
     */
    DefinitionParameterType getType();

    /**
     * 获取显示名称
     */
    String getDisplayName();
}
