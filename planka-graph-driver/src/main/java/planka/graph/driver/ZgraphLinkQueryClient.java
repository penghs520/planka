package planka.graph.driver;

import planka.graph.driver.proto.linkquery.LinkFetchRequest;
import planka.graph.driver.proto.linkquery.LinkQueryRequest;
import planka.graph.driver.proto.linkquery.LinkQueryResponse;
import planka.graph.driver.proto.request.Request;

import java.util.concurrent.CompletableFuture;

/**
 * @author penghs
 * 关联关系查询客户端
 * 负责处理所有与卡片查询相关的业务逻辑
 * 依赖ZgraphClient处理底层通信
 */
public class ZgraphLinkQueryClient {

    private final PlankaGraphClient client;

    /**
     * 构造函数
     *
     * @param client 底层通信客户端
     */
    public ZgraphLinkQueryClient(PlankaGraphClient client) {
        this.client = client;
    }


    /**
     * 发送关联查询请求
     *
     * @param request 关联查询请求
     */
    public CompletableFuture<LinkQueryResponse> queryLinks(LinkQueryRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setLinkQuery(request)
                .build();

        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasLinkQueryResponse()) {
                        return response.getLinkQueryResponse();
                    } else {
                        throw new RuntimeException("响应中不包含关联查询结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis()
        );
    }

    /**
     * 获取关联关系
     */
    public CompletableFuture<LinkQueryResponse> fetchLinks(LinkFetchRequest request) {
        Request fullRequest = RequestBuilder.create()
                .setLinkFetch(request)
                .build();
                
        return client.sendRequest(
                fullRequest,
                response -> {
                    if (response.hasLinkQueryResponse()) {
                        return response.getLinkQueryResponse();
                    } else {
                        throw new RuntimeException("响应中不包含关联查询结果");
                    }
                },
                client.getConfig().getHandleTimeoutMillis()
        );
    }

} 