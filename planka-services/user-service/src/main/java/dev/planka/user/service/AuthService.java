package dev.planka.user.service;

import dev.planka.api.card.CardServiceClient;
import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.Yield;
import dev.planka.api.user.dto.LoginResponse;
import dev.planka.api.user.dto.OrganizationDTO;
import dev.planka.api.user.dto.SwitchOrganizationResponse;
import dev.planka.api.user.dto.UserDTO;
import dev.planka.api.user.enums.UserStatus;
import dev.planka.api.user.request.ActivateRequest;
import dev.planka.api.user.request.LoginRequest;
import dev.planka.api.user.request.RefreshTokenRequest;
import dev.planka.api.user.request.SwitchOrganizationRequest;
import dev.planka.common.result.Result;
import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.domain.card.CardStyle;
import dev.planka.user.model.OrganizationEntity;
import dev.planka.user.model.RefreshTokenEntity;
import dev.planka.user.model.UserEntity;
import dev.planka.user.model.UserOrganizationEntity;
import dev.planka.user.repository.OrganizationRepository;
import dev.planka.user.repository.RefreshTokenRepository;
import dev.planka.user.repository.UserOrganizationRepository;
import dev.planka.user.repository.UserRepository;
import dev.planka.user.security.JwtTokenProvider;
import dev.planka.user.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final CardServiceClient cardServiceClient;

    @Value("${user.login.max-fail-count:5}")
    private int maxFailCount;

    /**
     * 用户登录
     */
    @Transactional
    public Result<LoginResponse> login(LoginRequest request, String clientIp) {
        // 1. 查找用户
        UserEntity user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            return Result.failure("AUTH_001", "用户不存在");
        }

        // 2. 检查用户状态
        String status = user.getStatus();
        if (UserStatus.PENDING_ACTIVATION.name().equals(status)) {
            return Result.failure("AUTH_002", "账号未激活，请先设置密码");
        }
        if (UserStatus.LOCKED.name().equals(status)) {
            return Result.failure("AUTH_003", "账号已锁定，请联系管理员");
        }
        if (UserStatus.DISABLED.name().equals(status)) {
            return Result.failure("AUTH_004", "账号已禁用");
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleLoginFailure(user);
            return Result.failure("AUTH_005", "密码错误");
        }

        // 4. 检查是否使用默认密码
        boolean requirePasswordChange = user.isUsingDefaultPassword();

        // 5. 生成 Token
        return generateLoginResponse(user, clientIp, requirePasswordChange);
    }

    /**
     * 账号激活（首次设置密码）
     */
    @Transactional
    public Result<LoginResponse> activate(ActivateRequest request, String clientIp) {
        // 1. 查找用户
        UserEntity user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            return Result.failure("AUTH_001", "用户不存在");
        }

        // 2. 检查状态
        if (!UserStatus.PENDING_ACTIVATION.name().equals(user.getStatus())) {
            return Result.failure("AUTH_006", "账号已激活，请直接登录");
        }

        // 3. 验证激活码
        if (user.getActivationCode() == null ||
                !user.getActivationCode().equals(request.activationCode())) {
            return Result.failure("AUTH_007", "激活码无效");
        }
        if (user.getActivationExpiresAt() != null &&
                user.getActivationExpiresAt().isBefore(LocalDateTime.now())) {
            return Result.failure("AUTH_008", "激活码已过期");
        }

        // 4. 设置密码并激活
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE.name());
        user.setActivationCode(null);
        user.setActivationExpiresAt(null);
        user.setUsingDefaultPassword(false);
        userRepository.save(user);

        log.info("User activated: {}", user.getEmail());

        // 5. 生成 Token（新激活用户不需要强制修改密码）
        return generateLoginResponse(user, clientIp, false);
    }

    /**
     * 刷新Token
     * <p>
     * 如果旧 token 包含组织上下文（orgId、memberCardId），则新 token 也会包含
     */
    @Transactional
    public Result<LoginResponse> refreshToken(RefreshTokenRequest request) {
        // 1. 验证 refresh token
        String tokenHash = passwordEncoder.hashToken(request.refreshToken());
        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);

        if (tokenEntity == null) {
            return Result.failure("AUTH_009", "无效的刷新令牌");
        }

        // 2. 查找用户
        UserEntity user = userRepository.findById(tokenEntity.getUserId()).orElse(null);
        if (user == null || !UserStatus.ACTIVE.name().equals(user.getStatus())) {
            return Result.failure("AUTH_010", "用户状态异常");
        }

        // 3. 撤销旧token
        refreshTokenRepository.revoke(tokenEntity.getId());

        // 4. 生成新Token（保留组织上下文）
        boolean requirePasswordChange = user.isUsingDefaultPassword();
        return generateLoginResponseWithOrgContext(user, null,
                tokenEntity.getOrgId(), tokenEntity.getMemberCardId(), tokenEntity.getRole(), requirePasswordChange);
    }

    /**
     * 登出
     */
    @Transactional
    public Result<Void> logout(String userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        return Result.success(null);
    }

    /**
     * 切换组织
     * <p>
     * 验证用户在目标组织中的成员卡是否存在且活跃，成功后返回包含组织信息的新 Token
     */
    @Transactional
    public Result<SwitchOrganizationResponse> switchOrganization(String userId, SwitchOrganizationRequest request) {
        // 1. 查找用户
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.failure("AUTH_001", "用户不存在");
        }

        // 2. 检查用户状态
        if (!UserStatus.ACTIVE.name().equals(user.getStatus())) {
            return Result.failure("AUTH_010", "用户状态异常");
        }

        // 3. 查找用户在组织中的成员关系
        UserOrganizationEntity userOrg = userOrganizationRepository
                .findByUserIdAndOrgId(userId, request.orgId())
                .orElse(null);
        if (userOrg == null) {
            return Result.failure("AUTH_015", "用户不属于该组织");
        }

        // 4. 检查成员关系状态
        if (!"ACTIVE".equals(userOrg.getStatus())) {
            return Result.failure("AUTH_016", "用户在该组织中已被禁用");
        }

        // 5. 检查成员卡ID是否存在
        String memberCardId = userOrg.getMemberCardId();
        if (memberCardId == null || memberCardId.isBlank()) {
            return Result.failure("AUTH_017", "成员卡片不存在");
        }

        // 6. 查询成员卡片状态
        Result<CardDTO> cardResult = cardServiceClient.findById(memberCardId, userId, Yield.basic());
        if (!cardResult.isSuccess()) {
            log.warn("查询成员卡片失败: userId={}, memberCardId={}, error={}",
                    userId, memberCardId, cardResult.getMessage());
            return Result.failure("AUTH_018", "成员卡片查询失败");
        }

        CardDTO memberCard = cardResult.getData();
        if (memberCard == null) {
            return Result.failure("AUTH_017", "成员卡片不存在");
        }

        // 7. 验证成员卡片是否活跃
        if (memberCard.getCardStyle() != CardStyle.ACTIVE) {
            return Result.failure("AUTH_019", "成员卡片已失效");
        }

        // 8. 生成带组织信息的 Token
        String accessToken = jwtTokenProvider.generateAccessTokenWithOrg(
                user, request.orgId(), memberCardId, userOrg.getRole());

        // 9. 生成新的 refreshToken（包含组织上下文），并撤销旧的
        refreshTokenRepository.revokeAllByUserId(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // 保存新的 refreshToken（包含组织上下文）
        RefreshTokenEntity tokenEntity = new RefreshTokenEntity();
        tokenEntity.setId(SnowflakeIdGenerator.generateStr());
        tokenEntity.setUserId(userId);
        tokenEntity.setTokenHash(passwordEncoder.hashToken(refreshToken));
        tokenEntity.setOrgId(request.orgId());
        tokenEntity.setMemberCardId(memberCardId);
        tokenEntity.setRole(userOrg.getRole());
        tokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration()));
        refreshTokenRepository.save(tokenEntity);

        log.info("User switched organization: userId={}, orgId={}", userId, request.orgId());

        return Result.success(new SwitchOrganizationResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration(),
                request.orgId(),
                memberCardId,
                userOrg.getRole()
        ));
    }

    private Result<LoginResponse> generateLoginResponse(UserEntity user, String clientIp, boolean requirePasswordChange) {
        return generateLoginResponseWithOrgContext(user, clientIp, null, null, null, requirePasswordChange);
    }

    /**
     * 生成登录响应（支持组织上下文）
     *
     * @param user                  用户实体
     * @param clientIp              客户端IP
     * @param orgId                 组织ID（可空）
     * @param memberCardId          成员卡片ID（可空）
     * @param role                  角色（可空）
     * @param requirePasswordChange 是否需要修改密码
     * @return 登录响应
     */
    private Result<LoginResponse> generateLoginResponseWithOrgContext(
            UserEntity user, String clientIp, String orgId, String memberCardId, String role, boolean requirePasswordChange) {
        // 生成 Token（根据是否有组织上下文决定生成哪种 token）
        String accessToken;
        if (orgId != null && memberCardId != null) {
            accessToken = jwtTokenProvider.generateAccessTokenWithOrg(user, orgId, memberCardId, role);
        } else {
            accessToken = jwtTokenProvider.generateAccessToken(user);
        }
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 保存 refresh token（包含组织上下文）
        RefreshTokenEntity tokenEntity = new RefreshTokenEntity();
        tokenEntity.setId(SnowflakeIdGenerator.generateStr());
        tokenEntity.setUserId(user.getId());
        tokenEntity.setTokenHash(passwordEncoder.hashToken(refreshToken));
        tokenEntity.setOrgId(orgId);
        tokenEntity.setMemberCardId(memberCardId);
        tokenEntity.setRole(role);
        tokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration()));
        refreshTokenRepository.save(tokenEntity);

        // 更新登录信息
        user.setLastLoginAt(LocalDateTime.now());
        if (clientIp != null) {
            user.setLastLoginIp(clientIp);
        }
        user.setLoginFailCount(0);
        userRepository.save(user);

        // 查询用户组织
        List<OrganizationDTO> organizations = getUserOrganizations(user.getId());

        LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration(),
                toUserDTO(user),
                organizations,
                requirePasswordChange
        );

        return Result.success(response);
    }

    private void handleLoginFailure(UserEntity user) {
        user.setLoginFailCount(user.getLoginFailCount() + 1);
        if (user.getLoginFailCount() >= maxFailCount) {
            user.setStatus(UserStatus.LOCKED.name());
            log.warn("User account locked due to too many failed attempts: {}", user.getEmail());
        }
        userRepository.save(user);
    }

    private List<OrganizationDTO> getUserOrganizations(String userId) {
        List<UserOrganizationEntity> userOrgs = userOrganizationRepository.findByUserId(userId);
        List<String> orgIds = userOrgs.stream()
                .map(UserOrganizationEntity::getOrgId)
                .collect(Collectors.toList());

        return organizationRepository.findByIds(orgIds).stream()
                .map(this::toOrganizationDTO)
                .collect(Collectors.toList());
    }

    private UserDTO toUserDTO(UserEntity user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatar(),
                user.getPhone(),
                user.isSuperAdmin(),
                user.getStatus(),
                user.isUsingDefaultPassword(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    private OrganizationDTO toOrganizationDTO(OrganizationEntity org) {
        return new OrganizationDTO(
                org.getId(),
                org.getName(),
                org.getDescription(),
                org.getLogo(),
                org.getMemberCardTypeId(),
                org.getAttendanceEnabled(),
                org.getStatus(),
                org.getCreatedBy(),
                org.getCreatedAt()
        );
    }
}
