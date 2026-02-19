package dev.planka.user.controller;

import dev.planka.api.user.dto.OrganizationDTO;
import dev.planka.api.user.dto.UserDTO;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 超管控制器
 * <p>
 * 所有接口需要超级管理员权限（在网关层校验 X-Super-Admin header）
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 获取所有组织列表
     */
    @GetMapping("/organizations")
    public Result<PageResult<OrganizationDTO>> listAllOrganizations(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return adminService.listAllOrganizations(page, size);
    }

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public Result<PageResult<UserDTO>> listAllUsers(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return adminService.listAllUsers(page, size);
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/users/{userId}/status")
    public Result<UserDTO> updateUserStatus(
            @PathVariable(name = "userId") String userId,
            @RequestParam(name = "status") String status) {
        return adminService.updateUserStatus(userId, status);
    }
}
