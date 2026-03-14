package cn.agilean.kanban.card.repository.impl;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.api.card.request.*;
import cn.agilean.kanban.card.converter.*;
import cn.agilean.kanban.card.model.CardEntity;
import cn.agilean.kanban.card.repository.CardRepository;
import cn.agilean.kanban.card.service.evaluator.ConditionResolver;
import cn.agilean.kanban.common.result.PageResult;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.card.CardTitle;
import cn.agilean.kanban.domain.schema.definition.condition.Condition;
import cn.agilean.kanban.domain.stream.StatusId;
import cn.agilean.kanban.domain.stream.StreamId;
import cn.agilean.kanban.infra.cache.card.model.CardBasicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import zgraph.driver.ZgraphCardQueryClient;
import zgraph.driver.ZgraphWriteClient;
import zgraph.driver.proto.model.Card;
import zgraph.driver.proto.query.CardQueryResponse;
import zgraph.driver.proto.write.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 基于 Zgraph 的卡片仓储实现
 */
@Repository
public class ZgraphCardRepository implements CardRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZgraphCardRepository.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ZgraphCardQueryClient queryClient;
    private final ZgraphWriteClient writeClient;
    private final ConditionResolver conditionResolver;

    public ZgraphCardRepository(ZgraphCardQueryClient queryClient, ZgraphWriteClient writeClient,
                                ConditionResolver conditionResolver) {
        this.queryClient = queryClient;
        this.writeClient = writeClient;
        this.conditionResolver = conditionResolver;
    }

    /**
     * 将条件转换为 proto 格式，先使用 ConditionResolver 解析动态日期
     */
    private zgraph.driver.proto.query.Condition toProtoCondition(Condition condition) {
        if (condition == null || condition.isEmpty()) {
            return zgraph.driver.proto.query.Condition.getDefaultInstance();
        }
        // 先解析条件（转换动态日期为静态日期）
        Condition resolvedCondition = conditionResolver.resolve(condition);
        return ConditionConverter.toProto(resolvedCondition);
    }

    // ==================== 写操作 ====================

    @Override
    public CardId create(CardEntity cardEntity) {
        Card protoCard = CardProtoConverter.toProtoCard(cardEntity);

        BatchCreateCardRequest batchRequest = BatchCreateCardRequest.newBuilder()
                .addCards(protoCard)
                .build();

        try {
            CompletableFuture<BatchCardCommonResponse> future = writeClient.batchCreateCard(batchRequest);
            BatchCardCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response.getSuccess() > 0) {
                logger.info("创建卡片成功，cardId: {}", cardEntity.getId().value());
                return cardEntity.getId();
            } else {
                throw new RuntimeException("创建卡片失败");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("创建卡片失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("创建卡片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(CardEntity cardEntity) {
        batchUpdate(List.of(cardEntity));
    }

    @Override
    public void discard(CardId cardId, String discardReason, String operatorId) {
        batchDiscard(List.of(cardId), discardReason, operatorId);
    }

    @Override
    public List<CardId> batchCreate(List<CardEntity> cardEntities) {
        if (cardEntities == null || cardEntities.isEmpty()) {
            return List.of();
        }

        List<Card> protoCards = cardEntities.stream()
                .map(CardProtoConverter::toProtoCard)
                .toList();

        BatchCreateCardRequest batchRequest = BatchCreateCardRequest.newBuilder()
                .addAllCards(protoCards)
                .build();

        try {
            CompletableFuture<BatchCardCommonResponse> future = writeClient.batchCreateCard(batchRequest);
            BatchCardCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            logger.info("批量创建卡片完成，成功: {}, 失败: {}",
                    response.getSuccess(), response.getFailedIdsCount());

            // 过滤掉失败的卡片ID，返回成功创建的卡片ID列表
            Set<String> failedIds = response.getFailedIdsList().stream()
                    .map(String::valueOf)
                    .collect(Collectors.toSet());

            return cardEntities.stream()
                    .map(CardEntity::getId)
                    .filter(id -> !failedIds.contains(id.value()))
                    .toList();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("批量创建卡片失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量创建卡片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public BatchCardCommonResponse batchUpdate(List<CardEntity> cardEntities) {
        if (cardEntities == null || cardEntities.isEmpty()) {
            return BatchCardCommonResponse.getDefaultInstance();
        }

        // 转换为 proto Card 列表
        List<Card> protoCards = cardEntities.stream()
                .map(CardProtoConverter::toProtoCard)
                .filter(Objects::nonNull)
                .toList();

        BatchUpdateCardRequest batchRequest = BatchUpdateCardRequest.newBuilder()
                .addAllCards(protoCards)
                .build();

        try {
            CompletableFuture<BatchCardCommonResponse> future = writeClient.batchUpdateCard(batchRequest);
            BatchCardCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            logger.info("批量更新卡片完成，成功: {}, 失败: {}",
                    response.getSuccess(), response.getFailedIdsCount());
            return response;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("批量更新卡片失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量更新卡片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void batchDiscard(List<CardId> cardIds, String discardReason, String operatorId) {
        if (cardIds == null || cardIds.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        List<UpdateCardFieldRequest> updateRequests = cardIds.stream()
                .map(cardId -> UpdateCardFieldRequest.newBuilder()
                        .setCardId(Long.parseLong(cardId.value()))
                        .setState(zgraph.driver.proto.common.CardState.Discarded)
                        .setDiscardedAt(now)
                        .setDiscardReason(discardReason != null ? discardReason : "")
                        .build())
                .toList();

        BatchUpdateCardFieldRequest batchRequest = BatchUpdateCardFieldRequest.newBuilder()
                .addAllRequests(updateRequests)
                .build();

        try {
            CompletableFuture<BatchCardCommonResponse> future = writeClient.batchUpdateCardField(batchRequest);
            BatchCardCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.info("批量丢弃卡片完成，成功: {}, 失败: {}",
                    response.getSuccess(), response.getFailedIdsCount());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("批量丢弃卡片失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量丢弃卡片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void batchArchive(List<CardId> cardIds, String operatorId) {
        if (cardIds == null || cardIds.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        List<UpdateCardFieldRequest> updateRequests = cardIds.stream()
                .map(cardId -> UpdateCardFieldRequest.newBuilder()
                        .setCardId(Long.parseLong(cardId.value()))
                        .setState(zgraph.driver.proto.common.CardState.Archived)
                        .setArchivedAt(now)
                        .build())
                .toList();

        BatchUpdateCardFieldRequest batchRequest = BatchUpdateCardFieldRequest.newBuilder()
                .addAllRequests(updateRequests)
                .build();

        try {
            CompletableFuture<BatchCardCommonResponse> future = writeClient.batchUpdateCardField(batchRequest);
            BatchCardCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.info("批量归档卡片完成，成功: {}, 失败: {}",
                    response.getSuccess(), response.getFailedIdsCount());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("批量归档卡片失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量归档卡片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void batchRestore(List<CardId> cardIds, String operatorId) {
        if (cardIds == null || cardIds.isEmpty()) {
            return;
        }

        List<UpdateCardFieldRequest> updateRequests = cardIds.stream()
                .map(cardId -> UpdateCardFieldRequest.newBuilder()
                        .setCardId(Long.parseLong(cardId.value()))
                        .setState(zgraph.driver.proto.common.CardState.Active)
                        .build())
                .toList();

        BatchUpdateCardFieldRequest batchRequest = BatchUpdateCardFieldRequest.newBuilder()
                .addAllRequests(updateRequests)
                .build();

        try {
            CompletableFuture<BatchCardCommonResponse> future = writeClient.batchUpdateCardField(batchRequest);
            BatchCardCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.info("批量还原卡片完成，成功: {}, 失败: {}",
                    response.getSuccess(), response.getFailedIdsCount());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("批量还原卡片失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量还原卡片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void archive(CardId cardId, String operatorId) {
        batchArchive(List.of(cardId), operatorId);
    }

    @Override
    public void restore(CardId cardId, String operatorId) {
        batchRestore(List.of(cardId), operatorId);
    }

    @Override
    public void updateStatus(CardId cardId, StreamId streamId, StatusId statusId, String operatorId) {
        batchUpdateStatus(List.of(cardId), streamId, statusId, operatorId);
    }

    @Override
    public void batchUpdateStatus(List<CardId> cardIds, StreamId streamId, StatusId statusId, String operatorId) {
        if (cardIds == null || cardIds.isEmpty()) {
            return;
        }

        // 使用 ValueStreamStatus 来更新状态
        ValueStreamStatus valueStreamStatus = ValueStreamStatus.newBuilder()
                .setStreamId(streamId.value())
                .setStatusId(statusId.value())
                .build();

        List<UpdateCardFieldRequest> updateRequests = cardIds.stream()
                .map(cardId -> UpdateCardFieldRequest.newBuilder()
                        .setCardId(Long.parseLong(cardId.value()))
                        .setValueStreamStatus(valueStreamStatus)
                        .build())
                .toList();

        BatchUpdateCardFieldRequest batchRequest = BatchUpdateCardFieldRequest.newBuilder()
                .addAllRequests(updateRequests)
                .build();

        try {
            CompletableFuture<BatchCardCommonResponse> future = writeClient.batchUpdateCardField(batchRequest);
            BatchCardCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.info("批量更新卡片状态完成，成功: {}, 失败: {}",
                    response.getSuccess(), response.getFailedIdsCount());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("批量更新卡片状态失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量更新卡片状态失败: " + e.getMessage(), e);
        }
    }

    // ==================== 读操作 ====================

    @Override
    public Optional<CardDTO> findById(CardId cardId, Yield yield, String operatorId) {
        if (cardId == null) {
            return Optional.empty();
        }

        List<CardDTO> cards = findByIds(List.of(cardId), yield, operatorId);
        return cards.isEmpty() ? Optional.empty() : Optional.of(cards.get(0));
    }

    @Override
    public List<CardDTO> findByIds(List<CardId> cardIds, Yield yield, String operatorId) {
        if (cardIds == null || cardIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建查询请求
        CardQueryRequest request = new CardQueryRequest();
        QueryContext queryContext = new QueryContext();
        queryContext.setOperatorId(operatorId);
        request.setQueryContext(queryContext);

        QueryScope queryScope = new QueryScope();
        queryScope.setCardIds(cardIds.stream().map(id -> String.valueOf(id.value())).toList());
        request.setQueryScope(queryScope);
        request.setYield(yield);

        return query(request);
    }

    @Override
    public List<CardDTO> query(CardQueryRequest request) {
        if (request == null) {
            return new ArrayList<>();
        }

        // 构建 proto 查询请求
        zgraph.driver.proto.query.CardQueryRequest protoRequest = buildProtoCardQueryRequest(request, null);

        try {
            CompletableFuture<CardQueryResponse> future = queryClient.query(protoRequest);
            CardQueryResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            return CardProtoConverter.toCardDTOList(response.getCardsList());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("查询卡片失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询卡片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PageResult<CardDTO> pageQuery(CardPageQueryRequest request) {
        if (request == null) {
            return PageResult.empty();
        }

        // 构建 proto 查询请求
        zgraph.driver.proto.query.CardQueryRequest protoRequest = buildProtoCardQueryRequest(request,
                request.getSortAndPage());

        try {
            CompletableFuture<CardQueryResponse> future = queryClient.query(protoRequest);
            CardQueryResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<CardDTO> cards = CardProtoConverter.toCardDTOList(response.getCardsList());

            // 从 sortAndPage 获取分页参数
            int page = 0;
            int size = DEFAULT_PAGE_SIZE;
            if (request.getSortAndPage() != null && request.getSortAndPage().getPage() != null) {
                page = request.getSortAndPage().getPage().getPageNum();
                size = request.getSortAndPage().getPage().getPageSize() > 0
                        ? request.getSortAndPage().getPage().getPageSize()
                        : DEFAULT_PAGE_SIZE;
            }

            return PageResult.of(cards, page, size, response.getTotal());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("分页查询卡片失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("分页查询卡片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> queryIds(CardIdQueryRequest request) {
        if (request == null) {
            return new ArrayList<>();
        }

        // 构建 proto 查询请求
        zgraph.driver.proto.query.CardIdQueryRequest protoRequest = zgraph.driver.proto.query.CardIdQueryRequest
                .newBuilder()
                .setQueryContext(QueryContextConverter.toProto(request.getQueryContext()))
                .setQueryScope(QueryScopeConverter.toProto(request.getQueryScope()))
                .setCondition(toProtoCondition(request.getCondition()))
                .build();

        try {
            CompletableFuture<Set<String>> future = queryClient.queryCardIds(protoRequest);
            Set<String> ids = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return new ArrayList<>(ids);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("查询卡片ID失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询卡片ID失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Integer count(CardCountRequest request) {
        if (request == null) {
            return 0;
        }

        // 构建 proto 计数请求
        zgraph.driver.proto.query.CardCountRequest protoRequest = zgraph.driver.proto.query.CardCountRequest
                .newBuilder()
                .setQueryContext(QueryContextConverter.toProto(request.getQueryContext()))
                .setQueryScope(QueryScopeConverter.toProto(request.getQueryScope()))
                .setCondition(toProtoCondition(request.getCondition()))
                .build();

        try {
            CompletableFuture<Integer> future = queryClient.countCards(protoRequest);
            return future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("统计卡片数量失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("统计卡片数量失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有方法 ====================

    private zgraph.driver.proto.query.CardQueryRequest buildProtoCardQueryRequest(
            CardQueryRequest request,
            SortAndPage sortAndPage) {

        zgraph.driver.proto.query.CardQueryRequest.Builder builder = zgraph.driver.proto.query.CardQueryRequest
                .newBuilder();

        if (request.getQueryContext() != null) {
            builder.setQueryContext(QueryContextConverter.toProto(request.getQueryContext()));
        }
        if (request.getQueryScope() != null) {
            builder.setQueryScope(QueryScopeConverter.toProto(request.getQueryScope()));
        }
        if (request.getCondition() != null) {
            builder.setCondition(toProtoCondition(request.getCondition()));
        }
        if (request.getYield() != null) {
            builder.setYield(YieldConverter.toProto(request.getYield()));
        }
        if (sortAndPage != null) {
            builder.setSortAndPage(SortAndPageConverter.toProto(sortAndPage));
        }

        return builder.build();
    }

    @Override
    public Map<String, CardTitle> queryCardNames(List<String> cardIds, String operatorId) {
        if (cardIds == null || cardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // 调用zgraph-driver批量查询卡片标题 (返回proto Title)
            CompletableFuture<Map<String, zgraph.driver.proto.model.Title>> future = queryClient.queryCardTitles(cardIds);
            Map<String, zgraph.driver.proto.model.Title> protoTitleMap = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // 转换proto Title为domain CardTitle
            if (protoTitleMap == null || protoTitleMap.isEmpty()) {
                return Collections.emptyMap();
            }

            return TitleConverter.fromProtoMap(protoTitleMap);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("批量查询卡片标题被中断", e);
            throw new RuntimeException("批量查询卡片标题被中断", e);
        } catch (ExecutionException e) {
            logger.error("批量查询卡片标题执行失败", e);
            throw new RuntimeException("批量查询卡片标题失败", e.getCause());
        } catch (TimeoutException e) {
            logger.error("批量查询卡片标题超时", e);
            throw new RuntimeException("批量查询卡片标题超时", e);
        }
    }

    @Override
    public Map<CardId, CardBasicInfo> findBasicInfoByIds(Set<CardId> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构建轻量级查询请求，只查询基础字段
        CardQueryRequest request = new CardQueryRequest();

        // 设置查询范围：指定卡片ID列表
        QueryScope queryScope = new QueryScope();
        queryScope.setCardIds(cardIds.stream()
                .map(id -> String.valueOf(id.value()))
                .toList());
        request.setQueryScope(queryScope);

        // 设置 Yield：只返回基础字段，不包含自定义属性和关联卡片
        Yield yield = Yield.basic();
        request.setYield(yield);

        try {
            // 执行查询
            List<CardDTO> cards = query(request);

            // 转换为 CardBasicInfo Map
            Map<CardId, CardBasicInfo> result = new HashMap<>();
            for (CardDTO card : cards) {
                CardBasicInfo basicInfo = convertToBasicInfo(card);
                result.put(basicInfo.cardId(), basicInfo);
            }

            logger.debug("批量查询卡片基础信息完成，请求: {}, 返回: {}", cardIds.size(), result.size());
            return result;
        } catch (Exception e) {
            logger.error("批量查询卡片基础信息失败", e);
            throw new RuntimeException("批量查询卡片基础信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 CardDTO 转换为 CardBasicInfo
     */
    private CardBasicInfo convertToBasicInfo(CardDTO card) {
        return new CardBasicInfo(
                card.getId(),
                card.getOrgId(),
                card.getTypeId(),
                card.getTitle(),
                card.getCustomCode() != null ? card.getCustomCode() : String.valueOf(card.getCodeInOrg()),
                card.getCardCycle(),
                card.getStreamId(),
                card.getStatusId()
        );
    }
}
