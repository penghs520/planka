package planka.graph.driver.config;

import java.util.Arrays;
import java.util.List;

public class ZgraphClientConfig {

    // 服务器地址列表
    private final List<ServerAddress> serverAddresses;
    // 用户名
    private final String username;
    // 密码
    private final String password;
    // 处理请求超时时间（毫秒）
    private final int handleTimeoutMillis;
    // 连接池配置
    private final ConnectionConfig connectionConfig;
    // 负载均衡策略
    private final LoadBalanceStrategy loadBalanceStrategy;

    /**
     * 服务器地址信息
     */
    public static class ServerAddress {
        private final String host;
        private final int port;

        public ServerAddress(String host, int port) {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("host cannot be blank");
            }
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("invalid port: " + port);
            }
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServerAddress that = (ServerAddress) o;
            return port == that.port && host.equals(that.host);
        }

        @Override
        public int hashCode() {
            return host.hashCode() * 31 + port;
        }
    }

    /**
     * 负载均衡策略
     */
    public enum LoadBalanceStrategy {
        /** 随机选择 */
        RANDOM,
    }

    /**
     * 连接池配置类
     */
    public static class ConnectionConfig {
        // 最大连接数
        private final int maxPoolSize;
        // 从连接池获取连接的超时时间（毫秒）
        private final int acquireTimeoutMillis;
        // 最大等待获取连接的队列大小
        private final int maxPendingAcquires;

        public ConnectionConfig(int maxPoolSize, int acquireTimeoutMillis, int maxPendingAcquires) {
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("maxPoolSize must be greater than 0");
            }
            if (acquireTimeoutMillis <= 0) {
                throw new IllegalArgumentException("acquireTimeoutMillis must be greater than 0");
            }
            if (maxPendingAcquires <= 0) {
                throw new IllegalArgumentException("maxPendingAcquires must be greater than 0");
            }

            this.maxPoolSize = maxPoolSize;
            this.acquireTimeoutMillis = acquireTimeoutMillis;
            this.maxPendingAcquires = maxPendingAcquires;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public int getAcquireTimeoutMillis() {
            return acquireTimeoutMillis;
        }

        public int getMaxPendingAcquires() {
            return maxPendingAcquires;
        }

        @Override
        public String toString() {
            return "ChannelPoolConfig{" +
                    "maxPoolSize=" + maxPoolSize +
                    ", acquireTimeoutMillis=" + acquireTimeoutMillis +
                    ", maxPendingAcquires=" + maxPendingAcquires +
                    '}';
        }

        /**
         * 创建默认连接池配置
         */
        public static ConnectionConfig defaultConfig() {
            return new ConnectionConfig(
                    100,                          // maxPoolSize
                    30 * 1000,                    // acquireTimeoutMillis (30秒)
                    1000                         // maxPendingAcquires
                    // acquireTimeoutAction
            );
        }

        /**
         * 创建连接池配置构建器
         */
        public static ChannelPoolConfigBuilder builder() {
            return new ChannelPoolConfigBuilder();
        }

        /**
         * 连接池配置构建器
         */
        public static class ChannelPoolConfigBuilder {
            private int maxPoolSize = 100;
            private int acquireTimeoutMillis = 30 * 1000;
            private int maxPendingAcquires = 1000;

            public ChannelPoolConfigBuilder maxPoolSize(int maxPoolSize) {
                this.maxPoolSize = maxPoolSize;
                return this;
            }

            public ChannelPoolConfigBuilder acquireTimeoutMillis(int acquireTimeoutMillis) {
                this.acquireTimeoutMillis = acquireTimeoutMillis;
                return this;
            }

            public ChannelPoolConfigBuilder maxPendingAcquires(int maxPendingAcquires) {
                this.maxPendingAcquires = maxPendingAcquires;
                return this;
            }

            public ConnectionConfig build() {
                return new ConnectionConfig(maxPoolSize, acquireTimeoutMillis,
                        maxPendingAcquires);
            }
        }
    }

    public static ZgraphClientConfig defaultConfig() {
        return new ZgraphClientConfig(
                List.of(new ServerAddress("127.0.0.1", 3897)),
                "zgraph", 
                "zgraph",
                60 * 1000,
                ConnectionConfig.defaultConfig(),
                LoadBalanceStrategy.RANDOM);
    }

    @Override
    public String toString() {
        return "ZgraphClientConfig{" +
                "serverAddresses=" + serverAddresses +
                ", handleTimeoutMillis=" + handleTimeoutMillis +
                ", connectionConfig=" + connectionConfig +
                ", loadBalanceStrategy=" + loadBalanceStrategy +
                '}';
    }

    /**
     * 创建一个新的 Builder 实例
     *
     * @return 配置构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public ZgraphClientConfig(List<ServerAddress> serverAddresses,
                              String username,
                              String password,
                              int handleTimeoutMillis,
                              ConnectionConfig connectionConfig,
                              LoadBalanceStrategy loadBalanceStrategy) {
        if (serverAddresses == null || serverAddresses.isEmpty()) {
            throw new IllegalArgumentException("serverAddresses cannot be empty");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username cannot be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password cannot be blank");
        }
        if (handleTimeoutMillis <= 0) {
            throw new IllegalArgumentException("handleTimeoutMillis must be greater than 0");
        }
        if (connectionConfig == null) {
            throw new IllegalArgumentException("connectionConfig cannot be null");
        }
        if (loadBalanceStrategy == null) {
            throw new IllegalArgumentException("loadBalanceStrategy cannot be null");
        }

        this.serverAddresses = serverAddresses;
        this.handleTimeoutMillis = handleTimeoutMillis;
        this.username = username;
        this.password = password;
        this.connectionConfig = connectionConfig;
        this.loadBalanceStrategy = loadBalanceStrategy;
    }


    public List<ServerAddress> getServerAddresses() {
        return serverAddresses;
    }

    public int getHandleTimeoutMillis() {
        return handleTimeoutMillis;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public LoadBalanceStrategy getLoadBalanceStrategy() {
        return loadBalanceStrategy;
    }

    /**
     * ZgraphClientConfig 的构建器类
     * 提供流式API创建配置对象
     */
    public static class Builder {
        // 默认值
        private List<ServerAddress> serverAddresses = Arrays.asList(new ServerAddress("127.0.0.1", 3897));
        private String username = "zgraph";
        private String password = "zgraph";
        private int handleTimeoutMillis = 60 * 1000;
        private ConnectionConfig connectionConfig = ConnectionConfig.defaultConfig();
        private LoadBalanceStrategy loadBalanceStrategy = LoadBalanceStrategy.RANDOM;

        private Builder() {
            // 私有构造函数，通过 ZgraphClientConfig.builder() 创建
        }

        /**
         * 设置服务器地址列表
         *
         * @param serverAddresses 服务器地址列表
         * @return Builder实例
         */
        public Builder serverAddresses(List<ServerAddress> serverAddresses) {
            this.serverAddresses = serverAddresses;
            return this;
        }

        /**
         * 设置服务器地址列表
         *
         * @param serverAddresses 服务器地址数组
         * @return Builder实例
         */
        public Builder serverAddresses(ServerAddress... serverAddresses) {
            this.serverAddresses = Arrays.asList(serverAddresses);
            return this;
        }

        /**
         * 设置处理请求超时时间（毫秒）
         *
         * @param handleTimeoutMillis 处理请求超时时间
         * @return Builder实例
         */
        public Builder handleTimeoutMillis(int handleTimeoutMillis) {
            this.handleTimeoutMillis = handleTimeoutMillis;
            return this;
        }

        /**
         * 设置用户名
         *
         * @param username 用户名
         * @return Builder实例
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * 设置密码
         *
         * @param password 密码
         * @return Builder实例
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * 设置连接池配置
         *
         * @param connectionConfig 连接池配置
         * @return Builder实例
         */
        public Builder channelPoolConfig(ConnectionConfig connectionConfig) {
            this.connectionConfig = connectionConfig;
            return this;
        }

        /**
         * 设置负载均衡策略
         *
         * @param loadBalanceStrategy 负载均衡策略
         * @return Builder实例
         */
        public Builder loadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) {
            this.loadBalanceStrategy = loadBalanceStrategy;
            return this;
        }

        /**
         * 构建 ZgraphClientConfig 实例
         *
         * @return 完整配置的 ZgraphClientConfig 对象
         */
        public ZgraphClientConfig build() {
            return new ZgraphClientConfig(
                    serverAddresses,
                    username,
                    password,
                    handleTimeoutMillis,
                    connectionConfig,
                    loadBalanceStrategy
            );
        }
    }
}

