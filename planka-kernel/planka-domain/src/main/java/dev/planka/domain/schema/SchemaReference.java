package dev.planka.domain.schema;

import java.util.Objects;

/**
 * Schema 引用关系值对象
 * <p>
 * 描述两个 Schema 之间的引用关系。
 * 采用显式声明模式，在创建/更新 Schema 时由调用方明确指定引用关系。
 */
public record SchemaReference(
        SchemaId sourceId,
        SchemaType sourceType,
        SchemaId targetId,
        SchemaType targetType,
        ReferenceType referenceType
) {

    public SchemaReference {
        Objects.requireNonNull(sourceId, "sourceId cannot be null");
        Objects.requireNonNull(sourceType, "sourceType cannot be null");
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Objects.requireNonNull(targetType, "targetType cannot be null");
        Objects.requireNonNull(referenceType, "referenceType cannot be null");
    }

    /**
     * 创建组合关系引用
     */
    public static SchemaReference composition(
            SchemaId sourceId, SchemaType sourceType,
            SchemaId targetId, SchemaType targetType) {
        return new SchemaReference(sourceId, sourceType, targetId, targetType, ReferenceType.COMPOSITION);
    }

    /**
     * 创建聚合关系引用
     */
    public static SchemaReference aggregation(
            SchemaId sourceId, SchemaType sourceType,
            SchemaId targetId, SchemaType targetType) {
        return new SchemaReference(sourceId, sourceType, targetId, targetType, ReferenceType.AGGREGATION);
    }

    /**
     * 是否是组合关系
     */
    public boolean isComposition() {
        return referenceType == ReferenceType.COMPOSITION;
    }

    /**
     * 是否是聚合关系
     */
    public boolean isAggregation() {
        return referenceType == ReferenceType.AGGREGATION;
    }
}
