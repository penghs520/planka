package dev.planka.common.util;

import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.exception.ErrorCode;
import dev.planka.common.exception.KanbanException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 断言工具类
 * <p>
 * 用于参数校验，校验失败时抛出KanbanException
 */
public final class AssertUtils {

    private AssertUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 断言对象不为null
     */
    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
    }

    /**
     * 断言对象不为null并返回
     */
    public static <T> T requireNotNull(T obj, String message) {
        if (obj == null) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
        return obj;
    }

    /**
     * 断言对象不为null
     */
    public static void notNull(Object obj, ErrorCode errorCode, Object... args) {
        if (obj == null) {
            throw new KanbanException(errorCode, args);
        }
    }

    /**
     * 断言对象不为null并返回
     */
    public static <T> T requireNotNull(T obj, ErrorCode errorCode, Object... args) {
        if (obj == null) {
            throw new KanbanException(errorCode, args);
        }
        return obj;
    }

    /**
     * 断言字符串不为空
     */
    public static void notBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
    }

    /**
     * 断言字符串不为空并返回
     */
    public static String requireNotBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
        return str;
    }

    /**
     * 断言字符串不为空
     */
    public static void notBlank(String str, ErrorCode errorCode, Object... args) {
        if (StringUtils.isBlank(str)) {
            throw new KanbanException(errorCode, args);
        }
    }

    /**
     * 断言字符串不为空并返回
     */
    public static String requireNotBlank(String str, ErrorCode errorCode, Object... args) {
        if (StringUtils.isBlank(str)) {
            throw new KanbanException(errorCode, args);
        }
        return str;
    }

    /**
     * 断言集合不为空
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
    }

    /**
     * 断言集合不为空并返回
     */
    public static <T> Collection<T> requireNotEmpty(Collection<T> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
        return collection;
    }

    /**
     * 断言集合不为空并返回
     */
    public static <T> List<T> requireNotEmpty(List<T> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
        return collection;
    }

    /**
     * 断言集合不为空
     */
    public static void notEmpty(Collection<?> collection, ErrorCode errorCode, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw new KanbanException(errorCode, args);
        }
    }

    /**
     * 断言集合不为空并返回
     */
    public static <T> Collection<T> requireNotEmpty(Collection<T> collection, ErrorCode errorCode, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw new KanbanException(errorCode, args);
        }
        return collection;
    }

    /**
     * 断言条件为真
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
    }

    /**
     * 断言条件为真
     */
    public static void isTrue(boolean condition, ErrorCode errorCode, Object... args) {
        if (!condition) {
            throw new KanbanException(errorCode, args);
        }
    }

    /**
     * 断言条件为假
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
    }

    /**
     * 断言条件为假
     */
    public static void isFalse(boolean condition, ErrorCode errorCode, Object... args) {
        if (condition) {
            throw new KanbanException(errorCode, args);
        }
    }

    /**
     * 断言两个对象相等
     */
    public static void equals(Object obj1, Object obj2, String message) {
        if (!Objects.equals(obj1, obj2)) {
            throw new KanbanException(CommonErrorCode.BAD_REQUEST, message);
        }
    }

    /**
     * 断言两个对象相等
     */
    public static void equals(Object obj1, Object obj2, ErrorCode errorCode, Object... args) {
        if (!Objects.equals(obj1, obj2)) {
            throw new KanbanException(errorCode, args);
        }
    }

    /**
     * 断言数据存在
     */
    public static <T> T requireFound(T obj, String entityName) {
        if (obj == null) {
            throw new KanbanException(CommonErrorCode.DATA_NOT_FOUND, entityName);
        }
        return obj;
    }

    /**
     * 断言数据存在
     */
    public static <T> T requireFound(T obj, ErrorCode errorCode, Object... args) {
        if (obj == null) {
            throw new KanbanException(errorCode, args);
        }
        return obj;
    }
}
