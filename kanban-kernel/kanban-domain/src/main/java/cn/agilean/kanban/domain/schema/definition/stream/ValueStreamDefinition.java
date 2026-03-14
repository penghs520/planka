package cn.agilean.kanban.domain.schema.definition.stream;

import cn.agilean.kanban.common.util.AssertUtils;
import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.schema.SchemaId;
import cn.agilean.kanban.domain.schema.SchemaSubType;
import cn.agilean.kanban.domain.schema.SchemaType;
import cn.agilean.kanban.domain.schema.definition.AbstractSchemaDefinition;
import cn.agilean.kanban.domain.stream.StatusId;
import cn.agilean.kanban.domain.stream.StepId;
import cn.agilean.kanban.domain.stream.StreamId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 价值流定义
 * <p>
 * 定义一个价值流，一个卡片类型仅允许创建一个价值流定义。
 * <p>
 * 仅在组织级定义中允许创建价值流。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ValueStreamDefinition extends AbstractSchemaDefinition<StreamId> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.VALUE_STREAM;
    }

    private final CardTypeId cardTypeId;

    private final List<StepConfig> stepList;

    @JsonCreator
    public ValueStreamDefinition(
            @JsonProperty("id") StreamId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("stepList") List<StepConfig> stepList) {
        super(id, orgId, name);
        this.cardTypeId = cardTypeId;
        this.stepList = initializeStepList(stepList);
    }

    /**
     * 初始化阶段列表，为空的ID生成雪花ID
     */
    private static List<StepConfig> initializeStepList(List<StepConfig> stepList) {
        AssertUtils.requireNotEmpty(stepList, "stepList can't be empty");
        for (StepConfig step : stepList) {
            // 如果阶段ID为空或空白，生成新的雪花ID
            if (step.getId() == null || step.getId().value().isBlank()) {
                step.setId(StepId.generate());
            }
            // 为状态生成ID
            if (step.getStatusList() != null) {
                for (StatusConfig status : step.getStatusList()) {
                    if (status.getId() == null || status.getId().value().isBlank()) {
                        status.setId(StatusId.generate());
                    }
                }
            }
        }
        return stepList;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.VALUE_STREAM;
    }

    @Override
    public SchemaId belongTo() {
        return cardTypeId;
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of(cardTypeId);
    }

    @Override
    protected StreamId newId() {
        return StreamId.generate();
    }

}
