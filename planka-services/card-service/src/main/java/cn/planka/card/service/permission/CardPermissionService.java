package cn.planka.card.service.permission;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.card.service.permission.model.BatchPermissionCheckResult;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.field.FieldId;
import cn.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import cn.planka.card.service.permission.exception.PermissionDeniedException;

import java.util.List;
import java.util.Map;
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
     * @param cardTypeId   实体类型ID
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
     * 属性编辑权限校验（含关联属性双向检查）
     * <p>
     * 在普通属性权限检查基础上，额外检查关联属性的对侧实体类型权限配置。
     * <ul>
     *     <li>当前侧权限不通过 → 正常阻断提示</li>
     *     <li>对侧权限不通过 → 提示对侧实体类型配置了权限限制</li>
     * </ul>
     *
     * @param targetCardId              目标卡片ID
     * @param operatorId                操作人ID（成员卡片ID）
     * @param changedFieldIds           变更的属性ID集合（普通属性 + 级联属性）
     * @param changedLinkFieldIds       变更的关联属性ID集合（linkFieldId 格式）
     * @param targetCardIdsByLinkField  每个 linkFieldId 对应的实际对端卡片ID列表
     * @throws PermissionDeniedException 无权限时抛出
     */
    void checkFieldEditPermission(
            CardId targetCardId,
            CardId operatorId,
            Set<FieldId> changedFieldIds,
            Set<String> changedLinkFieldIds,
            Map<String, List<String>> targetCardIdsByLinkField
    );

    /**
     * 批量应用字段读权限
     * <p>
     * 根据操作人的权限，直接修改 CardDTO 中 FieldValue 的 permissionStatus：
     * <ul>
     *     <li>有 READ 权限：不修改（permissionStatus 保持 null）</li>
     *     <li>无权限：设置 NO_PERMISSION，value 置为 null</li>
     * </ul>
     *
     * @param cards      卡片DTO列表
     * @param operatorId 操作人ID（成员卡片ID）
     */
    void applyFieldReadPermissions(
            List<CardDTO> cards,
            CardId operatorId
    );
}
