package cn.planka.user.repository;

import cn.planka.api.user.dto.SidebarPreferencesDTO;
import cn.planka.common.util.SnowflakeIdGenerator;
import cn.planka.user.mapper.OrgSidebarWorkspacePreferenceMapper;
import cn.planka.user.model.OrgSidebarWorkspacePreferenceEntity;
import cn.planka.user.sidebar.SidebarPinnedIds;
import cn.planka.user.sidebar.SidebarPreferencesPrefsCodec;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrgSidebarWorkspacePreferenceRepository {

    private final OrgSidebarWorkspacePreferenceMapper orgSidebarWorkspacePreferenceMapper;
    private final SidebarPreferencesPrefsCodec sidebarPreferencesPrefsCodec;

    public SidebarPreferencesDTO loadOrEmpty(String orgId) {
        OrgSidebarWorkspacePreferenceEntity row = orgSidebarWorkspacePreferenceMapper.selectOne(
                new LambdaQueryWrapper<OrgSidebarWorkspacePreferenceEntity>()
                        .eq(OrgSidebarWorkspacePreferenceEntity::getOrgId, orgId));
        if (row == null || row.getPrefs() == null || row.getPrefs().isBlank()) {
            return SidebarPreferencesDTO.builder().pinnedCascadeRelationIds(List.of()).build();
        }
        return sidebarPreferencesPrefsCodec.parse(row.getPrefs());
    }

    public SidebarPreferencesDTO save(String orgId, List<String> pinnedCascadeRelationIds) {
        List<String> normalized = SidebarPinnedIds.orderedDistinctNonBlank(pinnedCascadeRelationIds);
        SidebarPreferencesDTO dto = SidebarPreferencesDTO.builder().pinnedCascadeRelationIds(normalized).build();
        String json;
        try {
            json = sidebarPreferencesPrefsCodec.serialize(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize org workspace sidebar prefs failed", e);
        }

        LocalDateTime now = LocalDateTime.now();
        OrgSidebarWorkspacePreferenceEntity row = orgSidebarWorkspacePreferenceMapper.selectOne(
                new LambdaQueryWrapper<OrgSidebarWorkspacePreferenceEntity>()
                        .eq(OrgSidebarWorkspacePreferenceEntity::getOrgId, orgId));
        if (row == null) {
            OrgSidebarWorkspacePreferenceEntity insert = new OrgSidebarWorkspacePreferenceEntity();
            insert.setId(SnowflakeIdGenerator.generateStr());
            insert.setOrgId(orgId);
            insert.setPrefs(json);
            insert.setCreatedAt(now);
            insert.setUpdatedAt(now);
            orgSidebarWorkspacePreferenceMapper.insert(insert);
        } else {
            row.setPrefs(json);
            row.setUpdatedAt(now);
            orgSidebarWorkspacePreferenceMapper.updateById(row);
        }
        return dto;
    }
}
