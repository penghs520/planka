package planka.graph.driver;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.config.ZgraphClientConfig;
import planka.graph.driver.exception.RequestIdMismatchException;
import planka.graph.driver.proto.model.Card;
import planka.graph.driver.proto.query.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@Disabled // 这个测试需要依赖真实环境，在mvn test时不要自动执行
public class PlankaGraphClientTest {

        private static final Logger logger = LoggerFactory.getLogger(PlankaGraphClientTest.class);

        private PlankaGraphClient client;

        @BeforeEach
        void setUp() {
                // 使用测试配置，连接到实际运行的zgraph服务器
                ZgraphClientConfig config = ZgraphClientConfig.builder()
                                .serverAddresses(
                                                // new ZgraphClientConfig.ServerAddress("127.0.0.1", 3895),
                                                new ZgraphClientConfig.ServerAddress("127.0.0.1", 7009)
                                // new ZgraphClientConfig.ServerAddress("127.0.0.1", 3896)
                                )
                                .username("zgraph")
                                .password("zgraph")
                                .loadBalanceStrategy(ZgraphClientConfig.LoadBalanceStrategy.RANDOM)
                                .handleTimeoutMillis(10000)
                                .channelPoolConfig(ZgraphClientConfig.ConnectionConfig.builder()
                                                .maxPoolSize(100)
                                                .acquireTimeoutMillis(5000)
                                                .maxPendingAcquires(1000)
                                                .build())
                                .build();

                client = new PlankaGraphClient(config);
        }

        @AfterEach
        void tearDown() {
                if (client != null) {
                        client.close();
                }
        }

        @Test
        void testClientConfiguration() {
                assertNotNull(client);
                assertNotNull(client.getConfig());
                assertEquals(100, client.getConfig().getConnectionConfig().getMaxPoolSize());
        }

        @Test
        void testConnectionCounters() {
                // 初始状态下活跃连接数应该为0
                assertEquals(0, client.getActiveConnectionCount());

                // 总连接数应该等于最大连接数配置
                assertEquals(100, client.getTotalConnectionCount());

                // 空闲连接数应该等于最大连接数
                assertEquals(100, client.getIdleConnectionCount());
        }

        @Test
        void testClientClose() {
                assertNotNull(client);

                // 关闭客户端应该不抛出异常
                assertDoesNotThrow(() -> client.close());

                // 关闭后活跃连接数应该为0
                assertEquals(0, client.getActiveConnectionCount());
        }

        @Test
        void testClientCreationWithDefaultConfig() {
                PlankaGraphClient defaultClient = new PlankaGraphClient();
                assertNotNull(defaultClient);
                assertNotNull(defaultClient.getConfig());

                defaultClient.close();
        }

        // 测试单个查询请求
        @Test
        public void testCardQuery() throws ExecutionException, InterruptedException {
                System.out.println("\n===== 测试单个查询请求 =====");

                // 创建客户端并发送请求
                PlankaGraphCardQueryClient plankaGraphCardQueryClient = new PlankaGraphCardQueryClient(client);

                // 创建一个简单的CardQueryRequest
                QueryContext queryContext = QueryContext.newBuilder()
                                .setOrgId("test-org")
                                .build();

                QueryScope queryScope = QueryScope.newBuilder()
                                .addCardIds(1L)
                                .build();

                CardQueryRequest request = CardQueryRequest.newBuilder()
                                .setQueryContext(queryContext)
                                .setQueryScope(queryScope)
                                .build();

                System.out.println("创建请求: " + request);

                // 发送请求并获取结果
                CompletableFuture<CardQueryResponse> future = plankaGraphCardQueryClient.query(request);

                System.out.println("请求已发送，等待响应...");

                // 等待结果
                future.thenAccept(response -> {
                        List<Card> cards = response.getCardsList();
                        System.out.println("请求成功完成!");
                        System.out.println("获取到 " + cards.size() + " 张卡片");

                        // 打印卡片信息
                        for (int i = 0; i < cards.size(); i++) {
                                Card card = cards.get(i);
                                System.out.println("卡片 " + i + ": ID=" + card.getId() +
                                                ", 标题=" + card.getTitle() +
                                                ", 编码=" + card.getCodeInOrg());
                        }
                }).exceptionally(e -> {
                        System.out.println("请求失败: " + e.getMessage());
                        if (e.getCause() instanceof RequestIdMismatchException mismatchException) {
                                System.out.println("请求ID不匹配: 请求ID=" + mismatchException.getRequestId() +
                                                ", 响应请求ID=" + mismatchException.getResponseRequestId());
                        }
                        throw new RuntimeException(e);
                }).join(); // 等待结果完成
        }

        // 测试计数请求
        @Test
        public void testCardCount() throws Exception {
                System.out.println("\n===== 测试计数请求 =====");
                PlankaGraphCardQueryClient plankaGraphCardQueryClient = new PlankaGraphCardQueryClient(client);
                // 创建一个简单的CardCountRequest
                QueryContext queryContext = QueryContext.newBuilder()
                                .setOrgId("test-org")
                                .build();

                QueryScope queryScope = QueryScope.newBuilder()
                                .addCardTypeIds("vutId01")
                                .build();

                Condition condition = Condition.newBuilder().build();

                CardCountRequest request = CardCountRequest.newBuilder()
                                .setQueryContext(queryContext)
                                .setQueryScope(queryScope)
                                .setCondition(condition)
                                .build();

                System.out.println("创建计数请求: " + request);

                // 发送计数请求
                CompletableFuture<Integer> future = plankaGraphCardQueryClient.countCards(request);

                System.out.println("计数请求已发送，等待响应...");

                // 等待结果
                future.thenAccept(count -> {
                        System.out.println("计数请求成功完成!");
                        System.out.println("计数结果: " + count);
                }).exceptionally(e -> {
                        System.out.println("计数请求失败: " + e.getMessage());
                        if (e.getCause() instanceof RequestIdMismatchException mismatchException) {
                                System.out.println("请求ID不匹配: 请求ID=" + mismatchException.getRequestId() +
                                                ", 响应请求ID=" + mismatchException.getResponseRequestId());
                        }
                        e.printStackTrace();
                        throw new RuntimeException(e);
                }).join(); // 等待结果完成
        }

        // 测试请求ID校验
        @Test
        public void testRequestIdValidation() {
                System.out.println("\n===== 测试请求ID校验 =====");

                // 验证请求ID匹配的情况应该正常处理
                System.out.println("验证请求ID匹配的正常情况...");

                // 验证请求ID不匹配时应该抛出异常
                System.out.println("验证请求ID不匹配时应该抛出异常...");

                // 这部分需要通过模拟服务端或集成测试来实现
                // 在单元测试中我们可以验证异常类型是否正确定义
                RequestIdMismatchException exception = new RequestIdMismatchException(
                                "测试请求ID不匹配", "request-id-1", "different-response-id");

                Assertions.assertEquals("request-id-1", exception.getRequestId());
                Assertions.assertEquals("different-response-id", exception.getResponseRequestId());
                Assertions.assertEquals("测试请求ID不匹配", exception.getMessage());

                client.close();
        }

        // 测试连接空闲自动释放
        // @Test
        public void testConnectionIdleRelease() throws Exception {
                System.out.println("\n===== 测试连接空闲自动释放 =====");
                PlankaGraphCardQueryClient plankaGraphCardQueryClient = new PlankaGraphCardQueryClient(client);

                // 创建一个简单的请求
                QueryContext queryContext = QueryContext.newBuilder()
                                .setOrgId("test-org")
                                .build();

                QueryScope queryScope = QueryScope.newBuilder()
                                .addCardIds(1L)
                                .build();

                CardQueryRequest request = CardQueryRequest.newBuilder()
                                .setQueryContext(queryContext)
                                .setQueryScope(queryScope)
                                .build();

                // 记录初始连接状态
                int initialActiveCount = client.getActiveConnectionCount();
                int initialIdleCount = client.getIdleConnectionCount();
                int initialTotalCount = client.getTotalConnectionCount();
                int maxPoolSize = ZgraphClientConfig.defaultConfig().getConnectionConfig().getMaxPoolSize();

                System.out.println("初始状态: 总连接数=" + initialTotalCount +
                                ", 活跃连接数=" + initialActiveCount +
                                ", 空闲连接数=" + initialIdleCount);

                // 断言：初始状态总连接数不应超过最大连接数
                Assertions.assertTrue(
                                initialTotalCount <= maxPoolSize,
                                "初始总连接数不应超过最大连接数");

                // 断言：初始状态应该有预创建的空闲连接
                Assertions.assertTrue(
                                initialIdleCount > 0,
                                "应该有预创建的空闲连接");

                // 断言：初始状态活跃连接数应该为0
                Assertions.assertEquals(
                                0, initialActiveCount,
                                "初始状态活跃连接数应该为0");

                // 断言：总连接数应该等于活跃连接数加空闲连接数
                Assertions.assertEquals(
                                initialTotalCount, initialActiveCount + initialIdleCount,
                                "总连接数应该等于活跃连接数加空闲连接数");

                // 发送第一个请求
                System.out.println("发送第一个请求...");
                CompletableFuture<CardQueryResponse> future1 = plankaGraphCardQueryClient.query(request);

                try {
                        future1.get(); // 等待请求完成
                        System.out.println("第一个请求完成");
                } catch (Exception e) {
                        System.out.println("第一个请求失败: " + e.getMessage());
                        throw e; // 重新抛出异常，让测试失败
                }

                // 请求完成后立即检查连接状态
                int afterFirstRequestActiveCount = client.getActiveConnectionCount();
                int afterFirstRequestIdleCount = client.getIdleConnectionCount();
                int afterFirstRequestTotalCount = client.getTotalConnectionCount();

                System.out.println("第一个请求完成后（立即）: 总连接数=" + afterFirstRequestTotalCount +
                                ", 活跃连接数=" + afterFirstRequestActiveCount +
                                ", 空闲连接数=" + afterFirstRequestIdleCount);

                // 等待一小段时间确保连接完全释放
                Thread.sleep(100);

                int afterWaitActiveCount = client.getActiveConnectionCount();
                int afterWaitIdleCount = client.getIdleConnectionCount();
                int afterWaitTotalCount = client.getTotalConnectionCount();

                System.out.println("第一个请求完成后（等待后）: 总连接数=" + afterWaitTotalCount +
                                ", 活跃连接数=" + afterWaitActiveCount +
                                ", 空闲连接数=" + afterWaitIdleCount);

                // 验证连接池状态的一致性
                System.out.println("验证连接池状态一致性...");

                // 断言：第一个请求完成后，活跃连接数应该为0
                Assertions.assertEquals(
                                0, afterWaitActiveCount,
                                "第一个请求完成后，活跃连接数应该为0");

                // 断言：第一个请求完成后，总连接数不应改变（连接被重用）
                Assertions.assertEquals(
                                initialTotalCount, afterWaitTotalCount,
                                "第一个请求完成后，总连接数不应改变（连接被重用）");

                // 断言：第一个请求完成后，空闲连接数应该等于总连接数
                Assertions.assertEquals(
                                afterWaitTotalCount, afterWaitIdleCount,
                                "第一个请求完成后，空闲连接数应该等于总连接数");

                // 发送第二个请求，验证可以重用连接
                System.out.println("发送第二个请求，验证连接重用...");
                CompletableFuture<CardQueryResponse> future2 = plankaGraphCardQueryClient.query(request);

                try {
                        future2.get(); // 等待请求完成
                        System.out.println("第二个请求完成");
                } catch (Exception e) {
                        System.out.println("第二个请求失败: " + e.getMessage());
                        throw e; // 重新抛出异常，让测试失败
                }

                // 短暂等待确保连接释放
                Thread.sleep(100);

                // 记录第二个请求后的连接状态
                int afterSecondRequestActiveCount = client.getActiveConnectionCount();
                int afterSecondRequestIdleCount = client.getIdleConnectionCount();
                int afterSecondRequestTotalCount = client.getTotalConnectionCount();

                System.out.println("第二个请求完成后: 总连接数=" + afterSecondRequestTotalCount +
                                ", 活跃连接数=" + afterSecondRequestActiveCount +
                                ", 空闲连接数=" + afterSecondRequestIdleCount);

                // 验证连接复用
                System.out.println("验证连接池状态保持一致...");

                // 断言：第二个请求完成后，活跃连接数应该为0
                Assertions.assertEquals(
                                0, afterSecondRequestActiveCount,
                                "第二个请求完成后，活跃连接数应该为0");

                // 断言：第二个请求完成后，总连接数应该保持不变（连接被重用）
                Assertions.assertEquals(
                                initialTotalCount, afterSecondRequestTotalCount,
                                "第二个请求完成后，总连接数应该保持不变（连接被重用）");

                // 断言：第二个请求完成后，空闲连接数应该等于总连接数
                Assertions.assertEquals(
                                afterSecondRequestTotalCount, afterSecondRequestIdleCount,
                                "第二个请求完成后，空闲连接数应该等于总连接数");

                // 测试并发请求以验证连接池的正确性
                System.out.println("测试并发请求以验证连接池的正确性...");

                List<CompletableFuture<CardQueryResponse>> futures = new ArrayList<>();
                int concurrentRequestCount = Math.min(3, initialTotalCount); // 发送不超过连接池大小的并发请求

                for (int i = 0; i < concurrentRequestCount; i++) {
                        futures.add(plankaGraphCardQueryClient.query(request));
                }

                // 等待所有并发请求完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
                System.out.println("所有并发请求完成");

                // 短暂等待确保所有连接都被释放
                Thread.sleep(200);

                int afterConcurrentActiveCount = client.getActiveConnectionCount();
                int afterConcurrentIdleCount = client.getIdleConnectionCount();
                int afterConcurrentTotalCount = client.getTotalConnectionCount();

                System.out.println("并发请求完成后: 总连接数=" + afterConcurrentTotalCount +
                                ", 活跃连接数=" + afterConcurrentActiveCount +
                                ", 空闲连接数=" + afterConcurrentIdleCount);

                // 断言：并发请求完成后，活跃连接数应该为0
                Assertions.assertEquals(
                                0, afterConcurrentActiveCount,
                                "并发请求完成后，活跃连接数应该为0");

                // 断言：并发请求完成后，连接池状态应该保持一致
                Assertions.assertEquals(
                                afterConcurrentTotalCount, afterConcurrentIdleCount,
                                "并发请求完成后，空闲连接数应该等于总连接数");

                // 确保测试后连接池状态合理
                System.out.println("验证最终连接池状态...");

                // 断言：总连接数不应超过配置的最大值
                Assertions.assertTrue(
                                afterConcurrentTotalCount <= maxPoolSize,
                                "总连接数不应超过配置的最大连接数");

                // 断言：总连接数等于活跃连接数加空闲连接数
                Assertions.assertEquals(
                                afterConcurrentTotalCount,
                                afterConcurrentActiveCount + afterConcurrentIdleCount,
                                "总连接数应该等于活跃连接数加空闲连接数");

                System.out.println("连接池测试通过：连接能够正确创建、使用、释放和重用");
        }
}