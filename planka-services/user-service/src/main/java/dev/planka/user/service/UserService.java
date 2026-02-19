package dev.planka.user.service;

import dev.planka.api.user.dto.OrganizationDTO;
import dev.planka.api.user.dto.UserDTO;
import dev.planka.api.user.request.ChangePasswordRequest;
import dev.planka.api.user.request.UpdateUserRequest;
import dev.planka.common.result.Result;
import dev.planka.user.model.OrganizationEntity;
import dev.planka.user.model.UserEntity;
import dev.planka.user.model.UserOrganizationEntity;
import dev.planka.user.repository.OrganizationRepository;
import dev.planka.user.repository.UserOrganizationRepository;
import dev.planka.user.repository.UserRepository;
import dev.planka.user.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取当前用户信息
     */
    public Result<UserDTO> getCurrentUser(String userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.failure("USER_001", "用户不存在");
        }
        return Result.success(toUserDTO(user));
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public Result<UserDTO> updateCurrentUser(String userId, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.failure("USER_001", "用户不存在");
        }

        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        userRepository.save(user);
        return Result.success(toUserDTO(user));
    }

    /**
     * 修改密码
     */
    @Transactional
    public Result<Void> changePassword(String userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.failure("USER_001", "用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            return Result.failure("USER_002", "原密码错误");
        }

        // 设置新密码，清除默认密码标记
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUsingDefaultPassword(false);
        userRepository.save(user);

        log.info("User changed password: {}", user.getEmail());
        return Result.success(null);
    }

    /**
     * 获取用户的组织列表
     */
    public Result<List<OrganizationDTO>> getUserOrganizations(String userId) {
        List<UserOrganizationEntity> userOrgs = userOrganizationRepository.findByUserId(userId);
        List<String> orgIds = userOrgs.stream()
                .map(UserOrganizationEntity::getOrgId)
                .collect(Collectors.toList());

        List<OrganizationDTO> organizations = organizationRepository.findByIds(orgIds).stream()
                .map(this::toOrganizationDTO)
                .collect(Collectors.toList());

        return Result.success(organizations);
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
