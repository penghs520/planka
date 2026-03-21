package cn.planka.user.sidebar;

import cn.planka.api.user.dto.SidebarPreferencesDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 侧栏偏好表 prefs JSON 与 {@link SidebarPreferencesDTO} 互转
 */
@Component
@RequiredArgsConstructor
public class SidebarPreferencesPrefsCodec {

    private static final Logger log = LoggerFactory.getLogger(SidebarPreferencesPrefsCodec.class);

    private final ObjectMapper objectMapper;

    public SidebarPreferencesDTO parse(String json) {
        if (json == null || json.isBlank()) {
            return empty();
        }
        try {
            PrefsBody body = objectMapper.readValue(json, PrefsBody.class);
            List<String> ids = body.getPinnedStructureIds() != null
                    ? new ArrayList<>(body.getPinnedStructureIds())
                    : new ArrayList<>();
            return SidebarPreferencesDTO.builder().pinnedStructureIds(ids).build();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse sidebar prefs JSON: {}", e.getMessage());
            return empty();
        }
    }

    public String serialize(SidebarPreferencesDTO dto) throws JsonProcessingException {
        List<String> ids = dto.getPinnedStructureIds() != null
                ? new ArrayList<>(dto.getPinnedStructureIds())
                : new ArrayList<>();
        PrefsBody body = new PrefsBody();
        body.setPinnedStructureIds(ids);
        return objectMapper.writeValueAsString(body);
    }

    private static SidebarPreferencesDTO empty() {
        return SidebarPreferencesDTO.builder().pinnedStructureIds(new ArrayList<>()).build();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PrefsBody {
        private List<String> pinnedStructureIds = new ArrayList<>();
    }
}
