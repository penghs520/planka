package dev.planka.card.service.permission.exception;

import dev.planka.domain.card.CardId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限拒绝异常
 * <p>
 * 当用户没有权限执行某个操作时抛出此异常
 */
public class PermissionDeniedException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    private PermissionDeniedException(String errorCode, String message, Object[] args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 卡片操作权限拒绝
     *
     * @param operation    操作类型
     * @param cardId       卡片ID
     * @param alertMessage 提示信息
     * @return 权限拒绝异常
     */
    public static PermissionDeniedException cardOperation(
            CardOperation operation,
            CardId cardId,
            String alertMessage) {

        String message = alertMessage != null && !alertMessage.isBlank()
                ? alertMessage
                : String.format("无权限执行操作 %s，卡片ID: %s", operation, cardId.value());

        return new PermissionDeniedException(
                "PERMISSION_DENIED_CARD_OPERATION",
                message,
                new Object[]{operation, cardId.value()}
        );
    }

    /**
     * 属性编辑权限拒绝
     *
     * @param cardId       卡片ID
     * @param fieldIds     属性ID集合
     * @param alertMessage 提示信息
     * @return 权限拒绝异常
     */
    public static PermissionDeniedException fieldEdit(
            CardId cardId,
            Set<FieldId> fieldIds,
            String alertMessage) {

        String fieldIdsStr = fieldIds.stream()
                .map(FieldId::value)
                .collect(Collectors.joining(", "));

        String message = alertMessage != null && !alertMessage.isBlank()
                ? alertMessage
                : String.format("无权限编辑属性，卡片ID: %s, 属性ID: [%s]", cardId.value(), fieldIdsStr);

        return new PermissionDeniedException(
                "PERMISSION_DENIED_FIELD_EDIT",
                message,
                new Object[]{cardId.value(), fieldIdsStr}
        );
    }

    /**
     * 创建卡片权限拒绝
     *
     * @param cardTypeName 卡片类型名称
     * @param alertMessage 提示信息
     * @return 权限拒绝异常
     */
    public static PermissionDeniedException cardCreate(
            String cardTypeName,
            String alertMessage) {

        String message = alertMessage != null && !alertMessage.isBlank()
                ? alertMessage
                : String.format("无权限创建卡片，卡片类型: %s", cardTypeName);

        return new PermissionDeniedException(
                "PERMISSION_DENIED_CARD_CREATE",
                message,
                new Object[]{cardTypeName}
        );
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
