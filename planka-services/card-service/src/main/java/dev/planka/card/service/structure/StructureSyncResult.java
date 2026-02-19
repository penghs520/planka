package dev.planka.card.service.structure;

import java.util.Map;
import java.util.Set;

/**
 * 架构关联同步结果
 * <p>
 * 记录联动同步操作中额外产生的关联变更。
 *
 * @param synced       是否执行了同步
 * @param addedLinks   新增的关联 (linkFieldId -> cardIds)
 * @param removedLinks 删除的关联 (linkFieldId -> cardIds)
 */
public record StructureSyncResult(
        boolean synced,
        Map<String, Set<String>> addedLinks,
        Map<String, Set<String>> removedLinks
) {

    /**
     * 返回未执行同步的结果
     */
    public static StructureSyncResult noSync() {
        return new StructureSyncResult(false, Map.of(), Map.of());
    }

    /**
     * 创建同步结果
     *
     * @param addedLinks   新增的关联
     * @param removedLinks 删除的关联
     * @return 同步结果
     */
    public static StructureSyncResult of(Map<String, Set<String>> addedLinks,
                                         Map<String, Set<String>> removedLinks) {
        boolean hasChanges = !addedLinks.isEmpty() || !removedLinks.isEmpty();
        return new StructureSyncResult(hasChanges, addedLinks, removedLinks);
    }
}
