package dev.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 网页链接
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Url(
        @JsonProperty("url") String url,
        @JsonProperty("displayText") String displayText
) {
}
