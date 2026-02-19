package dev.planka.common.context;

/**
 * 请求上下文
 * <p>
 * 存储当前请求的来源信息，用于区分前端请求和内部服务调用。
 * 使用 ThreadLocal 存储，在请求开始时设置，请求结束时清理。
 */
public class RequestContext {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    /**
     * 内部请求标识请求头名称
     */
    public static final String HEADER_INTERNAL_REQUEST = "X-Internal-Request";

    /**
     * 组织ID请求头名称
     */
    public static final String HEADER_ORG_ID = "X-Org-Id";

    private final String orgId;
    private final boolean internalRequest;

    private RequestContext(String orgId, boolean internalRequest) {
        this.orgId = orgId;
        this.internalRequest = internalRequest;
    }

    /**
     * 设置请求上下文
     *
     * @param orgId           组织ID
     * @param internalRequest 是否为内部服务请求
     */
    public static void set(String orgId, boolean internalRequest) {
        CONTEXT.set(new RequestContext(orgId, internalRequest));
    }

    /**
     * 获取当前请求上下文
     *
     * @return 请求上下文，如果未设置则返回 null
     */
    public static RequestContext get() {
        return CONTEXT.get();
    }

    /**
     * 清理请求上下文
     * <p>
     * 必须在请求结束时调用，避免内存泄漏
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取组织ID
     */
    public String getOrgId() {
        return orgId;
    }

    /**
     * 是否为内部服务请求
     */
    public boolean isInternalRequest() {
        return internalRequest;
    }

    /**
     * 判断当前上下文是否为内部请求
     *
     * @return 如果是内部请求返回 true，否则返回 false
     */
    public static boolean isCurrentInternal() {
        RequestContext ctx = get();
        return ctx != null && ctx.isInternalRequest();
    }
}
