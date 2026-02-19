package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 关联值
 * <p>
 * 用于设置关联字段的卡片或人员引用。
 * 人员字段也是一种关联类型，使用相同的结构。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LinkValue implements FixedValue {

    /**
     * 关联ID列表（卡片ID或用户ID）
     */
    @JsonProperty("ids")
    private List<String> ids;

    @Override
    public String getValueType() {
        return "LINK";
    }
}
