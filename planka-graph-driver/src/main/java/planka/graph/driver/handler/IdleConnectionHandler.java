package planka.graph.driver.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 空闲连接处理器
 *
 * 功能：
 * 1. 监听连接空闲事件
 * 2. 空闲时发送心跳保活（如果协议支持）
 * 3. 或关闭长时间空闲的连接
 *
 * 配合 IdleStateHandler 使用：
 * pipeline.addLast(new IdleStateHandler(0, 0, 60, TimeUnit.SECONDS));
 * pipeline.addLast(new IdleConnectionHandler());
 */
public class IdleConnectionHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(IdleConnectionHandler.class);

    private final HeartbeatStrategy heartbeatStrategy;

    public IdleConnectionHandler(HeartbeatStrategy heartbeatStrategy) {
        this.heartbeatStrategy = heartbeatStrategy;
    }

    /**
     * 默认策略：关闭空闲连接
     */
    public static IdleConnectionHandler withDefaultStrategy() {
        return new IdleConnectionHandler(HeartbeatStrategy.CLOSE_ON_IDLE);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {

            if (event.state() == IdleState.ALL_IDLE) {
                handleIdleConnection(ctx);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void handleIdleConnection(ChannelHandlerContext ctx) {
        switch (heartbeatStrategy) {
            case SEND_HEARTBEAT:
                // TODO: 发送心跳包（需要协议支持）
                logger.debug("连接空闲，发送心跳: {}", ctx.channel().id());
                // sendHeartbeat(ctx);
                break;

            case CLOSE_ON_IDLE:
                // 关闭空闲连接，连接池会自动创建新连接
                logger.info("连接空闲超时，关闭连接: {}", ctx.channel().id());
                ctx.close();
                break;

            case LOG_ONLY:
                logger.debug("连接空闲: {}", ctx.channel().id());
                break;
        }
    }

    /**
     * 心跳策略
     */
    public enum HeartbeatStrategy {
        /** 发送心跳保活 */
        SEND_HEARTBEAT,

        /** 关闭空闲连接 */
        CLOSE_ON_IDLE,

        /** 仅记录日志 */
        LOG_ONLY
    }
}
