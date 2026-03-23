package cn.planka.api.card.request;

import cn.planka.domain.card.CardId;
import lombok.Data;

import java.util.List;

/**
 * 批量操作请求
 * <p>
 * 用于批量存档、还原、回收等操作
 */
@Data
public class BatchOperationRequest {
    /**
     * 卡片ID列表
     */
    private List<CardId> cardIds;

    /**
     * 回收原因（仅回收操作使用）
     */
    private String discardReason;
}
