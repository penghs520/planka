package cn.agilean.kanban.notification.service;

import cn.agilean.kanban.notification.config.NotificationChannelRegistry;
import cn.agilean.kanban.notification.plugin.NotificationChannel;
import cn.agilean.kanban.notification.plugin.NotificationRequest;
import cn.agilean.kanban.notification.plugin.NotificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationChannelRegistry channelRegistry;

    @Override
    public NotificationResult send(String channelId, NotificationRequest request) {
        return channelRegistry.getChannel(channelId)
                .map(channel -> {
                    try {
                        log.info("发送通知: channelId={}, title={}, recipients={}",
                                channelId, request.getTitle(), request.getRecipientUserIds().size());
                        return channel.send(request);
                    } catch (Exception e) {
                        log.error("发送通知失败: channelId={}, error={}", channelId, e.getMessage(), e);
                        return NotificationResult.failed(channelId, "SEND_ERROR", e.getMessage());
                    }
                })
                .orElseGet(() -> {
                    log.warn("通知渠道不存在: channelId={}", channelId);
                    return NotificationResult.failed(channelId, "CHANNEL_NOT_FOUND", "通知渠道不存在: " + channelId);
                });
    }

    @Override
    public List<NotificationResult> sendToChannels(List<String> channelIds, NotificationRequest request) {
        List<NotificationResult> results = new ArrayList<>();
        for (String channelId : channelIds) {
            results.add(send(channelId, request));
        }
        return results;
    }

    @Override
    public List<String> getAvailableChannels(String orgId) {
        // TODO: 根据组织配置过滤可用渠道
        return channelRegistry.getAllChannels().stream()
                .map(NotificationChannel::channelId)
                .toList();
    }

    @Override
    public boolean isChannelAvailable(String orgId, String channelId) {
        // TODO: 检查组织是否配置了该渠道
        return channelRegistry.hasChannel(channelId);
    }
}
