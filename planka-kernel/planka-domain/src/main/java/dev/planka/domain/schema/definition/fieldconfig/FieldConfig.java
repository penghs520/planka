package dev.planka.domain.schema.definition.fieldconfig;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.formula.FormulaId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.planka.domain.schema.definition.SchemaDefinition;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;

/**
 * 卡片类型属性配置基类
 * <p>
 * 属性配置直接挂载在卡片类型下，包含完整的属性定义信息。
 * <p>
 * ID设计规则：
 * 1. 新建FieldConfig时：id 和 fieldId 相同（都使用FieldConfigId生成）
 * 2. 继承的FieldConfig：fieldId 保持不变，id 重新随机生成
 * 3. 判断原始配置：当 id.equals(fieldId) 时为原始配置
 * 4. 删除限制：如果原始配置已被继承且子类已持久化，则不能删除
 * 5. 外部引用：其他地方引用属性时，使用 fieldId
 * <p>
 * belongTo: 所属的卡片类型ID
 * <p>
 * 注意：Jackson 多态注解已集中配置在 {@link SchemaDefinition} 接口上。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class FieldConfig extends AbstractSchemaDefinition<FieldConfigId> {

    /**
     * 所属卡片类型ID
     */
    @JsonProperty("cardTypeId")
    protected final CardTypeId cardTypeId;

    /**
     * 属性唯一标识（用于外部引用）
     */
    @JsonProperty("fieldId")
    protected final FieldId fieldId;

    /**
     * 属性编码（用于数据同步场景）
     */
    @JsonProperty("code")
    protected String code;

    /**
     * 是否系统内置属性
     */
    @JsonProperty("systemField")
    protected final boolean systemField;

    // ==================== 可覆盖的通用配置 ====================

    /**
     * 是否必填（null表示使用继承配置）
     */
    @JsonProperty("required")
    protected Boolean required;

    /**
     * 是否只读（null表示使用继承配置）
     */
    @JsonProperty("readOnly")
    protected Boolean readOnly;

    /**
     * 在区域内的排序
     */
    @JsonProperty("areaOrder")
    protected Integer areaOrder;

    // ==================== 值来源配置 ====================

    /**
     * 属性值来源
     * <p>
     * SYSTEM: 系统更新（不可切换）
     * MANUAL: 手动输入
     * FORMULA: 计算公式
     * REFERENCE: 引用
     */
    @JsonProperty("valueSource")
    protected ValueSource valueSource;

    /**
     * 计算公式ID（当 valueSource 为 FORMULA 时必填）
     */
    @JsonProperty("formulaId")
    protected FormulaId formulaId;

    /**
     * 引用属性ID（当 valueSource 为 REFERENCE 时必填）
     */
    @JsonProperty("referenceFieldId")
    protected String referenceFieldId;

    /**
     * 属性值校验规则列表
     * 支持配置多个校验规则，按顺序执行，遇到第一个失败就停止。
     */
    @JsonProperty("validationRules")
    protected java.util.List<ValidationRule> validationRules;

    /**
     * 完整构造函数（用于JSON反序列化或完整创建）
     *
     * @param id          Schema ID（可为null，由服务层设置）
     * @param orgId       组织ID（可为null，由服务层设置）
     * @param name        名称（必填）
     * @param cardTypeId  卡片类型ID（可为null，服务层后续设置）
     * @param fieldId     属性唯一标识（必填，不能为空）
     * @param systemField 是否系统内置属性
     */
    protected FieldConfig(FieldConfigId id, String orgId, String name,
                          CardTypeId cardTypeId, FieldId fieldId,
                          boolean systemField) {
        super(id, orgId, name);
        //创建时fieldId传入空
        this.fieldId = Objects.requireNonNullElseGet(fieldId, () -> FieldId.of(this.getId().value()));
        this.cardTypeId = cardTypeId;
        this.systemField = systemField;
    }

    /**
     * 判断是否为原始配置
     * <p>
     * 当 id.equals(fieldId) 时为原始配置，否则为继承配置
     */
    public boolean isOriginal() {
        return id != null && id.value().equals(fieldId.value());
    }

    /**
     * 获取字段类型
     *
     * @return 字段类型枚举
     */
    public abstract FieldType getFieldType();

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.FIELD_CONFIG;
    }

    @Override
    public SchemaId belongTo() {
        return cardTypeId;  // 属性配置属于某个卡片类型
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of(cardTypeId, fieldId);  // 属性配置的二级索引是所属卡片类型和FieldId
    }

    @Override
    protected FieldConfigId newId() {
        return FieldConfigId.generate();
    }

    @Override
    public void validate() {
        super.validate();
        if (fieldId == null) {
            throw new IllegalArgumentException("属性ID不能为空");
        }

        // 校验值来源相关字段
        if (valueSource != null) {
            switch (valueSource) {
                case FORMULA:
                    if (formulaId == null) {
                        throw new IllegalArgumentException("当 valueSource 为 FORMULA 时，formulaId 不能为空");
                    }
                    break;
                case REFERENCE:
                    if (referenceFieldId == null || referenceFieldId.isBlank()) {
                        throw new IllegalArgumentException("当 valueSource 为 REFERENCE 时，referenceFieldId 不能为空");
                    }
                    break;
                case MANUAL:
                    // 手动输入不需要额外字段
                    break;
            }
        }

        // 校验 validationRules
        if (validationRules != null) {
            for (ValidationRule rule : validationRules) {
                rule.validate();
            }
        }
    }

}
