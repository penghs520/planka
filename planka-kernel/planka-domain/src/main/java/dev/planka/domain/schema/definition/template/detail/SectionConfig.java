package dev.planka.domain.schema.definition.template.detail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 区域配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SectionConfig {

    /** 区域唯一标识 */
    @JsonProperty("sectionId")
    private String sectionId;

    /** 区域名称 */
    @JsonProperty("name")
    private String name;

    /** 是否折叠 */
    @JsonProperty("collapsed")
    private boolean collapsed = false;

    /** 是否可折叠 */
    @JsonProperty("collapsible")
    private boolean collapsible = true;

    /** 字段项配置列表 */
    @JsonProperty("fieldItems")
    private List<FieldItemConfig> fieldItems = new ArrayList<>();

}
