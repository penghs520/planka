package dev.planka.event.comment;

import dev.planka.event.DomainEvent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * 评论事件基类
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CommentCreationRequestedEvent.class, name = "comment.creation.requested"),
})
public abstract class CommentEvent extends DomainEvent {

    private static final String TOPIC = "kanban-comment-events";

    protected CommentEvent(String orgId, String operatorId, String sourceIp, String traceId) {
        super(orgId, operatorId, sourceIp, traceId);
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }

    @Override
    public String getPartitionKey() {
        return "Comment.Event";
    }
}
