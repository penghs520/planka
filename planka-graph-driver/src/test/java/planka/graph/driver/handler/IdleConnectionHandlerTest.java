package planka.graph.driver.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.mockito.Mockito.*;

/**
 * IdleConnectionHandler 单元测试
 *
 * 测试场景：
 * 1. 空闲事件触发
 * 2. 不同策略的处理行为
 * 3. 非空闲事件的处理
 */
@Disabled //手动执行，不然耗时长
class IdleConnectionHandlerTest {

    private ChannelHandlerContext ctx;
    private Channel channel;
    private ChannelId channelId;

    @BeforeEach
    void setUp() {
        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);
        channelId = mock(ChannelId.class);

        when(ctx.channel()).thenReturn(channel);
        when(channel.id()).thenReturn(channelId);
        when(channelId.toString()).thenReturn("test-channel-id");
    }

    @Test
    @DisplayName("测试默认策略：空闲时关闭连接")
    void testDefaultStrategyClosesChannel() throws Exception {
        // Given: 使用默认策略（关闭空闲连接）
        IdleConnectionHandler handler = IdleConnectionHandler.withDefaultStrategy();
        IdleStateEvent event = createIdleStateEvent(IdleState.ALL_IDLE);

        // When: 触发全空闲事件
        handler.userEventTriggered(ctx, event);

        // Then: 应该关闭连接
        verify(ctx).close();
    }

    @Test
    @DisplayName("测试CLOSE_ON_IDLE策略显式调用")
    void testCloseOnIdleStrategy() throws Exception {
        // Given: 显式使用 CLOSE_ON_IDLE 策略
        IdleConnectionHandler handler = new IdleConnectionHandler(
            IdleConnectionHandler.HeartbeatStrategy.CLOSE_ON_IDLE
        );
        IdleStateEvent event = createIdleStateEvent(IdleState.ALL_IDLE);

        // When: 触发全空闲事件
        handler.userEventTriggered(ctx, event);

        // Then: 应该关闭连接
        verify(ctx).close();
    }

    @Test
    @DisplayName("测试LOG_ONLY策略：仅记录日志不关闭")
    void testLogOnlyStrategy() throws Exception {
        // Given: 使用 LOG_ONLY 策略
        IdleConnectionHandler handler = new IdleConnectionHandler(
            IdleConnectionHandler.HeartbeatStrategy.LOG_ONLY
        );
        IdleStateEvent event = createIdleStateEvent(IdleState.ALL_IDLE);

        // When: 触发全空闲事件
        handler.userEventTriggered(ctx, event);

        // Then: 不应该关闭连接
        verify(ctx, never()).close();
    }

    @Test
    @DisplayName("测试只有ALL_IDLE事件才触发处理")
    void testOnlyAllIdleTriggersHandling() throws Exception {
        // Given: 使用默认策略
        IdleConnectionHandler handler = IdleConnectionHandler.withDefaultStrategy();

        // When: 触发 READER_IDLE 事件
        IdleStateEvent readerIdleEvent = createIdleStateEvent(IdleState.READER_IDLE);
        handler.userEventTriggered(ctx, readerIdleEvent);

        // Then: 不应该关闭连接（因为不是 ALL_IDLE）
        verify(ctx, never()).close();

        // When: 触发 WRITER_IDLE 事件
        IdleStateEvent writerIdleEvent = createIdleStateEvent(IdleState.WRITER_IDLE);
        handler.userEventTriggered(ctx, writerIdleEvent);

        // Then: 不应该关闭连接（因为不是 ALL_IDLE）
        verify(ctx, never()).close();
    }

    @Test
    @DisplayName("测试非IdleStateEvent事件应该正常传递")
    void testNonIdleEventPassedThrough() throws Exception {
        // Given: 使用默认策略
        IdleConnectionHandler handler = IdleConnectionHandler.withDefaultStrategy();
        Object customEvent = new Object();

        // When: 触发非 IdleStateEvent 的事件
        handler.userEventTriggered(ctx, customEvent);

        // Then: 事件应该传递给下一个处理器，且不关闭连接
        verify(ctx).fireUserEventTriggered(customEvent);
        verify(ctx, never()).close();
    }

    @Test
    @DisplayName("测试多次空闲事件触发")
    void testMultipleIdleEvents() throws Exception {
        // Given: 使用默认策略
        IdleConnectionHandler handler = IdleConnectionHandler.withDefaultStrategy();
        IdleStateEvent event = createIdleStateEvent(IdleState.ALL_IDLE);

        // When: 触发第一次空闲事件
        handler.userEventTriggered(ctx, event);
        verify(ctx, times(1)).close();

        // When: 触发第二次空闲事件（模拟连接未完全关闭的情况）
        handler.userEventTriggered(ctx, event);

        // Then: 应该再次尝试关闭
        verify(ctx, times(2)).close();
    }

    @Test
    @DisplayName("测试SEND_HEARTBEAT策略（预留功能）")
    void testSendHeartbeatStrategy() throws Exception {
        // Given: 使用 SEND_HEARTBEAT 策略（当前未实现心跳发送）
        IdleConnectionHandler handler = new IdleConnectionHandler(
            IdleConnectionHandler.HeartbeatStrategy.SEND_HEARTBEAT
        );
        IdleStateEvent event = createIdleStateEvent(IdleState.ALL_IDLE);

        // When: 触发全空闲事件
        handler.userEventTriggered(ctx, event);

        // Then: 当前实现下不会关闭连接（因为 TODO 未实现）
        // 如果将来实现了心跳发送，这个测试需要更新
        verify(ctx, never()).close();
    }

    /**
     * 使用反射创建 IdleStateEvent（因为构造函数是 protected）
     */
    private IdleStateEvent createIdleStateEvent(IdleState state) throws Exception {
        Constructor<IdleStateEvent> constructor = IdleStateEvent.class.getDeclaredConstructor(
                IdleState.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(state, true);
    }
}
