package planka.graph.driver;

import planka.graph.driver.proto.model.Title;
import planka.graph.driver.proto.query.*;
import planka.graph.driver.proto.request.Request;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 卡片查询客户端
 * 负责处理所有与卡片查询相关的业务逻辑
 * 依赖ZgraphClient处理底层通信
 */
public class PlankaGraphCardQueryClient {

    private final PlankaGraphClient client;

    /**
     * 构造函数
     *
     * @param client 底层通信客户端
     */
    public PlankaGraphCardQueryClient(PlankaGraphClient client) {
        this.client = client;
    }

    /**
     * 发送卡片查询请求
     *
     * @param request 卡片查询请求
     */
    public CompletableFuture<CardQueryResponse> query(CardQueryRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setCardQuery(request)
                .build();

        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasCardQueryResponse()) {
                        return response.getCardQueryResponse();
                    } else {
                        throw new RuntimeException("响应中不包含卡片查询结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis());
    }

    /**
     * 发送卡片计数请求
     *
     * @param request 卡片计数请求
     * @return 包含计数结果的CompletableFuture
     */
    public CompletableFuture<Integer> countCards(CardCountRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setCardCount(request)
                .build();

        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasCardCountResponse()) {
                        CardCountResponse countResponse = response.getCardCountResponse();
                        return countResponse.getCount();
                    } else {
                        throw new RuntimeException("响应中不包含卡片计数结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis());
    }

    public CompletableFuture<Set<String>> queryCardIds(CardIdQueryRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setCardIdQuery(request)
                .build();
        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasQueryIdsResponse()) {
                        QueryIdsResponse idsResponse = response.getQueryIdsResponse();
                        return idsResponse.getIdsList().stream()
                                .map(String::valueOf)
                                .collect(java.util.stream.Collectors.toSet());
                    } else {
                        throw new RuntimeException("响应中不包含卡片ID结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis());
    }

    /**
     * 发送按分组计数请求
     *
     * @param request 按分组计数请求
     * @return 包含分组计数结果的CompletableFuture
     */
    public CompletableFuture<Map<String, Integer>> countByGroup(CardCountByGroupRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setCardCountByGroup(request)
                .build();

        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasCardCountByGroupResponse()) {
                        CardCountByGroupResponse groupResponse = response.getCardCountByGroupResponse();
                        // 将uint32转换为Integer
                        return groupResponse.getCountsMap().entrySet().stream()
                                .collect(java.util.stream.Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> (Integer) entry.getValue().intValue()));
                    } else {
                        throw new RuntimeException("响应中不包含按分组计数结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis());
    }

    /**
     * 批量查询卡片标题
     * <p>
     * 轻量级查询，只返回卡片ID和proto Title的映射，用于审计日志等场景
     *
     * @param cardIds 卡片ID列表
     * @return 卡片ID到标题的映射 (proto Title)
     */
    public CompletableFuture<Map<String, Title>> queryCardTitles(List<String> cardIds) {
        QueryCardTitlesRequest request = QueryCardTitlesRequest.newBuilder()
                .addAllCardIds(cardIds)
                .build();

        Request fullRequest = RequestBuilder.create()
                .setQueryCardTitles(request)
                .build();

        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasQueryCardTitlesResponse()) {
                        QueryCardTitlesResponse titlesResponse = response.getQueryCardTitlesResponse();
                        return titlesResponse.getTitlesMap();
                    } else {
                        throw new RuntimeException("响应中不包含卡片标题结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis());
    }
}