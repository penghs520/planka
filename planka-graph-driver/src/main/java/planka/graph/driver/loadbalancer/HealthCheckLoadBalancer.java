package planka.graph.driver.loadbalancer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.config.ZgraphClientConfig.ServerAddress;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 支持健康检查的负载均衡器
 * 功能：
 * 1. 实时监控Zgraph服务端健康状况
 * 2. 自动排除不健康的Zgraph服务端
 * 3. 定期探测已故障的Zgraph服务端，自动恢复
 * 4. 支持多种负载均衡策略
 */
public class HealthCheckLoadBalancer implements LoadBalancer {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckLoadBalancer.class);

    private final LoadBalancer delegate;
    private final Map<ServerAddress, ServerHealth> serverHealthMap;
    private final ScheduledExecutorService healthCheckExecutor;
    private final Bootstrap bootstrap;

    // 健康检查配置
    private final long healthCheckIntervalMs;
    private final long connectionTimeoutMs;

    public HealthCheckLoadBalancer(LoadBalancer delegate,
                                   Bootstrap bootstrap,
                                   long healthCheckIntervalMs,
                                   long connectionTimeoutMs) {
        this.delegate = delegate;
        this.bootstrap = bootstrap;
        this.healthCheckIntervalMs = healthCheckIntervalMs;
        this.connectionTimeoutMs = connectionTimeoutMs;

        this.serverHealthMap = new ConcurrentHashMap<>();

        for (ServerAddress serverAddress : delegate.getServers()) {
            serverHealthMap.put(serverAddress, new ServerHealth(serverAddress));
        }

        this.healthCheckExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "zgraph-health-check");
            thread.setDaemon(true);
            return thread;
        });

        // 启动时初次检查
        performHealthCheck();

        // 启动定期健康检查
        startHealthCheck();
    }

    private void startHealthCheck() {
        healthCheckExecutor.scheduleWithFixedDelay(
                this::performHealthCheck,
                0,
                healthCheckIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    private void performHealthCheck() {
        for (ServerHealth serverHealth : serverHealthMap.values()) {
            checkServerHealth(serverHealth);
        }
    }

    private void checkServerHealth(ServerHealth serverHealth) {
        ServerAddress server = serverHealth.getServerAddress();

        Bootstrap checkBootstrap = bootstrap.clone()
                .remoteAddress(server.getHost(), server.getPort());

        ChannelFuture connectFuture = checkBootstrap.connect();

        // 设置连接超时
        connectFuture.awaitUninterruptibly(connectionTimeoutMs, TimeUnit.MILLISECONDS);

        if (connectFuture.isSuccess()) {
            // 连接成功，关闭连接
            Channel channel = connectFuture.channel();
            channel.close();
            serverHealth.markAvailable();
        } else {
            logger.error("Zgraph连接异常: {}:{}", server.getHost(), server.getPort());
            serverHealth.markUnavailable();
        }
    }

    @Override
    public ServerAddress selectServer() {
        List<ServerAddress> healthyServers = getHealthyServers();
        if (healthyServers.isEmpty()) {
            logger.warn("没有可达的Zgraph服务端可用！使用默认负载均衡器返回随机一个...");
            return delegate.selectServer();
        }
        return delegate.selectServer(healthyServers);
    }

    @Override
    public ServerAddress selectServer(List<ServerAddress> serverAddresses) {
        return delegate.selectServer(serverAddresses);
    }

    private List<ServerAddress> getHealthyServers() {
        return serverHealthMap.values().stream()
                .filter(ServerHealth::isAvailable)
                .map(ServerHealth::getServerAddress)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServerAddress> getServers() {
        return delegate.getServers();
    }
    /**
     * 关闭健康检查
     */
    public void close() {
        healthCheckExecutor.shutdown();
        try {
            if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Zgraph服务端健康状态管理
     */
    private class ServerHealth {
        private final ServerAddress serverAddress;
        private volatile boolean available = false;

        public ServerHealth(ServerAddress serverAddress) {
            this.serverAddress = serverAddress;
        }

        public ServerAddress getServerAddress() {
            return serverAddress;
        }


        public void markUnavailable() {
            available = false;
        }

        public void markAvailable() {
            available = true;
        }

        public boolean isAvailable() {
            return available;
        }

        @Override
        public String toString() {
            return "ServerHealth{" +
                    "serverAddress=" + serverAddress +
                    ", available=" + available +
                    '}';
        }
    }
} 