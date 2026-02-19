package dev.planka.api.view.request;

import dev.planka.domain.schema.definition.view.AbstractViewDefinition;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 视图预览请求
 * 用于在配置视图时预览数据效果
 */
@Data
public class ViewPreviewRequest {

    /**
     * 视图定义（用于预览，未持久化）
     */
    @NotNull(message = "视图定义不能为空")
    private AbstractViewDefinition viewDefinition;

    /**
     * 数据查询请求参数
     */
    private ViewDataRequest dataRequest;

    /**
     * 获取数据请求，如果为空则返回默认请求
     */
    public ViewDataRequest getDataRequestOrDefault() {
        return dataRequest != null ? dataRequest : ViewDataRequest.defaultRequest();
    }
}
