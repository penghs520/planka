package dev.planka.card.service.core;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.CreateCardRequest;
import dev.planka.api.card.request.UpdateCardRequest;
import dev.planka.card.model.CardEntity;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.flowrecord.ValueStreamHelper;
import dev.planka.card.service.sequence.SequenceSegmentService;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.card.CardStyle;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.field.StructureFieldValue;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.cardtype.CodeGenerationRule;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import dev.planka.infra.cache.schema.query.CardTypeCacheQuery;
import dev.planka.infra.cache.schema.query.ValueStreamCacheQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import dev.planka.domain.schema.definition.cardtype.TitleCompositionRule;

/**
 * 卡片实体转换器
 * <p>
 * 负责 Request 对象与 CardEntity 之间的转换。
 */
@Component
public class CardEntityConverter {

    private static final Logger logger = LoggerFactory.getLogger(CardEntityConverter.class);

    private final CardRepository cardRepository;
    private final SequenceSegmentService sequenceSegmentService;
    private final ValueStreamCacheQuery valueStreamCacheQuery;
    private final CardTypeCacheQuery cardTypeCacheQuery;
    private final TitleValueResolver titleValueResolver;

    public CardEntityConverter(CardRepository cardRepository,
                               SequenceSegmentService sequenceSegmentService,
                               ValueStreamCacheQuery valueStreamCacheQuery,
                               CardTypeCacheQuery cardTypeCacheQuery,
                               TitleValueResolver titleValueResolver) {
        this.cardRepository = cardRepository;
        this.sequenceSegmentService = sequenceSegmentService;
        this.valueStreamCacheQuery = valueStreamCacheQuery;
        this.cardTypeCacheQuery = cardTypeCacheQuery;
        this.titleValueResolver = titleValueResolver;
    }

    /**
     * 过滤创建请求中的 StructureFieldValue
     */
    public FilteredCreateRequest filterStructureFieldValues(CreateCardRequest request) {
        if (request.fieldValues() == null || request.fieldValues().isEmpty()) {
            return new FilteredCreateRequest(request, Map.of());
        }

        Map<String, FieldValue<?>> filteredFieldValues = new HashMap<>();
        Map<String, StructureFieldValue> structureFieldValues = new HashMap<>();

        for (Map.Entry<String, FieldValue<?>> entry : request.fieldValues().entrySet()) {
            if (entry.getValue() instanceof StructureFieldValue structureValue) {
                structureFieldValues.put(entry.getKey(), structureValue);
                logger.debug("创建请求中发现架构属性: fieldId={}", entry.getKey());
            } else {
                filteredFieldValues.put(entry.getKey(), entry.getValue());
            }
        }

        if (structureFieldValues.isEmpty()) {
            return new FilteredCreateRequest(request, Map.of());
        }

        CreateCardRequest filteredRequest = new CreateCardRequest(
                request.orgId(),
                request.typeId(),
                request.title(),
                request.description(),
                filteredFieldValues.isEmpty() ? null : filteredFieldValues,
                request.linkUpdates()
        );

        return new FilteredCreateRequest(filteredRequest, structureFieldValues);
    }

    /**
     * 处理标题生成
     * 如果卡片类型配置了标题组合规则，则根据规则生成标题
     */
    private CardTitle resolveTitle(CardTitle originalTitle, CardTypeId typeId, CardDTO contextCard) {
        String baseTitle = originalTitle.getValue();
        Optional<CardTypeDefinition> cardTypeOpt = cardTypeCacheQuery.getById(typeId);
        if (cardTypeOpt.isEmpty() || !(cardTypeOpt.get() instanceof EntityCardType cardType)) {
            return originalTitle;
        }

        TitleCompositionRule rule = cardType.getTitleCompositionRule();
        if (rule == null || !rule.isEnabled()) {
            return originalTitle;
        }

        List<String> parts = titleValueResolver.resolveParts(contextCard, rule);
        // 即使 parts 为空，如果规则启用了，可能也需要重置标题为 baseTitle？
        // 或者保留原状？
        // 如果 parts 为空， CardTitle.joint 会生成只有 baseTitle 的标题。

        return CardTitle.joint(baseTitle, rule.getArea(),
                List.of(new CardTitle.JointParts(
                        parts.stream().map(CardTitle.JointPart::new).collect(Collectors.toList())
                ))
        );
    }

    /**
     * 将 CreateCardRequest 转换为 CardEntity
     */
    public CardEntity toCardEntityForCreate(CreateCardRequest request) {
        CardId cardId = CardId.generate();
        long codeInOrg = sequenceSegmentService.getNextCodeInOrg(request.orgId().value());
        String customCode = null;

        // 生成自定义编号
        Optional<CardTypeDefinition> cardTypeOpt = cardTypeCacheQuery.getById(request.typeId());
        if (cardTypeOpt.isPresent() && cardTypeOpt.get() instanceof EntityCardType entityCardType) {
            CodeGenerationRule rule = entityCardType.getCodeGenerationRule();
            if (rule != null) {
                StringBuilder sb = new StringBuilder();
                if (rule.getPrefix() != null) {
                    sb.append(rule.getPrefix());
                }
                if (rule.getDateFormat() != null && !rule.getDateFormat().isBlank()) {
                    try {
                        sb.append(LocalDate.now().format(DateTimeFormatter.ofPattern(rule.getDateFormat())));
                        if (rule.getDateSequenceConnector() != null) {
                            sb.append(rule.getDateSequenceConnector());
                        }
                    } catch (Exception e) {
                        logger.warn("无效的日期格式: {}", rule.getDateFormat());
                    }
                }

                // 序列号
                long customSeq = sequenceSegmentService.getNextCode(request.orgId().value(), request.typeId(), sb.toString());
                // 序列号 padding
                String seqStr = String.valueOf(customSeq);
                if (seqStr.length() < rule.getSequenceLength()) {
                    sb.append("0".repeat(rule.getSequenceLength() - seqStr.length()));
                }
                sb.append(seqStr);

                customCode = sb.toString();
            }
        }

        StreamId streamId = null;
        StatusId statusId = null;

        Optional<ValueStreamDefinition> opt = valueStreamCacheQuery.getValueStreamByCardTypeId(request.typeId());
        if (opt.isPresent()) {
            ValueStreamDefinition valueStreamDefinition = opt.get();
            streamId = valueStreamDefinition.getId();
            statusId = ValueStreamHelper.getFirstStatusId(valueStreamDefinition);
        }

        // 构造上下文 CardDTO 用于标题计算
        CardDTO contextCard = new CardDTO();
        contextCard.setId(cardId);
        contextCard.setCodeInOrg(codeInOrg);
        contextCard.setCustomCode(customCode);
        contextCard.setOrgId(request.orgId());
        contextCard.setTypeId(request.typeId());
        contextCard.setStatusId(statusId);
        contextCard.setFieldValues(request.fieldValues());
        contextCard.setCreatedAt(LocalDateTime.now());

        // 计算标题
        CardTitle title = resolveTitle(request.title(), request.typeId(), contextCard);

        CardEntity entity = new CardEntity(
                cardId,
                codeInOrg,
                customCode,
                request.orgId(),
                request.typeId(),
                title,
                CardStyle.ACTIVE,
                streamId,
                statusId,
                contextCard.getCreatedAt());

        entity.setDescription(request.description());
        entity.setFieldValues(request.fieldValues());

        return entity;
    }

    /**
     * 将更新请求与现有卡片数据合并为 CardEntity
     * 
     * 注意：更新时只更新原始标题(value)，不更新拼接标题部分。
     * 拼接标题的更新由卡服务通过Kafka事件异步处理。
     */
    public CardEntity toCardEntityForUpdate(UpdateCardRequest request, CardDTO existingCard) {

        // 处理标题：只更新原始标题(value)，保留现有的拼接信息
        CardTitle resolvedTitle = resolveUpdateTitle(request.title(), existingCard.getTitle());

        CardEntity entity = new CardEntity(
                existingCard.getId(),
                existingCard.getCodeInOrg(),
                existingCard.getCustomCode(),
                existingCard.getOrgId(),
                existingCard.getTypeId(),
                resolvedTitle,
                existingCard.getCardStyle(),
                existingCard.getStreamId(),
                existingCard.getStatusId(),
                existingCard.getCreatedAt());

        mergeDescription(entity, request, existingCard);
        mergeFieldValues(entity, request, existingCard);

        entity.setUpdatedAt(System.currentTimeMillis());
        entity.setAbandonedAt(existingCard.getAbandonedAt());
        entity.setArchivedAt(existingCard.getArchivedAt());

        return entity;
    }

    /**
     * 处理更新时的标题
     * 只更新原始标题(value)，保留现有的拼接信息（如果有）
     * 拼接标题的更新由Kafka事件异步触发，此处不处理
     */
    private CardTitle resolveUpdateTitle(String newTitleValue, CardTitle existingTitle) {
        // 如果没有传入新标题，保持原标题不变
        if (newTitleValue == null || newTitleValue.isBlank()) {
            return existingTitle;
        }

        // 如果现有标题是拼接标题，保留拼接信息，只更新 value
        if (existingTitle instanceof CardTitle.JointTitle jointTitle) {
            return CardTitle.joint(newTitleValue, jointTitle.getArea(), jointTitle.getMultiParts());
        }

        // 否则创建纯标题
        return CardTitle.pure(newTitleValue);
    }


    private void mergeDescription(CardEntity entity, UpdateCardRequest request, CardDTO existingCard) {
        if (request.description() != null) {
            entity.setDescription(request.description());
        } else if (existingCard.getDescription() != null) {
            entity.setDescription(existingCard.getDescription().getValue());
        }
    }

    private void mergeFieldValues(CardEntity entity, UpdateCardRequest request, CardDTO existingCard) {
        if (existingCard.getFieldValues() != null) {
            entity.setFieldValues(new HashMap<>(existingCard.getFieldValues()));
        }
        if (request.fieldValues() != null) {
            if (entity.getFieldValues() == null) {
                entity.setFieldValues(new HashMap<>());
            }
            entity.getFieldValues().putAll(request.fieldValues());
        }
    }

    /**
     * 创建请求过滤结果
     */
    public record FilteredCreateRequest(
            CreateCardRequest request,
            Map<String, StructureFieldValue> structureFieldValues
    ) {
    }
}
