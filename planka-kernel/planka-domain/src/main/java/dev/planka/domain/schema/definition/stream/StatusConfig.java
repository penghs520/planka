package dev.planka.domain.schema.definition.stream;

import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StatusWorkType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 状态配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusConfig {
    /**
     * 状态ID
     */
    @JsonProperty("id")
    private StatusId id;

    /**
     * 状态名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 状态描述
     */
    @JsonProperty("desc")
    private String desc;

    /**
     * 状态工作类型（WAITING: 等待, WORKING: 工作中）
     */
    @JsonProperty("workType")
    private StatusWorkType workType;

    /**
     * 排序号（状态在阶段中的顺序）
     */
    @JsonProperty("sortOrder")
    private int sortOrder;
}
