package dev.planka.domain.history;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史记录消息 - 消息码+参数的国际化方案
 * <p>
 * 设计原则：
 * <ul>
 *     <li>OperateFieldArg 只存储 fieldId，名称在查询时填充</li>
 *     <li>FieldValueArg 只存储 ID，名称由 OperateFieldArg 提供</li>
 *     <li>查询时优先从 Schema 获取最新名称，获取不到则使用 fieldId 作为后备显示</li>
 * </ul>
 * <p>
 * 示例（存储格式）：
 * <pre>
 * {
 *   "messageKey": "history.field.updated",
 *   "args": [
 *     {"type": "OPERATE_FIELD", "fieldId": "123"},
 *     {"type": "FIELD_VALUE_ENUM", "fieldId": "123", "values": [{"optionId": "1", "optionName": "高"}]},
 *     {"type": "FIELD_VALUE_ENUM", "fieldId": "123", "values": [{"optionId": "2", "optionName": "低"}]}
 *   ]
 * }
 * </pre>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryMessage {

    /**
     * 消息码（对应国际化资源文件中的key）
     */
    private final String messageKey;

    /**
     * 消息参数列表
     */
    private final List<HistoryArgument> args;

    @JsonCreator
    public HistoryMessage(
            @JsonProperty("messageKey") String messageKey,
            @JsonProperty("args") List<HistoryArgument> args) {
        this.messageKey = messageKey;
        this.args = args != null ? args : new ArrayList<>();
    }

    /**
     * 创建无参数的消息
     */
    public static HistoryMessage of(String messageKey) {
        return new HistoryMessage(messageKey, List.of());
    }

    /**
     * 创建带参数的消息
     */
    public static HistoryMessage of(String messageKey, List<HistoryArgument> args) {
        return new HistoryMessage(messageKey, args);
    }

    /**
     * 创建带参数的消息（可变参数）
     */
    public static HistoryMessage of(String messageKey, HistoryArgument... args) {
        return new HistoryMessage(messageKey, List.of(args));
    }

    // ==================== 基础参数工厂方法 ====================

    /**
     * 创建文本参数
     */
    public static HistoryArgument text(String value) {
        return new HistoryArgument.TextArg(value);
    }

    /**
     * 创建文本差异参数
     */
    public static HistoryArgument textDiff(String oldText, String newText) {
        return TextDiffUtils.computeDiff(oldText, newText);
    }

    /**
     * 创建操作属性名称参数
     */
    public static HistoryArgument operateField(String fieldId) {
        return new HistoryArgument.OperateFieldArg(fieldId);
    }

    /**
     * 创建状态参数
     */
    public static HistoryArgument status(String statusId, String statusName) {
        return new HistoryArgument.StatusArg(statusId, statusName);
    }

    // ==================== 属性值参数工厂方法 ====================

    /**
     * 创建文本类型属性值
     */
    public static HistoryArgument textFieldValue(String fieldId, String value) {
        return new HistoryArgument.TextFieldValue(fieldId, value);
    }

    /**
     * 创建数字类型属性值
     */
    public static HistoryArgument numberFieldValue(String fieldId, BigDecimal value) {
        return new HistoryArgument.NumberFieldValue(fieldId, value);
    }

    /**
     * 创建日期类型属性值
     */
    public static HistoryArgument dateFieldValue(String fieldId, LocalDate value) {
        return new HistoryArgument.DateFieldValue(fieldId, value);
    }

    /**
     * 创建日期时间类型属性值
     */
    public static HistoryArgument dateTimeFieldValue(String fieldId, LocalDateTime value) {
        return new HistoryArgument.DateTimeFieldValue(fieldId, value);
    }

    /**
     * 创建枚举类型属性值
     */
    public static HistoryArgument enumFieldValue(String fieldId,
                                                  List<HistoryArgument.EnumFieldValue.EnumOption> values) {
        return new HistoryArgument.EnumFieldValue(fieldId, values);
    }

    /**
     * 创建枚举选项
     */
    public static HistoryArgument.EnumFieldValue.EnumOption enumOption(String optionId, String optionName) {
        return new HistoryArgument.EnumFieldValue.EnumOption(optionId, optionName);
    }

    /**
     * 创建架构类型属性值
     */
    public static HistoryArgument structureFieldValue(String fieldId,
                                                       List<HistoryArgument.StructureFieldValue.StructureNode> path) {
        return new HistoryArgument.StructureFieldValue(fieldId, path);
    }

    /**
     * 创建架构节点
     */
    public static HistoryArgument.StructureFieldValue.StructureNode structureNode(String nodeId, String nodeName) {
        return new HistoryArgument.StructureFieldValue.StructureNode(nodeId, nodeName);
    }

    /**
     * 创建关联类型属性值
     */
    public static HistoryArgument linkFieldValue(String fieldId,
                                                  List<HistoryArgument.LinkFieldValue.LinkedCardRef> cards) {
        return new HistoryArgument.LinkFieldValue(fieldId, cards);
    }

    /**
     * 创建关联卡片引用
     */
    public static HistoryArgument.LinkFieldValue.LinkedCardRef linkedCardRef(String cardId, String cardTypeId) {
        return new HistoryArgument.LinkFieldValue.LinkedCardRef(cardId, cardTypeId);
    }
}
