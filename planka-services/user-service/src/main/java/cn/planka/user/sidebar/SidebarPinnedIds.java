package cn.planka.user.sidebar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 侧栏 prefs 中的结构钉选 ID 列表：保序、去重、去空白。
 */
public final class SidebarPinnedIds {

    private SidebarPinnedIds() {
    }

    public static List<String> orderedDistinctNonBlank(List<String> structureIds) {
        if (structureIds == null || structureIds.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String s : structureIds) {
            if (s != null && !s.isBlank()) {
                seen.add(s);
            }
        }
        return new ArrayList<>(seen);
    }
}
