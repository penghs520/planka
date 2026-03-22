package cn.planka.user.service;

import cn.planka.api.user.dto.OrganizationDTO;
import cn.planka.api.user.enums.OrganizationRole;
import cn.planka.api.user.request.CreateOrganizationRequest;
import cn.planka.api.user.request.UpdateOrganizationRequest;
import cn.planka.common.result.Result;
import cn.planka.common.util.SnowflakeIdGenerator;
import cn.planka.user.model.OrganizationEntity;
import cn.planka.user.model.UserOrganizationEntity;
import cn.planka.user.repository.OrganizationRepository;
import cn.planka.user.repository.UserOrganizationRepository;
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
    private final MemberCardService memberCardService;
    private final BuiltinCardTypeService builtinCardTypeService;

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

        // 2. 创建成员特征类型（包含用户名、邮箱、电话字段定义）
        String memberAbstractCardTypeId = builtinCardTypeService.createMemberAbstractCardType(org.getId());

        // 3. 创建成员__PLANKA_EINST__（继承特征类型）
        String memberCardTypeId = builtinCardTypeService.createMemberCardType(org.getId(), memberAbstractCardTypeId);
        org.setMemberCardTypeId(memberCardTypeId);

        // 4. 创建任意卡特征类型及关联（创建人/归档人/丢弃人，关联目标为成员特征类型）
        builtinCardTypeService.createAnyTraitAndSystemMemberLinks(org.getId());

        // 5. 内置 Team / Project / Issue 及业务关联
        builtinCardTypeService.initBuiltinTypes(org.getId());

        organizationRepository.save(org);
        log.info("Organization created: {} by user {}", org.getId(), userId);

        // 6. 创建创建人的成员关系（OWNER）
        UserOrganizationEntity userOrg = new UserOrganizationEntity();
        userOrg.setId(SnowflakeIdGenerator.generateStr());
        userOrg.setUserId(userId);
        userOrg.setOrgId(org.getId());
        userOrg.setRole(OrganizationRole.OWNER.name());
        userOrg.setStatus("ACTIVE");
        userOrg.setInvitedBy(null); // 自己创建的
        userOrganizationRepository.save(userOrg);

        // 7. 创建成员卡片（预留接口）
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

        if (request.name() != null) {
            org.setName(request.name());
        }
        if (request.description() != null) {
            org.setDescription(request.description());
        }
        if (request.logo() != null) {
            org.setLogo(request.logo());
        }

        organizationRepository.save(org);

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
                org.getStatus(),
                org.getCreatedBy(),
                org.getCreatedAt()
        );
    }
}
