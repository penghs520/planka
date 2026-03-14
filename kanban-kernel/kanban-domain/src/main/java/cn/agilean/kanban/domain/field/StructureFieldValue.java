package cn.agilean.kanban.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 架构属性值（链表结构表示层级路径）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StructureFieldValue implements FieldValue<StructureItem> {

    private final String fieldId;
    private StructureItem value;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FieldPermissionStatus permissionStatus;

    @JsonCreator
    public StructureFieldValue(
            @JsonProperty("fieldId") String fieldId,
            @JsonProperty("value") StructureItem value) {
        this.fieldId = fieldId;
        this.value = value;
    }

    @Override
    public String getFieldId() {
        return fieldId;
    }

    @Override
    public StructureItem getValue() {
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
        StructureFieldValue that = (StructureFieldValue) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }
}
