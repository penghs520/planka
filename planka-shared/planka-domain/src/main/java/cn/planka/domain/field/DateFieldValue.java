package cn.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 日期属性值（使用时间戳，兼容性更好）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DateFieldValue implements FieldValue<Long> {

    private final String fieldId;
    private Long value;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FieldPermissionStatus permissionStatus;

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
        DateFieldValue that = (DateFieldValue) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }
}
