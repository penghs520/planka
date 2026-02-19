package dev.planka.domain.schema.definition.template.create;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 新建页区域配置
 * <p>
 * 在创建表单中对相关字段进行分组。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePageSectionConfig {

    /** 区域唯一标识 */
    @JsonProperty("sectionId")
    private String sectionId;

    /** 区域名称 */
    @JsonProperty("name")
    private String name;

    /** 是否可折叠 */
    @JsonProperty("collapsible")
    private boolean collapsible = true;

    /** 是否默认折叠 */
    @JsonProperty("collapsed")
    private boolean collapsed = false;

    /** 字段项配置列表 */
    @JsonProperty("fieldItems")
    private List<CreatePageFieldItemConfig> fieldItems = new ArrayList<>();

    public CreatePageSectionConfig(String sectionId, String name) {
        this.sectionId = sectionId;
        this.name = name;
    }
}
