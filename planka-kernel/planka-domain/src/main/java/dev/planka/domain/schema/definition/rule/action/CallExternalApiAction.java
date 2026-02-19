package dev.planka.domain.schema.definition.rule.action;

import dev.planka.domain.expression.TextExpressionTemplate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * 调用外部API动作
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CallExternalApiAction implements RuleAction {

    private final TextExpressionTemplate urlTemplate;
    private final HttpMethod method;
    private final Map<String, String> headers;
    private final TextExpressionTemplate bodyTemplate;
    private final int timeoutMs;
    private final int sortOrder;

    @JsonCreator
    public CallExternalApiAction(
            @JsonProperty("urlTemplate") TextExpressionTemplate urlTemplate,
            @JsonProperty("method") HttpMethod method,
            @JsonProperty("headers") Map<String, String> headers,
            @JsonProperty("bodyTemplate") TextExpressionTemplate bodyTemplate,
            @JsonProperty("timeoutMs") Integer timeoutMs,
            @JsonProperty("sortOrder") Integer sortOrder) {
        this.urlTemplate = Objects.requireNonNull(urlTemplate, "urlTemplate must not be null");
        this.method = method != null ? method : HttpMethod.POST;
        this.headers = headers;
        this.bodyTemplate = bodyTemplate;
        this.timeoutMs = timeoutMs != null ? timeoutMs : 30000;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    @JsonProperty("urlTemplate")
    public TextExpressionTemplate getUrlTemplate() {
        return urlTemplate;
    }

    @JsonProperty("method")
    public HttpMethod getMethod() {
        return method;
    }

    @JsonProperty("headers")
    public Map<String, String> getHeaders() {
        return headers;
    }

    @JsonProperty("bodyTemplate")
    public TextExpressionTemplate getBodyTemplate() {
        return bodyTemplate;
    }

    @JsonProperty("timeoutMs")
    public int getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public String getActionType() {
        return "CALL_EXTERNAL_API";
    }

    @Override
    @JsonProperty("sortOrder")
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * HTTP方法枚举
     */
    public enum HttpMethod {
        GET, POST, PUT, PATCH, DELETE
    }
}
