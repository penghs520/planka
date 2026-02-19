package dev.planka.api.view.request;

import dev.planka.domain.schema.definition.condition.Condition;
import lombok.Data;

import java.util.List;

/**
 * 视图数据查询请求
 */
@Data
public class ViewDataRequest {

    /**
     * 当前页码（从0开始）
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 用户自定义排序（优先级高于视图配置）
     */
    private List<UserSortField> sorts;

    /**
     * 用户额外过滤条件（与视图条件 AND 合并）
     */
    private Condition additionalCondition;

    /**
     * 关键词搜索（快速搜索，匹配标题等）
     */
    private String keyword;

    /**
     * 指定分组值（用于分组展开时查询特定分组的数据）
     */
    private String groupValue;

    /**
     * 创建默认请求
     */
    public static ViewDataRequest defaultRequest() {
        ViewDataRequest request = new ViewDataRequest();
        request.setPage(0);
        request.setSize(20);
        return request;
    }
}
