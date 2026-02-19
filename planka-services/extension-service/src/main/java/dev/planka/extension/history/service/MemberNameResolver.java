package dev.planka.extension.history.service;

import dev.planka.api.card.CardServiceClient;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTitle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 成员名称解析器
 * <p>
 * 批量查询成员卡片ID对应的显示名称
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberNameResolver {

    private final CardServiceClient cardServiceClient;

    /**
     * 系统操作人ID
     */
    private static final String SYSTEM_OPERATOR = "system";

    /**
     * 批量解析成员名称
     *
     * @param memberCardIds 成员卡片ID集合
     * @return 成员卡片ID -> 显示名称的映射
     */
    public Map<String, String> resolveNames(Collection<String> memberCardIds) {
        if (memberCardIds == null || memberCardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 过滤掉系统操作人和空值
        List<String> validIds = memberCardIds.stream()
                .filter(id -> id != null && !id.isEmpty() && !SYSTEM_OPERATOR.equals(id))
                .distinct()
                .toList();

        if (validIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();

        try {
            // 批量查询卡片名称
            // 使用 "system" 作为操作人，因为这是内部服务调用
            Result<Map<String, CardTitle>> queryResult = cardServiceClient.queryCardNames(SYSTEM_OPERATOR, validIds);

            if (queryResult.isSuccess() && queryResult.getData() != null) {
                queryResult.getData().forEach((cardId, title) -> {
                    if (title != null && title.getDisplayValue() != null) {
                        result.put(cardId, title.getDisplayValue());
                    }
                });
            }
        } catch (Exception e) {
            log.warn("批量查询成员名称失败，将使用ID作为显示名称", e);
        }

        return result;
    }

    /**
     * 获取单个成员名称
     *
     * @param memberCardId 成员卡片ID
     * @param nameMap      名称映射（从 resolveNames 获取）
     * @return 显示名称，如果找不到则返回ID本身
     */
    public String getName(String memberCardId, Map<String, String> nameMap) {
        if (memberCardId == null || memberCardId.isEmpty()) {
            return "未知";
        }

        if (SYSTEM_OPERATOR.equals(memberCardId)) {
            return "系统";
        }

        return nameMap.getOrDefault(memberCardId, memberCardId);
    }
}
