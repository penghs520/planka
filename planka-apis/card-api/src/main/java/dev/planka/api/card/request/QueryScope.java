package dev.planka.api.card.request;

import dev.planka.domain.card.CardStyle;
import lombok.Data;

import java.util.List;

/**
 * 查询范围
 */
@Data
public class QueryScope {
    /** 卡片类型ID列表 */
    private List<String> cardTypeIds;

    /** 卡片ID列表 */
    private List<String> cardIds;

    /** 状态列表 */
    private List<CardStyle> cardStyles;
}
