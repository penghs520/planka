package cn.planka.card.event;

import cn.planka.card.service.core.CardService;
import cn.planka.domain.card.CardId;
import cn.planka.event.card.CardMigrationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 卡片迁移事件监听器
 * <p>
 * 监听卡片迁移请求事件，执行实际的卡片状态迁移操作。
 * 由 schema-service 发送迁移请求，card-service 监听并执行。
 */
@Component
public class CardMigrationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CardMigrationEventListener.class);

    private final CardService cardService;

    public CardMigrationEventListener(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * 处理卡片迁移请求事件
     */
    @KafkaListener(topics = "planka-card-events", groupId = "card-migration",
            containerFactory = "cardEventListenerContainerFactory")
    public void handleCardMigrationRequestedEvent(CardMigrationRequestedEvent event) {
        logger.info("Received CardMigrationRequestedEvent: orgId={}, streamId={}, stepDeletion={}",
                event.getOrgId(), event.getStreamId(), event.isStepDeletion());

        if (event.getStatusMigrationMap() == null || event.getStatusMigrationMap().isEmpty()) {
            logger.warn("Empty migration map, skipping");
            return;
        }

        // 逐个处理状态迁移
        for (Map.Entry<String, String> entry : event.getStatusMigrationMap().entrySet()) {
            String sourceStatusId = entry.getKey();
            String targetStatusId = entry.getValue();

            try {
                var result = cardService.batchUpdateCardStatus(
                        event.getOrgId(),
                        sourceStatusId,
                        targetStatusId,
                        event.getStreamId(),
                        event.getCardTypeId(),
                        CardId.of(event.getOperatorId())
                );

                if (result.isSuccess()) {
                    logger.info("Card migration completed: sourceStatusId={}, targetStatusId={}, count={}",
                            sourceStatusId, targetStatusId, result.getData());
                } else {
                    logger.error("Card migration failed: sourceStatusId={}, error={}",
                            sourceStatusId, result.getMessage());
                }
            } catch (Exception e) {
                logger.error("Exception during card migration: sourceStatusId={}", sourceStatusId, e);
            }
        }
    }
}
