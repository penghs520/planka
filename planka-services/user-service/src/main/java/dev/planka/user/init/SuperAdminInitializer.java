package dev.planka.user.init;

import dev.planka.api.user.enums.UserStatus;
import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.user.model.UserEntity;
import dev.planka.user.repository.UserRepository;
import dev.planka.user.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 超级管理员初始化器
 * <p>
 * 应用首次启动时自动创建超级管理员账号
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${user.super-admin.email}")
    private String superAdminEmail;

    @Value("${user.super-admin.default-password}")
    private String defaultPassword;

    @Override
    public void run(ApplicationArguments args) {
        // 检查是否已存在超级管理员
        if (userRepository.existsSuperAdmin()) {
            log.debug("Super admin already exists, skip initialization");
            return;
        }

        // 检查是否已存在该邮箱的用户
        if (userRepository.existsByEmail(superAdminEmail)) {
            log.warn("User with email {} already exists but is not super admin", superAdminEmail);
            return;
        }

        // 创建超级管理员
        UserEntity superAdmin = new UserEntity();
        superAdmin.setId(SnowflakeIdGenerator.generateStr());
        superAdmin.setEmail(superAdminEmail);
        superAdmin.setPasswordHash(passwordEncoder.encode(defaultPassword));
        superAdmin.setNickname("超级管理员");
        superAdmin.setSuperAdmin(true);
        superAdmin.setStatus(UserStatus.ACTIVE.name());
        superAdmin.setUsingDefaultPassword(true);

        userRepository.save(superAdmin);
        log.info("Super admin created successfully: {}", superAdminEmail);
    }
}
