package dev.planka.card.service.rule.executor.action;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.rule.executor.ActionTargetResolver;
import dev.planka.card.service.rule.executor.RuleExecutionContext;
import dev.planka.card.service.rule.executor.RuleExecutionResult;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.definition.rule.action.CallExternalApiAction;
import dev.planka.infra.expression.TextExpressionTemplateResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 调用外部API动作执行器
 */
@Slf4j
@Component
public class CallExternalApiActionExecutor extends AbstractRuleActionExecutor<CallExternalApiAction> {

    private final TextExpressionTemplateResolver templateResolver;
    private final RestTemplate restTemplate;

    public CallExternalApiActionExecutor(ActionTargetResolver targetResolver,
                                          TextExpressionTemplateResolver templateResolver) {
        super(targetResolver);
        this.templateResolver = templateResolver;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getActionType() {
        return "CALL_EXTERNAL_API";
    }

    @Override
    public RuleExecutionResult.ActionExecutionResult execute(CallExternalApiAction action, RuleExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            CardId memberCardId = context.getOperatorId() != null ? CardId.of(Long.parseLong(context.getOperatorId())) : null;
            // 解析URL模板
            String url = templateResolver.resolve(action.getUrlTemplate(), context.getCardId(), memberCardId);

            // 构建请求头
            HttpHeaders headers = buildHeaders(action, context);

            // 构建请求体
            Object body = buildRequestBody(action, context);

            // 执行请求
            HttpMethod method = resolveMethod(action.getMethod());
            HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);

            log.info("调用外部API: method={}, url={}", method, url);

            ResponseEntity<String> response = restTemplate.exchange(url, method, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API调用返回非成功状态码: " + response.getStatusCode());
            }

            log.debug("外部API调用成功: url={}, status={}", url, response.getStatusCode());

            long duration = System.currentTimeMillis() - startTime;
            return RuleExecutionResult.ActionExecutionResult.builder()
                    .actionType(getActionType())
                    .sortOrder(action.getSortOrder())
                    .success(true)
                    .durationMs(duration)
                    .data(response.getBody())
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("调用外部API失败: urlTemplate={}, error={}", action.getUrlTemplate(), e.getMessage(), e);
            return RuleExecutionResult.ActionExecutionResult.failed(
                    getActionType(),
                    action.getSortOrder(),
                    duration,
                    e.getMessage()
            );
        }
    }

    @Override
    protected CardId executeOnCard(CallExternalApiAction action, CardDTO targetCard, RuleExecutionContext context) {
        // 此方法不使用
        return null;
    }

    private HttpHeaders buildHeaders(CallExternalApiAction action, RuleExecutionContext context) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        if (action.getHeaders() != null) {
            CardId memberCardId = context.getOperatorId() != null ? CardId.of(Long.parseLong(context.getOperatorId())) : null;
            for (Map.Entry<String, String> entry : action.getHeaders().entrySet()) {
                // Headers are simple strings, not expression templates
                headers.set(entry.getKey(), entry.getValue());
            }
        }

        return headers;
    }

    private Object buildRequestBody(CallExternalApiAction action, RuleExecutionContext context) {
        if (action.getBodyTemplate() == null) {
            return null;
        }

        // 解析模板中的表达式
        CardId memberCardId = context.getOperatorId() != null ? CardId.of(Long.parseLong(context.getOperatorId())) : null;
        String bodyString = templateResolver.resolve(action.getBodyTemplate(), context.getCardId(), memberCardId);

        // 如果是JSON格式，可以直接返回字符串
        return bodyString;
    }

    private HttpMethod resolveMethod(CallExternalApiAction.HttpMethod method) {
        if (method == null) {
            return HttpMethod.POST;
        }
        return switch (method) {
            case GET -> HttpMethod.GET;
            case POST -> HttpMethod.POST;
            case PUT -> HttpMethod.PUT;
            case DELETE -> HttpMethod.DELETE;
            case PATCH -> HttpMethod.PATCH;
        };
    }
}
