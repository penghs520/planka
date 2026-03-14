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

import java.util.List;

/**
 * 附件属性的卡片类型级别配置
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AttachmentFieldConfig extends FieldConfig {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.ATTACHMENT_FIELD;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.ATTACHMENT;
    }

    /** 允许的文件类型（扩展名） */
    @JsonProperty("allowedFileTypes")
    private List<String> allowedFileTypes;

    /** 最大文件大小（字节） */
    @JsonProperty("maxFileSize")
    private Long maxFileSize;

    /** 最大文件数量 */
    @JsonProperty("maxFileCount")
    private Integer maxFileCount;

    @JsonCreator
    public AttachmentFieldConfig(
            @JsonProperty("id") FieldConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("fieldId") FieldId fieldId,
            @JsonProperty("systemField") boolean systemField) {
        super(id, orgId, name, cardTypeId, fieldId, systemField);
    }

}
