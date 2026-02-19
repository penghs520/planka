package dev.planka.domain.schema.definition.view;

import dev.planka.common.page.SortField;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.ViewId;
import dev.planka.domain.schema.definition.condition.Condition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 列表视图定义
 * <p>
 * 列表视图以表格形式展示卡片数据，支持以下特性：
 * <ul>
 *     <li>自定义显示字段和顺序</li>
 *     <li>分组展示</li>
 *     <li>列宽度自定义</li>
 *     <li>冻结列</li>
 *     <li>分页配置</li>
 *     <li>排序配置</li>
 *     <li>过滤配置</li>
 * </ul>
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ListViewDefinition extends AbstractViewDefinition {

    /** 显示卡片类型ID */
    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    /** 列配置列表 */
    @JsonProperty("columnConfigs")
    private List<ColumnConfig> columnConfigs;

    /** 分组字段ID（可选） */
    @JsonProperty("groupBy")
    private String groupBy;

    /** 排序配置列表 */
    @JsonProperty("sorts")
    private List<SortField> sorts;

    /** 分页配置 */
    @JsonProperty("pageConfig")
    private PageConfig pageConfig;

    /** 过滤条件（支持复杂的嵌套逻辑） */
    @JsonProperty("condition")
    private Condition condition;


    @JsonCreator
    public ListViewDefinition(
            @JsonProperty("id") ViewId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
        this.pageConfig = new PageConfig();
    }

    @Override
    public SchemaId belongTo() {
        return null;
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of(cardTypeId);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.LIST_VIEW;
    }

    @Override
    public String getViewType() {
        return "LIST";
    }

    @Override
    public void validate() {
        super.validate();
        // 可以添加列表视图特定的验证逻辑
    }

    // ==================== 内部类定义 ====================

    /**
     * 列配置
     * <p>
     * 定义每一列的显示属性，包括宽度、可见性、是否可拖拽宽度大小等。
     * <p>
     * 对于关联字段（LINK类型），fieldId 格式为 "{linkTypeId}:{SOURCE|TARGET}"
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ColumnConfig {
        /**
         * 字段ID
         * <p>
         * 对于关联字段（LINK类型），格式为 "{linkTypeId}:{SOURCE|TARGET}"
         */
        @JsonProperty("fieldId")
        private String fieldId;

        /** 列宽（像素） */
        @JsonProperty("width")
        private Integer width;

        /** 是否可见 */
        @JsonProperty("visible")
        private boolean visible = true;

        /** 是否可拖拽宽度大小 */
        @JsonProperty("resizable")
        private boolean resizable = true;

        /** 是否冻结 */
        @JsonProperty("frozen")
        private boolean frozen = false;

        public ColumnConfig() {
        }

        public ColumnConfig(String fieldId) {
            this.fieldId = fieldId;
        }

        public ColumnConfig(String fieldId, Integer width) {
            this.fieldId = fieldId;
            this.width = width;
        }
    }

    /**
     * 分页配置
     * <p>
     * 定义列表视图的分页行为，包括默认每页大小、可选的页大小选项、虚拟滚动等。
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageConfig {
        /** 默认每页大小 */
        @JsonProperty("defaultPageSize")
        private int defaultPageSize = 20;

        /** 可选的每页大小列表 */
        @JsonProperty("pageSizeOptions")
        private List<Integer> pageSizeOptions = List.of(10, 20, 50, 100);

        /** 是否启用虚拟滚动（大数据量时推荐启用） */
        @JsonProperty("enableVirtualScroll")
        private boolean enableVirtualScroll = false;

        public PageConfig() {
        }

        public PageConfig(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }

        public PageConfig(int defaultPageSize, List<Integer> pageSizeOptions) {
            this.defaultPageSize = defaultPageSize;
            this.pageSizeOptions = pageSizeOptions;
        }
    }

}
