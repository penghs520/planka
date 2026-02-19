package dev.planka.user.service;

import dev.planka.api.user.dto.OrganizationDTO;
import dev.planka.api.user.dto.UserDTO;
import dev.planka.api.user.enums.UserStatus;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.user.model.OrganizationEntity;
import dev.planka.user.model.UserEntity;
import dev.planka.user.repository.OrganizationRepository;
import dev.planka.user.repository.UserRepository;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 超管服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * 获取所有组织列表
     */
    public Result<PageResult<OrganizationDTO>> listAllOrganizations(int page, int size) {
        Page<OrganizationEntity> pageResult = organizationRepository.findAll(page, size);

        List<OrganizationDTO> organizations = pageResult.getRecords().stream()
                .map(this::toOrganizationDTO)
                .collect(Collectors.toList());

        return Result.success(new PageResult<>(
                organizations,
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getTotal()
        ));
    }

    /**
     * 获取所有用户列表
     */
    public Result<PageResult<UserDTO>> listAllUsers(int page, int size) {
        Page<UserEntity> pageResult = userRepository.findAll(page, size);

        List<UserDTO> users = pageResult.getRecords().stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());

        return Result.success(new PageResult<>(
                users,
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getTotal()
        ));
    }

    /**
     * 修改用户状态
     */
    @Transactional
    public Result<UserDTO> updateUserStatus(String userId, String status) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.failure("USER_001", "用户不存在");
        }

        // 不能修改超管状态
        if (user.isSuperAdmin()) {
            return Result.failure("ADMIN_001", "不能修改超级管理员状态");
        }

        // 验证状态值
        try {
            UserStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return Result.failure("ADMIN_002", "无效的状态值");
        }

        user.setStatus(status);
        // 如果解锁，重置登录失败次数
        if (UserStatus.ACTIVE.name().equals(status)) {
            user.setLoginFailCount(0);
        }
        userRepository.save(user);

        log.info("User status updated: {} -> {}", userId, status);
        return Result.success(toUserDTO(user));
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
