package dev.planka.extension.history.vo;

import dev.planka.domain.card.CardTitle;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 历史消息参数 VO - 包含填充后的显示名称
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoryArgumentVO {

    /**
     * 参数类型
     */
    private String type;

    /**
     * 是否已删除（Schema 被删除时为 true，使用备份名称）
     */
    private Boolean deleted;

    // ========== TEXT 类型 ==========
    /**
     * 文本值
     */
    private String value;

    // ========== OPERATE_FIELD / FIELD_VALUE_* 类型 ==========
    /**
     * 属性ID
     */
    private String fieldId;

    /**
     * 属性名称（查询时填充，或使用备份）
     */
    private String fieldName;

    /**
     * 显示值（格式化后的值）
     */
    private String displayValue;

    // ========== STATUS 类型 ==========
    /**
     * 状态ID
     */
    private String statusId;

    /**
     * 状态名称（查询时填充，或使用备份）
     */
    private String statusName;

    // ========== FIELD_VALUE_LINK 类型 ==========
    /**
     * 关联卡片列表
     */
    private List<LinkedCardRefVO> cards;

    // ========== TEXT_DIFF 类型 ==========
    /**
     * 差异块列表
     */
    private List<DiffHunkVO> hunks;

    /**
     * 关联卡片引用 VO
     */
    public record LinkedCardRefVO(String cardId, CardTitle cardTitle, String cardTypeId) {}

    /**
     * 差异块 VO
     */
    public record DiffHunkVO(
            int oldStart,
            int oldCount,
            int newStart,
            int newCount,
            List<DiffLineVO> lines
    ) {}

    /**
     * 差异行 VO
     */
    public record DiffLineVO(String type, String content) {}
}
