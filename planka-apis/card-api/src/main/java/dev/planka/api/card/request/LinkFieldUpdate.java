package dev.planka.api.card.request;

import java.util.List;

/**
 * 关联属性更新
 * <p>
 * 用于在更新卡片时同时更新关联属性，使用覆盖式策略
 */
public record LinkFieldUpdate(
        /**
         * 关联属性ID
         * <p>
         * 格式为 "{linkTypeId}:{SOURCE|TARGET}"，如 "263671031548350464:SOURCE"
         */
        String linkFieldId,

        /**
         * 目标卡片ID列表
         * <p>
         * 覆盖模式：将关联关系替换为此列表中的卡片
         * 空列表表示清空所有关联
         */
        List<String> targetCardIds
) {
}
