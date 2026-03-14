package cn.agilean.kanban.api.card.request;

import cn.agilean.kanban.domain.card.CardCycle;
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
    private List<CardCycle> cardCycles;
}
