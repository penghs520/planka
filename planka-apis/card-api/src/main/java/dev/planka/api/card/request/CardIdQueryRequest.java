package dev.planka.api.card.request;

import dev.planka.domain.schema.definition.condition.Condition;
import lombok.Data;

/**
 * 卡片查询请求
 */
@Data
public class CardIdQueryRequest {

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
     * 返回定义
     */
    private Yield yield;


}
