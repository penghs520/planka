package cn.planka.notification.service;

import cn.planka.notification.plugin.NotificationRequest;
import cn.planka.notification.plugin.NotificationResult;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 发送通知到指定渠道
     *
     * @param channelId 渠道ID
     * @param request   通知请求
     * @return 发送结果
     */
    NotificationResult send(String channelId, NotificationRequest request);

    /**
     * 发送通知到多个渠道
     *
     * @param channelIds 渠道ID列表
     * @param request    通知请求
     * @return 发送结果列表
     */
    List<NotificationResult> sendToChannels(List<String> channelIds, NotificationRequest request);

    /**
     * 获取组织可用的渠道列表
     *
     * @param orgId 组织ID
     * @return 可用渠道ID列表
     */
    List<String> getAvailableChannels(String orgId);

    /**
     * 检查渠道是否可用
     *
     * @param orgId     组织ID
     * @param channelId 渠道ID
     * @return 是否可用
     */
    boolean isChannelAvailable(String orgId, String channelId);
}
