package cn.planka.user.service;

import cn.planka.api.user.dto.SidebarPreferencesDTO;
import cn.planka.api.user.request.UpdateSidebarPreferencesRequest;
import cn.planka.common.result.Result;
import cn.planka.user.model.UserOrganizationEntity;
import cn.planka.user.repository.UserOrganizationRepository;
import cn.planka.user.repository.UserSidebarPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户在某组织下的侧栏偏好（{@code sys_user_sidebar_preference}，每用户每组织一行）
 */
@Service
@RequiredArgsConstructor
public class UserSidebarPreferencesService {

    private final UserOrganizationRepository userOrganizationRepository;
    private final UserSidebarPreferenceRepository userSidebarPreferenceRepository;

    @Transactional(readOnly = true)
    public Result<SidebarPreferencesDTO> get(String userId, String orgId) {
        if (orgId == null || orgId.isBlank()) {
            return Result.failure("ORG_REQUIRED", "请先选择组织");
        }
        UserOrganizationEntity rel = userOrganizationRepository.findByUserIdAndOrgId(userId, orgId).orElse(null);
        if (rel == null) {
            return Result.failure("MEMBER_NOT_FOUND", "当前用户不在该组织中");
        }
        return Result.success(userSidebarPreferenceRepository.loadOrEmpty(userId, orgId));
    }

    @Transactional
    public Result<SidebarPreferencesDTO> update(String userId, String orgId, UpdateSidebarPreferencesRequest request) {
        if (orgId == null || orgId.isBlank()) {
            return Result.failure("ORG_REQUIRED", "请先选择组织");
        }
        UserOrganizationEntity rel = userOrganizationRepository.findByUserIdAndOrgId(userId, orgId).orElse(null);
        if (rel == null) {
            return Result.failure("MEMBER_NOT_FOUND", "当前用户不在该组织中");
        }
        List<String> requestIds = request.getPinnedCascadeRelationIds() != null
                ? new ArrayList<>(request.getPinnedCascadeRelationIds())
                : new ArrayList<>();
        SidebarPreferencesDTO saved = userSidebarPreferenceRepository.save(userId, orgId, requestIds);
        return Result.success(saved);
    }
}
