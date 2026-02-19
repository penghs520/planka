package dev.planka.common.feign;

import dev.planka.common.context.RequestContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Feign 内部请求拦截器
 * <p>
 * 自动为内部服务调用添加 X-Internal-Request=true 请求头，
 * 并传递当前请求的上下文信息（orgId）。
 */
public class InternalRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 标记为内部请求
        template.header(RequestContext.HEADER_INTERNAL_REQUEST, "true");

        // 传递当前请求上下文中的信息
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            template.header(RequestContext.HEADER_ORG_ID, context.getOrgId());
        }
    }
}
