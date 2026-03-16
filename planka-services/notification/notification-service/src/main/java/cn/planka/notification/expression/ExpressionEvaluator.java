package cn.planka.notification.expression;

import java.util.Map;

/**
 * 表达式解析器接口
 * 用于解析通知模板中的表达式
 */
public interface ExpressionEvaluator {
    /**
     * 解析表达式模板
     * 支持格式：${操作人}、${需求.标题}、${归档人}
     *
     * @param template 模板字符串
     * @param context  表达式上下文
     * @return 解析后的字符串
     */
    String evaluate(String template, Map<String, Object> context);
}
