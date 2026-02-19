package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;

/**
 * 引用源接口
 * <p>
 * 定义过滤条件值可以引用的数据源，支持引用当前卡片、参数卡片、成员、上下文卡片等。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReferenceSource.CurrentCard.class, name = "CURRENT_CARD"),
        @JsonSubTypes.Type(value = ReferenceSource.ParameterCard.class, name = "PARAMETER_CARD"),
        @JsonSubTypes.Type(value = ReferenceSource.Member.class, name = "MEMBER"),
        @JsonSubTypes.Type(value = ReferenceSource.ContextualCard.class, name = "CONTEXTUAL_CARD")
})
public interface ReferenceSource {

    /**
     * 引用当前卡片
     * <p>
     * 通过路径引用当前卡片的属性值。路径可以是多级的，例如：当前卡.父需求.创建时间
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class CurrentCard implements ReferenceSource {
        /**
         * 引用路径（可选）
         */
        private final Path path;

        @JsonCreator
        public CurrentCard(@JsonProperty("path") Path path) {
            this.path = path;
        }
    }

    /**
     * 引用参数卡片
     * <p>
     * 引用作为参数传入的卡片。可以通过路径进一步访问该卡片的关联卡片的属性。
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ParameterCard implements ReferenceSource {
        /**
         * 引用路径（可选）
         */
        private final Path path;

        /**
         * 参数卡片类型ID
         */
        private final String parameterCardTypeId;

        @JsonCreator
        public ParameterCard(@JsonProperty("path") Path path,
                            @JsonProperty("parameterCardTypeId") String parameterCardTypeId) {
            AssertUtils.notBlank(parameterCardTypeId, "parameterCardTypeId can't be blank");
            this.path = path;
            this.parameterCardTypeId = parameterCardTypeId;
        }
    }

    /**
     * 引用成员
     * <p>
     * 引用当前操作成员相关的属性。
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Member implements ReferenceSource {
        /**
         * 引用路径（可选）
         */
        private final Path path;

        @JsonCreator
        public Member(@JsonProperty("path") Path path) {
            this.path = path;
        }
    }

    /**
     * 引用上下文卡片
     * <p>
     * 引用特定的上下文卡片。上下文卡片是在运行时动态指定的卡片。
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ContextualCard implements ReferenceSource {
        /**
         * 引用路径（可选）
         */
        private final Path path;

        /**
         * 上下文卡片ID
         */
        private final String contextualCardId;

        /**
         * 上下文顶点ID（图数据库中的顶点ID）
         */
        private final Long contextualVertexId;

        @JsonCreator
        public ContextualCard(@JsonProperty("path") Path path,
                             @JsonProperty("contextualCardId") String contextualCardId,
                             @JsonProperty("contextualVertexId") Long contextualVertexId) {
            AssertUtils.notBlank(contextualCardId, "contextualCardId can't be blank");
            this.path = path;
            this.contextualCardId = contextualCardId;
            this.contextualVertexId = contextualVertexId;
        }
    }
}
