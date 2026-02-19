package dev.planka.domain.schema.definition.stream;

import dev.planka.domain.stream.StepId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 阶段配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepConfig {
    /**
     * 阶段ID
     */
    @JsonProperty("id")
    private StepId id;

    /**
     * 阶段名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 阶段描述
     */
    @JsonProperty("desc")
    private String desc;

    @JsonProperty("kind")
    private StepStatusKind kind;

    /**
     * 排序号（阶段在价值流中的顺序）
     */
    @JsonProperty("sortOrder")
    private int sortOrder;

    @JsonProperty("statusList")
    private List<StatusConfig> statusList;
}
