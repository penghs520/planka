package cn.agilean.kanban.api.card.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 更新关联关系请求
 * <p>
 * 替换指定关联属性下的所有关联关系
 */
@Data
@Builder
public class UpdateLinkRequest {

    /**
     * 源卡片 ID
     */
    @NotBlank(message = "卡片ID不能为空")
    private String cardId;

    /**
     * 关联属性ID
     * <p>
     * 格式为 "{linkTypeId}:{SOURCE|TARGET}"，如 "263671031548350464:SOURCE"
     */
    @NotBlank(message = "关联属性ID不能为空")
    private String linkFieldId;

    /**
     * 目标卡片 ID 列表
     * <p>
     * 替换模式：将关联关系替换为此列表中的卡片
     * 空列表表示清空所有关联
     */
    @NotNull(message = "目标卡片列表不能为null")
    private List<String> targetCardIds;

    /**
     * 是否跳过权限检查
     * <p>
     * 默认为 false，需要进行权限检查
     * 当从 CardService.update() 调用时设置为 true，因为已经在上层做过权限检查
     */
    private boolean skipPermissionCheck = false;
}
