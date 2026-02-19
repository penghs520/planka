package dev.planka.schema.controller;

import dev.planka.api.schema.request.notification.CreateNotificationChannelRequest;
import dev.planka.api.schema.request.notification.UpdateNotificationChannelRequest;
import dev.planka.api.schema.vo.notification.NotificationChannelConfigVO;
import dev.planka.common.result.Result;
import dev.planka.schema.service.notification.NotificationChannelConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知渠道配置 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/schemas/notification-channels")
@RequiredArgsConstructor
public class NotificationChannelConfigController {

    private final NotificationChannelConfigService notificationChannelConfigService;

    /**
     * 查询通知渠道配置列表
     */
    @GetMapping
    public Result<List<NotificationChannelConfigVO>> list(@RequestHeader("X-Org-Id") String orgId) {
        return notificationChannelConfigService.list(orgId);
    }

    /**
     * 根据 ID 获取通知渠道配置详情
     */
    @GetMapping("/{id}")
    public Result<NotificationChannelConfigVO> getById(@PathVariable("id") String id) {
        return notificationChannelConfigService.getById(id);
    }

    /**
     * 创建通知渠道配置
     */
    @PostMapping
    public Result<NotificationChannelConfigVO> create(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @Valid @RequestBody CreateNotificationChannelRequest request) {
        return notificationChannelConfigService.create(orgId, operatorId, request);
    }

    /**
     * 更新通知渠道配置
     */
    @PutMapping("/{id}")
    public Result<NotificationChannelConfigVO> update(
            @PathVariable("id") String id,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @Valid @RequestBody UpdateNotificationChannelRequest request) {
        return notificationChannelConfigService.update(id, operatorId, request);
    }

    /**
     * 删除通知渠道配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable("id") String id,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return notificationChannelConfigService.delete(id, operatorId);
    }

    /**
     * 启用通知渠道配置
     */
    @PutMapping("/{id}/activate")
    public Result<Void> activate(
            @PathVariable("id") String id,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return notificationChannelConfigService.activate(id, operatorId);
    }

    /**
     * 停用通知渠道配置
     */
    @PutMapping("/{id}/disable")
    public Result<Void> disable(
            @PathVariable("id") String id,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return notificationChannelConfigService.disable(id, operatorId);
    }
}