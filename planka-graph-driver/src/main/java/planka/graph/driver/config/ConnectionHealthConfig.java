package planka.graph.driver.config;

/**
 * 连接健康检查配置
 *
 * 用于配置连接池的健康检查和心跳策略
 */
public class ConnectionHealthConfig {

    // 连接最大空闲时间（毫秒），超过此时间视为不健康
    private final long maxIdleTimeMillis;

    // 是否启用应用层心跳
    private final boolean enableHeartbeat;

    // 心跳间隔（毫秒）
    private final long heartbeatIntervalMillis;

    // 是否启用 PING 验证
    private final boolean enablePingValidation;

    // PING 超时时间（毫秒）
    private final long pingTimeoutMillis;

    public ConnectionHealthConfig(long maxIdleTimeMillis,
                                   boolean enableHeartbeat,
                                   long heartbeatIntervalMillis,
                                   boolean enablePingValidation,
                                   long pingTimeoutMillis) {
        this.maxIdleTimeMillis = maxIdleTimeMillis;
        this.enableHeartbeat = enableHeartbeat;
        this.heartbeatIntervalMillis = heartbeatIntervalMillis;
        this.enablePingValidation = enablePingValidation;
        this.pingTimeoutMillis = pingTimeoutMillis;
    }

    /**
     * 默认配置
     * - 最大空闲时间: 60秒
     * - 启用心跳: 是
     * - 心跳间隔: 30秒
     * - PING验证: 否（需要协议支持）
     */
    public static ConnectionHealthConfig defaultConfig() {
        return new ConnectionHealthConfig(
                60_000,   // maxIdleTimeMillis
                true,     // enableHeartbeat
                30_000,   // heartbeatIntervalMillis
                false,    // enablePingValidation
                5_000     // pingTimeoutMillis
        );
    }

    /**
     * 高可用配置（更激进的检查）
     * - 最大空闲时间: 30秒
     * - 启用心跳: 是
     * - 心跳间隔: 15秒
     * - PING验证: 是
     */
    public static ConnectionHealthConfig highAvailabilityConfig() {
        return new ConnectionHealthConfig(
                30_000,   // maxIdleTimeMillis
                true,     // enableHeartbeat
                15_000,   // heartbeatIntervalMillis
                true,     // enablePingValidation
                3_000     // pingTimeoutMillis
        );
    }

    /**
     * 低开销配置（适用于稳定网络环境）
     * - 最大空闲时间: 120秒
     * - 启用心跳: 否
     * - PING验证: 否
     */
    public static ConnectionHealthConfig lowOverheadConfig() {
        return new ConnectionHealthConfig(
                120_000,  // maxIdleTimeMillis
                false,    // enableHeartbeat
                0,        // heartbeatIntervalMillis
                false,    // enablePingValidation
                0         // pingTimeoutMillis
        );
    }

    public long getMaxIdleTimeMillis() {
        return maxIdleTimeMillis;
    }

    public boolean isEnableHeartbeat() {
        return enableHeartbeat;
    }

    public long getHeartbeatIntervalMillis() {
        return heartbeatIntervalMillis;
    }

    public boolean isEnablePingValidation() {
        return enablePingValidation;
    }

    public long getPingTimeoutMillis() {
        return pingTimeoutMillis;
    }

    @Override
    public String toString() {
        return "ConnectionHealthConfig{" +
                "maxIdleTimeMillis=" + maxIdleTimeMillis +
                ", enableHeartbeat=" + enableHeartbeat +
                ", heartbeatIntervalMillis=" + heartbeatIntervalMillis +
                ", enablePingValidation=" + enablePingValidation +
                ", pingTimeoutMillis=" + pingTimeoutMillis +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long maxIdleTimeMillis = 60_000;
        private boolean enableHeartbeat = true;
        private long heartbeatIntervalMillis = 30_000;
        private boolean enablePingValidation = false;
        private long pingTimeoutMillis = 5_000;

        public Builder maxIdleTimeMillis(long maxIdleTimeMillis) {
            this.maxIdleTimeMillis = maxIdleTimeMillis;
            return this;
        }

        public Builder enableHeartbeat(boolean enableHeartbeat) {
            this.enableHeartbeat = enableHeartbeat;
            return this;
        }

        public Builder heartbeatIntervalMillis(long heartbeatIntervalMillis) {
            this.heartbeatIntervalMillis = heartbeatIntervalMillis;
            return this;
        }

        public Builder enablePingValidation(boolean enablePingValidation) {
            this.enablePingValidation = enablePingValidation;
            return this;
        }

        public Builder pingTimeoutMillis(long pingTimeoutMillis) {
            this.pingTimeoutMillis = pingTimeoutMillis;
            return this;
        }

        public ConnectionHealthConfig build() {
            return new ConnectionHealthConfig(
                    maxIdleTimeMillis,
                    enableHeartbeat,
                    heartbeatIntervalMillis,
                    enablePingValidation,
                    pingTimeoutMillis
            );
        }
    }
}
