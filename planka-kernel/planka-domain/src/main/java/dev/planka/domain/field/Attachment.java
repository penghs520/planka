package dev.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 附件信息
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Attachment(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("url") String url,
        @JsonProperty("size") long size,
        @JsonProperty("contentType") String contentType
) {
}
