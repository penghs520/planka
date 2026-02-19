package dev.planka.domain.schema.definition.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件组合节点
 * <p>
 * 通过逻辑运算符（AND/OR）组合多个子条件节点，支持任意嵌套。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConditionGroup extends ConditionNode {

    /**
     * 逻辑运算符
     */
    @JsonProperty("operator")
    private LogicOperator operator;

    /**
     * 子节点列表
     */
    @JsonProperty("children")
    private List<ConditionNode> children;

    public ConditionGroup() {
        this.operator = LogicOperator.AND;
        this.children = new ArrayList<>();
    }

    @JsonCreator
    public ConditionGroup(@JsonProperty("operator") LogicOperator operator,
                          @JsonProperty("children") List<ConditionNode> children) {
        this.operator = operator != null ? operator : LogicOperator.AND;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
    }

    /**
     * 创建AND组合
     */
    public static ConditionGroup and(ConditionNode... nodes) {
        ConditionGroup group = new ConditionGroup();
        group.setOperator(LogicOperator.AND);
        if (nodes != null) {
            for (ConditionNode node : nodes) {
                if (node != null) {
                    group.getChildren().add(node);
                }
            }
        }
        return group;
    }

    /**
     * 创建OR组合
     */
    public static ConditionGroup or(ConditionNode... nodes) {
        ConditionGroup group = new ConditionGroup();
        group.setOperator(LogicOperator.OR);
        if (nodes != null) {
            for (ConditionNode node : nodes) {
                if (node != null) {
                    group.getChildren().add(node);
                }
            }
        }
        return group;
    }

    @Override
    public String getNodeType() {
        return NodeType.GROUP;
    }

    public boolean isEmpty() {
        if (children == null || children.isEmpty()) {
            return true;
        }
        for (ConditionNode child : children) {
            if (child instanceof ConditionGroup group) {
                if (!group.isEmpty()) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 逻辑运算符
     */
    public enum LogicOperator {
        /**
         * 与 - 所有子条件都满足
         */
        AND,
        /**
         * 或 - 任意一个子条件满足
         */
        OR
    }
}
