package dev.planka.api.view.response;

import dev.planka.api.card.dto.CardDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分组卡片数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupedCardData {

    /**
     * 分组值（可能是枚举选项ID、日期等）
     */
    private Object groupValue;

    /**
     * 分组显示名称
     */
    private String groupLabel;

    /**
     * 该组的卡片数量
     */
    private int count;

    /**
     * 该组的卡片列表（支持懒加载，可能为空）
     */
    private List<CardDTO> cards;

    /**
     * 是否已展开（前端状态提示）
     */
    private boolean expanded;

    /**
     * 创建只包含摘要信息的分组（懒加载时使用）
     */
    public static GroupedCardData summary(Object groupValue, String groupLabel, int count) {
        return GroupedCardData.builder()
                .groupValue(groupValue)
                .groupLabel(groupLabel)
                .count(count)
                .cards(List.of())
                .expanded(false)
                .build();
    }
}
