package dev.planka.common.exception;

/**
 * 错误码接口
 * <p>
 * 所有错误码枚举需要实现此接口
 */
public interface ErrorCode {

    /**
     * 获取错误码
     *
     * @return 错误码字符串
     */
    String getCode();

    /**
     * 获取错误消息
     *
     * @return 错误消息
     */
    String getMessage();

    /**
     * 获取HTTP状态码
     *
     * @return HTTP状态码
     */
    int getHttpStatus();
}
