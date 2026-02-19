package dev.planka.api.card.request;

import dev.planka.domain.schema.definition.condition.Condition;
import lombok.Data;

/**
 * 卡片查询请求
 */
@Data
public class CardCountRequest {

    /**
     * 查询上下文
     */
    private QueryContext queryContext;

    /**
     * 查询范围
     */
    private QueryScope queryScope;

    /**
     * 查询条件
     */
    private Condition condition;

    /**
     * 指定去重字段（可选）
     */
    private String distinctField;

}
