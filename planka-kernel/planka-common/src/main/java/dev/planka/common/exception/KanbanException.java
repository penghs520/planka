package dev.planka.common.exception;

/**
 * 看板系统基础异常
 * <p>
 * 所有业务异常的基类，支持错误码和国际化消息
 */
public class KanbanException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public KanbanException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public KanbanException(ErrorCode errorCode, Object... args) {
        super(formatMessage(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
    }

    public KanbanException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public KanbanException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(formatMessage(errorCode.getMessage(), args), cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCode() {
        return errorCode.getCode();
    }

    public Object[] getArgs() {
        return args;
    }

    private static String formatMessage(String message, Object[] args) {
        if (args == null || args.length == 0) {
            return message.replace("：{}", "");
        }
        return String.format(message.replace("{}", "%s"), args);
    }
}
