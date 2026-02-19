package dev.planka.card.controller;

import dev.planka.api.card.request.LinkableCardsRequest;
import dev.planka.api.card.request.UpdateLinkRequest;
import dev.planka.card.service.core.LinkCardService;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.field.LinkedCard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 关联卡片控制器
 * <p>
 * 提供关联卡片的查询和更新 REST API
 */
@RestController
@RequestMapping("/api/v1/links")
public class LinkCardController {

    private final LinkCardService linkCardService;

    public LinkCardController(LinkCardService linkCardService) {
        this.linkCardService = linkCardService;
    }

    /**
     * 查询可关联的卡片列表
     *
     * @param operatorId 操作人ID
     * @param orgId      组织ID
     * @param request    查询请求
     * @return 可关联的卡片分页列表
     */
    @PostMapping("/linkable-cards")
    public Result<PageResult<LinkedCard>> queryLinkableCards(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestBody LinkableCardsRequest request) {
        return linkCardService.queryLinkableCards(request, orgId, operatorId);
    }

    /**
     * 更新关联关系
     *
     * @param operatorId     操作人ID
     * @param orgId          组织ID
     * @param request        更新请求
     * @param servletRequest HTTP请求（用于获取客户端IP）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> updateLink(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestBody UpdateLinkRequest request,
            HttpServletRequest servletRequest) {
        String sourceIp = getClientIp(servletRequest);
        return linkCardService.updateLink(request, orgId, operatorId, sourceIp);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
