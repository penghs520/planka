package dev.planka.common.exception;

/**
 * 通用错误码枚举
 */
public enum CommonErrorCode implements ErrorCode {

    // ==================== 成功 ====================
    SUCCESS("0", "操作成功", 200),

    // ==================== 客户端错误 4xx ====================
    BAD_REQUEST("400", "请求参数错误：{}", 400),
    UNAUTHORIZED("401", "未授权访问：{}", 401),
    FORBIDDEN("403", "禁止访问：{}", 403),
    NOT_FOUND("404", "资源不存在：{}", 404),
    METHOD_NOT_ALLOWED("405", "方法不允许：{}", 405),
    CONFLICT("409", "资源冲突：{}", 409),
    VALIDATION_ERROR("422", "数据验证失败：{}", 422),

    // ==================== 服务端错误 5xx ====================
    INTERNAL_ERROR("500", "系统内部错误：{}", 500),
    SERVICE_UNAVAILABLE("503", "服务暂时不可用：{}", 503),

    // ==================== 业务错误 1xxxx ====================
    BUSINESS_ERROR("10000", "业务处理失败：{}", 400),
    DATA_NOT_FOUND("10001", "数据不存在: {}", 404),
    DATA_ALREADY_EXISTS("10002", "数据已存在: {}", 409),
    OPERATION_NOT_ALLOWED("10003", "操作不允许: {}", 403),
    INVALID_STATE("10004", "状态无效: {}", 400),

    // ==================== 权限错误 2xxxx ====================
    PERMISSION_DENIED("20001", "权限不足：{}", 403),
    FIELD_NOT_READABLE("20002", "字段不可读: {}", 403),
    FIELD_NOT_WRITABLE("20003", "字段不可写: {}", 403),

    // ==================== 配置错误 3xxxx ====================
    CONFIG_ERROR("30001", "配置错误: {}", 500),
    SCHEMA_NOT_FOUND("30002", "Schema定义不存在: {}", 404);

    private final String code;
    private final String message;
    private final int httpStatus;

    CommonErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}
