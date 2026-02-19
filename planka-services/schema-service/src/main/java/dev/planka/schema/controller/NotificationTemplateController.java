package dev.planka.schema.controller;

import dev.planka.api.schema.request.notification.CreateNotificationTemplateRequest;
import dev.planka.api.schema.request.notification.UpdateNotificationTemplateRequest;
import dev.planka.api.schema.vo.notification.NotificationTemplateVO;
import dev.planka.common.result.Result;
import dev.planka.schema.service.notification.NotificationTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知模板 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/schemas/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    /**
     * 查询通知模板列表
     */
    @GetMapping
    public Result<List<NotificationTemplateVO>> list(@RequestHeader("X-Org-Id") String orgId) {
        return notificationTemplateService.list(orgId);
    }

    /**
     * 根据卡片类型查询通知模板列表
     */
    @GetMapping("/by-card-type/{cardTypeId}")
    public Result<List<NotificationTemplateVO>> listByCardType(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable("cardTypeId") String cardTypeId) {
        return notificationTemplateService.listByCardType(orgId, cardTypeId);
    }

    /**
     * 根据 ID 获取通知模板详情
     */
    @GetMapping("/{id}")
    public Result<NotificationTemplateVO> getById(@PathVariable("id") String id) {
        return notificationTemplateService.getById(id);
    }

    /**
     * 创建通知模板
     */
    @PostMapping
    public Result<NotificationTemplateVO> create(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @Valid @RequestBody CreateNotificationTemplateRequest request) {
        return notificationTemplateService.create(orgId, operatorId, request);
    }

    /**
     * 更新通知模板
     */
    @PutMapping("/{id}")
    public Result<NotificationTemplateVO> update(
            @PathVariable("id") String id,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @Valid @RequestBody UpdateNotificationTemplateRequest request) {
        return notificationTemplateService.update(id, operatorId, request);
    }

    /**
     * 删除通知模板
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable("id") String id,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return notificationTemplateService.delete(id, operatorId);
    }

    /**
     * 启用通知模板
     */
    @PutMapping("/{id}/activate")
    public Result<Void> activate(
            @PathVariable("id") String id,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return notificationTemplateService.activate(id, operatorId);
    }

    /**
     * 停用通知模板
     */
    @PutMapping("/{id}/disable")
    public Result<Void> disable(
            @PathVariable("id") String id,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return notificationTemplateService.disable(id, operatorId);
    }
}
