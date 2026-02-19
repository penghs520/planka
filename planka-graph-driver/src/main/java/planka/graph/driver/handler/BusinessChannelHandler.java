package planka.graph.driver.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.pool.ChannelPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.PlankaGraphClient.ResponseProcessor;
import planka.graph.driver.exception.RequestIdMismatchException;
import planka.graph.driver.pool.ZgraphChannelHealthChecker;
import planka.graph.driver.proto.request.Request;
import planka.graph.driver.proto.response.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 业务请求响应处理器
 * <p>
 * 处理业务请求的响应。
 * 前置条件：pipeline 中已有 LengthFieldBasedFrameDecoder，
 * 因此 channelRead0 接收到的 ByteBuf 是完整的帧（不含长度前缀）。
 */
public class BusinessChannelHandler<T> extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(BusinessChannelHandler.class);

    private final Channel channel;
    private final Request request;
    private final ResponseProcessor<T> responseProcessor;
    private final CompletableFuture<T> future;
    private final ChannelPool channelPool;
    private final ScheduledFuture<?> timeoutFuture;

    public BusinessChannelHandler(Channel channel,
                                  Request request,
                                  ResponseProcessor<T> responseProcessor,
                                  int timeout,
                                  CompletableFuture<T> future,
                                  ChannelPool channelPool) {
        this.channel = channel;
        this.request = request;
        this.responseProcessor = responseProcessor;
        this.future = future;
        this.channelPool = channelPool;

        this.timeoutFuture = channel.eventLoop().schedule(() -> {
            if (!future.isDone()) {
                logger.warn("请求超时了，TCP连接状态：{}-active={}-open={}", channel.id(), channel.isActive(),
                        channel.isOpen());
                future.completeExceptionally(new RuntimeException("请求超时"));
                cleanupAndReleaseConnection();
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    private void cleanupAndReleaseConnection() {
        if (!timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
        }
        try {
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.context("businessHandler") != null) {
                pipeline.remove("businessHandler");
            }
        } catch (Exception e) {
            logger.error("移除管道处理器失败", e);
        }
        channelPool.release(channel);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        // LengthFieldBasedFrameDecoder 已完成帧拆包，msg 是完整的 protobuf 数据
        byte[] responseBytes = new byte[msg.readableBytes()];
        msg.readBytes(responseBytes);

        try {
            Response response = Response.parseFrom(responseBytes);

            if (!future.isDone()) {
                try {
                    checkResponseStatus(response);
                    validateRequestId(request, response);

                    T result = responseProcessor.process(response);
                    future.complete(result);
                    ZgraphChannelHealthChecker.markActive(channel);
                } catch (Exception e) {
                    future.completeExceptionally(
                            new RuntimeException("请求失败: " + e.getMessage(), e));
                }
            }

            cleanupAndReleaseConnection();

            if (logger.isDebugEnabled()) {
                logger.debug("成功解析完整消息，大小: {} 字节", responseBytes.length);
            }
        } catch (InvalidProtocolBufferException e) {
            if (logger.isDebugEnabled()) {
                logger.error("消息解析失败: {}", e.getMessage());
            }
            if (!future.isDone()) {
                future.completeExceptionally(e);
            }
            cleanupAndReleaseConnection();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (!future.isDone()) {
            future.completeExceptionally(new RuntimeException("连接关闭，但未收到完整响应"));
        }
        cleanupAndReleaseConnection();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!future.isDone()) {
            future.completeExceptionally(cause);
        }
        cleanupAndReleaseConnection();
    }

    private void checkResponseStatus(Response response) {
        if (response.getCode() != 200) {
            throw new RuntimeException("请求失败，响应码: " + response.getCode() +
                    "，消息: " + response.getMessage());
        }
    }

    private void validateRequestId(Request request, Response response) {
        String requestId = request.getRequestId();
        String responseRequestId = response.getRequestId();
        if (!requestId.equals(responseRequestId)) {
            throw new RequestIdMismatchException("请求ID不匹配", requestId, responseRequestId);
        }
    }
}
