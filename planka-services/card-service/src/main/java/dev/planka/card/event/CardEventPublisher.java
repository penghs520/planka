package dev.planka.card.event;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.MoveCardRequest;
import dev.planka.api.card.request.UpdateCardRequest;
import dev.planka.card.model.CardEntity;
import dev.planka.card.service.flowrecord.ValueStreamHelper;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.history.OperationSourceContext;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StatusWorkType;
import dev.planka.domain.stream.StepId;
import dev.planka.event.card.CardAbandonedEvent;
import dev.planka.event.card.CardArchivedEvent;
import dev.planka.event.card.CardCreatedEvent;
import dev.planka.event.card.CardEvent;
import dev.planka.event.card.CardMovedEvent;
import dev.planka.event.card.CardRestoredEvent;
import dev.planka.event.card.CardUpdatedEvent;
import dev.planka.event.publisher.EventPublisher;
import dev.planka.infra.cache.schema.query.ValueStreamCacheQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 卡片事件发布服务
 * <p>
 * 负责发布卡片相关的领域事件，包括创建、更新、移动等事件。
 */
@Component
public class CardEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(CardEventPublisher.class);

    private final EventPublisher eventPublisher;
    private final ValueStreamHelper valueStreamHelper;
    private final ValueStreamCacheQuery valueStreamCacheQuery;
    public CardEventPublisher(EventPublisher eventPublisher,
                              ValueStreamHelper valueStreamHelper, ValueStreamCacheQuery valueStreamCacheQuery) {
        this.eventPublisher = eventPublisher;
        this.valueStreamHelper = valueStreamHelper;
        this.valueStreamCacheQuery = valueStreamCacheQuery;
    }

    /**
     * 发布卡片创建事件
     */
    public void publishCreated(CardEntity cardEntity, String operatorId) {
        CardCreatedEvent event = new CardCreatedEvent(
                cardEntity.getOrgId().value(),
                operatorId,
                null,
                null,
                cardEntity.getTypeId().value(),
                String.valueOf(cardEntity.getId().value()));

        event.setCodeInOrg(cardEntity.getCodeInOrg());
        event.setCustomCode(cardEntity.getCustomCode());
        event.setTitle(cardEntity.getTitle());
        event.setDescription(cardEntity.getDescription());
        event.setCardStyle(cardEntity.getCardStyle());
        event.setStreamId(cardEntity.getStreamId() != null ? cardEntity.getStreamId().value() : null);
        event.setStatusId(cardEntity.getStatusId() != null ? cardEntity.getStatusId().value() : null);
        event.setFieldValues(cardEntity.getFieldValues());
        event.setCreatedAt(cardEntity.getCreatedAt());

        eventPublisher.publishAsync(applyOperationSource(event));
    }

    /**
     * 发布卡片更新事件
     */
    public void publishUpdated(CardDTO existingCard, UpdateCardRequest request, String operatorId) {
        CardUpdatedEvent event = new CardUpdatedEvent(
                existingCard.getOrgId().value(),
                operatorId,
                null,
                null,
                existingCard.getTypeId().value(),
                String.valueOf(existingCard.getId().value()));

        detectTitleChange(event, existingCard, request);
        detectDescriptionChange(event, existingCard, request);
        detectFieldValueChanges(event, existingCard, request);

        if (event.hasChanges()) {
            eventPublisher.publishAsync(applyOperationSource(event));
            logger.debug("发布卡片更新事件: cardId={}", existingCard.getId());
        }
    }

    /**
     * 发布卡片移动事件
     */
    public void publishMoved(CardDTO existingCard, MoveCardRequest request, String operatorId) {
        CardMovedEvent event = buildMoveEvent(
                existingCard,
                request.streamId().value(),
                request.cardId().value(),
                existingCard.getStatusId(),
                request.toStatusId(),
                operatorId);
        eventPublisher.publishAsync(applyOperationSource(event));
    }

    /**
     * 批量发布卡片移动事件
     */
    public void publishAllMoved(List<CardMovedEvent> events) {
        events.forEach(this::applyOperationSource);
        eventPublisher.publishAll(events);
    }

    /**
     * 发布卡片归档事件
     */
    public void publishArchived(CardDTO card, String operatorId, String sourceIp) {
        CardArchivedEvent event = new CardArchivedEvent(
                card.getOrgId().value(),
                operatorId,
                sourceIp,
                null,
                card.getTypeId().value(),
                card.getId().value());
        eventPublisher.publishAsync(applyOperationSource(event));
        logger.debug("发布卡片归档事件: cardId={}", card.getId());
    }

    /**
     * 批量发布卡片归档事件
     */
    public void publishAllArchived(List<CardDTO> cards, String operatorId, String sourceIp) {
        List<CardArchivedEvent> events = cards.stream()
                .map(card -> applyOperationSource(new CardArchivedEvent(
                        card.getOrgId().value(),
                        operatorId,
                        sourceIp,
                        null,
                        card.getTypeId().value(),
                        card.getId().value())))
                .toList();
        eventPublisher.publishAll(events);
        logger.debug("批量发布卡片归档事件: count={}", events.size());
    }

    /**
     * 发布卡片丢弃事件
     */
    public void publishAbandoned(CardDTO card, String operatorId, String sourceIp, String reason) {
        CardAbandonedEvent event = new CardAbandonedEvent(
                card.getOrgId().value(),
                operatorId,
                sourceIp,
                null,
                card.getTypeId().value(),
                card.getId().value(),
                reason);
        eventPublisher.publishAsync(applyOperationSource(event));
        logger.debug("发布卡片丢弃事件: cardId={}", card.getId());
    }

    /**
     * 批量发布卡片丢弃事件
     */
    public void publishAllAbandoned(List<CardDTO> cards, String operatorId, String sourceIp, String reason) {
        List<CardAbandonedEvent> events = cards.stream()
                .map(card -> applyOperationSource(new CardAbandonedEvent(
                        card.getOrgId().value(),
                        operatorId,
                        sourceIp,
                        null,
                        card.getTypeId().value(),
                        card.getId().value(),
                        reason)))
                .toList();
        eventPublisher.publishAll(events);
        logger.debug("批量发布卡片丢弃事件: count={}", events.size());
    }

    /**
     * 发布卡片还原事件
     */
    public void publishRestored(CardDTO card, String operatorId, String sourceIp) {
        CardRestoredEvent event = new CardRestoredEvent(
                card.getOrgId().value(),
                operatorId,
                sourceIp,
                null,
                card.getTypeId().value(),
                card.getId().value());
        eventPublisher.publishAsync(applyOperationSource(event));
        logger.debug("发布卡片还原事件: cardId={}", card.getId());
    }

    /**
     * 批量发布卡片还原事件
     */
    public void publishAllRestored(List<CardDTO> cards, String operatorId, String sourceIp) {
        List<CardRestoredEvent> events = cards.stream()
                .map(card -> applyOperationSource(new CardRestoredEvent(
                        card.getOrgId().value(),
                        operatorId,
                        sourceIp,
                        null,
                        card.getTypeId().value(),
                        card.getId().value())))
                .toList();
        eventPublisher.publishAll(events);
        logger.debug("批量发布卡片还原事件: count={}", events.size());
    }

    /**
     * 构建卡片移动事件
     */
    public CardMovedEvent buildMoveEvent(CardDTO existingCard, String streamId, String cardId,
                                         StatusId fromStatusId, StatusId toStatusId, String operatorId) {
        CardMovedEvent event = new CardMovedEvent(
                existingCard.getOrgId().value(),
                operatorId,
                null,
                null,
                existingCard.getTypeId().value(),
                cardId);

        event.setStreamId(streamId);
        populateStatusChangeInfo(event, existingCard.getTypeId(), fromStatusId, toStatusId);
        return event;
    }

    /**
     * 填充卡片移动事件的状态变更信息
     */
    public void populateStatusChangeInfo(CardMovedEvent event, CardTypeId cardTypeId,
                                         StatusId fromStatusId, StatusId toStatusId) {
        Optional<ValueStreamDefinition> valueStreamOpt = valueStreamCacheQuery.getValueStreamByCardTypeId(cardTypeId);

        if (valueStreamOpt.isEmpty()) {
            event.withStatusChange(null, fromStatusId.value(), fromStatusId.value(), null,
                    null, toStatusId.value(), toStatusId.value(), null, false);
            return;
        }

        ValueStreamDefinition valueStream = valueStreamOpt.get();
        String fromStatusName = valueStreamHelper.getStatusName(valueStream, fromStatusId);
        String toStatusName = valueStreamHelper.getStatusName(valueStream, toStatusId);

        Optional<StepId> fromStepIdOpt = valueStreamHelper.findStepIdByStatusId(valueStream, fromStatusId);
        StepId fromStepId = fromStepIdOpt.orElse(null);
        StatusWorkType fromWorkType = fromStepId != null
                ? valueStreamHelper.getStatusWorkType(valueStream, fromStepId, fromStatusId)
                : StatusWorkType.WORKING;

        Optional<StepId> toStepIdOpt = valueStreamHelper.findStepIdByStatusId(valueStream, toStatusId);
        StepId toStepId = toStepIdOpt.orElse(null);
        StatusWorkType toWorkType = toStepId != null
                ? valueStreamHelper.getStatusWorkType(valueStream, toStepId, toStatusId)
                : StatusWorkType.WORKING;

        boolean rollback = false;
        if (fromStepId != null && toStepId != null) {
            rollback = valueStreamHelper.isRollback(valueStream, fromStepId, fromStatusId, toStepId, toStatusId);
        }

        event.withStatusChange(
                fromStepId != null ? fromStepId.value() : null,
                fromStatusId.value(),
                fromStatusName,
                fromWorkType.name(),
                toStepId != null ? toStepId.value() : null,
                toStatusId.value(),
                toStatusName,
                toWorkType.name(),
                rollback);
    }

    private void detectTitleChange(CardUpdatedEvent event, CardDTO existingCard, UpdateCardRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            return;
        }
        String oldTitle = existingCard.getTitle() != null ? existingCard.getTitle().getDisplayValue() : "";
        String newTitle = request.title();
        if (!oldTitle.equals(newTitle)) {
            event.withTitleChange(oldTitle, newTitle);
        }
    }

    private void detectDescriptionChange(CardUpdatedEvent event, CardDTO existingCard, UpdateCardRequest request) {
        if (request.description() == null) {
            return;
        }
        String oldDesc = existingCard.getDescription() != null ? existingCard.getDescription().getValueOrEmpty() : "";
        String newDesc = request.description();
        if (!oldDesc.equals(newDesc)) {
            event.withDescriptionChange(oldDesc, newDesc);
        }
    }

    private void detectFieldValueChanges(CardUpdatedEvent event, CardDTO existingCard, UpdateCardRequest request) {
        if (request.fieldValues() == null || request.fieldValues().isEmpty()) {
            return;
        }
        Map<String, FieldValue<?>> existingFieldValues = existingCard.getFieldValues();
        for (Map.Entry<String, FieldValue<?>> entry : request.fieldValues().entrySet()) {
            String fieldId = entry.getKey();
            FieldValue<?> newFieldValue = entry.getValue();
            FieldValue<?> oldFieldValue = existingFieldValues != null ? existingFieldValues.get(fieldId) : null;

            if (!Objects.equals(oldFieldValue, newFieldValue)) {
                event.addFieldChange(fieldId, oldFieldValue, newFieldValue);
            }
        }
    }

    /**
     * 应用操作来源上下文到事件
     * <p>
     * 从 OperationSourceContext 获取当前线程的操作来源（如业务规则、API调用等），
     * 并设置到事件中。如果没有设置操作来源，默认使用用户操作来源。
     */
    private <T extends CardEvent> T applyOperationSource(T event) {
        return event.withOperationSource(OperationSourceContext.current());
    }
}
