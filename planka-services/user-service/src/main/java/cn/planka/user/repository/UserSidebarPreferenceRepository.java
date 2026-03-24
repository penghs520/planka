package cn.planka.user.repository;

import cn.planka.api.user.dto.SidebarPreferencesDTO;
import cn.planka.common.util.SnowflakeIdGenerator;
import cn.planka.user.mapper.UserSidebarPreferenceMapper;
import cn.planka.user.model.UserSidebarPreferenceEntity;
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
public class UserSidebarPreferenceRepository {

    private final UserSidebarPreferenceMapper userSidebarPreferenceMapper;
    private final SidebarPreferencesPrefsCodec sidebarPreferencesPrefsCodec;

    public SidebarPreferencesDTO loadOrEmpty(String userId, String orgId) {
        UserSidebarPreferenceEntity row = userSidebarPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserSidebarPreferenceEntity>()
                        .eq(UserSidebarPreferenceEntity::getUserId, userId)
                        .eq(UserSidebarPreferenceEntity::getOrgId, orgId));
        if (row == null || row.getPrefs() == null || row.getPrefs().isBlank()) {
            return SidebarPreferencesDTO.builder().pinnedCascadeRelationIds(List.of()).build();
        }
        return sidebarPreferencesPrefsCodec.parse(row.getPrefs());
    }

    public SidebarPreferencesDTO save(String userId, String orgId, List<String> pinnedCascadeRelationIds) {
        List<String> normalized = SidebarPinnedIds.orderedDistinctNonBlank(pinnedCascadeRelationIds);
        SidebarPreferencesDTO dto = SidebarPreferencesDTO.builder().pinnedCascadeRelationIds(normalized).build();
        String json;
        try {
            json = sidebarPreferencesPrefsCodec.serialize(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize user sidebar prefs failed", e);
        }

        LocalDateTime now = LocalDateTime.now();
        UserSidebarPreferenceEntity row = userSidebarPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserSidebarPreferenceEntity>()
                        .eq(UserSidebarPreferenceEntity::getUserId, userId)
                        .eq(UserSidebarPreferenceEntity::getOrgId, orgId));
        if (row == null) {
            UserSidebarPreferenceEntity insert = new UserSidebarPreferenceEntity();
            insert.setId(SnowflakeIdGenerator.generateStr());
            insert.setUserId(userId);
            insert.setOrgId(orgId);
            insert.setPrefs(json);
            insert.setCreatedAt(now);
            insert.setUpdatedAt(now);
            userSidebarPreferenceMapper.insert(insert);
        } else {
            row.setPrefs(json);
            row.setUpdatedAt(now);
            userSidebarPreferenceMapper.updateById(row);
        }
        return dto;
    }
}
