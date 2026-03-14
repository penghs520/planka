package cn.agilean.kanban.notification.context;

import cn.agilean.kanban.domain.schema.definition.rule.BizRuleDefinition.TriggerEvent;
import cn.agilean.kanban.event.notification.CardChangedNotificationEvent;
import cn.agilean.kanban.event.notification.NotificationEvent;
import cn.agilean.kanban.notification.model.CardSnapshot;
import cn.agilean.kanban.notification.model.NotificationContext;
import cn.agilean.kanban.notification.model.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知上下文工厂
 * 从事件创建通知上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationContextFactory {

    // TODO: 注入 CardRepository 和 UserRepository (Feign Client)

    /**
     * 从事件创建通知上下文
     */
    public NotificationContext createContext(NotificationEvent event) {
        NotificationContext.NotificationContextBuilder builder = NotificationContext.builder()
                .orgId(event.getOrgId())
                .triggerEvent(TriggerEvent.ON_STATUS_MOVE) // TODO: 从模板中获取实际的触发事件
                .operatorId(event.getOperatorId())
                .occurredAt(event.getOccurredAt());

        // 根据事件类型提取数据
        if (event instanceof CardChangedNotificationEvent) {
            CardChangedNotificationEvent cardEvent = (CardChangedNotificationEvent) event;
            builder.sourceCardId(cardEvent.getCurrentCardId().value());
            builder.eventData(extractCardEventData(cardEvent));

            // 加载卡片快照
            CardSnapshot cardSnapshot = loadCardSnapshot(cardEvent.getCurrentCardId().value());
            builder.cardSnapshot(cardSnapshot);
        }

        // 加载操作人信息
        UserInfo operator = loadUserInfo(event.getOperatorId());
        builder.operator(operator);

        return builder.build();
    }

    /**
     * 提取卡片事件数据
     */
    private Map<String, Object> extractCardEventData(CardChangedNotificationEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("currentCardId", event.getCurrentCardId().value());
        data.put("notificationTemplateId", event.getNotificationTemplateId().value());
        return data;
    }

    /**
     * 加载卡片快照
     * TODO: 通过 Feign Client 调用 card-service 获取卡片数据
     */
    private CardSnapshot loadCardSnapshot(String cardId) {
        if (cardId == null) {
            return null;
        }

        try {
            // TODO: 调用 card-service API 获取卡片完整数据
            // CardDTO card = cardClient.getCard(cardId);

            // 临时返回空快照
            return CardSnapshot.builder()
                    .cardId(cardId)
                    .cardTypeId("unknown")
                    .cardTypeName("未知")
                    .fieldValues(Collections.emptyMap())
                    .snapshotAt(Instant.now())
                    .build();
        } catch (Exception e) {
            log.error("Failed to load card snapshot for cardId: {}", cardId, e);
            return null;
        }
    }

    /**
     * 加载用户信息
     * TODO: 通过 Feign Client 调用 user-service 获取用户数据
     */
    private UserInfo loadUserInfo(String userId) {
        if (userId == null) {
            return null;
        }

        try {
            // TODO: 调用 user-service API 获取用户数据
            // UserDTO user = userClient.getUser(userId);

            // 临时返回空用户信息
            return UserInfo.builder()
                    .userId(userId)
                    .username("unknown")
                    .displayName("未知用户")
                    .attributes(Collections.emptyMap())
                    .build();
        } catch (Exception e) {
            log.error("Failed to load user info for userId: {}", userId, e);
            return null;
        }
    }
}
