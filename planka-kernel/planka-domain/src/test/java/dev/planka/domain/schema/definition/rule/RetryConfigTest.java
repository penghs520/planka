package dev.planka.domain.schema.definition.rule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RetryConfig 单元测试")
class RetryConfigTest {

    @Nested
    @DisplayName("calculateDelay 方法")
    class CalculateDelayTests {

        @Test
        @DisplayName("固定间隔模式应返回固定延迟")
        void shouldReturnFixedDelay_whenExponentialBackoffDisabled() {
            RetryConfig config = new RetryConfig();
            config.setExponentialBackoff(false);
            config.setRetryIntervalMs(2000);

            assertThat(config.calculateDelay(1)).isEqualTo(2000);
            assertThat(config.calculateDelay(2)).isEqualTo(2000);
            assertThat(config.calculateDelay(5)).isEqualTo(2000);
        }

        @Test
        @DisplayName("指数退避应正确计算延迟")
        void shouldCalculateExponentialDelay() {
            RetryConfig config = new RetryConfig();
            config.setExponentialBackoff(true);
            config.setRetryIntervalMs(1000);
            config.setMaxRetryIntervalMs(60000);

            assertThat(config.calculateDelay(1)).isEqualTo(1000);  // 1000 * 2^0
            assertThat(config.calculateDelay(2)).isEqualTo(2000);  // 1000 * 2^1
            assertThat(config.calculateDelay(3)).isEqualTo(4000);  // 1000 * 2^2
            assertThat(config.calculateDelay(4)).isEqualTo(8000);  // 1000 * 2^3
        }

        @Test
        @DisplayName("指数退避不应超过最大间隔")
        void shouldNotExceedMaxRetryInterval() {
            RetryConfig config = new RetryConfig();
            config.setExponentialBackoff(true);
            config.setRetryIntervalMs(1000);
            config.setMaxRetryIntervalMs(5000);

            assertThat(config.calculateDelay(1)).isEqualTo(1000);
            assertThat(config.calculateDelay(2)).isEqualTo(2000);
            assertThat(config.calculateDelay(3)).isEqualTo(4000);
            assertThat(config.calculateDelay(4)).isEqualTo(5000); // capped
            assertThat(config.calculateDelay(10)).isEqualTo(5000); // capped
        }
    }

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("defaultConfig 应返回默认配置")
        void shouldReturnDefaultConfig() {
            RetryConfig config = RetryConfig.defaultConfig();

            assertThat(config.getMaxRetries()).isEqualTo(3);
            assertThat(config.getRetryIntervalMs()).isEqualTo(1000);
            assertThat(config.isExponentialBackoff()).isTrue();
            assertThat(config.getMaxRetryIntervalMs()).isEqualTo(60000);
        }

        @Test
        @DisplayName("noRetry 应返回零重试配置")
        void shouldReturnNoRetryConfig() {
            RetryConfig config = RetryConfig.noRetry();

            assertThat(config.getMaxRetries()).isZero();
        }
    }
}