package dev.planka.event.comment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 第三方API调用来源
 */
public class ApiCallOperationSource implements OperationSource {

    public static final String TYPE = "API_CALL";

    private final String appName;

    @JsonCreator
    public ApiCallOperationSource(@JsonProperty("appName") String appName) {
        this.appName = appName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getDisplayName() {
        return appName;
    }

    public String getAppName() {
        return appName;
    }
}
