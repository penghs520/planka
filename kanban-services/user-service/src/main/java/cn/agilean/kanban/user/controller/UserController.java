package cn.agilean.kanban.user.controller;

import cn.agilean.kanban.api.user.dto.OrganizationDTO;
import cn.agilean.kanban.api.user.dto.UserDTO;
import cn.agilean.kanban.api.user.request.ChangePasswordRequest;
import cn.agilean.kanban.api.user.request.UpdateUserRequest;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.user.service.UserService;
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
}
