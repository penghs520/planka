package cn.agilean.kanban.card.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zgraph.driver.ZgraphCardQueryClient;
import zgraph.driver.ZgraphClient;
import zgraph.driver.ZgraphWriteClient;
import zgraph.driver.config.ZgraphClientConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Zgraph 客户端配置
 */
@Configuration
public class ZgraphConfig {

    @Bean
    @ConfigurationProperties(prefix = "zgraph")
    public ZgraphProperties zgraphProperties() {
        return new ZgraphProperties();
    }

    @Bean
    public ZgraphClientConfig zgraphClientConfig(ZgraphProperties properties) {
        ZgraphClientConfig.Builder builder = ZgraphClientConfig.builder();

        // 配置服务器地址
        List<ZgraphClientConfig.ServerAddress> serverAddresses = new ArrayList<>();
        if (properties.getServers() != null && !properties.getServers().isEmpty()) {
            for (ZgraphProperties.ServerAddress server : properties.getServers()) {
                serverAddresses.add(new ZgraphClientConfig.ServerAddress(server.getHost(), server.getPort()));
            }
        } else {
            // 默认地址
            serverAddresses.add(new ZgraphClientConfig.ServerAddress("127.0.0.1", 3897));
        }
        builder.serverAddresses(serverAddresses);

        // 配置认证信息
        if (properties.getUsername() != null) {
            builder.username(properties.getUsername());
        }
        if (properties.getPassword() != null) {
            builder.password(properties.getPassword());
        }

        // 配置连接池
        if (properties.getConnection() != null) {
            ZgraphProperties.ConnectionConfig conn = properties.getConnection();
            ZgraphClientConfig.ConnectionConfig.ChannelPoolConfigBuilder poolBuilder = ZgraphClientConfig.ConnectionConfig
                    .builder();
            if (conn.getMaxPoolSize() > 0) {
                poolBuilder.maxPoolSize(conn.getMaxPoolSize());
            }
            if (conn.getAcquireTimeoutMillis() > 0) {
                poolBuilder.acquireTimeoutMillis(conn.getAcquireTimeoutMillis());
            }
            builder.channelPoolConfig(poolBuilder.build());
        }

        return builder.build();
    }

    @Bean(destroyMethod = "close")
    public ZgraphClient zgraphClient(ZgraphClientConfig config) {
        return new ZgraphClient(config);
    }

    @Bean
    public ZgraphCardQueryClient zgraphCardQueryClient(ZgraphClient client) {
        return new ZgraphCardQueryClient(client);
    }

    @Bean
    public ZgraphWriteClient zgraphWriteClient(ZgraphClient client) {
        return new ZgraphWriteClient(client);
    }

    /**
     * Zgraph 配置属性类
     */
    public static class ZgraphProperties {
        private List<ServerAddress> servers;
        private String username = "zgraph";
        private String password = "zgraph";
        private ConnectionConfig connection;

        public List<ServerAddress> getServers() {
            return servers;
        }

        public void setServers(List<ServerAddress> servers) {
            this.servers = servers;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public ConnectionConfig getConnection() {
            return connection;
        }

        public void setConnection(ConnectionConfig connection) {
            this.connection = connection;
        }

        public static class ServerAddress {
            private String host = "127.0.0.1";
            private int port = 3897;

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public int getPort() {
                return port;
            }

            public void setPort(int port) {
                this.port = port;
            }
        }

        public static class ConnectionConfig {
            private int maxPoolSize = 10;
            private int acquireTimeoutMillis = 5000;

            public int getMaxPoolSize() {
                return maxPoolSize;
            }

            public void setMaxPoolSize(int maxPoolSize) {
                this.maxPoolSize = maxPoolSize;
            }

            public int getAcquireTimeoutMillis() {
                return acquireTimeoutMillis;
            }

            public void setAcquireTimeoutMillis(int acquireTimeoutMillis) {
                this.acquireTimeoutMillis = acquireTimeoutMillis;
            }
        }
    }
}
