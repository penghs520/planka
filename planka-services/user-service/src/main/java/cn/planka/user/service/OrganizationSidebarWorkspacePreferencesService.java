package cn.planka.user.service;

import cn.planka.api.user.dto.SidebarPreferencesDTO;
import cn.planka.api.user.request.UpdateSidebarPreferencesRequest;
import cn.planka.common.result.Result;
import cn.planka.user.model.OrganizationEntity;
import cn.planka.user.repository.OrgSidebarWorkspacePreferenceRepository;
import cn.planka.user.repository.OrganizationRepository;
import cn.planka.user.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织工作空间侧栏默认（{@code sys_org_sidebar_workspace_preference}，每组织一行），仅管理员可写
 */
@Service
@RequiredArgsConstructor
public class OrganizationSidebarWorkspacePreferencesService {

    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationService organizationService;
    private final OrgSidebarWorkspacePreferenceRepository orgSidebarWorkspacePreferenceRepository;

    @Transactional(readOnly = true)
    public Result<SidebarPreferencesDTO> get(String orgId, String userId) {
        if (orgId == null || orgId.isBlank()) {
            return Result.failure("ORG_REQUIRED", "组织ID无效");
        }
        if (userOrganizationRepository.findByUserIdAndOrgId(userId, orgId).isEmpty()) {
            return Result.failure("ORG_002", "您不是该组织的成员");
        }
        OrganizationEntity org = organizationRepository.findById(orgId).orElse(null);
        if (org == null) {
            return Result.failure("ORG_001", "组织不存在");
        }
        return Result.success(orgSidebarWorkspacePreferenceRepository.loadOrEmpty(orgId));
    }

    @Transactional
    public Result<SidebarPreferencesDTO> update(String orgId, String userId, UpdateSidebarPreferencesRequest request) {
        Result<Void> permCheck = organizationService.checkAdminPermission(orgId, userId);
        if (!permCheck.isSuccess()) {
            return Result.failure(permCheck.getCode(), permCheck.getMessage());
        }
        OrganizationEntity org = organizationRepository.findById(orgId).orElse(null);
        if (org == null) {
            return Result.failure("ORG_001", "组织不存在");
        }
        List<String> requestIds = request.getPinnedStructureIds() != null
                ? new ArrayList<>(request.getPinnedStructureIds())
                : new ArrayList<>();
        SidebarPreferencesDTO saved = orgSidebarWorkspacePreferenceRepository.save(orgId, requestIds);
        return Result.success(saved);
    }
}
