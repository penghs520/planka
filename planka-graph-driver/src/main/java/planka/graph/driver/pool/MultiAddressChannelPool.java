package planka.graph.driver.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.config.ZgraphClientConfig.ServerAddress;
import planka.graph.driver.loadbalancer.LoadBalancer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支持多地址的连接池
 * 内部管理多个单地址连接池，通过负载均衡器选择连接池
 */
public class MultiAddressChannelPool implements ChannelPool {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiAddressChannelPool.class);
    
    private final Map<ServerAddress, ChannelPool> poolMap;
    private final LoadBalancer loadBalancer;
    private final Bootstrap bootstrap;
    private final ChannelPoolHandler channelPoolHandler;
    private final ChannelHealthChecker healthChecker;
    private final FixedChannelPool.AcquireTimeoutAction acquireTimeoutAction;
    private final long acquireTimeoutMillis;
    private final int maxConnections;
    private final int maxPendingAcquires;
    
    public MultiAddressChannelPool(
            Bootstrap bootstrap,
            LoadBalancer loadBalancer,
            ChannelPoolHandler channelPoolHandler,
            ChannelHealthChecker healthChecker,
            FixedChannelPool.AcquireTimeoutAction acquireTimeoutAction,
            long acquireTimeoutMillis,
            int maxConnections,
            int maxPendingAcquires) {
        
        this.bootstrap = bootstrap;
        this.loadBalancer = loadBalancer;
        this.channelPoolHandler = channelPoolHandler;
        this.healthChecker = healthChecker;
        this.acquireTimeoutAction = acquireTimeoutAction;
        this.acquireTimeoutMillis = acquireTimeoutMillis;
        this.maxConnections = maxConnections;
        this.maxPendingAcquires = maxPendingAcquires;
        this.poolMap = new ConcurrentHashMap<>();
        
        initializePools();
    }
    
    private void initializePools() {
        for (ServerAddress serverAddress : loadBalancer.getServers()) {
            ChannelPool pool = createPoolForServer(serverAddress);
            poolMap.put(serverAddress, pool);
        }
    }
    
    private ChannelPool createPoolForServer(ServerAddress serverAddress) {
        Bootstrap serverBootstrap = bootstrap.clone()
                .remoteAddress(serverAddress.getHost(), serverAddress.getPort());
        
        return new FixedChannelPool(
                serverBootstrap,
                channelPoolHandler,
                healthChecker,
                acquireTimeoutAction,
                acquireTimeoutMillis,
                maxConnections,
                maxPendingAcquires
        );
    }
    
    @Override
    public Future<Channel> acquire() {
        return acquire(bootstrap.config().group().next().newPromise());
    }
    
    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        // 使用负载均衡器选择服务器
        ServerAddress selectedServer = loadBalancer.selectServer();
        ChannelPool selectedPool = poolMap.get(selectedServer);
        
        if (selectedPool == null) {
            promise.setFailure(new RuntimeException("未找到服务器 " + selectedServer + " 对应的连接池"));
            return promise;
        }

        // 从选中的连接池获取连接
        Future<Channel> channelFuture = selectedPool.acquire(promise);
        
        // 为连接添加服务器地址标识，便于后续释放时知道归还到哪个池
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                Channel channel = (Channel) future.get();
                // 在Channel的属性中存储服务器地址，用于释放时定位连接池
                channel.attr(SERVER_ADDRESS_KEY).set(selectedServer);
            }
        });
        
        return channelFuture;
    }
    
    @Override
    public Future<Void> release(Channel channel) {
        return release(channel, bootstrap.config().group().next().newPromise());
    }
    
    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        // 从Channel属性中获取服务器地址
        ServerAddress serverAddress = channel.attr(SERVER_ADDRESS_KEY).get();
        
        if (serverAddress == null) {
            promise.setFailure(new RuntimeException("无法确定连接对应的服务器地址"));
            return promise;
        }
        
        ChannelPool pool = poolMap.get(serverAddress);
        if (pool == null) {
            promise.setFailure(new RuntimeException("未找到服务器 " + serverAddress + " 对应的连接池"));
            return promise;
        }
        
        logger.debug("将连接释放回服务器 {} 的连接池", serverAddress);
        return pool.release(channel, promise);
    }
    
    @Override
    public void close() {
        for (Map.Entry<ServerAddress, ChannelPool> entry : poolMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception ignored) {
            }
        }
        
        poolMap.clear();
    }
    
    // 用于在Channel中存储服务器地址的AttributeKey
    private static final io.netty.util.AttributeKey<ServerAddress> SERVER_ADDRESS_KEY = 
            io.netty.util.AttributeKey.valueOf("serverAddress");
} 