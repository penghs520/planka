package cn.agilean.kanban.notification.expression;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单表达式解析器实现
 * 基于正则替换，支持 ${...} 格式的表达式
 */
@Slf4j
@Component
public class SimpleExpressionEvaluator implements ExpressionEvaluator {
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    public String evaluate(String template, Map<String, Object> context) {
        if (template == null) {
            return null;
        }

        if (context == null || context.isEmpty()) {
            return template;
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String expression = matcher.group(1);
            Object value = resolveExpression(expression, context);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 解析表达式
     * 支持点号分隔的路径：需求.标题
     */
    private Object resolveExpression(String expression, Map<String, Object> context) {
        // 支持点号分隔的路径
        String[] parts = expression.split("\\.");

        Object current = context.get(parts[0]);
        for (int i = 1; i < parts.length && current != null; i++) {
            current = getProperty(current, parts[i]);
        }

        return current;
    }

    /**
     * 获取对象属性
     * 支持 Map 和反射获取
     */
    private Object getProperty(Object obj, String propertyName) {
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).get(propertyName);
        }

        // 使用反射获取属性
        try {
            String getterName = "get" +
                    propertyName.substring(0, 1).toUpperCase() +
                    propertyName.substring(1);
            Method getter = obj.getClass().getMethod(getterName);
            return getter.invoke(obj);
        } catch (Exception e) {
            log.warn("Failed to get property {} from object {}: {}",
                    propertyName, obj.getClass().getName(), e.getMessage());
            return null;
        }
    }
}
