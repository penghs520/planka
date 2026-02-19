package dev.planka.api.view.response;

import dev.planka.api.card.renderconfig.FieldRenderConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列元数据
 * <p>
 * 描述视图中每一列的显示信息，包括：
 * - 基础显示配置（标题、宽度、冻结、排序等）
 * - 字段类型信息
 * - 编辑相关配置（是否可编辑、是否必填）
 * - 类型特有的渲染配置（多态结构）
 * <p>
 * 对于关联字段（LINK类型），fieldId 格式为 "{linkTypeId}:{SOURCE|TARGET}"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMeta {

    /**
     * 字段ID
     * <p>
     * 对于关联字段（LINK类型），格式为 "{linkTypeId}:{SOURCE|TARGET}"
     */
    private String fieldId;

    /**
     * 列标题
     */
    private String title;

    /**
     * 字段类型
     * <p>
     * 可选值：TEXT, NUMBER, DATE, ENUM, ATTACHMENT, LINK, STRUCTURE, WEB_URL, MARKDOWN 等
     */
    private String fieldType;

    /**
     * 列宽（像素）
     */
    private Integer width;

    /**
     * 是否冻结
     */
    private boolean frozen;

    /**
     * 是否可排序
     */
    private boolean sortable;

    /**
     * 是否可见
     */
    private boolean visible;

    /**
     * 是否可编辑
     * <p>
     * 由字段的 readOnly 和 systemField 配置决定
     */
    private boolean editable;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 是否为内置字段
     * <p>
     * 内置字段是卡片的系统属性，如创建时间、更新时间、卡片状态等
     */
    private boolean builtin;

    /**
     * 字段渲染配置
     * <p>
     * 根据 fieldType 返回不同类型的配置，如：
     * - ENUM: EnumRenderConfig（包含 multiSelect、options）
     * - DATE: DateRenderConfig（包含 dateFormat）
     * - NUMBER: NumberRenderConfig（包含 precision、unit 等）
     */
    private FieldRenderConfig renderConfig;
}
