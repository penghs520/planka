package dev.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

/**
 * 网页链接属性值
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
public final class WebLinkFieldValue implements FieldValue<Url> {

    private final String fieldId;
    private final Url value;
    private boolean readable = true;

    public WebLinkFieldValue(String fieldId, String url, String displayText) {
        this(fieldId, new Url(url, displayText));
    }

    @JsonCreator
    public WebLinkFieldValue(
            @JsonProperty("fieldId") String fieldId,
            @JsonProperty("value") Url value) {
        this.fieldId = fieldId;
        this.value = value;
    }

    @Override
    public String getFieldId() {
        return fieldId;
    }

    @Override
    public Url getValue() {
        return readable ? value : null;
    }

    @Override
    public boolean isReadable() {
        return readable;
    }

    @Override
    public boolean isEmpty() {
        return value == null || value.url() == null || value.url().isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebLinkFieldValue that = (WebLinkFieldValue) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }
}
