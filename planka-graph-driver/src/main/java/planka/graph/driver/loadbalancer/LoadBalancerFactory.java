package planka.graph.driver.loadbalancer;

import io.netty.bootstrap.Bootstrap;
import planka.graph.driver.config.ZgraphClientConfig;
import planka.graph.driver.config.ZgraphClientConfig.ServerAddress;

import java.util.List;

/**
 * 负载均衡器工厂
 */
public class LoadBalancerFactory {
    
    private static LoadBalancer createLoadBalancer(
            ZgraphClientConfig.LoadBalanceStrategy strategy,
            List<ServerAddress> servers) {
        return switch (strategy) {
            case RANDOM -> new RandomLoadBalancer(servers);
        };
    }
    
    /**
     * 创建带健康检查的负载均衡器
     * 
     * @param strategy 负载均衡策略
     * @param servers 服务器列表
     * @param bootstrap Netty Bootstrap，用于健康检查
     * @return 负载均衡器
     */
    public static LoadBalancer createLoadBalancer(
            ZgraphClientConfig.LoadBalanceStrategy strategy,
            List<ServerAddress> servers,
            Bootstrap bootstrap) {
        
        LoadBalancer baseLoadBalancer = createLoadBalancer(strategy, servers);
        // 健康检查配置
        long healthCheckIntervalMs = 5000;  // 5秒检查一次
        long connectionTimeoutMs = 2000;    // 2秒连接超时
        return new HealthCheckLoadBalancer(
                baseLoadBalancer,
                bootstrap,
                healthCheckIntervalMs,
                connectionTimeoutMs
        );
    }

} 