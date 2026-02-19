package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 系统用户字段条件项
 * <p>
 * 用于系统内置的用户字段过滤条件，如创建人、更新人。
 * 这些字段不需要 fieldId，因为是系统内置的。
 * <p>
 * nodeType 可以是：CREATED_BY, UPDATED_BY
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SystemUserConditionItem extends AbstractConditionItem {

    /**
     * 节点类型（由 Jackson 反序列化时自动设置）
     */
    private final String nodeType;

    private final SystemUserSubject subject;

    private final UserOperator operator;

    @JsonCreator
    public SystemUserConditionItem(
            @JsonProperty("nodeType") String nodeType,
            @JsonProperty("subject") SystemUserSubject subject,
            @JsonProperty("operator") UserOperator operator) {
        AssertUtils.notBlank(nodeType, "nodeType can't be blank");
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.nodeType = nodeType;
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return nodeType;
    }

    /**
     * 系统用户字段主体
     * <p>
     * 只包含可选的路径，用于引用关联卡片的系统用户字段。
     */
    public record SystemUserSubject(Path path) {

        @JsonCreator
        public SystemUserSubject(@JsonProperty("path") Path path) {
            this.path = path;
        }
    }

    /**
     * 用户操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserOperator.Equal.class, name = "EQ"),
            @JsonSubTypes.Type(value = UserOperator.NotEqual.class, name = "NE"),
            @JsonSubTypes.Type(value = UserOperator.In.class, name = "IN"),
            @JsonSubTypes.Type(value = UserOperator.NotIn.class, name = "NOT_IN"),
            @JsonSubTypes.Type(value = UserOperator.IsCurrentUser.class, name = "IS_CURRENT_USER"),
            @JsonSubTypes.Type(value = UserOperator.IsNotCurrentUser.class, name = "IS_NOT_CURRENT_USER"),
            @JsonSubTypes.Type(value = UserOperator.IsEmpty.class, name = "IS_EMPTY"),
            @JsonSubTypes.Type(value = UserOperator.IsNotEmpty.class, name = "IS_NOT_EMPTY"),
    })
    public interface UserOperator {

        /**
         * 等于
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Equal implements UserOperator {
            private final String userId;

            @JsonCreator
            public Equal(@JsonProperty("userId") String userId) {
                AssertUtils.notBlank(userId, "userId can't be blank");
                this.userId = userId;
            }
        }

        /**
         * 不等于
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotEqual implements UserOperator {
            private final String userId;

            @JsonCreator
            public NotEqual(@JsonProperty("userId") String userId) {
                AssertUtils.notBlank(userId, "userId can't be blank");
                this.userId = userId;
            }
        }

        /**
         * 在列表中
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class In implements UserOperator {
            private final List<String> userIds;

            @JsonCreator
            public In(@JsonProperty("userIds") List<String> userIds) {
                AssertUtils.notEmpty(userIds, "userIds can't be empty");
                this.userIds = userIds;
            }
        }

        /**
         * 不在列表中
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotIn implements UserOperator {
            private final List<String> userIds;

            @JsonCreator
            public NotIn(@JsonProperty("userIds") List<String> userIds) {
                AssertUtils.notEmpty(userIds, "userIds can't be empty");
                this.userIds = userIds;
            }
        }

        /**
         * 是当前用户
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsCurrentUser implements UserOperator {
        }

        /**
         * 不是当前用户
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsNotCurrentUser implements UserOperator {
        }

        /**
         * 为空
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsEmpty implements UserOperator {
        }

        /**
         * 不为空
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsNotEmpty implements UserOperator {
        }
    }
}
