package cn.agilean.kanban.event.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 卡片迁移请求事件
 * <p>
 * 当价值流中的状态被删除时，请求将指定状态下的所有卡片迁移到目标状态。
 * 由 schema-service 发布，card-service 监听并执行实际的迁移操作。
 */
@Getter
@Setter
public class CardMigrationRequestedEvent extends CardEvent {

    private static final String EVENT_TYPE = "card.migration.requested";

    /**
     * 价值流ID
     */
    private String streamId;

    /**
     * 状态迁移映射：源状态ID -> 目标状态ID
     */
    private Map<String, String> statusMigrationMap;

    /**
     * 是否为删除阶段触发的迁移（true=删除阶段，false=删除单个状态）
     */
    private boolean stepDeletion;

    @JsonCreator
    public CardMigrationRequestedEvent(@JsonProperty("orgId") String orgId,
                                       @JsonProperty("operatorId") String operatorId,
                                       @JsonProperty("sourceIp") String sourceIp,
                                       @JsonProperty("traceId") String traceId,
                                       @JsonProperty("cardTypeId") String cardTypeId) {
        super(orgId, operatorId, sourceIp, traceId, "BATCH_MIGRATION", cardTypeId);
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    /**
     * 构建器方法：设置迁移信息
     */
    public CardMigrationRequestedEvent withMigrationInfo(String streamId,
                                                          Map<String, String> statusMigrationMap,
                                                          boolean stepDeletion) {
        this.streamId = streamId;
        this.statusMigrationMap = statusMigrationMap;
        this.stepDeletion = stepDeletion;
        return this;
    }
}
