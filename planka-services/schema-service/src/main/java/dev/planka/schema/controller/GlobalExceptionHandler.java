package dev.planka.schema.controller;

import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.exception.KanbanException;
import dev.planka.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(KanbanException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleKanbanException(KanbanException e) {
        log.warn("Business exception: {}", e.getMessage());
        return Result.failure(e.getErrorCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation exception: {}", message);
        return Result.failure(CommonErrorCode.VALIDATION_ERROR, message);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return Result.failure(CommonErrorCode.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        log.warn("Illegal state: {}", e.getMessage());
        return Result.failure(CommonErrorCode.INVALID_STATE, e.getMessage());
    }

    /**
     * 处理请求体反序列化异常（如JSON格式错误、必填字段缺失等）
     * <p>
     * 当构造函数中抛出 KanbanException 时，会被 Jackson 包装为 HttpMessageNotReadableException，
     * 此处从异常链中提取原始 KanbanException 并按业务异常处理。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        KanbanException kanbanException = extractKanbanException(e);
        if (kanbanException != null) {
            log.warn("Business exception during deserialization: {}", kanbanException.getMessage());
            return Result.failure(kanbanException.getErrorCode(), kanbanException.getMessage());
        }
        Throwable cause = e.getMostSpecificCause();
        String message = cause != null ? cause.getMessage() : e.getMessage();
        log.warn("Message not readable: {}", message);
        return Result.failure(CommonErrorCode.BAD_REQUEST, message);
    }

    private KanbanException extractKanbanException(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof KanbanException ke) {
                return ke;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return Result.failure(CommonErrorCode.INTERNAL_ERROR, "服务器内部错误，请稍后重试");
    }
}
