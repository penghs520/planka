package dev.planka.domain.schema.definition.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 条件定义
 * <p>
 * 支持任意嵌套的逻辑条件结构。条件由一个根节点组成，
 * 根节点可以是单个条件项或条件组合。
 * <p>
 * 示例：(状态=进行中) AND ((负责人=张三) OR (创建人=张三))
 * <pre>
 * Condition
 * └── root: ConditionGroup(AND)
 *     ├── StatusConditionItem(状态=进行中)
 *     └── ConditionGroup(OR)
 *         ├── EnumConditionItem(负责人=张三)
 *         └── EnumConditionItem(创建人=张三)
 * </pre>
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Condition {

    /** 根条件节点 */
    @JsonProperty("root")
    private ConditionNode root;

    public Condition() {
    }

    @JsonCreator
    public Condition(@JsonProperty("root") ConditionNode root) {
        this.root = root;
    }

    /**
     * 使用条件组创建
     */
    public static Condition of(ConditionGroup group) {
        return new Condition(group);
    }

    /**
     * 使用单个条件项创建
     */
    public static Condition of(AbstractConditionItem item) {
        return new Condition(item);
    }

    /**
     * 创建AND条件
     */
    public static Condition and(ConditionNode... nodes) {
        return new Condition(ConditionGroup.and(nodes));
    }

    /**
     * 创建OR条件
     */
    public static Condition or(ConditionNode... nodes) {
        return new Condition(ConditionGroup.or(nodes));
    }

    /**
     * 是否为空条件（无任何限制）
     */
    public boolean isEmpty() {
        if (root == null){
            return true;
        }
        if (root instanceof ConditionGroup group){
            return group.isEmpty();
        }
        return false;
    }
}
