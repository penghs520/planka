package dev.planka.domain.schema.definition.fieldconfig;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.SchemaSubType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 网页链接属性的卡片类型级别配置
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class WebUrlFieldConfig extends FieldConfig {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.WEB_URL_FIELD;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.WEB_URL;
    }

    /** 是否验证URL格式 */
    @JsonProperty("validateUrl")
    private boolean validateUrl = true;

    /** 是否显示链接预览 */
    @JsonProperty("showPreview")
    private boolean showPreview = false;

    /** 默认URL */
    @JsonProperty("defaultUrl")
    private String defaultUrl;

    /** 默认链接文本 */
    @JsonProperty("defaultLinkText")
    private String defaultLinkText;

    @JsonCreator
    public WebUrlFieldConfig(
            @JsonProperty("id") FieldConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("fieldId") FieldId fieldId,
            @JsonProperty("systemField") boolean systemField) {
        super(id, orgId, name, cardTypeId, fieldId, systemField);
    }

}
