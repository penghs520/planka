package dev.planka.event.comment;

import dev.planka.common.util.AssertUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 评论创建请求事件
 * <p>
 * 由业务规则触发，请求创建评论。评论服务监听此事件并实际创建评论。
 */
@Getter
public class CommentCreationRequestedEvent extends CommentEvent {

    private static final String EVENT_TYPE = "comment.creation.requested";

    private final String cardId;
    private final String cardTypeId;
    private final String content;

    /**
     * 操作来源信息
     * 业务规则触发时包含规则ID和名称
     */
    private final OperationSource operationSource;

    @JsonCreator
    public CommentCreationRequestedEvent(@JsonProperty("orgId") String orgId,
                                         @JsonProperty("operatorId") String operatorId,
                                         @JsonProperty("sourceIp") String sourceIp,
                                         @JsonProperty("traceId") String traceId,
                                         @JsonProperty("cardId") String cardId,
                                         @JsonProperty("cardTypeId") String cardTypeId,
                                         @JsonProperty("content") String content,
                                         @JsonProperty("operationSource") OperationSource operationSource) {
        super(orgId, operatorId, sourceIp, traceId);
        this.cardId = AssertUtils.requireNotBlank(cardId, "cardId can't be blank");
        this.cardTypeId = AssertUtils.requireNotBlank(cardTypeId, "cardTypeId can't be blank");
        this.content = AssertUtils.requireNotBlank(content, "content can't be blank");
        this.operationSource = operationSource;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
