package dev.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 附件属性值
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AttachmentFieldValue implements FieldValue<List<Attachment>> {

    private final String fieldId;
    private final List<Attachment> attachments;
    private final boolean readable;

    public AttachmentFieldValue(String fieldId, List<Attachment> attachments) {
        this(fieldId, attachments, true);
    }

    @JsonCreator
    public AttachmentFieldValue(
            @JsonProperty("fieldId") String fieldId,
            @JsonProperty("attachments") List<Attachment> attachments,
            @JsonProperty("readable") boolean readable) {
        this.fieldId = fieldId;
        this.attachments = attachments;
        this.readable = readable;
    }

    @Override
    public String getFieldId() {
        return fieldId;
    }

    @Override
    @JsonProperty("attachments")
    public List<Attachment> getValue() {
        return readable ? attachments : null;
    }

    @Override
    public boolean isReadable() {
        return readable;
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
