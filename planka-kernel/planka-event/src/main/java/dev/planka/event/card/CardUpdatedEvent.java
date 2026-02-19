package dev.planka.event.card;

import dev.planka.domain.field.FieldValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡片更新事件
 * <p>
 * 记录卡片属性（标题、描述、自定义属性）的变更
 */
@Getter
@Setter
public class CardUpdatedEvent extends CardEvent {

    private static final String EVENT_TYPE = "card.updated";

    /**
     * 标题变更（如果有）
     */
    private TitleChange titleChange;

    /**
     * 描述变更（如果有）
     */
    private DescriptionChange descriptionChange;

    /**
     * 自定义属性变更列表
     */
    private List<FieldChange> fieldChanges;

    @JsonCreator
    public CardUpdatedEvent(@JsonProperty("orgId") String orgId,
                            @JsonProperty("operatorId") String operatorId,
                            @JsonProperty("sourceIp") String sourceIp,
                            @JsonProperty("traceId") String traceId,
                            @JsonProperty("cardTypeId") String cardTypeId,
                            @JsonProperty("cardId") String cardId) {
        super(orgId, operatorId, sourceIp, traceId, cardId, cardTypeId);
        this.fieldChanges = new ArrayList<>();
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    /**
     * 设置标题变更
     */
    public CardUpdatedEvent withTitleChange(String oldValue, String newValue) {
        this.titleChange = new TitleChange(oldValue, newValue);
        return this;
    }

    /**
     * 设置描述变更
     */
    public CardUpdatedEvent withDescriptionChange(String oldValue, String newValue) {
        this.descriptionChange = new DescriptionChange(oldValue, newValue);
        return this;
    }

    /**
     * 添加自定义属性变更
     *
     * @param fieldId       属性ID
     * @param oldFieldValue 旧属性值
     * @param newFieldValue 新属性值
     */
    public CardUpdatedEvent addFieldChange(String fieldId,
                                           FieldValue<?> oldFieldValue, FieldValue<?> newFieldValue) {
        if (this.fieldChanges == null) {
            this.fieldChanges = new ArrayList<>();
        }
        this.fieldChanges.add(new FieldChange(fieldId, oldFieldValue, newFieldValue));
        return this;
    }

    /**
     * 判断是否有变更
     */
    public boolean hasChanges() {
        return titleChange != null || descriptionChange != null ||
                (fieldChanges != null && !fieldChanges.isEmpty());
    }

    /**
     * 标题变更记录
     */
    @Getter
    public static class TitleChange {
        private final String oldValue;
        private final String newValue;

        @JsonCreator
        public TitleChange(@JsonProperty("oldValue") String oldValue,
                           @JsonProperty("newValue") String newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    /**
     * 描述变更记录
     */
    @Getter
    public static class DescriptionChange {
        private final String oldValue;
        private final String newValue;

        @JsonCreator
        public DescriptionChange(@JsonProperty("oldValue") String oldValue,
                                 @JsonProperty("newValue") String newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    /**
     * 自定义属性变更记录
     * <p>
     * 直接使用 FieldValue 结构存储新旧值，保留完整的属性类型信息。
     * 字段名称在记录操作历史时从 Schema 服务获取。
     */
    @Getter
    public static class FieldChange {
        /**
         * 属性ID
         */
        private final String fieldId;

        /**
         * 旧属性值
         */
        private final FieldValue<?> oldValue;

        /**
         * 新属性值
         */
        private final FieldValue<?> newValue;

        @JsonCreator
        public FieldChange(@JsonProperty("fieldId") String fieldId,
                           @JsonProperty("oldValue") FieldValue<?> oldValue,
                           @JsonProperty("newValue") FieldValue<?> newValue) {
            this.fieldId = fieldId;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
