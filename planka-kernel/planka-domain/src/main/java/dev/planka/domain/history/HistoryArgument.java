package dev.planka.domain.history;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 历史消息参数 - sealed interface + Jackson 多态序列化
 * <p>
 * 支持多种参数类型，用于构建国际化消息模板的参数。
 * <p>
 * 设计原则：
 * <ul>
 *     <li>只存储 ID，名称在查询时由后端填充到 VO 中</li>
 *     <li>同时存储名称作为备份，当 Schema 被删除时使用存储的名称</li>
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HistoryArgument.TextArg.class, name = "TEXT"),
        @JsonSubTypes.Type(value = HistoryArgument.TextDiffArg.class, name = "TEXT_DIFF"),
        @JsonSubTypes.Type(value = HistoryArgument.OperateFieldArg.class, name = "OPERATE_FIELD"),
        @JsonSubTypes.Type(value = HistoryArgument.StatusArg.class, name = "STATUS"),
        // FieldValue 多态子类型
        @JsonSubTypes.Type(value = HistoryArgument.TextFieldValue.class, name = "FIELD_VALUE_TEXT"),
        @JsonSubTypes.Type(value = HistoryArgument.NumberFieldValue.class, name = "FIELD_VALUE_NUMBER"),
        @JsonSubTypes.Type(value = HistoryArgument.DateFieldValue.class, name = "FIELD_VALUE_DATE"),
        @JsonSubTypes.Type(value = HistoryArgument.DateTimeFieldValue.class, name = "FIELD_VALUE_DATETIME"),
        @JsonSubTypes.Type(value = HistoryArgument.EnumFieldValue.class, name = "FIELD_VALUE_ENUM"),
        @JsonSubTypes.Type(value = HistoryArgument.StructureFieldValue.class, name = "FIELD_VALUE_STRUCTURE"),
        @JsonSubTypes.Type(value = HistoryArgument.LinkFieldValue.class, name = "FIELD_VALUE_LINK")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public sealed interface HistoryArgument {

    /**
     * 获取参数类型
     */
    String getType();

    // ==================== 基础参数类型 ====================

    /**
     * 纯文本参数
     */
    record TextArg(String value) implements HistoryArgument {
        @Override
        public String getType() {
            return "TEXT";
        }
    }

    /**
     * 文本差异参数 - 存储行级 diff 结果
     */
    record TextDiffArg(List<DiffHunk> hunks) implements HistoryArgument {
        @Override
        public String getType() {
            return "TEXT_DIFF";
        }

        /**
         * 差异块 - 包含变化行及其上下文
         */
        public record DiffHunk(
                int oldStart,
                int oldCount,
                int newStart,
                int newCount,
                List<DiffLine> lines
        ) {}

        /**
         * 差异行
         */
        public record DiffLine(DiffLineType type, String content) {}

        /**
         * 差异行类型
         */
        public enum DiffLineType {
            CONTEXT,
            ADD,
            DELETE
        }
    }

    /**
     * 操作属性名称参数 - 存储 fieldId
     */
    record OperateFieldArg(String fieldId) implements HistoryArgument {
        @Override
        public String getType() {
            return "OPERATE_FIELD";
        }
    }

    /**
     * 状态参数 - 存储 statusId 和 statusName（备份）
     */
    record StatusArg(String statusId, String statusName) implements HistoryArgument {
        @Override
        public String getType() {
            return "STATUS";
        }
    }

    // ==================== 属性值参数类型（多态） ====================

    /**
     * 属性值参数基类 - 只包含 fieldId，fieldName 由 OperateFieldArg 提供
     */
    sealed interface FieldValueArg extends HistoryArgument {
        String fieldId();
    }

    /**
     * 文本类型属性值
     */
    record TextFieldValue(String fieldId, String value) implements FieldValueArg {
        @Override
        public String getType() {
            return "FIELD_VALUE_TEXT";
        }
    }

    /**
     * 数字类型属性值
     */
    record NumberFieldValue(String fieldId, BigDecimal value) implements FieldValueArg {
        @Override
        public String getType() {
            return "FIELD_VALUE_NUMBER";
        }
    }

    /**
     * 日期类型属性值
     */
    record DateFieldValue(String fieldId, LocalDate value) implements FieldValueArg {
        @Override
        public String getType() {
            return "FIELD_VALUE_DATE";
        }
    }

    /**
     * 日期时间类型属性值
     */
    record DateTimeFieldValue(String fieldId, LocalDateTime value) implements FieldValueArg {
        @Override
        public String getType() {
            return "FIELD_VALUE_DATETIME";
        }
    }

    /**
     * 枚举类型属性值 - 存储枚举选项ID和名称（备份）
     */
    record EnumFieldValue(
            String fieldId,
            List<EnumOption> values
    ) implements FieldValueArg {
        @Override
        public String getType() {
            return "FIELD_VALUE_ENUM";
        }

        /**
         * 枚举选项
         */
        public record EnumOption(String optionId, String optionName) {}
    }

    /**
     * 架构/组织结构类型属性值 - 存储节点ID和名称（备份）
     */
    record StructureFieldValue(
            String fieldId,
            List<StructureNode> path
    ) implements FieldValueArg {
        @Override
        public String getType() {
            return "FIELD_VALUE_STRUCTURE";
        }

        /**
         * 架构节点
         */
        public record StructureNode(String nodeId, String nodeName) {}
    }

    /**
     * 关联类型属性值 - 存储关联卡片ID和标题（备份）
     */
    record LinkFieldValue(
            String fieldId,
            List<LinkedCardRef> cards
    ) implements FieldValueArg {
        @Override
        public String getType() {
            return "FIELD_VALUE_LINK";
        }

        /**
         * 关联卡片引用
         */
        public record LinkedCardRef(String cardId, String cardTypeId) {}
    }
}
