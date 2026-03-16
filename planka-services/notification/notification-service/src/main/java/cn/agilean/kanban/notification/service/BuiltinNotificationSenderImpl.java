package cn.planka.notification.service;

import cn.planka.notification.model.SystemNotificationEntity;
import cn.planka.notification.plugin.NotificationRequest;
import cn.planka.notification.plugin.builtin.BuiltinNotificationSender;
import cn.planka.notification.repository.SystemNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 站内信发送器实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuiltinNotificationSenderImpl implements BuiltinNotificationSender {

    private final SystemNotificationRepository repository;

    @Override
    public void send(String orgId, String userId, String title, String content, String richContent,
                     NotificationRequest.NotificationSource source) {
        SystemNotificationEntity entity = new SystemNotificationEntity();
        entity.setOrgId(orgId);
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setRichContent(richContent);

        if (source != null) {
            entity.setSourceType("RULE");
            entity.setSourceId(source.getRuleId());
            entity.setCardId(source.getCardId());
        }

        repository.save(entity);
        log.debug("站内信已保存: userId={}, title={}", userId, title);
    }
}
