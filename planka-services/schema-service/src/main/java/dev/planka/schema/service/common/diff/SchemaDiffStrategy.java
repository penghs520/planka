package dev.planka.schema.service.common.diff;

import dev.planka.domain.schema.changelog.ChangeDetail;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Schema差异比较策略接口
 * <p>
 * 为不同类型的Schema定义提供特化的差异比较逻辑。
 * 实现类应使用 @Component 注解，以便自动注入到策略列表中。
 */
public interface SchemaDiffStrategy {

    /**
     * 判断策略是否支持指定的Schema子类型
     *
     * @param schemaSubType Schema子类型标识
     * @return 是否支持
     */
    boolean supports(String schemaSubType);

    /**
     * 比较���个Schema快照，生成变更详情
     *
     * @param before 变更前的JSON节点
     * @param after  变更后的JSON节点
     * @return 变更详情
     */
    ChangeDetail diff(JsonNode before, JsonNode after);

    /**
     * 获取策略优先级（数值越小优先级越高）
     * <p>
     * 默认优先级为100，特化策略应返回更小的值。
     *
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
}
