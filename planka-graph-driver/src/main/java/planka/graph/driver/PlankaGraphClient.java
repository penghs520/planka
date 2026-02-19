package planka.graph.driver;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.config.ZgraphClientConfig;
import planka.graph.driver.config.ZgraphClientConfig.ServerAddress;
import planka.graph.driver.exception.AuthenticationException;
import planka.graph.driver.handler.AuthChannelHandler;
import planka.graph.driver.handler.BusinessChannelHandler;
import planka.graph.driver.handler.IdleConnectionHandler;
import planka.graph.driver.handler.ZgraphChannelPoolHandler;
import planka.graph.driver.loadbalancer.HealthCheckLoadBalancer;
import planka.graph.driver.loadbalancer.LoadBalancer;
import planka.graph.driver.loadbalancer.LoadBalancerFactory;
import planka.graph.driver.pool.MultiAddressChannelPool;
import planka.graph.driver.pool.ZgraphChannelHealthChecker;
import planka.graph.driver.proto.auth.AuthRequest;
import planka.graph.driver.proto.request.Request;
import planka.graph.driver.proto.response.Response;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于Netty的Zgraph客户端
 * <p>
 * 特性：
 * 1. 使用Netty框架进行异步非阻塞通信
 * 2. 使用Netty官方连接池管理，复用TCP连接
 * 3. 使用LengthFieldBasedFrameDecoder处理帧拆包
 * 4. 支持超时控制和异常处理
 */
public class PlankaGraphClient {

    private static final Logger logger = LoggerFactory.getLogger(PlankaGraphClient.class);

    // 用于标记连接是否已认证的AttributeKey
    static final AttributeKey<Boolean> AUTHENTICATED_KEY = AttributeKey.valueOf("authenticated");

    private final ZgraphClientConfig config;
    private final EventLoopGroup group;
    private final ChannelPool channelPool;
    private final Bootstrap bootstrap;
    private volatile boolean closed = false;
    private LoadBalancer loadBalancer;

    // 连接统计计数器
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalCreatedConnections = new AtomicInteger(0);
    private final AtomicInteger aliveConnections = new AtomicInteger(0);

    // 连接池监控
    private ScheduledExecutorService monitorScheduler;
    private static final double WARNING_THRESHOLD = 0.8;

    public PlankaGraphClient() {
        this(ZgraphClientConfig.defaultConfig());
    }

    public PlankaGraphClient(ZgraphClientConfig config) {
        this.config = config;
        logger.info("zgraph配置：{}", config);

        this.group = new NioEventLoopGroup();
        this.bootstrap = initializeBootstrap();
        this.channelPool = createChannelPool();
        startConnectionPoolMonitor();
    }

    private ChannelPool createChannelPool() {
        List<ServerAddress> serverAddresses = config.getServerAddresses();
        ZgraphChannelPoolHandler poolHandler = new ZgraphChannelPoolHandler(
                activeConnections, totalCreatedConnections, aliveConnections);

        if (serverAddresses.size() == 1) {
            ServerAddress serverAddress = serverAddresses.get(0);
            logger.info("创建单地址连接池，服务器: {}", serverAddress);
            return new FixedChannelPool(
                    bootstrap.remoteAddress(serverAddress.getHost(), serverAddress.getPort()),
                    poolHandler,
                    ZgraphChannelHealthChecker.defaultChecker(),
                    FixedChannelPool.AcquireTimeoutAction.FAIL,
                    config.getConnectionConfig().getAcquireTimeoutMillis(),
                    config.getConnectionConfig().getMaxPoolSize(),
                    config.getConnectionConfig().getMaxPendingAcquires());
        } else {
            logger.info("创建多地址连接池，服务器列表: {}, 负载均衡策略: {}",
                    serverAddresses, config.getLoadBalanceStrategy());

            LoadBalancer lb = LoadBalancerFactory.createLoadBalancer(
                    config.getLoadBalanceStrategy(), serverAddresses, bootstrap);

            MultiAddressChannelPool multiPool = new MultiAddressChannelPool(
                    bootstrap, lb, poolHandler,
                    ZgraphChannelHealthChecker.defaultChecker(),
                    FixedChannelPool.AcquireTimeoutAction.FAIL,
                    config.getConnectionConfig().getAcquireTimeoutMillis(),
                    config.getConnectionConfig().getMaxPoolSize(),
                    config.getConnectionConfig().getMaxPendingAcquires());

            this.loadBalancer = lb;
            return multiPool;
        }
    }

    private void startConnectionPoolMonitor() {
        this.monitorScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "zgraph-pool-monitor");
            thread.setDaemon(true);
            return thread;
        });
        monitorScheduler.scheduleAtFixedRate(() -> {
            if (closed) {
                return;
            }

            int maxSize = config.getConnectionConfig().getMaxPoolSize();
            int activeCount = getActiveConnectionCount();
            int aliveCount = getAliveConnectionCount();
            int totalCreated = getTotalCreatedConnectionCount();
            double usageRatio = (double) activeCount / maxSize;

            logger.info("连接池状态: 活跃={}, 存活={}, 累计创建={}, 最大={}, 使用率={}%",
                    activeCount, aliveCount, totalCreated, maxSize,
                    String.format("%.1f", usageRatio * 100));

            if (usageRatio >= WARNING_THRESHOLD) {
                logger.warn("警告: 连接池使用率达到{}%, 接近最大容量 {}",
                        String.format("%.1f", usageRatio * 100), maxSize);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private Bootstrap initializeBootstrap() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionConfig().getAcquireTimeoutMillis())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // LengthFieldBasedFrameDecoder 和 LengthFieldPrepender
                        // 在 ZgraphChannelPoolHandler.channelCreated 中添加
                    }
                });
        return bootstrap;
    }

    /**
     * 对通道进行认证
     */
    private CompletableFuture<Boolean> authenticateChannel(Channel channel) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        AuthRequest authRequest = AuthRequest.newBuilder()
                .setUsername(config.getUsername())
                .setPassword(config.getPassword())
                .build();

        Request request = RequestBuilder.create()
                .setAuth(authRequest)
                .build();

        ChannelHandler authHandler = new AuthChannelHandler(
                channel, future, request,
                config.getConnectionConfig().getAcquireTimeoutMillis());

        channel.pipeline().addLast("authHandler", authHandler);

        try {
            sendRawRequest(channel, request);
        } catch (Exception e) {
            future.completeExceptionally(e);
            try {
                channel.pipeline().remove("authHandler");
            } catch (Exception ex) {
                // 忽略
            }
        }

        return future;
    }

    /**
     * 发送原始请求数据（LengthFieldPrepender 会自动添加长度前缀）
     */
    private void sendRawRequest(Channel channel, Request request) {
        byte[] requestBytes = request.toByteArray();
        channel.writeAndFlush(Unpooled.wrappedBuffer(requestBytes));
    }

    /**
     * 通用方法：发送请求并获取响应
     *
     * @param <T>               响应处理结果类型
     * @param request           请求消息
     * @param responseProcessor 响应处理器
     * @param timeout           请求超时时间(毫秒)
     * @return 包含处理结果的CompletableFuture
     */
    public <T> CompletableFuture<T> sendRequest(
            Request request,
            ResponseProcessor<T> responseProcessor,
            int timeout) {

        CompletableFuture<T> future = new CompletableFuture<>();

        if (closed) {
            future.completeExceptionally(new IllegalStateException("客户端已关闭"));
            return future;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("request:{}", request.getRequestId());
        }

        Future<Channel> channelFuture = channelPool.acquire();
        channelFuture.addListener((FutureListener<Channel>) cf -> {
            if (!cf.isSuccess()) {
                future.completeExceptionally(new RuntimeException("获取连接失败", cf.cause()));
                return;
            }

            Channel channel = cf.getNow();

            Boolean authenticated = channel.attr(AUTHENTICATED_KEY).get();
            if (authenticated == null || !authenticated) {
                authenticateChannel(channel).thenAccept(authSuccess -> {
                    if (authSuccess) {
                        channel.attr(AUTHENTICATED_KEY).set(true);

                        // 认证成功后添加空闲检测处理器
                        if (channel.pipeline().get("idleStateHandler") == null) {
                            channel.pipeline().addFirst("idleStateHandler",
                                    new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS));
                            channel.pipeline().addAfter("idleStateHandler", "idleConnectionHandler",
                                    IdleConnectionHandler.withDefaultStrategy());
                        }

                        doSendBusinessRequest(channel, request, responseProcessor, timeout, future);
                    } else {
                        channelPool.release(channel);
                        future.completeExceptionally(new AuthenticationException("连接认证失败"));
                    }
                }).exceptionally(authError -> {
                    channelPool.release(channel);
                    future.completeExceptionally(
                            new AuthenticationException("认证过程中发生异常: " + authError.getMessage(), authError));
                    return null;
                });
            } else {
                doSendBusinessRequest(channel, request, responseProcessor, timeout, future);
            }
        });

        return future;
    }

    /**
     * 发送业务请求
     */
    private <T> void doSendBusinessRequest(
            Channel channel,
            Request request,
            ResponseProcessor<T> responseProcessor,
            int timeout,
            CompletableFuture<T> future) {

        channel.pipeline().addLast("businessHandler",
                new BusinessChannelHandler<>(channel, request, responseProcessor, timeout, future, channelPool));

        try {
            sendRawRequest(channel, request);
            ZgraphChannelHealthChecker.markActive(channel);
        } catch (Exception e) {
            future.completeExceptionally(e);
            channelPool.release(channel);
        }
    }

    /**
     * 响应处理器接口，用于处理响应并转换为所需结果类型
     */
    @FunctionalInterface
    public interface ResponseProcessor<T> {
        T process(Response response) throws Exception;
    }

    public ZgraphClientConfig getConfig() {
        return config;
    }

    public void close() {
        closed = true;

        if (monitorScheduler != null) {
            monitorScheduler.shutdownNow();
        }

        if (loadBalancer instanceof HealthCheckLoadBalancer healthCheckLb) {
            healthCheckLb.close();
        }

        if (channelPool != null) {
            channelPool.close();
        }

        if (group != null) {
            group.shutdownGracefully();
        }
    }

    public int getActiveConnectionCount() {
        return activeConnections.get();
    }

    public int getIdleConnectionCount() {
        return Math.max(0, config.getConnectionConfig().getMaxPoolSize() - activeConnections.get());
    }

    public int getTotalConnectionCount() {
        return config.getConnectionConfig().getMaxPoolSize();
    }

    public int getTotalCreatedConnectionCount() {
        return totalCreatedConnections.get();
    }

    public int getTotalDestroyedConnectionCount() {
        return Math.max(0, totalCreatedConnections.get() - aliveConnections.get());
    }

    public int getAliveConnectionCount() {
        return aliveConnections.get();
    }
}
