package dev.planka.domain.schema.definition.rule.action;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 规则动作定义
 * <p>
 * 使用 sealed interface 确保类型安全，支持多种动作类型。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "actionType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DiscardCardAction.class, name = "DISCARD_CARD"),
        @JsonSubTypes.Type(value = ArchiveCardAction.class, name = "ARCHIVE_CARD"),
        @JsonSubTypes.Type(value = RestoreCardAction.class, name = "RESTORE_CARD"),
        @JsonSubTypes.Type(value = MoveCardAction.class, name = "MOVE_CARD"),
        @JsonSubTypes.Type(value = UpdateCardAction.class, name = "UPDATE_CARD"),
        @JsonSubTypes.Type(value = CreateCardAction.class, name = "CREATE_CARD"),
        @JsonSubTypes.Type(value = CreateLinkedCardAction.class, name = "CREATE_LINKED_CARD"),
        @JsonSubTypes.Type(value = CommentCardAction.class, name = "COMMENT_CARD"),
        @JsonSubTypes.Type(value = SendNotificationAction.class, name = "SEND_NOTIFICATION"),
        @JsonSubTypes.Type(value = TrackUserBehaviorAction.class, name = "TRACK_USER_BEHAVIOR"),
        @JsonSubTypes.Type(value = CallExternalApiAction.class, name = "CALL_EXTERNAL_API")
})
public sealed interface RuleAction
        permits DiscardCardAction, ArchiveCardAction, RestoreCardAction,
        MoveCardAction, UpdateCardAction, CreateCardAction,
        CreateLinkedCardAction, CommentCardAction, SendNotificationAction,
        TrackUserBehaviorAction, CallExternalApiAction {

    /**
     * 获取动作类型标识
     */
    String getActionType();

    /**
     * 获取动作执行顺序（默认0）
     */
    default int getSortOrder() {
        return 0;
    }
}
