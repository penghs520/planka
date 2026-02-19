package dev.planka.domain.schema.definition.cardtype;

import dev.planka.domain.card.CardTitle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 卡片标题组合规则
 * <p>
 * 用于配置卡片标题的自动生成规则。
 * 当规则启用时，卡片标题将由配置的多个部分（字段值）自动组合生成，
 * 支持配置为前缀或后缀模式。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TitleCompositionRule {

    /**
     * 是否启用规则
     */
    @JsonProperty("enabled")
    private boolean enabled;

    /**
     * 拼接区域（前缀或后缀）
     */
    @JsonProperty("area")
    private CardTitle.JointArea area;

    /**
     * 拼接部分列表
     * <p>
     * 每个部分由 Path（关联路径）和 fieldId（字段ID）组成。
     * 多个部分将按顺序拼接到标题中。
     */
    @JsonProperty("parts")
    private List<TitlePart> parts;

    @JsonCreator
    public TitleCompositionRule(
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("area") CardTitle.JointArea area,
            @JsonProperty("parts") List<TitlePart> parts) {
        this.enabled = enabled;
        this.area = area != null ? area : CardTitle.JointArea.PREFIX;
        this.parts = parts;
    }
}
