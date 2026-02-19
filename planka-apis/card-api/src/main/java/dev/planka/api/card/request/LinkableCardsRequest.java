package dev.planka.api.card.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可选关联卡片查询请求
 * <p>
 * 根据关联属性ID查询可选的目标卡片列表，支持分页和关键词搜索
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkableCardsRequest {

    /**
     * 关联属性ID
     * <p>
     * 格式为 "{linkTypeId}:{SOURCE|TARGET}"，用于确定对侧卡片类型
     */
    @NotBlank(message = "关联属性ID不能为空")
    private String linkFieldId;

    /**
     * 搜索关键词
     * <p>
     * 用于搜索卡片标题，为空时返回全部
     */
    private String keyword;

    /**
     * 页码（从 0 开始）
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * 每页大小
     */
    @Builder.Default
    private Integer size = 20;
}
