package planka.graph.driver.loadbalancer;

import planka.graph.driver.config.ZgraphClientConfig.ServerAddress;

import java.util.List;
import java.util.Random;

/**
 * 负载均衡器接口
 */
public interface LoadBalancer {
    
    /**
     * 选择一个服务器地址
     * 
     * @return 选中的服务器地址
     */
    ServerAddress selectServer();

    /**
     * 根据传入的服务器地址，选择一个服务器地址
     * @param serverAddresses 提供的服务器地址
     * @return 选中的服务器地址
     */
    ServerAddress selectServer(List<ServerAddress> serverAddresses);

    /**
     * 获取所有可用的服务器
     * 
     * @return 可用服务器列表
     */
    List<ServerAddress> getServers();

}

/**
 * 随机负载均衡器
 */
class RandomLoadBalancer implements LoadBalancer {
    private final List<ServerAddress> servers;
    private final Random random = new Random();
    
    public RandomLoadBalancer(List<ServerAddress> servers) {
        if (servers == null || servers.isEmpty()) {
            throw new IllegalArgumentException("服务器列表不能为空");
        }
        this.servers = servers;
    }
    
    @Override
    public ServerAddress selectServer() {
        if (servers.isEmpty()) {
            throw new RuntimeException("没有可用的服务器");
        }
        
        int index = random.nextInt(servers.size());
        return servers.get(index);
    }

    @Override
    public ServerAddress selectServer(List<ServerAddress> serverAddresses) {
        if (serverAddresses.isEmpty()) {
            throw new RuntimeException("没有可用的服务器");
        }
        int index = random.nextInt(serverAddresses.size());
        return serverAddresses.get(index);
    }

    @Override
    public List<ServerAddress> getServers() {
        return servers;
    }
}

