package cn.agilean.kanban.user.controller;

import cn.agilean.kanban.api.user.dto.LoginResponse;
import cn.agilean.kanban.api.user.dto.SwitchOrganizationResponse;
import cn.agilean.kanban.api.user.request.ActivateRequest;
import cn.agilean.kanban.api.user.request.LoginRequest;
import cn.agilean.kanban.api.user.request.RefreshTokenRequest;
import cn.agilean.kanban.api.user.request.SwitchOrganizationRequest;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.user.service.AuthService;
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
