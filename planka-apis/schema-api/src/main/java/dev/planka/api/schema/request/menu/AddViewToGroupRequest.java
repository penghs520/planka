package dev.planka.api.schema.request.menu;

import lombok.Data;

/**
 * 添加视图到分组请求
 */
@Data
public class AddViewToGroupRequest {

    /**
     * 视图 ID
     */
    private String viewId;

    /**
     * 排序号（可选，不填则追加到末尾）
     */
    private Integer sortOrder;

    /**
     * 自定义显示名称（可选）
     */
    private String displayName;
}
