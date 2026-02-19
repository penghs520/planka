package dev.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

import java.util.List;

/**
 * 枚举属性值
 * 存储枚举选项的 ID 列表，显示时依赖 renderConfig 中的 options 获取标签
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
public final class EnumFieldValue implements FieldValue<List<String>> {

    private final String fieldId;
    private final List<String> value;
    private boolean readable = true;


    @JsonCreator
    public EnumFieldValue(
            @JsonProperty("fieldId") String fieldId,
            @JsonProperty("value") List<String> value) {
        this.fieldId = fieldId;
        this.value = value;
    }

    @Override
    public String getFieldId() {
        return fieldId;
    }

    @Override
    public List<String> getValue() {
        return readable ? value : null;
    }

    @Override
    public boolean isReadable() {
        return readable;
    }

    @Override
    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnumFieldValue that = (EnumFieldValue) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }
}
