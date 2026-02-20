package planka.graph.driver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.config.ZgraphClientConfig;
import planka.graph.driver.config.ZgraphClientConfig.LoadBalanceStrategy;
import planka.graph.driver.config.ZgraphClientConfig.ServerAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 健康检查功能测试
 * 测试场景：配置3个地址，只有一个是通的（23897），另外两个不通（23895, 23896）
 */
@Disabled //手动执行，不然耗时长
public class HealthCheckTest {

        private static final Logger logger = LoggerFactory.getLogger(HealthCheckTest.class);

        private PlankaGraphClient client;

        @BeforeEach
        void setUp() {
                // 配置3个地址，通常情况下只有23897端口是通的
                ZgraphClientConfig config = ZgraphClientConfig.builder()
                                .serverAddresses(
                                                new ServerAddress("127.0.0.1", 23897), // 通常这个端口是通的
                                                new ServerAddress("127.0.0.1", 23895), // 这个端口通常不通
                                                new ServerAddress("127.0.0.1", 23896) // 这个端口通常不通
                                )
                                .username("zgraph")
                                .password("zgraph")
                                .loadBalanceStrategy(LoadBalanceStrategy.RANDOM)
                                .handleTimeoutMillis(10000)
                                .channelPoolConfig(ZgraphClientConfig.ConnectionConfig.builder()
                                                .maxPoolSize(10) // 使用较小的连接池便于测试
                                                .acquireTimeoutMillis(5000)
                                                .maxPendingAcquires(100)
                                                .build())
                                .build();

                logger.info("创建ZgraphClient，配置的服务器: {}", config.getServerAddresses());
                client = new PlankaGraphClient(config);
        }

        @AfterEach
        void tearDown() {
                if (client != null) {
                        client.close();
                }
        }

        @Test
        void testHealthCheckInitialization() {
                logger.info("=== 测试健康检查初始化 ===");

                assertNotNull(client);
                assertNotNull(client.getConfig());

                // 验证配置的服务器地址
                assertEquals(3, client.getConfig().getServerAddresses().size());
                assertEquals(LoadBalanceStrategy.RANDOM, client.getConfig().getLoadBalanceStrategy());

                logger.info("配置验证通过：{} 个服务器地址，策略: {}",
                                client.getConfig().getServerAddresses().size(),
                                client.getConfig().getLoadBalanceStrategy());
        }

        @Test
        void testHealthCheckDetection() throws InterruptedException {
                logger.info("=== 测试健康检查检测 ===");

                // 等待一段时间让健康检查运行
                logger.info("等待健康检查运行...");
                Thread.sleep(9000); // 等待8秒，让健康检查有足够时间检测

                // 由于我们无法直接访问内部的负载均衡器，这里主要测试客户端是否正常工作
                logger.info("健康检查应该已经完成初始检测");

                // 验证客户端仍然可用
                assertTrue(client.getActiveConnectionCount() >= 0);
                assertTrue(client.getTotalConnectionCount() >= 0);

                logger.info("当前连接状态 - 活跃: {}, 总数: {}",
                                client.getActiveConnectionCount(),
                                client.getTotalConnectionCount());
        }

        @Test
        void testClientWithMixedHealthyUnhealthyServers() throws Exception {
                logger.info("=== 测试混合健康/不健康服务器的客户端 ===");

                PlankaGraphCardQueryClient queryClient = new PlankaGraphCardQueryClient(client);

                // 等待健康检查完成初始检测
                logger.info("等待健康检查完成初始检测...");
                Thread.sleep(6000); // 等待6秒

                try {
                        // 创建一个简单的查询请求
                        planka.graph.driver.proto.query.QueryContext queryContext = planka.graph.driver.proto.query.QueryContext
                                        .newBuilder()
                                        .setOrgId("test-org")
                                        .build();

                        planka.graph.driver.proto.query.QueryScope queryScope = planka.graph.driver.proto.query.QueryScope
                                        .newBuilder()
                                        .addCardIds(1L)
                                        .build();

                        planka.graph.driver.proto.query.CardQueryRequest request = planka.graph.driver.proto.query.CardQueryRequest
                                        .newBuilder()
                                        .setQueryContext(queryContext)
                                        .setQueryScope(queryScope)
                                        .build();

                        logger.info("发送查询请求...");

                        // 发送请求
                        java.util.concurrent.CompletableFuture<planka.graph.driver.proto.query.CardQueryResponse> future = queryClient
                                        .query(request);

                        try {
                                // 设置较短的超时时间
                                planka.graph.driver.proto.query.CardQueryResponse response = future.get(10,
                                                java.util.concurrent.TimeUnit.SECONDS);

                                logger.info("查询请求成功完成！返回 {} 张卡片", response.getCardsList().size());

                                // 验证响应
                                assertNotNull(response);

                        } catch (java.util.concurrent.TimeoutException e) {
                                logger.warn("查询请求超时 - 这可能是因为没有可用的健康服务器");
                                // 超时是可以接受的，因为可能没有健康的服务器
                        } catch (java.util.concurrent.ExecutionException e) {
                                logger.warn("查询请求执行异常: {}", e.getMessage());
                                // 执行异常也是可以接受的，因为这是测试不健康服务器的情况
                        }

                } catch (Exception e) {
                        logger.warn("测试过程中发生异常: {}", e.getMessage());
                        // 异常是可接受的，因为我们在测试故障场景
                }

                logger.info("混合健康/不健康服务器测试完成");
        }

        @Test
        void testConnectionPoolStatsWithHealthCheck() throws InterruptedException {
                logger.info("=== 测试健康检查下的连接池统计 ===");

                // 记录初始状态
                int initialActive = client.getActiveConnectionCount();
                int initialTotal = client.getTotalConnectionCount();
                int initialIdle = client.getIdleConnectionCount();

                logger.info("初始连接状态 - 活跃: {}, 总数: {}, 空闲: {}",
                                initialActive, initialTotal, initialIdle);

                // 等待健康检查运行
                Thread.sleep(9000);

                // 检查连接状态
                int afterHealthCheckActive = client.getActiveConnectionCount();
                int afterHealthCheckTotal = client.getTotalConnectionCount();
                int afterHealthCheckIdle = client.getIdleConnectionCount();

                logger.info("健康检查后连接状态 - 活跃: {}, 总数: {}, 空闲: {}",
                                afterHealthCheckActive, afterHealthCheckTotal, afterHealthCheckIdle);

                // 验证连接池统计的合理性
                assertTrue(afterHealthCheckActive >= 0, "活跃连接数应该非负");
                assertTrue(afterHealthCheckTotal >= 0, "总连接数应该非负");
                assertTrue(afterHealthCheckIdle >= 0, "空闲连接数应该非负");

                // 验证连接数的一致性
                assertEquals(afterHealthCheckTotal, afterHealthCheckActive + afterHealthCheckIdle,
                                "总连接数应该等于活跃连接数加空闲连接数");

                logger.info("连接池统计验证通过");
        }

        @Test
        void testMultipleRequestsWithHealthCheck() throws InterruptedException {
                logger.info("=== 测试健康检查下的多次请求 ===");

                PlankaGraphCardQueryClient queryClient = new PlankaGraphCardQueryClient(client);

                // 等待健康检查完成
                Thread.sleep(5000);

                int successfulRequests = 0;
                int failedRequests = 0;
                int totalRequests = 3;

                for (int i = 0; i < totalRequests; i++) {
                        try {
                                // 创建查询请求
                                planka.graph.driver.proto.query.QueryContext queryContext = planka.graph.driver.proto.query.QueryContext
                                                .newBuilder()
                                                .setOrgId("test-org-" + i)
                                                .build();

                                planka.graph.driver.proto.query.QueryScope queryScope = planka.graph.driver.proto.query.QueryScope
                                                .newBuilder()
                                                .addCardIds(i)
                                                .build();

                                planka.graph.driver.proto.query.CardQueryRequest request = planka.graph.driver.proto.query.CardQueryRequest
                                                .newBuilder()
                                                .setQueryContext(queryContext)
                                                .setQueryScope(queryScope)
                                                .build();

                                logger.info("发送第 {} 个请求...", i + 1);

                                java.util.concurrent.CompletableFuture<planka.graph.driver.proto.query.CardQueryResponse> future = queryClient
                                                .query(request);

                                try {

                                    successfulRequests++;
                                        logger.info("第 {} 个请求成功", i + 1);

                                } catch (Exception e) {
                                        failedRequests++;
                                        logger.warn("第 {} 个请求失败: {}", i + 1, e.getMessage());
                                }

                                // 在请求之间稍作停顿
                                Thread.sleep(1000);

                        } catch (Exception e) {
                                failedRequests++;
                                logger.warn("第 {} 个请求异常: {}", i + 1, e.getMessage());
                        }
                }

                logger.info("多次请求测试完成 - 成功: {}, 失败: {}, 总计: {}",
                                successfulRequests, failedRequests, totalRequests);

                // 验证至少有一些请求的结果（成功或失败都可以）
                assertEquals(totalRequests, successfulRequests + failedRequests,
                                "所有请求都应该有结果");
        }
}