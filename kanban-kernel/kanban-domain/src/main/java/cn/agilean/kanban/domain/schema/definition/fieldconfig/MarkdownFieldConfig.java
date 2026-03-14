package cn.agilean.kanban.domain.schema.definition.fieldconfig;

import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.field.FieldConfigId;
import cn.agilean.kanban.domain.field.FieldId;
import cn.agilean.kanban.domain.schema.SchemaSubType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Markdown属性的卡片类型级别配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class MarkdownFieldConfig extends FieldConfig {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.MARKDOWN_FIELD;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.MARKDOWN;
    }

    /** 最大长度 */
    @JsonProperty("maxLength")
    private Integer maxLength;

    /** 默认值 */
    @JsonProperty("defaultValue")
    private String defaultValue;

    /** 输入提示 */
    @JsonProperty("placeholder")
    private String placeholder;

    @JsonCreator
    public MarkdownFieldConfig(
            @JsonProperty("id") FieldConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("fieldId") FieldId fieldId,
            @JsonProperty("systemField") boolean systemField) {
        super(id, orgId, name, cardTypeId, fieldId, systemField);
    }
}
