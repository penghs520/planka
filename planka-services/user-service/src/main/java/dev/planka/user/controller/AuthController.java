package dev.planka.user.controller;

import dev.planka.api.user.dto.LoginResponse;
import dev.planka.api.user.dto.SwitchOrganizationResponse;
import dev.planka.api.user.request.ActivateRequest;
import dev.planka.api.user.request.LoginRequest;
import dev.planka.api.user.request.RefreshTokenRequest;
import dev.planka.api.user.request.SwitchOrganizationRequest;
import dev.planka.common.result.Result;
import dev.planka.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        return authService.login(request, clientIp);
    }

    /**
     * 账号激活（首次设置密码）
     */
    @PostMapping("/activate")
    public Result<LoginResponse> activate(
            @Valid @RequestBody ActivateRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        return authService.activate(request, clientIp);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(name = "X-User-Id") String userId) {
        return authService.logout(userId);
    }

    /**
     * 切换组织
     * <p>
     * 验证用户在目标组织中的成员卡是否存在且活跃，成功后返回包含组织信息的新 Token
     */
    @PostMapping("/switch-organization")
    public Result<SwitchOrganizationResponse> switchOrganization(
            @RequestHeader(name = "X-User-Id") String userId,
            @Valid @RequestBody SwitchOrganizationRequest request) {
        return authService.switchOrganization(userId, request);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
