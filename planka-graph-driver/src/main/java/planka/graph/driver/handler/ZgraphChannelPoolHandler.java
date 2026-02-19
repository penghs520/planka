package planka.graph.driver.handler;

import io.netty.channel.*;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.pool.ZgraphChannelHealthChecker;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义连接池处理器，处理连接的创建、获取和释放
 */
public class ZgraphChannelPoolHandler implements ChannelPoolHandler {

    private static final Logger logger = LoggerFactory.getLogger(ZgraphChannelPoolHandler.class);

    private final AtomicInteger activeConnections;
    private final AtomicInteger totalCreatedConnections;
    private final AtomicInteger aliveConnections;

    public ZgraphChannelPoolHandler(AtomicInteger activeConnections,
                                    AtomicInteger totalCreatedConnections,
                                    AtomicInteger aliveConnections) {
        this.activeConnections = activeConnections;
        this.totalCreatedConnections = totalCreatedConnections;
        this.aliveConnections = aliveConnections;
    }

    @Override
    public void channelReleased(Channel ch) {
        if (logger.isDebugEnabled()) {
            logger.debug("连接已释放回连接池: {}", ch.id());
        }
        activeConnections.decrementAndGet();

        // 清理Channel的pipeline，移除业务处理器（保留帧解码器和空闲检测等基础处理器）
        ChannelPipeline pipeline = ch.pipeline();
        removeHandlerIfPresent(pipeline, "businessHandler");
        removeHandlerIfPresent(pipeline, "authHandler");
    }

    @Override
    public void channelAcquired(Channel ch) {
        if (logger.isDebugEnabled()) {
            logger.debug("从连接池获取连接: {}", ch.id());
        }
        activeConnections.incrementAndGet();
    }

    @Override
    public void channelCreated(Channel ch) {
        if (logger.isDebugEnabled()) {
            logger.debug("创建新连接: {}", ch.id());
        }
        SocketChannel socketChannel = (SocketChannel) ch;
        socketChannel.config().setKeepAlive(true);
        socketChannel.config().setTcpNoDelay(true);

        totalCreatedConnections.incrementAndGet();
        aliveConnections.incrementAndGet();

        // 标记连接的初始活跃时间（用于健康检查）
        ZgraphChannelHealthChecker.markActive(ch);

        // 添加帧解码器到 pipeline：4字节长度前缀，最大帧 64MB
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("frameDecoder",
                new LengthFieldBasedFrameDecoder(64 * 1024 * 1024, 0, 4, 0, 4));
        pipeline.addLast("framePrepender", new LengthFieldPrepender(4));

        // 添加连接关闭监听器，用于统计
        ch.closeFuture().addListener((ChannelFutureListener) future ->
                aliveConnections.decrementAndGet());
    }

    private void removeHandlerIfPresent(ChannelPipeline pipeline, String name) {
        try {
            if (pipeline.context(name) != null) {
                pipeline.remove(name);
            }
        } catch (Exception e) {
            logger.warn("移除处理器失败: {}", e.getMessage());
        }
    }
}
