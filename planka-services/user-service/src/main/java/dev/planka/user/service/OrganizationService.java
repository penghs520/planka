package dev.planka.user.service;

import dev.planka.api.schema.OutsourcingConfigClient;
import dev.planka.api.user.dto.OrganizationDTO;
import dev.planka.api.user.enums.OrganizationRole;
import dev.planka.api.user.request.CreateOrganizationRequest;
import dev.planka.api.user.request.UpdateOrganizationRequest;
import dev.planka.common.result.Result;
import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.user.model.OrganizationEntity;
import dev.planka.user.model.UserOrganizationEntity;
import dev.planka.user.repository.OrganizationRepository;
import dev.planka.user.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 组织服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final MemberCardTypeService memberCardTypeService;
    private final MemberCardService memberCardService;
    private final RootCardTypeService rootCardTypeService;
    private final OutsourcingConfigClient outsourcingConfigClient;

    /**
     * 创建组织
     */
    @Transactional
    public Result<OrganizationDTO> createOrganization(String userId, CreateOrganizationRequest request) {
        // 1. 创建组织
        OrganizationEntity org = new OrganizationEntity();
        org.setId(SnowflakeIdGenerator.generateStr());
        org.setName(request.name());
        org.setDescription(request.description());
        org.setLogo(request.logo());
        org.setStatus("ACTIVE");
        org.setCreatedBy(userId);

        // 2. 创建成员属性集（包含用户名、邮箱、电话字段定义）
        String memberAbstractCardTypeId = memberCardTypeService.createMemberAbstractCardType(org.getId());

        // 3. 创建成员实体类型（继承属性集）
        String memberCardTypeId = memberCardTypeService.createMemberCardType(org.getId(), memberAbstractCardTypeId);
        org.setMemberCardTypeId(memberCardTypeId);

        // 4. 创建任意卡属性集及关联（创建人/归档人/丢弃人，关联目标为成员属性集）
        rootCardTypeService.createRootCardType(org.getId());

        // 注意：考勤配置采用懒加载方式，不在组织创建时初始化
        // 当用户首次开启考勤功能时，前端会调用 createDefaultConfig 接口创建配置

        organizationRepository.save(org);
        log.info("Organization created: {} by user {}", org.getId(), userId);

        // 5. 创建创建人的成员关系（OWNER）
        UserOrganizationEntity userOrg = new UserOrganizationEntity();
        userOrg.setId(SnowflakeIdGenerator.generateStr());
        userOrg.setUserId(userId);
        userOrg.setOrgId(org.getId());
        userOrg.setRole(OrganizationRole.OWNER.name());
        userOrg.setStatus("ACTIVE");
        userOrg.setInvitedBy(null); // 自己创建的
        userOrganizationRepository.save(userOrg);

        // 6. 创建成员卡片（预留接口）
        String memberCardId = memberCardService.createMemberCard(org.getId(), memberCardTypeId, userId);
        if (memberCardId != null) {
            userOrg.setMemberCardId(memberCardId);
            userOrganizationRepository.save(userOrg);
        }

        return Result.success(toDTO(org));
    }

    /**
     * 获取组织详情
     */
    public Result<OrganizationDTO> getOrganization(String orgId) {
        OrganizationEntity org = organizationRepository.findById(orgId).orElse(null);
        if (org == null) {
            return Result.failure("ORG_001", "组织不存在");
        }
        return Result.success(toDTO(org));
    }

    /**
     * 更新组织信息
     */
    @Transactional
    public Result<OrganizationDTO> updateOrganization(String orgId, String userId, UpdateOrganizationRequest request) {
        // 检查权限
        Result<Void> permCheck = checkAdminPermission(orgId, userId);
        if (!permCheck.isSuccess()) {
            return Result.failure(permCheck.getCode(), permCheck.getMessage());
        }

        OrganizationEntity org = organizationRepository.findById(orgId).orElse(null);
        if (org == null) {
            return Result.failure("ORG_001", "组织不存在");
        }

        // 检测考勤功能是否从关闭变为开启
        boolean wasAttendanceDisabled = org.getAttendanceEnabled() == null || !org.getAttendanceEnabled();
        boolean willAttendanceBeEnabled = request.attendanceEnabled() != null && request.attendanceEnabled();
        boolean isFirstTimeEnabling = wasAttendanceDisabled && willAttendanceBeEnabled;

        if (request.name() != null) {
            org.setName(request.name());
        }
        if (request.description() != null) {
            org.setDescription(request.description());
        }
        if (request.logo() != null) {
            org.setLogo(request.logo());
        }
        if (request.attendanceEnabled() != null) {
            org.setAttendanceEnabled(request.attendanceEnabled());
        }

        organizationRepository.save(org);

        // 首次开启考勤功能时，初始化考勤卡片类型
        if (isFirstTimeEnabling) {
            try {
                log.info("首次开启考勤功能，开始初始化考勤卡片类型: orgId={}", orgId);
                Result<Void> initResult = outsourcingConfigClient.initOutsourcingForOrg(orgId);
                if (initResult.isSuccess()) {
                    log.info("考勤卡片类型初始化成功: orgId={}", orgId);
                } else {
                    log.error("考勤卡片类型初始化失败: orgId={}, error={}", orgId, initResult.getMessage());
                    // 不影响组织更新，只记录错误
                }
            } catch (Exception e) {
                log.error("考勤卡片类型初始化异常: orgId={}", orgId, e);
                // 不影响组织更新，只记录错误
            }
        }

        return Result.success(toDTO(org));
    }

    /**
     * 删除组织
     */
    @Transactional
    public Result<Void> deleteOrganization(String orgId, String userId) {
        // 检查权限（只有 OWNER 可以删除）
        Result<Void> permCheck = checkOwnerPermission(orgId, userId);
        if (!permCheck.isSuccess()) {
            return permCheck;
        }

        OrganizationEntity org = organizationRepository.findById(orgId).orElse(null);
        if (org == null) {
            return Result.failure("ORG_001", "组织不存在");
        }

        // 删除所有成员关系
        userOrganizationRepository.deleteByOrgId(orgId);

        // 软删除组织
        organizationRepository.delete(orgId);
        log.info("Organization deleted: {} by user {}", orgId, userId);

        return Result.success(null);
    }

    /**
     * 检查用户是否有管理员权限（OWNER 或 ADMIN）
     */
    public Result<Void> checkAdminPermission(String orgId, String userId) {
        UserOrganizationEntity userOrg = userOrganizationRepository
                .findByUserIdAndOrgId(userId, orgId).orElse(null);

        if (userOrg == null) {
            return Result.failure("ORG_002", "您不是该组织的成员");
        }

        String role = userOrg.getRole();
        if (!OrganizationRole.OWNER.name().equals(role) &&
                !OrganizationRole.ADMIN.name().equals(role)) {
            return Result.failure("ORG_003", "权限不足，需要管理员权限");
        }

        return Result.success(null);
    }

    /**
     * 检查用户是否有所有者权限
     */
    public Result<Void> checkOwnerPermission(String orgId, String userId) {
        UserOrganizationEntity userOrg = userOrganizationRepository
                .findByUserIdAndOrgId(userId, orgId).orElse(null);

        if (userOrg == null) {
            return Result.failure("ORG_002", "您不是该组织的成员");
        }

        if (!OrganizationRole.OWNER.name().equals(userOrg.getRole())) {
            return Result.failure("ORG_004", "权限不足，需要所有者权限");
        }

        return Result.success(null);
    }

    private OrganizationDTO toDTO(OrganizationEntity org) {
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
