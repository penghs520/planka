package dev.planka.domain.schema.definition.stream;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.stream.StreamId;
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
        this.stepList = AssertUtils.requireNotEmpty(stepList,"stepList can't be empty");
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
