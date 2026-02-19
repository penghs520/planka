package dev.planka.view.executor;

import dev.planka.api.view.request.ViewDataRequest;
import dev.planka.api.view.response.ViewDataResponse;
import dev.planka.domain.schema.definition.view.AbstractViewDefinition;

/**
 * 视图执行器接口
 * <p>
 * 不同视图类型（列表、看板、甘特图等）实现此接口，
 * 采用策略模式支持多种视图类型的扩展。
 *
 * @param <T> 视图定义类型
 * @param <R> 响应类型
 */
public interface ViewExecutor<T extends AbstractViewDefinition, R extends ViewDataResponse> {

    /**
     * 获取支持的视图类型
     *
     * @return 视图类型标识，如 "LIST", "KANBAN", "GANTT"
     */
    String getViewType();

    /**
     * 执行视图查询
     *
     * @param viewDefinition 视图定义
     * @param request        用户请求（包含额外条件、分页等）
     * @param operatorId     操作人ID
     * @return 视图数据响应
     */
    R execute(T viewDefinition, ViewDataRequest request, String operatorId);
}
