package cn.agilean.kanban.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 数字属性值
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NumberFieldValue implements FieldValue<Double> {

    private final String fieldId;
    private Double value;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FieldPermissionStatus permissionStatus;

    @JsonCreator
    public NumberFieldValue(
            @JsonProperty("fieldId") String fieldId,
            @JsonProperty("value") Double value) {
        this.fieldId = fieldId;
        this.value = value;
    }

    @Override
    public String getFieldId() {
        return fieldId;
    }

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public FieldPermissionStatus getPermissionStatus() {
        return permissionStatus;
    }

    @Override
    public void setPermissionStatus(FieldPermissionStatus status) {
        this.permissionStatus = status;
        if (status == FieldPermissionStatus.NO_PERMISSION) {
            this.value = null;
        }
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
