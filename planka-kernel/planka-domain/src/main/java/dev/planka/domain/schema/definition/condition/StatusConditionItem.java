package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 状态条件项
 * <p>
 * 用于价值流状态的过滤条件。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StatusConditionItem extends AbstractConditionItem {

    private final StatusSubject subject;

    private final StatusOperator operator;

    @JsonCreator
    public StatusConditionItem(@JsonProperty("subject") StatusSubject subject, @JsonProperty("operator") StatusOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.STATUS;
    }

    public record StatusSubject(Path path, String streamId) {

        @JsonCreator
        public StatusSubject(@JsonProperty("path") Path path,
                             @JsonProperty("streamId") String streamId) {
            AssertUtils.notBlank(streamId, "streamId can't be blank");
            this.path = path;
            this.streamId = streamId;
        }
    }

    /**
     * 状态操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = StatusOperator.Equal.class, name = "EQ"),
            @JsonSubTypes.Type(value = StatusOperator.NotEqual.class, name = "NE"),
            @JsonSubTypes.Type(value = StatusOperator.In.class, name = "IN"),
            @JsonSubTypes.Type(value = StatusOperator.NotIn.class, name = "NOT_IN"),
            @JsonSubTypes.Type(value = StatusOperator.Reached.class, name = "REACHED"),
            @JsonSubTypes.Type(value = StatusOperator.NotReached.class, name = "NOT_REACHED"),
            @JsonSubTypes.Type(value = StatusOperator.Passed.class, name = "PASSED"),
    })
    public interface StatusOperator {

        /**
         * 等于（单选）
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Equal implements StatusOperator {
            private final String statusId;

            @JsonCreator
            public Equal(@JsonProperty("statusId") String statusId) {
                this.statusId = statusId;
            }
        }

        /**
         * 不等于
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotEqual implements StatusOperator {
            private final String statusId;

            @JsonCreator
            public NotEqual(@JsonProperty("statusId") String statusId) {
                this.statusId = statusId;
            }
        }

        /**
         * 在列表中
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class In implements StatusOperator {
            private final List<String> statusIds;

            @JsonCreator
            public In(@JsonProperty("statusIds") List<String> statusIds) {
                this.statusIds = statusIds;
            }
        }

        /**
         * 不在列表中
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotIn implements StatusOperator {
            private final List<String> statusIds;

            @JsonCreator
            public NotIn(@JsonProperty("statusIds") List<String> statusIds) {
                this.statusIds = statusIds;
            }
        }

        /**
         * 已到达
         * <p>
         * 表示卡片已经到达指定状态（包含当前处于该状态）
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Reached implements StatusOperator {
            private final String statusId;

            @JsonCreator
            public Reached(@JsonProperty("statusId") String statusId) {
                this.statusId = statusId;
            }
        }

        /**
         * 未到达
         * <p>
         * 表示卡片尚未到达指定状态（在价值流中该状态还在后面）
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotReached implements StatusOperator {
            private final String statusId;

            @JsonCreator
            public NotReached(@JsonProperty("statusId") String statusId) {
                this.statusId = statusId;
            }
        }

        /**
         * 已超过
         * <p>
         * 表示卡片已经超过指定状态（在价值流中该状态已经是过去式）
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Passed implements StatusOperator {
            private final String statusId;

            @JsonCreator
            public Passed(@JsonProperty("statusId") String statusId) {
                this.statusId = statusId;
            }
        }
    }
}
