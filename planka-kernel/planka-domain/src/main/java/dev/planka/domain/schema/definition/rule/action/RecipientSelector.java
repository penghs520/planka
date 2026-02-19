package dev.planka.domain.schema.definition.rule.action;

import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 接收者选择器
 * <p>
 * 定义通知的接收人选择策略。
 * 支持两种模式：
 * 1. 单选择器模式（向后兼容）：使用 selectorType + memberIds/fieldId/linkPath
 * 2. 多选择器模式：使用 selectors 列表，多个选择器的结果取并集
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipientSelector {

    // ==================== 多选择器模式（推荐） ====================

    /**
     * 选择器列表
     * <p>
     * 多个选择器的结果取并集。如果此字段非空，则忽略单选择器模式的字段。
     */
    @JsonProperty("selectors")
    private List<SelectorItem> selectors;

    // ==================== 单选择器模式（向后兼容） ====================

    /**
     * 选择类型
     */
    @JsonProperty("selectorType")
    private SelectorType selectorType;

    /**
     * 固定成员ID列表（selectorType=FIXED_MEMBERS时使用）
     */
    @JsonProperty("memberIds")
    private List<String> memberIds;

    /**
     * 字段ID（selectorType=FROM_FIELD时使用）
     * <p>
     * 可以是人员字段或关联字段。
     */
    @JsonProperty("fieldId")
    private String fieldId;

    /**
     * 关联路径（可选，用于跨卡片取人员）
     */
    @JsonProperty("linkPath")
    private Path linkPath;

    /**
     * 选择器项
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SelectorItem {
        @JsonProperty("selectorType")
        private SelectorType selectorType;

        @JsonProperty("memberIds")
        private List<String> memberIds;

        @JsonProperty("fieldId")
        private String fieldId;

        @JsonProperty("linkPath")
        private Path linkPath;

        public static SelectorItem currentOperator() {
            SelectorItem item = new SelectorItem();
            item.setSelectorType(SelectorType.CURRENT_OPERATOR);
            return item;
        }

        public static SelectorItem fixedMembers(List<String> memberIds) {
            SelectorItem item = new SelectorItem();
            item.setSelectorType(SelectorType.FIXED_MEMBERS);
            item.setMemberIds(memberIds);
            return item;
        }

        public static SelectorItem fromField(String fieldId) {
            SelectorItem item = new SelectorItem();
            item.setSelectorType(SelectorType.FROM_FIELD);
            item.setFieldId(fieldId);
            return item;
        }

        public static SelectorItem linkedCardField(Path linkPath, String fieldId) {
            SelectorItem item = new SelectorItem();
            item.setSelectorType(SelectorType.LINKED_CARD_FIELD);
            item.setLinkPath(linkPath);
            item.setFieldId(fieldId);
            return item;
        }

        public static SelectorItem cardWatchers() {
            SelectorItem item = new SelectorItem();
            item.setSelectorType(SelectorType.CARD_WATCHERS);
            return item;
        }
    }

    /**
     * 选择类型枚举
     */
    public enum SelectorType {
        /** 当前操作人 */
        CURRENT_OPERATOR,
        /** 固定成员列表 */
        FIXED_MEMBERS,
        /** 从卡片字段获取 */
        FROM_FIELD,
        /** 卡片的所有关注者 */
        CARD_WATCHERS,
        /** 从关联卡片字段获取（新增） */
        LINKED_CARD_FIELD
    }

    /**
     * 是否使用多选择器模式
     */
    public boolean usesMultipleSelectors() {
        return selectors != null && !selectors.isEmpty();
    }

    /**
     * 获取所有选择器项（统一接口）
     * <p>
     * 如果使用多选择器模式，返回 selectors 列表；
     * 否则将单选择器转换为列表返回。
     */
    public List<SelectorItem> getAllSelectors() {
        if (usesMultipleSelectors()) {
            return selectors;
        }
        // 向后兼容：将单选择器转换为列表
        if (selectorType != null) {
            SelectorItem item = new SelectorItem();
            item.setSelectorType(selectorType);
            item.setMemberIds(memberIds);
            item.setFieldId(fieldId);
            item.setLinkPath(linkPath);
            return List.of(item);
        }
        return List.of();
    }

    // ==================== 工厂方法（单选择器模式，向后兼容） ====================

    /**
     * 创建当前操作人选择器
     */
    public static RecipientSelector currentOperator() {
        RecipientSelector selector = new RecipientSelector();
        selector.setSelectorType(SelectorType.CURRENT_OPERATOR);
        return selector;
    }

    /**
     * 创建固定成员选择器
     */
    public static RecipientSelector fixedMembers(List<String> memberIds) {
        RecipientSelector selector = new RecipientSelector();
        selector.setSelectorType(SelectorType.FIXED_MEMBERS);
        selector.setMemberIds(memberIds);
        return selector;
    }

    /**
     * 创建从字段获取选择器
     */
    public static RecipientSelector fromField(String fieldId) {
        RecipientSelector selector = new RecipientSelector();
        selector.setSelectorType(SelectorType.FROM_FIELD);
        selector.setFieldId(fieldId);
        return selector;
    }

    // ==================== 工厂方法（多选择器模式） ====================

    /**
     * 创建多选择器
     */
    public static RecipientSelector multiple(List<SelectorItem> items) {
        RecipientSelector selector = new RecipientSelector();
        selector.setSelectors(new ArrayList<>(items));
        return selector;
    }

    /**
     * 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<SelectorItem> items = new ArrayList<>();

        public Builder addCurrentOperator() {
            items.add(SelectorItem.currentOperator());
            return this;
        }

        public Builder addFixedMembers(List<String> memberIds) {
            items.add(SelectorItem.fixedMembers(memberIds));
            return this;
        }

        public Builder addFromField(String fieldId) {
            items.add(SelectorItem.fromField(fieldId));
            return this;
        }

        public Builder addLinkedCardField(Path linkPath, String fieldId) {
            items.add(SelectorItem.linkedCardField(linkPath, fieldId));
            return this;
        }

        public Builder addCardWatchers() {
            items.add(SelectorItem.cardWatchers());
            return this;
        }

        public RecipientSelector build() {
            if (items.size() == 1) {
                // 单选择器模式
                SelectorItem item = items.get(0);
                RecipientSelector selector = new RecipientSelector();
                selector.setSelectorType(item.getSelectorType());
                selector.setMemberIds(item.getMemberIds());
                selector.setFieldId(item.getFieldId());
                selector.setLinkPath(item.getLinkPath());
                return selector;
            }
            // 多选择器模式
            return multiple(items);
        }
    }
}
