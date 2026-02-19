package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.service.permission.model.BatchPermissionCheckResult;
import dev.planka.card.service.permission.model.FieldValueWithPermission;
import dev.planka.card.service.permission.exception.PermissionDeniedException;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;

import java.util.List;
import java.util.Set;

/**
 * 卡片权限服务
 * <p>
 * 负责卡片操作权限和属性级权限的校验
 * <p>
 * 核心功能：
 * <ul>
 *     <li>单个卡片操作权限校验</li>
 *     <li>批量卡片操作权限校验</li>
 *     <li>创建卡片权限校验（仅组织级）</li>
 *     <li>属性编辑权限校验</li>
 *     <li>属性权限应用（查询时）</li>
 * </ul>
 */
public interface CardPermissionService {

    /**
     * 单个卡片操作权限校验
     * <p>
     * 校验操作人是否有权限对目标卡片执行指定操作
     *
     * @param operation     操作类型
     * @param targetCardId  目标卡片ID
     * @param operatorId    操作人ID（成员卡片ID）
     * @throws PermissionDeniedException 无权限时抛出
     */
    void checkCardOperation(
            CardOperation operation,
            CardId targetCardId,
            CardId operatorId
    );

    /**
     * 创建卡片权限校验（仅组织级）
     * <p>
     * 创建时卡片不存在，无法评估卡片条件，因此仅校验操作人条件（组织级）
     *
     * @param operation    操作类型（通常为 CREATE）
     * @param cardTypeId   卡片类型ID
     * @param operatorId   操作人ID（成员卡片ID）
     * @throws PermissionDeniedException 无权限时抛出
     */
    void checkCardOperationForCreate(
            CardOperation operation,
            CardTypeId cardTypeId,
            CardId operatorId
    );

    /**
     * 批量卡片操作权限校验
     * <p>
     * 批量校验多个卡片的操作权限，返回允许和拒绝的卡片列表
     * <p>
     * 性能优化：
     * <ul>
     *     <li>通过 CardCacheService 快速获取卡片基础信息</li>
     *     <li>卫语句快速判断无权限配置场景</li>
     *     <li>按 (CardTypeId, SpaceId) 分组减少重复查询</li>
     * </ul>
     *
     * @param operation      操作类型
     * @param targetCardIds  目标卡片ID列表
     * @param operatorId     操作人ID（成员卡片ID）
     * @return 批量权限校验结果（包含允许和拒绝的卡片列表）
     */
    BatchPermissionCheckResult batchCheckCardOperation(
            CardOperation operation,
            List<CardId> targetCardIds,
            CardId operatorId
    );

    /**
     * 属性编辑权限校验
     * <p>
     * 校验操作人是否有权限编辑目标卡片的指定属性
     *
     * @param targetCardId     目标卡片ID
     * @param operatorId       操作人ID（成员卡片ID）
     * @param changedFieldIds  变更的属性ID集合
     * @throws PermissionDeniedException 无权限时抛出
     */
    void checkFieldEditPermission(
            CardId targetCardId,
            CardId operatorId,
            Set<FieldId> changedFieldIds
    );

    /**
     * 属性权限应用（查询时）
     * <p>
     * 根据操作人的权限，对卡片的属性值进行权限处理：
     * <ul>
     *     <li>有 READ 权限：返回完整值</li>
     *     <li>仅 DESENSITIZED_READ 权限：返回脱敏值</li>
     *     <li>无权限：返回 null + 状态 + 提示信息</li>
     * </ul>
     *
     * @param card       卡片DTO
     * @param operatorId 操作人ID（成员卡片ID）
     * @return 带权限状态的属性值列表
     */
    List<FieldValueWithPermission> applyFieldPermissions(
            CardDTO card,
            CardId operatorId
    );
}
