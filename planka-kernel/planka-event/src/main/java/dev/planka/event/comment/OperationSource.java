package dev.planka.event.comment;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 评论操作来源
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserOperationSource.class, name = "USER"),
    @JsonSubTypes.Type(value = BizRuleOperationSource.class, name = "BIZ_RULE"),
    @JsonSubTypes.Type(value = ApiCallOperationSource.class, name = "API_CALL")
})
public interface OperationSource {
    String getType();
    String getDisplayName();
}
