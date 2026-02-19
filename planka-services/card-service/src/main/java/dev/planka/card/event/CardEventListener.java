package dev.planka.card.event;

import dev.planka.card.service.flowrecord.CardStatusChangeContext;
import dev.planka.card.service.flowrecord.FlowRecordService;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import dev.planka.event.card.CardCreatedEvent;
import dev.planka.event.card.CardEvent;
import dev.planka.event.card.CardMovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 卡片事件监听器
 * <p>
 * 监听卡片相关事件，记录流动信息
 */
@Component
public class CardEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CardEventListener.class);

    private final FlowRecordService flowRecordService;

    public CardEventListener(FlowRecordService flowRecordService) {
        this.flowRecordService = flowRecordService;
    }

    /**
     * 统一监听卡片事件，根据事件类型分发处理
     */
    @KafkaListener(topics = "planka-card-events", groupId = "card-flow-record",
            containerFactory = "cardEventListenerContainerFactory")
    public void handleCardEvent(CardEvent event) {
        logger.debug("Received CardEvent: type={}, cardId={}", event.getEventType(), event.getCardId());

        if (event instanceof CardCreatedEvent createdEvent) {
            handleCardCreatedEvent(createdEvent);
        } else if (event instanceof CardMovedEvent movedEvent) {
            handleCardMovedEvent(movedEvent);
        } else {
            logger.debug("Unhandled card event type: {}", event.getEventType());
        }
    }

    /**
     * 处理卡片创建事件
     * <p>
     * 卡片创建时，如果有价值流配置，记录进入初始状态的流动记录
     */
    private void handleCardCreatedEvent(CardCreatedEvent event) {
        logger.debug("Processing CardCreatedEvent: cardId={}, statusId={}",
                event.getCardId(), event.getStatusId());

        // 如果没有配置价值流，不记录流动信息
        if (event.getStreamId() == null || event.getStatusId() == null) {
            logger.debug("Skip flow record for card without value stream: cardId={}", event.getCardId());
            return;
        }

        try {
            // 构建状态变更上下文（新建卡片，fromStatusId 为 null）
            CardStatusChangeContext context = CardStatusChangeContext.builder()
                    .cardId(CardId.of(event.getCardId()))
                    .cardTypeId(CardTypeId.of(event.getCardTypeId()))
                    .streamId(StreamId.of(event.getStreamId()))
                    .toStatusId(StatusId.of(event.getStatusId()))
                    .rollback(false)
                    .eventTime(LocalDateTime.now())
                    .operatorId(event.getOperatorId())
                    .build();

            flowRecordService.recordStatusChange(context);
            logger.info("Recorded flow record for card creation: cardId={}", event.getCardId());
        } catch (Exception e) {
            logger.error("Failed to record flow record for card creation: cardId={}",
                    event.getCardId(), e);
        }
    }

    /**
     * 处理卡片移动事件
     * <p>
     * 跨多个状态移动时，会生成中间状态的流动记录
     */
    private void handleCardMovedEvent(CardMovedEvent event) {
        logger.debug("Processing CardMovedEvent: cardId={}, from={}, to={}, rollback={}",
                event.getCardId(), event.getFromStatusId(), event.getToStatusId(), event.isRollback());

        try {
            // 构建状态变更上下文
            CardStatusChangeContext context = CardStatusChangeContext.builder()
                    .cardId(CardId.of(event.getCardId()))
                    .cardTypeId(CardTypeId.of(event.getCardTypeId()))
                    .streamId(StreamId.of(event.getStreamId()))
                    .fromStatusId(StatusId.of(event.getFromStatusId()))
                    .toStatusId(StatusId.of(event.getToStatusId()))
                    .rollback(event.isRollback())
                    .eventTime(LocalDateTime.now())
                    .operatorId(event.getOperatorId())
                    .build();

            flowRecordService.recordStatusChange(context);
            logger.info("Recorded flow record for card move: cardId={}, rollback={}",
                    event.getCardId(), event.isRollback());
        } catch (Exception e) {
            logger.error("Failed to record flow record for card move: cardId={}",
                    event.getCardId(), e);
        }
    }
}
