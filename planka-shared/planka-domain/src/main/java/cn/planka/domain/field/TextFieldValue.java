package cn.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 文本属性值
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TextFieldValue implements FieldValue<String> {

    private final String fieldId;
    private String value;
    @Getter
    private final Long maxStringLength;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FieldPermissionStatus permissionStatus;

    public TextFieldValue(String fieldId, String value) {
        this(fieldId, value, null);
    }

    @JsonCreator
    public TextFieldValue(
            @JsonProperty("fieldId") String fieldId,
            @JsonProperty("value") String value,
            @JsonProperty("maxStringLength") Long maxStringLength) {
        this.fieldId = fieldId;
        this.value = value;
        this.maxStringLength = maxStringLength;
    }

    @Override
    public String getFieldId() {
        return fieldId;
    }

    @Override
    public String getValue() {
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
        return value == null || value.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextFieldValue that = (TextFieldValue) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }
}
