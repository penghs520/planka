package dev.planka.domain.schema.definition.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 重试配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetryConfig {

    /** 最大重试次数 */
    @JsonProperty("maxRetries")
    private int maxRetries = 3;

    /** 重试间隔（毫秒） */
    @JsonProperty("retryIntervalMs")
    private long retryIntervalMs = 1000;

    /** 是否使用指数退避 */
    @JsonProperty("exponentialBackoff")
    private boolean exponentialBackoff = true;

    /** 最大重试间隔（毫秒） */
    @JsonProperty("maxRetryIntervalMs")
    private long maxRetryIntervalMs = 60000;

    /**
     * 创建默认重试配置
     */
    public static RetryConfig defaultConfig() {
        return new RetryConfig();
    }

    /**
     * 创建无重试配置
     */
    public static RetryConfig noRetry() {
        RetryConfig config = new RetryConfig();
        config.setMaxRetries(0);
        return config;
    }

    /**
     * 计算第 attempt 次重试的延迟时间（毫秒）
     *
     * @param attempt 重试次数（从1开始）
     * @return 延迟时间（毫秒）
     */
    public long calculateDelay(int attempt) {
        if (!exponentialBackoff) {
            return retryIntervalMs;
        }
        long delay = retryIntervalMs * (1L << (attempt - 1));
        return Math.min(delay, maxRetryIntervalMs);
    }
}
