package dev.planka.api.schema.vo.formuladefinition;

import dev.planka.api.schema.vo.cardtype.CardTypeInfo;
import dev.planka.domain.schema.SchemaSubType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计算公式定义 VO 基类
 * <p>
 * 使用 Jackson 多态序列化，不同类型返回不同字段。
 */
@Data
@SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "schemaSubType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TimePointFormulaDefinitionVO.class, name = SchemaSubType.TIME_POINT_FORMULA_DEFINITION),
        @JsonSubTypes.Type(value = TimeRangeFormulaDefinitionVO.class, name = SchemaSubType.TIME_RANGE_FORMULA_DEFINITION),
        @JsonSubTypes.Type(value = DateCollectionFormulaDefinitionVO.class, name = SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION),
        @JsonSubTypes.Type(value = CardCollectionFormulaDefinitionVO.class, name = SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION),
        @JsonSubTypes.Type(value = NumberCalculationFormulaDefinitionVO.class, name = SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION)
})
public abstract class FormulaDefinitionVO {

    /**
     * 获取 Schema 子类型标识
     */
    public abstract String getSchemaSubType();

    /**
     * 公式 ID
     */
    private String id;

    /**
     * 公式名称
     */
    private String name;

    /**
     * 公式编码
     */
    private String code;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 描述
     */
    private String description;

    /**
     * 关联的卡片类型列表（包含 ID 和名称）
     */
    private List<CardTypeInfo> cardTypes;

    /**
     * 内容版本号（乐观锁）
     */
    private int contentVersion;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
