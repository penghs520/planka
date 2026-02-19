package dev.planka.domain.schema.definition.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 跳转页面执行类型
 * <p>
 * 执行动作时跳转到指定页面。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NavigateToPageExecution implements ActionExecutionType {

    /**
     * 目标URL
     * <p>
     * 支持变量替换，如 ${card.id}
     */
    @JsonProperty("targetUrl")
    private String targetUrl;

    /**
     * URL参数
     */
    @JsonProperty("urlParams")
    private Map<String, String> urlParams;

    /**
     * 是否新窗口打开
     */
    @JsonProperty("openInNewWindow")
    private boolean openInNewWindow = false;

    @Override
    public String getType() {
        return "NAVIGATE_TO_PAGE";
    }
}
