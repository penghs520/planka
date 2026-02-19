package dev.planka.domain.schema.definition.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 调用外部接口执行类型
 * <p>
 * 执行动作时调用外部 HTTP 接口。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CallExternalApiExecution implements ActionExecutionType {

    /**
     * 请求URL
     * <p>
     * 支持变量替换，如 ${card.id}
     */
    @JsonProperty("url")
    private String url;

    /**
     * HTTP方法
     */
    @JsonProperty("method")
    private HttpMethod method = HttpMethod.POST;

    /**
     * 请求头
     */
    @JsonProperty("headers")
    private Map<String, String> headers;

    /**
     * 请求体模板
     * <p>
     * 支持变量替换，如 ${card.title}
     */
    @JsonProperty("bodyTemplate")
    private String bodyTemplate;

    /**
     * 超时时间（毫秒）
     */
    @JsonProperty("timeoutMs")
    private int timeoutMs = 30000;

    /**
     * 是否等待响应
     * <p>
     * true: 等待接口返回后再继续
     * false: 异步调用，不等待响应
     */
    @JsonProperty("waitForResponse")
    private boolean waitForResponse = true;

    @Override
    public String getType() {
        return "CALL_EXTERNAL_API";
    }

    /**
     * HTTP方法枚举
     */
    public enum HttpMethod {
        GET, POST, PUT, PATCH, DELETE
    }
}
