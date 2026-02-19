package dev.planka.domain.schema.definition.template.create;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 新建页字段项配置
 * <p>
 * 定义单个字段在创建表单中的行为和展示。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePageFieldItemConfig {

    /** 字段定义ID */
    @JsonProperty("fieldId")
    private String fieldId;

    /** 宽度百分比（25%, 33%, 50%, 66%, 75%, 100%） */
    @JsonProperty("widthPercent")
    private Integer widthPercent = 50;

    /** 是否从新行开始（强制换行） */
    @JsonProperty("startNewRow")
    private boolean startNewRow = false;

    public CreatePageFieldItemConfig(String fieldId) {
        this.fieldId = fieldId;
    }
}
