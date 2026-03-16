package cn.planka.notification.plugin;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 通知请求
 * <p>
 * 封装发送通知所需的所有信息
 */
@Getter
@Builder
public class NotificationRequest {

    /**
     * 组织ID
     */
    private final String orgId;

    /**
     * 通知标题
     */
    private final String title;

    /**
     * 通知内容（纯文本）
     */
    private final String content;

    /**
     * 富文本内容（HTML/Markdown）
     * <p>
     * 用于支持富文本的渠道（如邮件）
     */
    private final String richContent;

    /**
     * 接收者用户ID列表
     */
    private final List<String> recipientUserIds;

    /**
     * 接收者详细信息
     * <p>
     * 包含邮箱、手机号、外部平台用户ID等
     */
    private final List<RecipientInfo> recipients;

    /**
     * 渠道配置
     * <p>
     * 从 NotificationChannelConfigDefinition 获取的配置参数
     */
    private final Map<String, Object> channelConfig;

    /**
     * 附件列表
     */
    private final List<Attachment> attachments;

    /**
     * 扩展数据
     */
    private final Map<String, Object> extras;

    /**
     * 来源信息（用于追踪）
     */
    private final NotificationSource source;

    /**
     * 接收者信息
     */
    @Getter
    @Builder
    public static class RecipientInfo {
        /**
         * 系统用户ID
         */
        private final String userId;

        /**
         * 用户姓名
         */
        private final String name;

        /**
         * 邮箱地址
         */
        private final String email;

        /**
         * 手机号
         */
        private final String mobile;

        /**
         * 飞书用户ID
         */
        private final String feishuUserId;

        /**
         * 钉钉用户ID
         */
        private final String dingtalkUserId;

        /**
         * 企业微信用户ID
         */
        private final String wecomUserId;
    }

    /**
     * 附件信息
     */
    @Getter
    @Builder
    public static class Attachment {
        /**
         * 附件名称
         */
        private final String name;

        /**
         * 附件URL
         */
        private final String url;

        /**
         * MIME类型
         */
        private final String mimeType;

        /**
         * 文件大小（字节）
         */
        private final long size;
    }

    /**
     * 通知来源信息
     */
    @Getter
    @Builder
    public static class NotificationSource {
        /**
         * 规则ID
         */
        private final String ruleId;

        /**
         * 规则名称
         */
        private final String ruleName;

        /**
         * 触发卡片ID
         */
        private final String cardId;

        /**
         * 触发卡片标题
         */
        private final String cardTitle;

        /**
         * 操作人ID
         */
        private final String operatorId;

        /**
         * 操作人姓名
         */
        private final String operatorName;
    }
}
