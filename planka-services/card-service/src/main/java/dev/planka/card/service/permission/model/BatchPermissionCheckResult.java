package dev.planka.card.service.permission.model;

import dev.planka.domain.card.CardId;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量权限校验结果
 * <p>
 * 包含允许和拒绝的卡片列表，用于批量操作时返回部分成功的结果
 */
public class BatchPermissionCheckResult {

    private final List<CardId> allowed;
    private final List<PermissionDeniedItem> denied;

    public BatchPermissionCheckResult(List<CardId> allowed, List<PermissionDeniedItem> denied) {
        this.allowed = allowed != null ? allowed : new ArrayList<>();
        this.denied = denied != null ? denied : new ArrayList<>();
    }

    /**
     * 创建空结果
     */
    public static BatchPermissionCheckResult empty() {
        return new BatchPermissionCheckResult(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * 创建全部允许的结果
     */
    public static BatchPermissionCheckResult allAllowed(List<CardId> cardIds) {
        return new BatchPermissionCheckResult(new ArrayList<>(cardIds), new ArrayList<>());
    }

    /**
     * 创建全部拒绝的结果
     */
    public static BatchPermissionCheckResult allDenied(List<CardId> cardIds, String message) {
        List<PermissionDeniedItem> denied = new ArrayList<>();
        for (CardId cardId : cardIds) {
            denied.add(new PermissionDeniedItem(cardId, message));
        }
        return new BatchPermissionCheckResult(new ArrayList<>(), denied);
    }

    /**
     * 获取允许的卡片ID列表
     */
    public List<CardId> getAllowed() {
        return allowed;
    }

    /**
     * 获取拒绝的卡片列表（含原因）
     */
    public List<PermissionDeniedItem> getDenied() {
        return denied;
    }

    /**
     * 是否全部允许
     */
    public boolean isAllAllowed() {
        return denied.isEmpty();
    }

    /**
     * 是否全部拒绝
     */
    public boolean isAllDenied() {
        return allowed.isEmpty();
    }

    /**
     * 是否部分成功
     */
    public boolean hasPartialSuccess() {
        return !allowed.isEmpty() && !denied.isEmpty();
    }

    @Override
    public String toString() {
        return "BatchPermissionCheckResult{" +
                "allowed=" + allowed.size() +
                ", denied=" + denied.size() +
                '}';
    }
}
