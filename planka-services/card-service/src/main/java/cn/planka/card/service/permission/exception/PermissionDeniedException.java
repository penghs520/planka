package cn.planka.card.service.permission.exception;

import cn.planka.domain.card.CardId;
import cn.planka.domain.field.FieldId;
import cn.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;

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
                : "无权限执行该操作";

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
                : "无权限编辑该属性";

        return new PermissionDeniedException(
                "PERMISSION_DENIED_FIELD_EDIT",
                message,
                new Object[]{cardId.value(), fieldIdsStr}
        );
    }

    /**
     * 创建卡片权限拒绝
     *
     * @param cardTypeName __PLANKA_EINST__名称
     * @param alertMessage 提示信息
     * @return 权限拒绝异常
     */
    public static PermissionDeniedException cardCreate(
            String cardTypeName,
            String alertMessage) {

        String message = alertMessage != null && !alertMessage.isBlank()
                ? alertMessage
                : String.format("无权限创建卡片，__PLANKA_EINST__: %s", cardTypeName);

        return new PermissionDeniedException(
                "PERMISSION_DENIED_CARD_CREATE",
                message,
                new Object[]{cardTypeName}
        );
    }

    /**
     * 对侧__PLANKA_EINST__关联属性编辑权限拒绝
     *
     * @param cardId       当前卡片ID
     * @param fieldId      关联属性ID
     * @param fieldName    属性名称
     * @param alertMessage 自定义提示信息（可为 null）
     * @return 权限拒绝异常
     */
    public static PermissionDeniedException oppositeSideFieldEdit(
            CardId cardId,
            FieldId fieldId,
            String fieldName,
            String alertMessage) {

        String message = "关联属性" + fieldName + "对侧编辑权限校验不通过";
        if (alertMessage != null && !alertMessage.isBlank()) {
            message = message + "：" + alertMessage;
        }

        return new PermissionDeniedException(
                "PERMISSION_DENIED_OPPOSITE_FIELD_EDIT",
                message,
                new Object[]{cardId.value(), fieldId.value(), fieldName}
        );
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
