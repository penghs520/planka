package dev.planka.common.result;

import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.exception.ErrorCode;

import java.io.Serializable;

/**
 * 统一API响应结果
 *
 * @param <T> 数据类型
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 响应码 */
    private String code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 是否成功 */
    private boolean success;

    /** 时间戳 */
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    private Result(String code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 成功响应 ====================

    public static <T> Result<T> success() {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(),
                CommonErrorCode.SUCCESS.getMessage(), null, true);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(),
                CommonErrorCode.SUCCESS.getMessage(), data, true);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), message, data, true);
    }

    // ==================== 失败响应 ====================

    public static <T> Result<T> failure(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null, false);
    }

    public static <T> Result<T> failure(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null, false);
    }

    public static <T> Result<T> failure(String code, String message) {
        return new Result<>(code, message, null, false);
    }

    // ==================== Getter/Setter ====================

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
