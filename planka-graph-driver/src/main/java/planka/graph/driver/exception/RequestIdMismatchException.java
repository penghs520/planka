package planka.graph.driver.exception;

/**
 * 当响应中的请求ID与原始请求的请求ID不匹配时抛出此异常
 */
public class RequestIdMismatchException extends RuntimeException {
    private final String requestId;
    private final String responseRequestId;

    public RequestIdMismatchException(String message, String requestId, String responseRequestId) {
        super(message);
        this.requestId = requestId;
        this.responseRequestId = responseRequestId;
    }

    public RequestIdMismatchException(String message, String requestId, String responseRequestId, Throwable cause) {
        super(message, cause);
        this.requestId = requestId;
        this.responseRequestId = responseRequestId;
    }

    /**
     * 获取原始请求的请求ID
     * @return 请求ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 获取响应中的请求ID
     * @return 响应中的请求ID
     */
    public String getResponseRequestId() {
        return responseRequestId;
    }
} 