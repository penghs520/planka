package cn.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 附件属性值
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AttachmentFieldValue implements FieldValue<List<Attachment>> {

    private final String fieldId;
    private List<Attachment> attachments;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FieldPermissionStatus permissionStatus;

    public AttachmentFieldValue(String fieldId, List<Attachment> attachments) {
        this.fieldId = fieldId;
        this.attachments = attachments;
    }

    @JsonCreator
    public AttachmentFieldValue(
            @JsonProperty("fieldId") String fieldId,
            @JsonProperty("attachments") List<Attachment> attachments,
            @JsonProperty("permissionStatus") FieldPermissionStatus permissionStatus) {
        this.fieldId = fieldId;
        this.attachments = attachments;
        this.permissionStatus = permissionStatus;
    }

    @Override
    public String getFieldId() {
        return fieldId;
    }

    @Override
    @JsonProperty("attachments")
    public List<Attachment> getValue() {
        return attachments;
    }

    @Override
    public FieldPermissionStatus getPermissionStatus() {
        return permissionStatus;
    }

    @Override
    public void setPermissionStatus(FieldPermissionStatus status) {
        this.permissionStatus = status;
        if (status == FieldPermissionStatus.NO_PERMISSION) {
            this.attachments = null;
        }
    }

    @Override
    public boolean isEmpty() {
        return attachments == null || attachments.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttachmentFieldValue that = (AttachmentFieldValue) o;
        return java.util.Objects.equals(attachments, that.attachments);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(attachments);
    }
}
