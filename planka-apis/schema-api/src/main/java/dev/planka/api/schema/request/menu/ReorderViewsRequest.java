package dev.planka.api.schema.request.menu;

import lombok.Data;

import java.util.List;

/**
 * 重新排序视图请求
 */
@Data
public class ReorderViewsRequest {

    /**
     * 视图 ID 列表（按新的排序顺序）
     */
    private List<String> viewIds;
}
