package dev.planka.api.card.request;

import lombok.Data;

import java.util.Set;

/**
 * 指定需要返回的字段
 */
@Data
public class YieldField {
    /**
     * 是否包含所有字段
     */
    private boolean allFields;

    /**
     * 指定返回的字段ID集合
     */
    private Set<String> fieldIds;

    /**
     * 是否包含描述
     */
    private boolean includeDescription;

    public static YieldField all() {
        YieldField yieldField = new YieldField();
        yieldField.setAllFields(true);
        yieldField.setIncludeDescription(true);
        return yieldField;
    }

    /**
     * 返回基本字段（id、title、typeId、code等内置字段），不包含自定义字段和描述
     */
    public static YieldField basic() {
        YieldField yieldField = new YieldField();
        yieldField.setAllFields(false);
        yieldField.setIncludeDescription(false);
        // 不指定 fieldIds 表示只返回内置字段
        return yieldField;
    }
}
