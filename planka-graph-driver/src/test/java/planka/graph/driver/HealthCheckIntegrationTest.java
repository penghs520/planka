package planka.graph.driver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.config.ZgraphClientConfig;
import planka.graph.driver.proto.auth.AuthResponse;
import planka.graph.driver.proto.request.Request;
import planka.graph.driver.proto.response.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 健康检查集成测试
 * 测试场景：
 * 1. 连接空闲30秒后自动关闭并重建
 * 2. 连接空闲60秒后健康检查失败
 * 3. 正常请求流程中的活跃时间更新
 * 4. 多次请求复用连接
 */
@DisplayName("健康检查集成测试")
@Disabled //手动执行，不然耗时长
class HealthCheckIntegrationTest {

    private static final int TEST_PORT = 29001; // 测试端口
    private MockZgraphServer mockServer;
    private PlankaGraphClient client;

    @BeforeEach
    void setUp() throws Exception {
        // 启动模拟服务器
        mockServer = new MockZgraphServer(TEST_PORT);
        mockServer.start();

        // 创建客户端配置
        ZgraphClientConfig config = ZgraphClientConfig.builder()
                .serverAddresses(new ZgraphClientConfig.ServerAddress("127.0.0.1", TEST_PORT))
                .username("test")
                .password("test")
                .handleTimeoutMillis(5000)
                .build();

        // 创建客户端
        client = new PlankaGraphClient(config);

        // 等待客户端初始化
        Thread.sleep(500);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        if (mockServer != null) {
            mockServer.stop();
        }
        // 等待资源释放
        Thread.sleep(500);
    }

    @Test
    @DisplayName("测试正常请求流程")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testNormalRequest() throws Exception {
        // When: 发送第一次请求
        int initialConnections = mockServer.getConnectionCount();
        sendTestRequest();

        // Then: 应该创建一个连接
        assertEquals(initialConnections + 1, mockServer.getConnectionCount());

        // When: 短时间内发送第二次请求
        Thread.sleep(1000);
        sendTestRequest();

        // Then: 应该复用连接，连接数不变
        assertEquals(initialConnections + 1, mockServer.getConnectionCount());
    }

    @Test
    @DisplayName("测试空闲30秒后连接关闭")
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    void testIdleConnectionClosedAfter30Seconds() throws Exception {
        // Given: 发送一次请求建立连接
        sendTestRequest();
        int initialConnections = mockServer.getConnectionCount();

        // When: 等待35秒（超过30秒空闲阈值）
        System.out.println("等待35秒测试空闲连接关闭...");
        Thread.sleep(35_000);

        // Then: 空闲连接应该被关闭
        // 注意：连接关闭后，服务端的连接数会减少
        int currentConnections = mockServer.getConnectionCount();
        assertTrue(currentConnections < initialConnections,
                "空闲连接应该被关闭。预期小于 " + initialConnections + ", 实际: " + currentConnections);

        // When: 再次发送请求
        sendTestRequest();

        // Then: 应该创建新连接
        assertTrue(mockServer.getConnectionCount() >= currentConnections,
                "应该创建新连接");
    }

    @Test
    @DisplayName("测试连续请求保持连接活跃")
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    void testContinuousRequestsKeepConnectionAlive() throws Exception {
        // Given: 发送第一次请求
        sendTestRequest();
        int initialConnections = mockServer.getConnectionCount();

        // When: 每10秒发送一次请求，持续35秒
        for (int i = 0; i < 3; i++) {
            Thread.sleep(10_000);
            sendTestRequest();
        }

        // Then: 连接数应该保持不变（因为一直有活动）
        assertEquals(initialConnections, mockServer.getConnectionCount(),
                "连续活动应该保持连接不被关闭");
    }

    @Test
    @DisplayName("测试多个客户端连接")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMultipleClientConnections() throws Exception {
        // Given: 创建第二个客户端
        ZgraphClientConfig config2 = ZgraphClientConfig.builder()
                .serverAddresses(new ZgraphClientConfig.ServerAddress("127.0.0.1", TEST_PORT))
                .username("test2")
                .password("test2")
                .build();
        PlankaGraphClient client2 = new PlankaGraphClient(config2);
        Thread.sleep(500);

        int initialConnections = mockServer.getConnectionCount();

        // When: 两个客户端同时发送请求
        sendTestRequest();
        sendTestRequestWithClient(client2);

        // Then: 应该有两个连接
        assertTrue(mockServer.getConnectionCount() >= initialConnections + 2);

        // Cleanup
        client2.close();
    }

    @Test
    @DisplayName("测试客户端关闭后连接释放")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testClientCloseReleasesConnections() throws Exception {
        // Given: 发送请求建立连接
        sendTestRequest();
        int connectionsBeforeClose = mockServer.getConnectionCount();
        assertTrue(connectionsBeforeClose > 0);

        // When: 关闭客户端
        client.close();
        client = null; // 避免 tearDown 再次关闭
        Thread.sleep(1000);

        // Then: 连接应该被关闭
        int connectionsAfterClose = mockServer.getConnectionCount();
        assertTrue(connectionsAfterClose < connectionsBeforeClose,
                "客户端关闭后连接应该被释放");
    }

    /**
     * 发送测试请求的辅助方法
     */
    private void sendTestRequest() throws Exception {
        sendTestRequestWithClient(client);
    }

    private void sendTestRequestWithClient(PlankaGraphClient testClient) throws Exception {
        Request request = Request.newBuilder()
                .setRequestId("test-" + System.currentTimeMillis())
                .build();

        CompletableFuture<String> future = testClient.sendRequest(
                request,
                response -> "success",
                5000
        );

        String result = future.get(5, TimeUnit.SECONDS);
        assertEquals("success", result);
    }

    /**
     * 模拟 Zgraph 服务器
     */
    private static class MockZgraphServer {
        private static final Logger logger = LoggerFactory.getLogger(MockZgraphServer.class);
        private final int port;
        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;
        private Channel serverChannel;
        private final AtomicInteger connectionCount = new AtomicInteger(0);

        public MockZgraphServer(int port) {
            this.port = port;
        }

        public void start() throws InterruptedException {
            logger.info("MockZgraphServer 正在启动，端口: {}", port);
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            connectionCount.incrementAndGet();
                            ch.pipeline().addLast(new MockServerHandler());

                            // 监听连接关闭事件
                            ch.closeFuture().addListener(future -> connectionCount.decrementAndGet());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverChannel = bootstrap.bind(port).sync().channel();
            logger.info("MockZgraphServer 启动成功，端口: {}", port);
        }

        public void stop() throws InterruptedException {
            logger.info("MockZgraphServer 正在停止");
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            logger.info("MockZgraphServer 已停止");
        }

        public int getConnectionCount() {
            return connectionCount.get();
        }
    }

    /**
     * 模拟服务器处理器
     */
    private static class MockServerHandler extends ChannelInboundHandlerAdapter {
        private static final Logger logger = LoggerFactory.getLogger(MockServerHandler.class);
        private final ByteBuf buffer = Unpooled.buffer();
        private int expectedLength = -1;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("MockServer: 客户端连接建立 - {}", ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            try {

                // 累积接收的数据
                buffer.writeBytes(byteBuf);

                logger.info("MockServer收到数据: {} 字节, 累积: {} 字节", byteBuf.readableBytes(), buffer.readableBytes());

                // 持续处理buffer中的消息，直到数据不足
                while (true) {
                    // 解析消息长度
                    if (expectedLength == -1) {
                        if (buffer.readableBytes() < 4) {
                            break; // 等待更多数据
                        }
                        expectedLength = buffer.readInt();
                        logger.info("MockServer解析消息长度: {}", expectedLength);
                    }

                    // 检查是否接收完整消息
                    if (buffer.readableBytes() >= expectedLength) {
                        logger.info("MockServer收到完整消息，开始处理");
                        byte[] requestBytes = new byte[expectedLength];
                        buffer.readBytes(requestBytes);

                        // 解析请求
                        Request request = Request.parseFrom(requestBytes);
                        logger.info("MockServer解析请求成功: {}", request.getRequestId());

                        // 创建响应
                        Response.Builder responseBuilder = Response.newBuilder()
                                .setRequestId(request.getRequestId())
                                .setCode(200)
                                .setMessage("OK");

                        // 如果是认证请求，添加认证响应
                        if (request.hasAuth()) {
                            AuthResponse authResponse = AuthResponse.newBuilder()
                                    .setSuccess(true)
                                    .setMessage("Authentication successful")
                                    .build();
                            responseBuilder.setAuthResponse(authResponse);
                            logger.info("MockServer处理认证请求");
                        }

                        Response response = responseBuilder.build();

                        // 发送响应
                        byte[] responseBytes = response.toByteArray();
                        ByteBuf responseBuf = Unpooled.buffer(4 + responseBytes.length);
                        responseBuf.writeInt(responseBytes.length);
                        responseBuf.writeBytes(responseBytes);

                        ctx.writeAndFlush(responseBuf);
                        logger.info("MockServer发送响应: {} 字节", responseBytes.length);

                        // 重置状态准备下一条消息
                        expectedLength = -1;
                    } else {
                        // 数据不足，等待更多数据
                        break;
                    }
                }
            } finally {
                // 释放接收到的ByteBuf
                byteBuf.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("MockServer异常: {}", cause.getMessage(), cause);
            // 不立即关闭连接，让客户端有机会读取响应
            // ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.info("MockServer: 客户端连接断开 - {}", ctx.channel().remoteAddress());
            if (buffer != null) {
                buffer.release();
            }
        }
    }
}
