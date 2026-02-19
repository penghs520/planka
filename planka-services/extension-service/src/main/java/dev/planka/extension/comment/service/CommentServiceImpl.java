package dev.planka.extension.comment.service;

import dev.planka.api.comment.dto.CardRefDTO;
import dev.planka.api.comment.dto.CommentDTO;
import dev.planka.api.comment.dto.CommentListResponse;
import dev.planka.api.comment.dto.MentionDTO;
import dev.planka.api.comment.request.CreateCommentRequest;
import dev.planka.api.comment.request.UpdateCommentRequest;
import dev.planka.domain.schema.EntityState;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.event.comment.BizRuleOperationSource;
import dev.planka.event.comment.OperationSource;
import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.card.CardId;
import dev.planka.extension.comment.config.CommentProperties;
import dev.planka.extension.comment.converter.CommentConverter;
import dev.planka.extension.comment.model.CommentCardRefEntity;
import dev.planka.extension.comment.model.CommentEntity;
import dev.planka.extension.comment.model.CommentMentionEntity;
import dev.planka.extension.comment.repository.CommentCardRefRepository;
import dev.planka.extension.comment.repository.CommentMentionRepository;
import dev.planka.extension.comment.repository.CommentRepository;
import dev.planka.infra.cache.card.CardCacheService;
import dev.planka.infra.cache.card.model.CardBasicInfo;
import dev.planka.infra.cache.schema.query.BizRuleCacheQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    /**
     * 业务规则名称信息
     */
    private record RuleNameInfo(String name, boolean deleted) {
    }

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String STATUS_DELETED = "DELETED";

    private final CommentRepository commentRepository;
    private final CommentMentionRepository mentionRepository;
    private final CommentCardRefRepository cardRefRepository;
    private final CommentProperties commentProperties;
    private final CardCacheService cardCacheService;
    private final ObjectMapper objectMapper;
    private final BizRuleCacheQuery bizRuleCacheQuery;

    public CommentServiceImpl(CommentRepository commentRepository,
                              CommentMentionRepository mentionRepository,
                              CommentCardRefRepository cardRefRepository,
                              CommentProperties commentProperties,
                              CardCacheService cardCacheService,
                              ObjectMapper objectMapper,
                              BizRuleCacheQuery bizRuleCacheQuery) {
        this.commentRepository = commentRepository;
        this.mentionRepository = mentionRepository;
        this.cardRefRepository = cardRefRepository;
        this.commentProperties = commentProperties;
        this.cardCacheService = cardCacheService;
        this.objectMapper = objectMapper;
        this.bizRuleCacheQuery = bizRuleCacheQuery;
    }

    @Override
    public CommentListResponse listComments(String orgId, String cardId, int page, int size) {
        // MyBatis Plus 分页从 1 开始，前端传的 page 从 0 开始
        Page<CommentEntity> pageParam = new Page<>(page + 1, size);
        LambdaQueryWrapper<CommentEntity> query = new LambdaQueryWrapper<CommentEntity>()
                .eq(CommentEntity::getOrgId, orgId)
                .eq(CommentEntity::getCardId, cardId)
                .isNull(CommentEntity::getParentId)
                .ne(CommentEntity::getStatus, STATUS_DELETED)
                .orderByAsc(CommentEntity::getCreatedAt);

        Page<CommentEntity> result = commentRepository.selectPage(pageParam, query);

        // 收集所有需要查询的成员卡片ID
        Set<String> memberCardIds = new HashSet<>();
        for (CommentEntity comment : result.getRecords()) {
            collectMemberCardIds(comment, memberCardIds);
        }

        // 批量查询成员信息
        Map<String, CardBasicInfo> memberInfoMap = getMemberInfoMap(memberCardIds);

        // 批量查询业务规则名称
        Map<String, RuleNameInfo> ruleNameMap = fetchBizRuleNames(result.getRecords());

        List<CommentDTO> comments = result.getRecords().stream()
                .map(entity -> convertToDTO(entity, memberInfoMap, ruleNameMap))
                .collect(Collectors.toList());

        return new CommentListResponse(
                comments,
                result.getTotal(),
                page,
                size,
                result.hasNext()
        );
    }

    /**
     * 递归收集评论及其回复中的所有成员卡片ID
     */
    private void collectMemberCardIds(CommentEntity entity, Set<String> memberCardIds) {
        memberCardIds.add(entity.getAuthorId());
        if (entity.getReplyToMemberId() != null) {
            memberCardIds.add(entity.getReplyToMemberId());
        }

        // 查询回复
        if (entity.getParentId() == null) {
            List<CommentEntity> replies = commentRepository.selectList(
                    new LambdaQueryWrapper<CommentEntity>()
                            .eq(CommentEntity::getRootId, entity.getId())
                            .ne(CommentEntity::getStatus, STATUS_DELETED)
            );
            for (CommentEntity reply : replies) {
                memberCardIds.add(reply.getAuthorId());
                if (reply.getReplyToMemberId() != null) {
                    memberCardIds.add(reply.getReplyToMemberId());
                }
            }
        }

        // 查询 mentions
        List<CommentMentionEntity> mentions = mentionRepository.selectList(
                new LambdaQueryWrapper<CommentMentionEntity>()
                        .eq(CommentMentionEntity::getCommentId, entity.getId())
        );
        for (CommentMentionEntity mention : mentions) {
            memberCardIds.add(mention.getMentionedMemberId());
        }
    }

    /**
     * 批量获取成员信息
     */
    private Map<String, CardBasicInfo> getMemberInfoMap(Set<String> memberCardIds) {
        if (memberCardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<CardId> cardIds = memberCardIds.stream()
                .map(CardId::of)
                .collect(Collectors.toSet());

        Map<CardId, CardBasicInfo> cardInfoMap = cardCacheService.getBasicInfoByIds(cardIds);

        return cardInfoMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().value(),
                        Map.Entry::getValue
                ));
    }

    /**
     * 从成员信息中获取名称
     */
    private String getMemberName(String memberId, Map<String, CardBasicInfo> memberInfoMap) {
        if (memberId == null) {
            return null;
        }
        CardBasicInfo info = memberInfoMap.get(memberId);
        return info != null ? info.title().getDisplayValue() : null;
    }

    /**
     * 批量获取业务规则名称
     *
     * @param entities 评论实体列表
     * @return Map<ruleId, RuleNameInfo>
     */
    private Map<String, RuleNameInfo> fetchBizRuleNames(List<CommentEntity> entities) {
        // 收集所有业务规则ID
        Set<String> ruleIds = entities.stream()
                .map(CommentEntity::getOperationSource)
                .filter(Objects::nonNull)
                .map(this::extractRuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ruleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // 批量查询业务规则（包含已删除的）
            List<BizRuleDefinition> bizRuleDefinitions = bizRuleCacheQuery.getByIds(ruleIds);

            Map<String, RuleNameInfo> ruleNameMap = new HashMap<>(bizRuleDefinitions.size());

            // 处理返回的规则
            for (BizRuleDefinition ruleDefinition : bizRuleDefinitions) {
                if (ruleDefinition.getId() != null) {
                    String name = ruleDefinition.getName() != null ? ruleDefinition.getName() : "";
                    boolean isDeleted = ruleDefinition.getState() == EntityState.DELETED;
                    ruleNameMap.put(ruleDefinition.getId().toString(), new RuleNameInfo(name, isDeleted));
                }
            }

            // 对于未找到的规则，标记为已删除并使用存储的原名称
            for (String ruleId : ruleIds) {
                if (!ruleNameMap.containsKey(ruleId)) {
                    ruleNameMap.put(ruleId, new RuleNameInfo("", true));
                }
            }

            return ruleNameMap;
        } catch (Exception e) {
            log.warn("批量获取业务规则名称失败: {}", ruleIds, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 从操作来源JSON中提取规则ID
     */
    private String extractRuleId(String operationSourceJson) {
        try {
            OperationSource source = objectMapper.readValue(operationSourceJson, OperationSource.class);
            if (source instanceof BizRuleOperationSource bizRuleSource) {
                return bizRuleSource.getRuleId();
            }
        } catch (Exception e) {
            log.debug("提取规则ID失败: {}", operationSourceJson);
        }
        return null;
    }

    @Override
    @Transactional
    public CommentDTO createComment(String orgId, String memberId, CreateCommentRequest request) {
        Long parentIdLong = request.getParentIdAsLong();

        CommentEntity entity = new CommentEntity();
        entity.setId(SnowflakeIdGenerator.generate());
        entity.setOrgId(orgId);
        entity.setCardId(request.cardId());
        entity.setCardTypeId(request.cardTypeId());
        entity.setParentId(parentIdLong);
        entity.setReplyToMemberId(request.replyToMemberId());
        entity.setContent(request.content());
        entity.setStatus(STATUS_ACTIVE);
        entity.setEditCount(0);
        entity.setAuthorId(memberId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        // 设置操作来源（如业务规则信息）
        if (request.operationSource() != null) {
            try {
                entity.setOperationSource(objectMapper.writeValueAsString(request.operationSource()));
            } catch (JsonProcessingException e) {
                log.warn("序列化操作来源失败: {}", request.operationSource(), e);
            }
        }

        // 设置 rootId
        if (parentIdLong != null) {
            CommentEntity parent = commentRepository.selectById(parentIdLong);
            if (parent != null) {
                entity.setRootId(parent.getRootId() != null ? parent.getRootId() : parent.getId());
            }
        }

        commentRepository.insert(entity);

        // 保存 mentions
        if (request.mentions() != null) {
            for (CreateCommentRequest.MentionInput mention : request.mentions()) {
                CommentMentionEntity mentionEntity = new CommentMentionEntity();
                mentionEntity.setId(SnowflakeIdGenerator.generate());
                mentionEntity.setCommentId(entity.getId());
                mentionEntity.setOrgId(orgId);
                mentionEntity.setMentionedMemberId(mention.mentionedMemberId());
                mentionEntity.setStartOffset(mention.startOffset());
                mentionEntity.setEndOffset(mention.endOffset());
                mentionEntity.setCreatedAt(LocalDateTime.now());
                mentionRepository.insert(mentionEntity);
            }
        }

        // 保存 cardRefs
        if (request.cardRefs() != null) {
            for (CreateCommentRequest.CardRefInput cardRef : request.cardRefs()) {
                CommentCardRefEntity cardRefEntity = new CommentCardRefEntity();
                cardRefEntity.setId(SnowflakeIdGenerator.generate());
                cardRefEntity.setCommentId(entity.getId());
                cardRefEntity.setOrgId(orgId);
                cardRefEntity.setRefCardId(cardRef.refCardId());
                cardRefEntity.setRefCardCode(cardRef.refCardCode());
                cardRefEntity.setStartOffset(cardRef.startOffset());
                cardRefEntity.setEndOffset(cardRef.endOffset());
                cardRefEntity.setCreatedAt(LocalDateTime.now());
                cardRefRepository.insert(cardRefEntity);
            }
        }

        // 查询成员信息
        Set<String> memberCardIds = new HashSet<>();
        memberCardIds.add(entity.getAuthorId());
        if (entity.getReplyToMemberId() != null) {
            memberCardIds.add(entity.getReplyToMemberId());
        }
        Map<String, CardBasicInfo> memberInfoMap = getMemberInfoMap(memberCardIds);

        // 查询业务规则名称
        Map<String, RuleNameInfo> ruleNameMap = fetchBizRuleNames(Collections.singletonList(entity));

        return convertToDTO(entity, memberInfoMap, ruleNameMap);
    }

    @Override
    @Transactional
    public CommentDTO updateComment(Long id, String orgId, String memberId, UpdateCommentRequest request) {
        CommentEntity entity = commentRepository.selectById(id);
        if (entity == null || !entity.getOrgId().equals(orgId)) {
            throw new RuntimeException("评论不存在");
        }
        if (!entity.getAuthorId().equals(memberId)) {
            throw new RuntimeException("只能编辑自己的评论");
        }
        if (!STATUS_WITHDRAWN.equals(entity.getStatus())) {
            throw new RuntimeException("只能编辑已撤回的评论");
        }

        entity.setContent(request.content());
        entity.setStatus(STATUS_ACTIVE);
        entity.setEditCount(entity.getEditCount() + 1);
        entity.setLastEditedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        commentRepository.updateById(entity);

        // 删除旧的 mentions 和 cardRefs
        mentionRepository.delete(new LambdaQueryWrapper<CommentMentionEntity>()
                .eq(CommentMentionEntity::getCommentId, id));
        cardRefRepository.delete(new LambdaQueryWrapper<CommentCardRefEntity>()
                .eq(CommentCardRefEntity::getCommentId, id));

        // 保存新的 mentions
        if (request.mentions() != null) {
            for (CreateCommentRequest.MentionInput mention : request.mentions()) {
                CommentMentionEntity mentionEntity = new CommentMentionEntity();
                mentionEntity.setId(SnowflakeIdGenerator.generate());
                mentionEntity.setCommentId(entity.getId());
                mentionEntity.setOrgId(orgId);
                mentionEntity.setMentionedMemberId(mention.mentionedMemberId());
                mentionEntity.setStartOffset(mention.startOffset());
                mentionEntity.setEndOffset(mention.endOffset());
                mentionEntity.setCreatedAt(LocalDateTime.now());
                mentionRepository.insert(mentionEntity);
            }
        }

        // 保存新的 cardRefs
        if (request.cardRefs() != null) {
            for (CreateCommentRequest.CardRefInput cardRef : request.cardRefs()) {
                CommentCardRefEntity cardRefEntity = new CommentCardRefEntity();
                cardRefEntity.setId(SnowflakeIdGenerator.generate());
                cardRefEntity.setCommentId(entity.getId());
                cardRefEntity.setOrgId(orgId);
                cardRefEntity.setRefCardId(cardRef.refCardId());
                cardRefEntity.setRefCardCode(cardRef.refCardCode());
                cardRefEntity.setStartOffset(cardRef.startOffset());
                cardRefEntity.setEndOffset(cardRef.endOffset());
                cardRefEntity.setCreatedAt(LocalDateTime.now());
                cardRefRepository.insert(cardRefEntity);
            }
        }

        // 查询成员信息
        Set<String> memberCardIds = new HashSet<>();
        memberCardIds.add(entity.getAuthorId());
        if (entity.getReplyToMemberId() != null) {
            memberCardIds.add(entity.getReplyToMemberId());
        }
        Map<String, CardBasicInfo> memberInfoMap = getMemberInfoMap(memberCardIds);

        Map<String, RuleNameInfo> ruleNameMap = fetchBizRuleNames(Collections.singletonList(entity));

        return convertToDTO(entity, memberInfoMap, ruleNameMap);
    }

    @Override
    @Transactional
    public CommentDTO withdrawComment(Long id, String orgId, String memberId) {
        CommentEntity entity = commentRepository.selectById(id);
        if (entity == null || !entity.getOrgId().equals(orgId)) {
            throw new RuntimeException("评论不存在");
        }
        if (!entity.getAuthorId().equals(memberId)) {
            throw new RuntimeException("只能撤回自己的评论");
        }
        if (!STATUS_ACTIVE.equals(entity.getStatus())) {
            throw new RuntimeException("评论状态无效");
        }

        // 检查是否在撤回时间限制内
        long secondsSinceCreation = ChronoUnit.SECONDS.between(entity.getCreatedAt(), LocalDateTime.now());
        if (secondsSinceCreation > commentProperties.getWithdrawTimeLimit()) {
            throw new RuntimeException("已超过撤回时间限制");
        }

        entity.setStatus(STATUS_WITHDRAWN);
        entity.setUpdatedAt(LocalDateTime.now());
        commentRepository.updateById(entity);

        // 查询成员信息
        Set<String> memberCardIds = new HashSet<>();
        memberCardIds.add(entity.getAuthorId());
        Map<String, CardBasicInfo> memberInfoMap = getMemberInfoMap(memberCardIds);

        Map<String, RuleNameInfo> ruleNameMap = fetchBizRuleNames(Collections.singletonList(entity));

        return convertToDTO(entity, memberInfoMap, ruleNameMap);
    }

    @Override
    @Transactional
    public void deleteComment(Long id, String orgId, String memberId) {
        CommentEntity entity = commentRepository.selectById(id);
        if (entity == null || !entity.getOrgId().equals(orgId)) {
            throw new RuntimeException("评论不存在");
        }

        entity.setStatus(STATUS_DELETED);
        entity.setUpdatedAt(LocalDateTime.now());
        commentRepository.updateById(entity);
    }

    private CommentDTO convertToDTO(CommentEntity entity, Map<String, CardBasicInfo> memberInfoMap,
                                      Map<String, RuleNameInfo> ruleNameMap) {
        return convertToDTOInternal(entity, memberInfoMap, ruleNameMap, new HashSet<>());
    }

    private CommentDTO convertToDTOInternal(CommentEntity entity, Map<String, CardBasicInfo> memberInfoMap,
                                            Map<String, RuleNameInfo> ruleNameMap, Set<Long> processedIds) {
        // 防止循环引用
        if (processedIds.contains(entity.getId())) {
            return null;
        }
        processedIds.add(entity.getId());

        // 获取作者信息
        String authorName = getMemberName(entity.getAuthorId(), memberInfoMap);
        String authorAvatar = null; // 成员卡片没有头像字段，保持为 null

        // 获取被回复人名称
        String replyToMemberName = getMemberName(entity.getReplyToMemberId(), memberInfoMap);

        // 查询 mentions
        List<CommentMentionEntity> mentionEntities = mentionRepository.selectList(
                new LambdaQueryWrapper<CommentMentionEntity>()
                        .eq(CommentMentionEntity::getCommentId, entity.getId())
        );
        List<MentionDTO> mentions = mentionEntities.stream()
                .map(m -> CommentConverter.toMentionDTO(m, getMemberName(m.getMentionedMemberId(), memberInfoMap)))
                .collect(Collectors.toList());

        // 查询 cardRefs
        List<CommentCardRefEntity> cardRefEntities = cardRefRepository.selectList(
                new LambdaQueryWrapper<CommentCardRefEntity>()
                        .eq(CommentCardRefEntity::getCommentId, entity.getId())
        );
        // 获取引用卡片的标题
        Set<String> refCardIds = cardRefEntities.stream()
                .map(CommentCardRefEntity::getRefCardId)
                .collect(Collectors.toSet());
        Map<String, CardBasicInfo> refCardInfoMap = getMemberInfoMap(refCardIds);

        List<CardRefDTO> cardRefs = cardRefEntities.stream()
                .map(c -> {
                    CardBasicInfo cardInfo = refCardInfoMap.get(c.getRefCardId());
                    String cardTitle = cardInfo != null ? cardInfo.title().getDisplayValue() : null;
                    return CommentConverter.toCardRefDTO(c, cardTitle);
                })
                .collect(Collectors.toList());

        // 查询回复
        List<CommentDTO> replies = new ArrayList<>();
        if (entity.getParentId() == null) {
            List<CommentEntity> replyEntities = commentRepository.selectList(
                    new LambdaQueryWrapper<CommentEntity>()
                            .eq(CommentEntity::getRootId, entity.getId())
                            .ne(CommentEntity::getStatus, STATUS_DELETED)
                            .orderByAsc(CommentEntity::getCreatedAt)
            );
            replies = replyEntities.stream()
                    .map(reply -> convertToDTOInternal(reply, memberInfoMap, ruleNameMap, processedIds))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 解析并更新操作来源
        OperationSource operationSource = resolveOperationSource(entity, ruleNameMap);

        return CommentConverter.toDTO(entity, authorName, authorAvatar, replyToMemberName, mentions, cardRefs, replies, operationSource);
    }

    /**
     * 解析操作来源，根据 ruleId 查询实时名称
     */
    private OperationSource resolveOperationSource(CommentEntity entity, Map<String, RuleNameInfo> ruleNameMap) {
        if (entity.getOperationSource() == null) {
            return null;
        }

        try {
            OperationSource source = objectMapper.readValue(entity.getOperationSource(), OperationSource.class);
            if (source instanceof BizRuleOperationSource bizRuleSource) {
                String ruleId = bizRuleSource.getRuleId();
                RuleNameInfo ruleInfo = ruleNameMap.get(ruleId);

                if (ruleInfo != null) {
                    // 如果规则被删除，返回原名称并标记为已删除（通过 displayName 传回前端判断）
                    String displayName = ruleInfo.deleted()
                            ? bizRuleSource.getRuleName() + " (已删除)"
                            : (ruleInfo.name().isEmpty() ? bizRuleSource.getRuleName() : ruleInfo.name());

                    // 创建新的 BizRuleOperationSource，包含实时名称
                    return new BizRuleOperationSource(ruleId, displayName);
                }
            }
            return source;
        } catch (JsonProcessingException e) {
            log.debug("解析操作来源失败: {}", entity.getOperationSource());
            return null;
        }
    }
}
