package dev.planka.user.controller;

import dev.planka.api.user.dto.OrganizationDTO;
import dev.planka.api.user.request.CreateOrganizationRequest;
import dev.planka.api.user.request.UpdateOrganizationRequest;
import dev.planka.common.result.Result;
import dev.planka.user.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 组织控制器
 */
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * 创建组织
     */
    @PostMapping
    public Result<OrganizationDTO> createOrganization(
            @RequestHeader(name = "X-User-Id") String userId,
            @Valid @RequestBody CreateOrganizationRequest request) {
        return organizationService.createOrganization(userId, request);
    }

    /**
     * 获取组织详情
     */
    @GetMapping("/{orgId}")
    public Result<OrganizationDTO> getOrganization(@PathVariable(name = "orgId") String orgId) {
        return organizationService.getOrganization(orgId);
    }

    /**
     * 更新组织信息
     */
    @PutMapping("/{orgId}")
    public Result<OrganizationDTO> updateOrganization(
            @PathVariable(name = "orgId") String orgId,
            @RequestHeader(name = "X-User-Id") String userId,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        return organizationService.updateOrganization(orgId, userId, request);
    }

    /**
     * 删除组织
     */
    @DeleteMapping("/{orgId}")
    public Result<Void> deleteOrganization(
            @PathVariable(name = "orgId") String orgId,
            @RequestHeader(name = "X-User-Id") String userId) {
        return organizationService.deleteOrganization(orgId, userId);
    }
}
