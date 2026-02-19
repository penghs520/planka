package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 枚举值
 * <p>
 * 支持单选和多选枚举
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EnumValue implements FixedValue {

    /**
     * 枚举值ID列表
     * <p>
     * 单选枚举：列表只有一个元素
     * 多选枚举：列表可以有多个元素
     */
    @JsonProperty("enumValueIds")
    private List<String> enumValueIds;

    @Override
    public String getValueType() {
        return "ENUM";
    }
}
