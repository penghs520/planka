package dev.planka.domain.card;

import dev.planka.common.util.AssertUtils;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;

import java.util.List;

/**
 * 卡片标题
 * <p>
 * 支持两种类型的标题：
 * 1. 纯标题（PureTitle）：简单的文本标题
 * 2. 拼接标题（JointTitle）：由多个部分组成的标题，支持前缀/后缀拼接
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardTitle.PureTitle.class, name = "PURE"),
        @JsonSubTypes.Type(value = CardTitle.JointTitle.class, name = "JOINT")
})
public sealed interface CardTitle permits CardTitle.PureTitle, CardTitle.JointTitle {

    /**
     * 获取标题的显示值
     */
    String getDisplayValue();

    String getValue();

    String getType();

    /**
     * 纯标题
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    final class PureTitle implements CardTitle {
        private final String value;

        @JsonCreator
        public PureTitle(@JsonProperty("value") String value) {
            AssertUtils.notBlank(value, "title value can't be blank");
            this.value = value;
        }

        @Override
        public String getDisplayValue() {
            return value;
        }

        @Override
        public String getType() {
            return "PURE";
        }
    }

    /**
     * 拼接标题
     * <p>
     * 由基础名称和拼接部分组成，支持前缀或后缀拼接
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    final class JointTitle implements CardTitle {
        /**
         * 基础名称
         */
        private final String value;

        /**
         * 拼接区域（前缀或后缀）
         */
        private final JointArea area;

        /**
         * 拼接部分列表
         */
        private final List<JointParts> multiParts;

        @JsonCreator
        public JointTitle(@JsonProperty("value") String value,
                @JsonProperty("area") JointArea area,
                @JsonProperty("multiParts") List<JointParts> multiParts) {
            AssertUtils.notBlank(value, "joint title name can't be blank");
            this.value = value;
            this.area = area != null ? area : JointArea.SUFFIX;
            this.multiParts = multiParts;
        }

        @Override
        public String getDisplayValue() {
            if (multiParts == null || multiParts.isEmpty()) {
                return value;
            }

            StringBuilder jointValue = new StringBuilder();
            for (JointParts parts : multiParts) {
                if (parts.parts() != null) {
                    for (JointPart part : parts.parts()) {
                        if (part.name() != null) {
                            jointValue.append(part.name());
                        }
                    }
                }
            }

            return area == JointArea.PREFIX
                    ? jointValue + value
                    : value + jointValue;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getType() {
            return "JOINT";
        }
    }

    /**
     * 拼接区域枚举
     */
    enum JointArea {
        PREFIX, // 前缀
        SUFFIX // 后缀
    }

    /**
     * 拼接标题的组成部分 - 多项
     */
    record JointParts(List<JointPart> parts) {
        @JsonCreator
        public JointParts(@JsonProperty("parts") List<JointPart> parts) {
            this.parts = parts;
        }
    }

    /**
     * 拼接标题的组成部分 - 单项
     */
    record JointPart(String name) {
        @JsonCreator
        public JointPart(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    /**
     * 创建纯标题的便捷方法
     */
    static CardTitle pure(String value) {
        return new PureTitle(value);
    }

    /**
     * 创建拼接标题的便捷方法
     */
    static CardTitle joint(String name, JointArea area, List<JointParts> multiParts) {
        return new JointTitle(name, area, multiParts);
    }
}
