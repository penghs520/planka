package dev.planka.domain.schema.definition.condition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 条件节点抽象基类
 * <p>
 * 支持任意嵌套的逻辑条件结构。条件节点分为两种类型：
 * <ul>
 *     <li>叶子节点（ConditionItem）：具体的条件项，如"状态=进行中"</li>
 *     <li>组合节点（ConditionGroup）：通过逻辑运算符（AND/OR）组合多个子节点</li>
 * </ul>
 * <p>
 * 示例：(A AND B) OR (C AND (D OR E)) 可以表示为：
 * <pre>
 * ConditionGroup(OR)
 * ├── ConditionGroup(AND)
 * │   ├── ConditionItem(A)
 * │   └── ConditionItem(B)
 * └── ConditionGroup(AND)
 *     ├── ConditionItem(C)
 *     └── ConditionGroup(OR)
 *         ├── ConditionItem(D)
 *         └── ConditionItem(E)
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "nodeType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConditionGroup.class, name = NodeType.GROUP),
        // 自定义字段条件类型
        @JsonSubTypes.Type(value = TextConditionItem.class, name = NodeType.TEXT),
        @JsonSubTypes.Type(value = NumberConditionItem.class, name = NodeType.NUMBER),
        @JsonSubTypes.Type(value = DateConditionItem.class, name = NodeType.DATE),
        @JsonSubTypes.Type(value = EnumConditionItem.class, name = NodeType.ENUM),
        @JsonSubTypes.Type(value = WebUrlConditionItem.class, name = NodeType.WEB_URL),
        @JsonSubTypes.Type(value = LinkConditionItem.class, name = NodeType.LINK),
        // 价值流状态条件
        @JsonSubTypes.Type(value = StatusConditionItem.class, name = NodeType.STATUS),
        // 卡片生命周期状态条件
        @JsonSubTypes.Type(value = CardCycleConditionItem.class, name = NodeType.CARD_CYCLE),
        // 系统字段条件类型
        @JsonSubTypes.Type(value = TitleConditionItem.class, name = NodeType.TITLE),
        @JsonSubTypes.Type(value = CodeConditionItem.class, name = NodeType.CODE),
        @JsonSubTypes.Type(value = KeywordConditionItem.class, name = NodeType.KEYWORD),
        // 系统用户字段条件类型
        @JsonSubTypes.Type(value = SystemUserConditionItem.class, name = NodeType.CREATED_BY),
        @JsonSubTypes.Type(value = SystemUserConditionItem.class, name = NodeType.UPDATED_BY),
})
public abstract class ConditionNode {

    /**
     * 获取节点类型
     */
    public abstract String getNodeType();

    /**
     * 判断条件节点是否为空
     */
    public abstract boolean isEmpty();

}
