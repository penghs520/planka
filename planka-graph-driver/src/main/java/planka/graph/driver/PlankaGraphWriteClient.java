package planka.graph.driver;

import planka.graph.driver.proto.request.Request;
import planka.graph.driver.proto.write.*;

import java.util.concurrent.CompletableFuture;

/**
 * @author penghs
 */
public class PlankaGraphWriteClient {

    private final PlankaGraphClient client;

    /**
     * 构造函数
     *
     * @param client 底层通信客户端
     */
    public PlankaGraphWriteClient(PlankaGraphClient client) {
        this.client = client;
    }


    /**
     * 发送批量创建卡片请求
     *
     * @param request 批量创建卡片请求
     * @return 返回成功创建的卡片数量和失败的卡片id
     */
    public CompletableFuture<BatchCardCommonResponse> batchCreateCard(BatchCreateCardRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setBatchCreateCard(request)
                .build();
                
        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasBatchCardCommonResponse()) {
                        return response.getBatchCardCommonResponse();
                    } else {
                        throw new RuntimeException("响应中不包含批量创建卡片结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis()
        );
    }

    /**
     * 发送批量更新卡片请求
     *
     * @param request 批量更新卡片请求
     * @return 返回成功更新的卡片数量和失败的卡片id
     */
    public CompletableFuture<BatchCardCommonResponse> batchUpdateCard(BatchUpdateCardRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setBatchUpdateCard(request)
                .build();
                
        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasBatchCardCommonResponse()) {
                        return response.getBatchCardCommonResponse();
                    } else {
                        throw new RuntimeException("响应中不包含批量更新卡片结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis()
        );
    }

    /**
     * 发送批量部分更新卡片属性请求
     * 该方法只会更新指定的属性，未指定的属性将保持原值不变
     *
     * @param request 批量部分更新卡片属性请求
     * @return 返回成功更新的卡片数量和失败的卡片id
     */
    public CompletableFuture<BatchCardCommonResponse> batchUpdateCardField(BatchUpdateCardFieldRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setBatchUpdateCardFields(request)
                .build();
                
        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasBatchCardCommonResponse()) {
                        return response.getBatchCardCommonResponse();
                    } else {
                        throw new RuntimeException("响应中不包含批量更新卡片属性结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis()
        );
    }

    /**
     * 发送批量更新卡片标题请求
     *
     * @param request 批量更新卡片标题请求
     * @return 返回成功更新的卡片数量和失败的卡片id
     */
    public CompletableFuture<BatchCardCommonResponse> batchUpdateCardTitle(BatchUpdateCardTitleRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setBatchUpdateCardTitle(request)
                .build();
                
        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasBatchCardCommonResponse()) {
                        return response.getBatchCardCommonResponse();
                    } else {
                        throw new RuntimeException("响应中不包含批量更新卡片标题结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis()
        );
    }

    /**
     * 发送批量创建关联关系请求
     *
     * @param request 批量创建关联关系请求
     * @return 返回成功创建的数量和失败的Link
     */
    public CompletableFuture<BatchLinkCommonResponse> batchCreateLink(BatchCreateLinkRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setBatchCreateLink(request)
                .build();
                
        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasBatchLinkCommonResponse()) {
                        return response.getBatchLinkCommonResponse();
                    } else {
                        throw new RuntimeException("响应中不包含批量创建关联关系结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis()
        );
    }

    /**
     * 发送批量更新关联关系请求
     *
     * @param request 批量更新关联关系请求
     * @return 返回成功更新的数量和失败的Link
     */
    public CompletableFuture<BatchLinkCommonResponse> batchUpdateLink(BatchUpdateLinkRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setBatchUpdateLink(request)
                .build();
                
        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasBatchLinkCommonResponse()) {
                        return response.getBatchLinkCommonResponse();
                    } else {
                        throw new RuntimeException("响应中不包含批量更新关联关系结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis()
        );
    }

    /**
     * 发送批量删除关联关系请求
     *
     * @param request 批量删除关联关系请求
     * @return 返回成功删除的数量和失败的Link
     */
    public CompletableFuture<BatchLinkCommonResponse> batchDeleteLink(BatchDeleteLinkRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setBatchDeleteLink(request)
                .build();
                
        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasBatchLinkCommonResponse()) {
                        return response.getBatchLinkCommonResponse();
                    } else {
                        throw new RuntimeException("响应中不包含批量删除关联关系结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis()
        );
    }

} 