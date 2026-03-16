package cn.planka.notification.plugin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知发送结果
 */
@Getter
@Builder
public class NotificationResult {

    /**
     * 是否发送成功
     */
    private final boolean success;

    /**
     * 渠道ID
     */
    private final String channelId;

    /**
     * 渠道返回的消息ID
     */
    private final String messageId;

    /**
     * 发送时间
     */
    private final LocalDateTime sentAt;

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 错误信息
     */
    private final String errorMessage;

    /**
     * 各接收者的发送结果
     */
    private final List<RecipientResult> recipientResults;

    /**
     * 接收者发送结果
     */
    @Getter
    @Builder
    public static class RecipientResult {
        /**
         * 用户ID
         */
        private final String userId;

        /**
         * 是否发送成功
         */
        private final boolean success;

        /**
         * 错误信息
         */
        private final String errorMessage;
    }

    /**
     * 创建成功结果
     */
    public static NotificationResult success(String channelId, String messageId) {
        return NotificationResult.builder()
                .success(true)
                .channelId(channelId)
                .messageId(messageId)
                .sentAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建成功结果（带接收者结果）
     */
    public static NotificationResult success(String channelId, String messageId, List<RecipientResult> recipientResults) {
        return NotificationResult.builder()
                .success(true)
                .channelId(channelId)
                .messageId(messageId)
                .sentAt(LocalDateTime.now())
                .recipientResults(recipientResults)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static NotificationResult failed(String channelId, String errorCode, String errorMessage) {
        return NotificationResult.builder()
                .success(false)
                .channelId(channelId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .sentAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建部分成功结果
     */
    public static NotificationResult partial(String channelId, String messageId, List<RecipientResult> recipientResults) {
        boolean allSuccess = recipientResults.stream().allMatch(RecipientResult::isSuccess);
        boolean allFailed = recipientResults.stream().noneMatch(RecipientResult::isSuccess);

        return NotificationResult.builder()
                .success(!allFailed)
                .channelId(channelId)
                .messageId(messageId)
                .sentAt(LocalDateTime.now())
                .recipientResults(recipientResults)
                .errorMessage(allSuccess ? null : "部分接收者发送失败")
                .build();
    }
}
