package dev.planka.view.converter;

import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.condition.ConditionGroup;
import org.springframework.stereotype.Component;

/**
 * 条件合并器
 * 用于合并视图条件和用户额外条件
 */
@Component
public class ConditionMerger {

    /**
     * 合并视图条件和用户额外条件
     * 使用 AND 逻辑连接两个条件
     *
     * @param viewCondition 视图配置的过滤条件
     * @param userCondition 用户额外的过滤条件
     * @return 合并后的条件
     */
    public Condition merge(Condition viewCondition, Condition userCondition) {
        // 两个都为空
        if (isEmpty(viewCondition) && isEmpty(userCondition)) {
            return null;
        }

        // 只有一个有效
        if (isEmpty(viewCondition)) {
            return userCondition;
        }
        if (isEmpty(userCondition)) {
            return viewCondition;
        }

        // 两个都有效，使用 AND 连接
        ConditionGroup andGroup = ConditionGroup.and(
                viewCondition.getRoot(),
                userCondition.getRoot()
        );
        return Condition.of(andGroup);
    }

    private boolean isEmpty(Condition condition) {
        return condition == null || condition.isEmpty();
    }
}
