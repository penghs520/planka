package cn.planka.api.user.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 将当前 HTTP 请求的认证与组织上下文头转发到 user-service（无 Web 上下文时不处理）
 */
public class AuthForwardingFeignInterceptor implements RequestInterceptor {

    private static void copyHeader(RequestTemplate template, HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v != null && !v.isBlank()) {
            template.header(name, v);
        }
    }

    @Override
    public void apply(RequestTemplate template) {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return;
        }
        HttpServletRequest request = servletAttrs.getRequest();
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && !auth.isBlank()) {
            template.header(HttpHeaders.AUTHORIZATION, auth);
        }
        copyHeader(template, request, "X-User-Id");
        copyHeader(template, request, "X-Org-Id");
        copyHeader(template, request, "X-Member-Card-Id");
        copyHeader(template, request, "X-Role");
        copyHeader(template, request, "X-User-Email");
    }
}
