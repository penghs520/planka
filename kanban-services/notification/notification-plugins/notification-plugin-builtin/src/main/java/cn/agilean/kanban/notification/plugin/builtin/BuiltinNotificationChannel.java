package cn.agilean.kanban.notification.plugin.builtin;

import cn.agilean.kanban.notification.plugin.*;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

import java.util.List;
import java.util.UUID;

/**
 * 系统内置通知渠道（站内信）
 * <p>
 * 将通知存储到数据库，用户可在系统内查看
 * 同时支持 PF4J 插件模式和 Spring SPI 模式
 */
@Slf4j
@Extension
public class BuiltinNotificationChannel implements NotificationChannelPlugin {

    public static final String CHANNEL_ID = "builtin";

    private final BuiltinNotificationSender sender;
    private final NotificationChannelDef def;

    public BuiltinNotificationChannel(BuiltinNotificationSender sender) {
        this.sender = sender;
        this.def = NotificationChannelDef.builder()
                .id(CHANNEL_ID)
                .name("系统通知")
                .description("系统内置站内信通知，用户可在通知中心查看")
                .version(version())
                .provider("Agilean")
                .supportsRichContent(true)
                .supportsAttachment(false)
                .configFields(List.of())  // 无需额外配置
                .build();
    }

    @Override
    public String channelId() {
        return CHANNEL_ID;
    }

    @Override
    public String name() {
        return "系统通知";
    }

    @Override
    public NotificationChannelDef def() {
        return def;
    }

    @Override
    public NotificationResult send(NotificationRequest request) {
        try {
            String messageId = "builtin_" + UUID.randomUUID().toString().replace("-", "");

            // 为每个接收者创建站内信记录
            List<NotificationResult.RecipientResult> recipientResults = request.getRecipientUserIds().stream()
                    .map(userId -> {
                        try {
                            sender.send(
                                    request.getOrgId(),
                                    userId,
                                    request.getTitle(),
                                    request.getContent(),
                                    request.getRichContent(),
                                    request.getSource()
                            );
                            return NotificationResult.RecipientResult.builder()
                                    .userId(userId)
                                    .success(true)
                                    .build();
                        } catch (Exception e) {
                            log.error("站内信发送失败: userId={}, error={}", userId, e.getMessage());
                            return NotificationResult.RecipientResult.builder()
                                    .userId(userId)
                                    .success(false)
                                    .errorMessage(e.getMessage())
                                    .build();
                        }
                    })
                    .toList();

            boolean allSuccess = recipientResults.stream().allMatch(NotificationResult.RecipientResult::isSuccess);
            if (allSuccess) {
                log.info("站内信发送成功: messageId={}, recipients={}", messageId, request.getRecipientUserIds().size());
                return NotificationResult.success(CHANNEL_ID, messageId, recipientResults);
            } else {
                return NotificationResult.partial(CHANNEL_ID, messageId, recipientResults);
            }
        } catch (Exception e) {
            log.error("站内信发送失败: error={}", e.getMessage(), e);
            return NotificationResult.failed(CHANNEL_ID, "SEND_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean supportsRichContent() {
        return true;
    }
}
