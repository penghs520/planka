package dev.planka.card.service.action;

import java.util.List;

/**
 * 动作执行结果
 */
public record ActionExecutionResult(
        ResultType type,
        String message,
        Object data,
        String navigateUrl,
        Boolean openInNewWindow,
        List<RequiredInput> requiredInputs
) {

    /**
     * 结果类型
     */
    public enum ResultType {
        /**
         * 执行成功
         */
        SUCCESS,

        /**
         * 需要跳转页面
         */
        NAVIGATE,

        /**
         * 需要用户输入（前端弹框）
         */
        REQUIRE_INPUT,

        /**
         * 执行失败
         */
        ERROR
    }

    /**
     * 需要用户输入的字段定义
     */
    public record RequiredInput(
            /**
             * 字段ID
             */
            String fieldId,

            /**
             * 字段标签（显示名称）
             */
            String label,

            /**
             * 输入类型（text, textarea, number, date, enum 等）
             */
            String inputType,

            /**
             * 输入提示文字
             */
            String placeholder,

            /**
             * 是否必填
             */
            Boolean required
    ) {
        public static RequiredInput text(String fieldId, String label, String placeholder, boolean required) {
            return new RequiredInput(fieldId, label, "text", placeholder, required);
        }

        public static RequiredInput textarea(String fieldId, String label, String placeholder, boolean required) {
            return new RequiredInput(fieldId, label, "textarea", placeholder, required);
        }
    }

    /**
     * 创建成功结果
     */
    public static ActionExecutionResult success(String message) {
        return new ActionExecutionResult(ResultType.SUCCESS, message, null, null, null, null);
    }

    /**
     * 创建成功结果（带数据）
     */
    public static ActionExecutionResult success(String message, Object data) {
        return new ActionExecutionResult(ResultType.SUCCESS, message, data, null, null, null);
    }

    /**
     * 创建跳转结果
     */
    public static ActionExecutionResult navigate(String url, boolean openInNewWindow) {
        return new ActionExecutionResult(ResultType.NAVIGATE, null, null, url, openInNewWindow, null);
    }

    /**
     * 创建需要用户输入结果
     */
    public static ActionExecutionResult requireInput(String message, List<RequiredInput> requiredInputs) {
        return new ActionExecutionResult(ResultType.REQUIRE_INPUT, message, null, null, null, requiredInputs);
    }

    /**
     * 创建错误结果
     */
    public static ActionExecutionResult error(String message) {
        return new ActionExecutionResult(ResultType.ERROR, message, null, null, null, null);
    }
}
