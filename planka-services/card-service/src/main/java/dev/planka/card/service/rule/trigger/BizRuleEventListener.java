package dev.planka.card.service.rule.trigger;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.flowrecord.ValueStreamHelper;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.history.source.BizRuleOperationSource;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StepId;
import dev.planka.event.card.*;
import dev.planka.infra.cache.schema.query.ValueStreamCacheQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 业务规则事件监听器
 * <p>
 * 监听卡片事件，触发相应的业务规则。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizRuleEventListener {

    private final BizRuleTriggerService triggerService;
    private final CardRepository cardRepository;
    private final ValueStreamCacheQuery valueStreamCacheQuery;
    private final ValueStreamHelper valueStreamHelper;

    /**
     * 监听卡片事件
     */
    @KafkaListener(topics = "kanban-card-events", groupId = "biz-rule-trigger",
            containerFactory = "cardEventListenerContainerFactory")
    public void handleCardEvent(CardEvent event) {
        log.debug("收到卡片事件: type={}, cardId={}", event.getEventType(), event.getCardId());

        // 检查是否由规则触发（防止循环）
        if (isTriggeredByRule(event)) {
            log.debug("跳过规则触发的事件: cardId={}, eventType={}", event.getCardId(), event.getEventType());
            return;
        }

        try {
            if (event instanceof CardCreatedEvent createdEvent) {
                handleCreatedEvent(createdEvent);
            } else if (event instanceof CardUpdatedEvent updatedEvent) {
                handleUpdatedEvent(updatedEvent);
            } else if (event instanceof CardMovedEvent movedEvent) {
                handleMovedEvent(movedEvent);
            } else if (event instanceof CardArchivedEvent archivedEvent) {
                handleArchivedEvent(archivedEvent);
            } else if (event instanceof CardAbandonedEvent abandonedEvent) {
                handleAbandonedEvent(abandonedEvent);
            } else if (event instanceof CardRestoredEvent restoredEvent) {
                handleRestoredEvent(restoredEvent);
            } else if (event instanceof CardLinkUpdatedEvent linkEvent) {
                handleLinkUpdatedEvent(linkEvent);
            } else {
                log.debug("未处理的事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("处理卡片事件失败: cardId={}, eventType={}, error={}",
                    event.getCardId(), event.getEventType(), e.getMessage(), e);
        }
    }

    /**
     * 处理卡片创建事件
     */
    private void handleCreatedEvent(CardCreatedEvent event) {
        log.debug("处理卡片创建事件: cardId={}", event.getCardId());
        Optional<CardDTO> cardOpt = loadCard(event.getCardId(), event.getOperatorId());
        if (cardOpt.isEmpty()) {
            return;
        }
        triggerService.triggerOnCreate(cardOpt.get(), event.getOperatorId());
    }

    /**
     * 处理卡片更新事件
     */
    private void handleUpdatedEvent(CardUpdatedEvent event) {
        List<String> changedFieldIds = extractChangedFields(event);
        log.debug("处理卡片更新事件: cardId={}, changedFields={}",
                event.getCardId(), changedFieldIds);

        Optional<CardDTO> cardOpt = loadCard(event.getCardId(), event.getOperatorId());
        if (cardOpt.isEmpty()) {
            return;
        }

        if (!changedFieldIds.isEmpty()) {
            triggerService.triggerOnFieldChange(cardOpt.get(), changedFieldIds, event.getOperatorId());
        }
    }

    /**
     * 处理卡片移动事件
     */
    private void handleMovedEvent(CardMovedEvent event) {
        log.debug("处理卡片移动事件: cardId={}, from={}, to={}, rollback={}",
                event.getCardId(), event.getFromStatusId(), event.getToStatusId(), event.isRollback());

        Optional<CardDTO> cardOpt = loadCard(event.getCardId(), event.getOperatorId());
        if (cardOpt.isEmpty()) {
            return;
        }

        CardDTO card = cardOpt.get();
        StatusId fromStatusId = event.getFromStatusId() != null
                ? StatusId.of(event.getFromStatusId()) : null;
        StatusId toStatusId = StatusId.of(event.getToStatusId());

        // 获取价值流定义
        ValueStreamDefinition valueStream = valueStreamCacheQuery
                .getValueStreamByCardTypeId(CardTypeId.of(event.getCardTypeId()))
                .orElseThrow(() -> new IllegalStateException(
                        "找不到卡片类型对应的价值流定义: cardTypeId=" + event.getCardTypeId()));

        // 通过价值流独立判断移动方向
        boolean isRollback = false;
        if (fromStatusId != null && event.getFromStepId() != null && event.getToStepId() != null) {
            StepId fromStepId = StepId.of(event.getFromStepId());
            StepId toStepId = StepId.of(event.getToStepId());
            isRollback = valueStreamHelper.isRollback(valueStream, fromStepId, fromStatusId, toStepId, toStatusId);
        }

        triggerService.triggerOnStatusMove(card, fromStatusId, toStatusId,
                event.getOperatorId(), isRollback, valueStream);
    }

    /**
     * 处理卡片归档事件
     */
    private void handleArchivedEvent(CardArchivedEvent event) {
        log.debug("处理卡片归档事件: cardId={}", event.getCardId());
        Optional<CardDTO> cardOpt = loadCard(event.getCardId(), event.getOperatorId());
        if (cardOpt.isEmpty()) {
            return;
        }
        triggerService.triggerOnArchive(cardOpt.get(), event.getOperatorId());
    }

    /**
     * 处理卡片丢弃事件
     */
    private void handleAbandonedEvent(CardAbandonedEvent event) {
        log.debug("处理卡片丢弃事件: cardId={}", event.getCardId());
        Optional<CardDTO> cardOpt = loadCard(event.getCardId(), event.getOperatorId());
        if (cardOpt.isEmpty()) {
            return;
        }
        triggerService.triggerOnDiscard(cardOpt.get(), event.getOperatorId());
    }

    /**
     * 处理卡片还原事件
     */
    private void handleRestoredEvent(CardRestoredEvent event) {
        log.debug("处理卡片还原事件: cardId={}", event.getCardId());
        Optional<CardDTO> cardOpt = loadCard(event.getCardId(), event.getOperatorId());
        if (cardOpt.isEmpty()) {
            return;
        }
        triggerService.triggerOnRestore(cardOpt.get(), event.getOperatorId());
    }

    /**
     * 处理卡片关联变更事件
     */
    private void handleLinkUpdatedEvent(CardLinkUpdatedEvent event) {
        if (!event.hasChanges()) {
            return;
        }
        log.debug("处理卡片关联变更事件: cardId={}, linkFieldId={}",
                event.getCardId(), event.getLinkFieldId());

        Optional<CardDTO> cardOpt = loadCard(event.getCardId(), event.getOperatorId());
        if (cardOpt.isEmpty()) {
            return;
        }

        triggerService.triggerOnFieldChange(cardOpt.get(),
                List.of(event.getLinkFieldId()), event.getOperatorId());
    }

    /**
     * 加载卡片信息
     */
    private Optional<CardDTO> loadCard(String cardId, String operatorId) {
        try {
            return cardRepository.findById(CardId.of(cardId), null, operatorId);
        } catch (Exception e) {
            log.error("加载卡片失败: cardId={}, error={}", cardId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 提取变更的字段列表
     */
    private List<String> extractChangedFields(CardUpdatedEvent event) {
        List<String> changedFields = new ArrayList<>();

        // 从 fieldChanges 提取变更的字段ID
        if (event.getFieldChanges() != null && !event.getFieldChanges().isEmpty()) {
            for (CardUpdatedEvent.FieldChange fieldChange : event.getFieldChanges()) {
                if (fieldChange.getFieldId() != null) {
                    changedFields.add(fieldChange.getFieldId());
                }
            }
        }

        // 检查标题变更
        if (event.getTitleChange() != null) {
            changedFields.add("title");
        }

        // 检查描述变更
        if (event.getDescriptionChange() != null) {
            changedFields.add("description");
        }

        return changedFields;
    }

    /**
     * 检查事件是否由规则触发
     */
    private boolean isTriggeredByRule(CardEvent event) {
        return event.getOperationSource() instanceof BizRuleOperationSource;
    }
}
