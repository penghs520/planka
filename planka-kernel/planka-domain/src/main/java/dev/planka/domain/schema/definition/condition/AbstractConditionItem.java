package dev.planka.domain.schema.definition.condition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * 条件项抽象基类（叶子节点）
 * <p>
 * 表示一个具体的过滤条件项，包含字段标识和操作符。
 * 继承自ConditionNode，作为条件树的叶子节点，是一个具体的过滤条件。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractConditionItem extends ConditionNode  {

    /**
     * 条件项始终不为空
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

}
