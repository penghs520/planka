package dev.planka.api.view.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * 视图数据响应基类
 * 使用 Jackson 多态支持不同视图类型的响应
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "viewType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ListViewDataResponse.class, name = "LIST"),
        // 未来扩展：
        // @JsonSubTypes.Type(value = plankaViewDataResponse.class, name = "planka"),
        // @JsonSubTypes.Type(value = GanttViewDataResponse.class, name = "GANTT"),
})
public abstract class ViewDataResponse {

    /**
     * 视图ID
     */
    private String viewId;

    /**
     * 视图名称
     */
    private String viewName;

    /**
     * 获取视图类型
     */
    public abstract String getViewType();
}
