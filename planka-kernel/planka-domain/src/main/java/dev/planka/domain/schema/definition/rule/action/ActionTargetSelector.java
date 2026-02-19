package dev.planka.domain.schema.definition.rule.action;

import dev.planka.domain.link.Path;
import dev.planka.domain.schema.definition.condition.Condition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 动作目标选择器
 * <p>
 * 定义规则动作的执行目标：当前卡片或关联卡片。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionTargetSelector {

    private final TargetType targetType;
    private final Path linkPath;
    private final Condition filterCondition;

    @JsonCreator
    public ActionTargetSelector(
            @JsonProperty("targetType") TargetType targetType,
            @JsonProperty("linkPath") Path linkPath,
            @JsonProperty("filterCondition") Condition filterCondition) {
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.linkPath = linkPath;
        this.filterCondition = filterCondition;
    }

    @JsonProperty("targetType")
    public TargetType getTargetType() {
        return targetType;
    }

    @JsonProperty("linkPath")
    public Path getLinkPath() {
        return linkPath;
    }

    @JsonProperty("filterCondition")
    public Condition getFilterCondition() {
        return filterCondition;
    }

    /**
     * 目标类型枚举
     */
    public enum TargetType {
        /** 当前触发规则的卡片 */
        CURRENT_CARD,
        /** 通过关联路径找到的卡片 */
        LINKED_CARD
    }

    /**
     * 创建当前卡片目标
     */
    public static ActionTargetSelector currentCard() {
        return new ActionTargetSelector(TargetType.CURRENT_CARD, null, null);
    }

    /**
     * 创建关联卡片目标
     */
    public static ActionTargetSelector linkedCard(Path path) {
        return new ActionTargetSelector(TargetType.LINKED_CARD, path, null);
    }
}
