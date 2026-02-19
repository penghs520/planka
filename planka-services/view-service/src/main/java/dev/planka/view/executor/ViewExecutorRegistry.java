package dev.planka.view.executor;

import dev.planka.domain.schema.definition.view.AbstractViewDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视图执行器注册表
 * <p>
 * 管理所有视图执行器，根据视图类型获取对应的执行器。
 * 采用自动注册机制，新增视图类型只需实现 ViewExecutor 接口即可。
 */
@Component
public class ViewExecutorRegistry {

    private final Map<String, ViewExecutor<?, ?>> executors = new HashMap<>();

    @Autowired
    public ViewExecutorRegistry(List<ViewExecutor<?, ?>> executorList) {
        for (ViewExecutor<?, ?> executor : executorList) {
            executors.put(executor.getViewType(), executor);
        }
    }

    /**
     * 获取指定视图类型的执行器
     *
     * @param viewType 视图类型
     * @param <T>      视图定义类型
     * @param <R>      响应类型
     * @return 视图执行器
     * @throws IllegalArgumentException 如果不支持该视图类型
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractViewDefinition,
            R extends dev.planka.api.view.response.ViewDataResponse>
    ViewExecutor<T, R> getExecutor(String viewType) {
        ViewExecutor<?, ?> executor = executors.get(viewType);
        if (executor == null) {
            throw new IllegalArgumentException("不支持的视图类型: " + viewType);
        }
        return (ViewExecutor<T, R>) executor;
    }

    /**
     * 检查是否支持指定视图类型
     *
     * @param viewType 视图类型
     * @return 是否支持
     */
    public boolean supports(String viewType) {
        return executors.containsKey(viewType);
    }
}
