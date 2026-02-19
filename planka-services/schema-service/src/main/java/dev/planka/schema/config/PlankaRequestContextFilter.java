package dev.planka.schema.config;

import dev.planka.common.context.RequestContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 看板请求上下文过滤器
 * <p>
 * 在请求开始时从请求头解析并设置 RequestContext，请求结束时清理。
 * 用于区分前端请求和内部服务调用。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlankaRequestContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                setupRequestContext(httpRequest);
            }
            chain.doFilter(request, response);
        } finally {
            // 确保请求结束时清理上下文，避免内存泄漏
            RequestContext.clear();
        }
    }

    private void setupRequestContext(HttpServletRequest request) {
        String orgId = request.getHeader(RequestContext.HEADER_ORG_ID);
        String internalHeader = request.getHeader(RequestContext.HEADER_INTERNAL_REQUEST);

        boolean isInternal = "true".equalsIgnoreCase(internalHeader);

        RequestContext.set(orgId, isInternal);
    }
}
