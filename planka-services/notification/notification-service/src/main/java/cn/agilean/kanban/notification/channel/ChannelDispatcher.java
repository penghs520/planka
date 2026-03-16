package cn.planka.notification.channel;

import cn.planka.domain.notification.LongNotificationContent;
import cn.planka.domain.notification.NotificationTemplateDefinition;
import cn.planka.notification.config.NotificationChannelRegistry;
import cn.planka.notification.model.AppliedTemplate;
import cn.planka.notification.model.NotificationSendContext;
import cn.planka.notification.plugin.NotificationChannel;
import cn.planka.notification.plugin.NotificationRequest;
import cn.planka.notification.plugin.NotificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 渠道分发器
 * 根据模板配置的渠道列表分发通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelDispatcher {

    private final NotificationChannelRegistry channelRegistry;

    /**
     * 根据模板配置的渠道列表分发通知
     */
    public List<NotificationResult> dispatch(NotificationTemplateDefinition template, NotificationSendContext sendContext) {
        List<NotificationResult> results = new ArrayList<>();

        for (String channelId : template.getChannels()) {
            try {
                Optional<NotificationChannel> channelOpt = channelRegistry.getChannel(channelId);
                if (channelOpt.isEmpty()) {
                    log.warn("Channel not found: {}", channelId);
                    continue;
                }

                NotificationChannel channel = channelOpt.get();
                NotificationRequest request = buildRequest(sendContext, channelId);
                NotificationResult result = channel.send(request);
                results.add(result);

                log.info("Sent notification to channel: {}, result: {}", channelId, result.isSuccess());
            } catch (Exception e) {
                log.error("Failed to send notification to channel: {}", channelId, e);
                results.add(NotificationResult.failed(channelId, "SEND_ERROR", e.getMessage()));
            }
        }

        return results;
    }

    /**
     * 为特定渠道构建通知请求
     */
    private NotificationRequest buildRequest(NotificationSendContext sendContext, String channelId) {
        AppliedTemplate applied = sendContext.getAppliedTemplate();

        NotificationRequest.NotificationRequestBuilder builder = NotificationRequest.builder()
                .orgId(sendContext.getOriginalContext().getOrgId())
                .title(applied.getTitle())
                .recipientUserIds(extractUserIds(sendContext.getRecipients()))
                .recipients(sendContext.getRecipients())
                .source(NotificationRequest.NotificationSource.builder()
                        .ruleId(sendContext.getRuleId())
                        .cardId(sendContext.getCardId())
                        .operatorId(sendContext.getOperatorId())
                        .build());

        // 根据渠道类型设置内容
        if ("email".equals(channelId)) {
            builder.richContent(applied.getRichContent());

            // 邮件渠道支持抄送
            if (applied.getTemplate().getContent() instanceof LongNotificationContent) {
                LongNotificationContent longContent = (LongNotificationContent) applied.getTemplate().getContent();
                if (longContent.getCcSelector() != null) {
                    // TODO: 解析 ccSelector 获取抄送人列表
                    // builder.extras(Map.of("cc", ccRecipients));
                }
            }
        } else {
            builder.content(applied.getContent());
        }

        return builder.build();
    }

    /**
     * 提取用户 ID 列表
     */
    private List<String> extractUserIds(List<NotificationRequest.RecipientInfo> recipients) {
        return recipients.stream()
                .map(NotificationRequest.RecipientInfo::getUserId)
                .collect(Collectors.toList());
    }
}
