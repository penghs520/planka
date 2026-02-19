package dev.planka.api.user;

import dev.planka.api.user.dto.LoginResponse;
import dev.planka.api.user.dto.MemberDTO;
import dev.planka.api.user.dto.OrganizationDTO;
import dev.planka.api.user.dto.UserDTO;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.api.user.request.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务契约
 * <p>
 * 定义用户服务的所有API接口，供其他服务通过Feign调用
 */
@FeignClient(name = "user-service", path = "/api/v1")
public interface UserServiceContract {

    // ==================== 认证 API ====================

    /**
     * 用户登录
     */
    @PostMapping("/auth/login")
    Result<LoginResponse> login(@RequestBody LoginRequest request);

    /**
     * 刷新Token
     */
    @PostMapping("/auth/refresh")
    Result<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request);

    /**
     * 账号激活（首次设置密码）
     */
    @PostMapping("/auth/activate")
    Result<LoginResponse> activate(@RequestBody ActivateRequest request);

    // ==================== 用户 API ====================

    /**
     * 获取当前用户信息
     */
    @GetMapping("/users/me")
    Result<UserDTO> getCurrentUser(@RequestHeader("X-User-Id") String userId);

    /**
     * 更新当前用户信息
     */
    @PutMapping("/users/me")
    Result<UserDTO> updateCurrentUser(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateUserRequest request);

    /**
     * 修改密码
     */
    @PutMapping("/users/me/password")
    Result<Void> changePassword(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ChangePasswordRequest request);

    /**
     * 获取当前用户的组织列表
     */
    @GetMapping("/users/me/organizations")
    Result<List<OrganizationDTO>> getUserOrganizations(@RequestHeader("X-User-Id") String userId);

    // ==================== 组织 API ====================

    /**
     * 创建组织
     */
    @PostMapping("/organizations")
    Result<OrganizationDTO> createOrganization(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateOrganizationRequest request);

    /**
     * 获取组织详情
     */
    @GetMapping("/organizations/{orgId}")
    Result<OrganizationDTO> getOrganization(@PathVariable("orgId") String orgId);

    /**
     * 更新组织信息
     */
    @PutMapping("/organizations/{orgId}")
    Result<OrganizationDTO> updateOrganization(
            @PathVariable("orgId") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateOrganizationRequest request);

    /**
     * 删除组织
     */
    @DeleteMapping("/organizations/{orgId}")
    Result<Void> deleteOrganization(
            @PathVariable("orgId") String orgId,
            @RequestHeader("X-User-Id") String userId);

    // ==================== 成员 API ====================

    /**
     * 获取组织成员列表
     */
    @GetMapping("/members")
    Result<PageResult<MemberDTO>> listMembers(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size);

    /**
     * 添加成员
     */
    @PostMapping("/members")
    Result<MemberDTO> addMember(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody AddMemberRequest request);

    /**
     * 获取成员详情
     */
    @GetMapping("/members/{memberId}")
    Result<MemberDTO> getMember(@PathVariable("memberId") String memberId);

    /**
     * 移除成员
     */
    @DeleteMapping("/members/{memberId}")
    Result<Void> removeMember(
            @PathVariable("memberId") String memberId,
            @RequestHeader("X-User-Id") String userId);

    /**
     * 修改成员角色
     */
    @PutMapping("/members/{memberId}/role")
    Result<MemberDTO> updateMemberRole(
            @PathVariable("memberId") String memberId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateMemberRoleRequest request);

    // ==================== 超管 API ====================

    /**
     * 获取所有组织列表（超管）
     */
    @GetMapping("/admin/organizations")
    Result<PageResult<OrganizationDTO>> listAllOrganizations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size);

    /**
     * 获取所有用户列表（超管）
     */
    @GetMapping("/admin/users")
    Result<PageResult<UserDTO>> listAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size);

    /**
     * 修改用户状态（超管）
     */
    @PutMapping("/admin/users/{userId}/status")
    Result<UserDTO> updateUserStatus(
            @PathVariable("userId") String userId,
            @RequestParam String status);
}
