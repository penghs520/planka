package dev.planka.domain.history.source;

import dev.planka.domain.history.OperationSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 三方系统同步来源
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ThirdPartyOperationSource implements OperationSource {

    public static final String TYPE = "THIRD_PARTY";
    private static final String MESSAGE_KEY = "history.source.thirdparty";

    /**
     * 三方系统ID
     */
    private final String systemId;

    /**
     * 三方系统名称
     */
    private final String systemName;

    @JsonCreator
    public ThirdPartyOperationSource(
            @JsonProperty("systemId") String systemId,
            @JsonProperty("systemName") String systemName) {
        this.systemId = systemId;
        this.systemName = systemName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}
