package dev.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 数字属性值
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NumberFieldValue implements FieldValue<Double> {

    private final String fieldId;
    private final Double value;
    private boolean readable = true;


    @JsonCreator
    public NumberFieldValue(
            @JsonProperty("fieldId") String fieldId,
            @JsonProperty("value") Double value) {
        this.fieldId = fieldId;
        this.value = value;
        this.readable = readable;
    }

    @Override
    public String getFieldId() {
        return fieldId;
    }

    @Override
    public Double getValue() {
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
        NumberFieldValue that = (NumberFieldValue) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }
}
