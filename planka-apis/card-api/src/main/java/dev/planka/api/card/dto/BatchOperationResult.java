package dev.planka.api.card.dto;

import dev.planka.domain.card.CardId;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果
 * <p>
 * 用于返回批量更新、批量创建等操作的结果
 */
@Data
public class BatchOperationResult {
    /**
     * 成功的卡片ID列表
     */
    private List<CardId> successIds = new ArrayList<>();

    /**
     * 失败的卡片ID列表
     */
    private List<CardId> failedIds = new ArrayList<>();


    public static BatchOperationResult success(List<CardId> successIds) {
        BatchOperationResult result = new BatchOperationResult();
        result.setSuccessIds(successIds);
        return result;
    }

    public static BatchOperationResult failed(List<CardId> failedIds) {
        BatchOperationResult result = new BatchOperationResult();
        result.setFailedIds(failedIds);
        return result;
    }

    public boolean isAllSuccess() {
        return failedIds.isEmpty();
    }

    public int getTotalCount() {
        return successIds.size() + failedIds.size();
    }

    public int getSuccessCount() {
        return successIds.size();
    }

    public int getFailedCount() {
        return failedIds.size();
    }

    /**
     * 失败详情
     */
    @Data
    public static class FailureDetail {
        private CardId cardId;
        private String reason;

        public FailureDetail() {
        }

        public FailureDetail(CardId cardId, String reason) {
            this.cardId = cardId;
            this.reason = reason;
        }
    }
}
