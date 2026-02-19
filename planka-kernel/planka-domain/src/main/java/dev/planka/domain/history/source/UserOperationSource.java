package dev.planka.domain.history.source;

import dev.planka.domain.history.OperationSource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 用户操作来源（默认）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class UserOperationSource implements OperationSource {

    public static final String TYPE = "USER";
    private static final String MESSAGE_KEY = "history.source.user";

    /**
     * 单例实例（用户操作无需额外信息）
     */
    public static final UserOperationSource INSTANCE = new UserOperationSource();

    public UserOperationSource() {
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
