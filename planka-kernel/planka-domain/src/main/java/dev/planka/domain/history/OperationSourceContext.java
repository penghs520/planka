package dev.planka.domain.history;

import dev.planka.domain.history.source.UserOperationSource;

/**
 * 操作来源上下文
 * <p>
 * 使用 ThreadLocal 在线程内传递操作来源信息，用于在事件发布时标记操作来源。
 * <p>
 * 使用方式：
 * <pre>
 * try (var ignored = OperationSourceContext.with(new BizRuleOperationSource(ruleId, ruleName))) {
 *     cardService.update(...);
 * }
 * </pre>
 */
public final class OperationSourceContext {

    private static final ThreadLocal<OperationSource> CONTEXT = new ThreadLocal<>();

    private OperationSourceContext() {
    }

    /**
     * 设置操作来源并返回 AutoCloseable，便于 try-with-resources 使用
     */
    public static AutoCloseable with(OperationSource source) {
        CONTEXT.set(source);
        return CONTEXT::remove;
    }

    /**
     * 获取当前线程的操作来源，默认返回 UserOperationSource
     */
    public static OperationSource current() {
        OperationSource source = CONTEXT.get();
        return source != null ? source : UserOperationSource.INSTANCE;
    }

    /**
     * 判断当前是否有设置操作来源（非用户默认操作）
     */
    public static boolean hasSource() {
        return CONTEXT.get() != null;
    }

    /**
     * 清除当前线程的操作来源
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
