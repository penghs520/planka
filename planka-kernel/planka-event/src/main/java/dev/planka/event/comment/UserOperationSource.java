package dev.planka.event.comment;

/**
 * 用户操作来源
 */
public class UserOperationSource implements OperationSource {

    public static final String TYPE = "USER";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getDisplayName() {
        return null;
    }
}
