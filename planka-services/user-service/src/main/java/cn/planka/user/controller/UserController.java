package cn.planka.user.controller;

import cn.planka.api.user.dto.OrganizationDTO;
import cn.planka.api.user.dto.SidebarPreferencesDTO;
import cn.planka.api.user.dto.UserDTO;
import cn.planka.api.user.request.ChangePasswordRequest;
import cn.planka.api.user.request.UpdateSidebarPreferencesRequest;
import cn.planka.api.user.request.UpdateUserRequest;
import cn.planka.common.result.Result;
import cn.planka.user.service.UserService;
import cn.planka.user.service.UserSidebarPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSidebarPreferencesService userSidebarPreferencesService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser(@RequestHeader(name = "X-User-Id") String userId) {
        return userService.getCurrentUser(userId);
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/me")
    public Result<UserDTO> updateCurrentUser(
            @RequestHeader(name = "X-User-Id") String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateCurrentUser(userId, request);
    }

    /**
     * 修改密码
     */
    @PutMapping("/me/password")
    public Result<Void> changePassword(
            @RequestHeader(name = "X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(userId, request);
    }

    /**
     * 获取当前用户的组织列表
     */
    @GetMapping("/me/organizations")
    public Result<List<OrganizationDTO>> getUserOrganizations(@RequestHeader(name = "X-User-Id") String userId) {
        return userService.getUserOrganizations(userId);
    }

    /**
     * 获取当前用户在当前组织下的侧栏偏好
     */
    @GetMapping("/me/sidebar-preferences")
    public Result<SidebarPreferencesDTO> getSidebarPreferences(
            @RequestHeader(name = "X-User-Id") String userId,
            @RequestHeader(name = "X-Org-Id") String orgId) {
        return userSidebarPreferencesService.get(userId, orgId);
    }

    /**
     * 更新当前用户在当前组织下的侧栏偏好
     */
    @PutMapping("/me/sidebar-preferences")
    public Result<SidebarPreferencesDTO> updateSidebarPreferences(
            @RequestHeader(name = "X-User-Id") String userId,
            @RequestHeader(name = "X-Org-Id") String orgId,
            @Valid @RequestBody UpdateSidebarPreferencesRequest request) {
        return userSidebarPreferencesService.update(userId, orgId, request);
    }
}
