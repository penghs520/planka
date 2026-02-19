package dev.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

/**
 * 日期属性值（使用时间戳，兼容性更好）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
public final class DateFieldValue implements FieldValue<Long> {

    private final String fieldId;
    private final Long value;
    private boolean readable = true;

    @JsonCreator
    public DateFieldValue(
            @JsonProperty("fieldId") String fieldId,
            @JsonProperty("value") Long value) {
        this.fieldId = fieldId;
        this.value = value;
    }

    @Override
    public String getFieldId() {
        return fieldId;
    }

    @Override
    public Long getValue() {
        return readable ? value : null;
    }

    @Override
    public boolean isReadable() {
        return readable;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateFieldValue that = (DateFieldValue) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }
}
