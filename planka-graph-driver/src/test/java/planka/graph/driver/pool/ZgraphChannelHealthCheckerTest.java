package planka.graph.driver.pool;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * ZgraphChannelHealthChecker 单元测试
 *
 * 测试场景：
 * 1. Channel 状态检查（active/open/writable）
 * 2. 连接空闲时间检查
 * 3. 活跃时间标记和更新
 */
@Disabled //手动执行，不然耗时长
class ZgraphChannelHealthCheckerTest {

    private Channel channel;
    private EventLoop eventLoop;
    @SuppressWarnings("unchecked")
    private Promise<Boolean> promise;
    @SuppressWarnings("unchecked")
    private Attribute<Long> lastActiveTimeAttr;

    @BeforeEach
    void setUp() {
        channel = mock(Channel.class);
        eventLoop = mock(EventLoop.class);
        promise = mock(Promise.class);
        lastActiveTimeAttr = mock(Attribute.class);

        when(channel.eventLoop()).thenReturn(eventLoop);
        when(eventLoop.newPromise()).thenAnswer(invocation -> promise);
        when(channel.attr(any(AttributeKey.class))).thenAnswer(invocation -> lastActiveTimeAttr);
    }

    @Test
    @DisplayName("测试连接不活跃时应返回false")
    void testInactiveChannel() {
        // Given: Channel 不活跃
        when(channel.isActive()).thenReturn(false);

        ZgraphChannelHealthChecker checker = ZgraphChannelHealthChecker.defaultChecker();

        // When: 执行健康检查
        Future<Boolean> result = checker.isHealthy(channel);

        // Then: 应该返回 false
        verify(promise).setSuccess(false);
    }

    @Test
    @DisplayName("测试连接未打开时应返回false")
    void testClosedChannel() {
        // Given: Channel 活跃但未打开
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(false);

        ZgraphChannelHealthChecker checker = ZgraphChannelHealthChecker.defaultChecker();

        // When: 执行健康检查
        Future<Boolean> result = checker.isHealthy(channel);

        // Then: 应该返回 false
        verify(promise).setSuccess(false);
    }

    @Test
    @DisplayName("测试连接不可写时应返回false")
    void testUnwritableChannel() {
        // Given: Channel 活跃且打开，但不可写
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.isWritable()).thenReturn(false);

        ZgraphChannelHealthChecker checker = ZgraphChannelHealthChecker.defaultChecker();

        // When: 执行健康检查
        Future<Boolean> result = checker.isHealthy(channel);

        // Then: 应该返回 false
        verify(promise).setSuccess(false);
    }

    @Test
    @DisplayName("测试新连接（无活跃时间）应返回true")
    void testNewChannelWithoutActiveTime() {
        // Given: 健康的 Channel，无活跃时间记录
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);
        when(lastActiveTimeAttr.get()).thenReturn(null);

        ZgraphChannelHealthChecker checker = ZgraphChannelHealthChecker.defaultChecker();

        // When: 执行健康检查
        Future<Boolean> result = checker.isHealthy(channel);

        // Then: 应该返回 true，并更新活跃时间
        verify(promise).setSuccess(true);
        verify(lastActiveTimeAttr).set(anyLong());
    }

    @Test
    @DisplayName("测试空闲时间未超过阈值应返回true")
    void testChannelWithinIdleThreshold() {
        // Given: 健康的 Channel，空闲时间30秒（小于60秒阈值）
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        long now = System.currentTimeMillis();
        long lastActiveTime = now - 30_000; // 30秒前
        when(lastActiveTimeAttr.get()).thenReturn(lastActiveTime);

        ZgraphChannelHealthChecker checker = ZgraphChannelHealthChecker.defaultChecker();

        // When: 执行健康检查
        Future<Boolean> result = checker.isHealthy(channel);

        // Then: 应该返回 true
        verify(promise).setSuccess(true);
        verify(lastActiveTimeAttr).set(anyLong()); // 更新活跃时间
    }

    @Test
    @DisplayName("测试空闲时间超过阈值应返回false")
    void testChannelExceedsIdleThreshold() {
        // Given: 健康的 Channel，但空闲时间65秒（超过60秒阈值）
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        long now = System.currentTimeMillis();
        long lastActiveTime = now - 65_000; // 65秒前
        when(lastActiveTimeAttr.get()).thenReturn(lastActiveTime);

        ZgraphChannelHealthChecker checker = ZgraphChannelHealthChecker.defaultChecker();

        // When: 执行健康检查
        Future<Boolean> result = checker.isHealthy(channel);

        // Then: 应该返回 false（空闲时间过长）
        verify(promise).setSuccess(false);
    }

    @Test
    @DisplayName("测试严格模式检查器（30秒阈值）")
    void testStrictChecker() {
        // Given: 健康的 Channel，空闲时间35秒
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        long now = System.currentTimeMillis();
        long lastActiveTime = now - 35_000; // 35秒前
        when(lastActiveTimeAttr.get()).thenReturn(lastActiveTime);

        ZgraphChannelHealthChecker checker = ZgraphChannelHealthChecker.strictChecker();

        // When: 执行健康检查
        Future<Boolean> result = checker.isHealthy(channel);

        // Then: 严格模式下，35秒超过30秒阈值，应该返回 false
        verify(promise).setSuccess(false);
    }

    @Test
    @DisplayName("测试标记连接为活跃状态")
    void testMarkActive() {
        // Given: 一个 Channel
        when(channel.attr(any(AttributeKey.class))).thenAnswer(invocation -> lastActiveTimeAttr);

        // When: 标记为活跃
        long beforeMark = System.currentTimeMillis();
        ZgraphChannelHealthChecker.markActive(channel);
        long afterMark = System.currentTimeMillis();

        // Then: 应该设置当前时间戳
        verify(lastActiveTimeAttr).set(longThat(timestamp ->
            timestamp >= beforeMark && timestamp <= afterMark
        ));
    }

    @Test
    @DisplayName("测试自定义空闲时间阈值")
    void testCustomIdleThreshold() {
        // Given: 自定义120秒空闲阈值的检查器
        ZgraphChannelHealthChecker checker = new ZgraphChannelHealthChecker(120_000, false);

        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        long now = System.currentTimeMillis();
        long lastActiveTime = now - 100_000; // 100秒前
        when(lastActiveTimeAttr.get()).thenReturn(lastActiveTime);

        // When: 执行健康检查
        Future<Boolean> result = checker.isHealthy(channel);

        // Then: 100秒小于120秒阈值，应该返回 true
        verify(promise).setSuccess(true);
    }

    @Test
    @DisplayName("测试边界情况：超过60秒1毫秒")
    void testJustOverThreshold() {
        // Given: 超过60秒1毫秒
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);

        long now = System.currentTimeMillis();
        long lastActiveTime = now - 60_001; // 60秒+1毫秒
        when(lastActiveTimeAttr.get()).thenReturn(lastActiveTime);

        ZgraphChannelHealthChecker checker = ZgraphChannelHealthChecker.defaultChecker();

        // When: 执行健康检查
        Future<Boolean> result = checker.isHealthy(channel);

        // Then: 应该返回 false（超过阈值）
        verify(promise).setSuccess(false);
    }
}
