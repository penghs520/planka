package dev.planka.domain.history.source;

import dev.planka.domain.history.OperationSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * API 调用来源
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ApiCallOperationSource implements OperationSource {

    public static final String TYPE = "API_CALL";
    private static final String MESSAGE_KEY = "history.source.api";

    /**
     * API 客户端ID
     */
    private final String clientId;

    /**
     * API 客户端名称
     */
    private final String clientName;

    @JsonCreator
    public ApiCallOperationSource(
            @JsonProperty("clientId") String clientId,
            @JsonProperty("clientName") String clientName) {
        this.clientId = clientId;
        this.clientName = clientName;
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
