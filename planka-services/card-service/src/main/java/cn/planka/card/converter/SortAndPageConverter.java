package cn.planka.card.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zgraph.driver.proto.common.SortWay;
import zgraph.driver.proto.query.*;
import zgraph.driver.proto.query.Page;
import zgraph.driver.proto.query.Sort;
import zgraph.driver.proto.query.SortAndPage;
import zgraph.driver.proto.query.SortField;

/**
 * 排序和分页转换器
 * 将 card-api 的 SortAndPage 转换为 zgraph protobuf 的 SortAndPage
 */
public class SortAndPageConverter {

    private static final Logger log = LoggerFactory.getLogger(SortAndPageConverter.class);

    private SortAndPageConverter() {
    }

    /**
     * 转换为 protobuf SortAndPage
     */
    public static SortAndPage toProto(cn.planka.api.card.request.SortAndPage sortAndPage) {
        if (sortAndPage == null) {
            return SortAndPage.getDefaultInstance();
        }

        SortAndPage.Builder builder = SortAndPage.newBuilder();

        // 转换分页信息
        if (sortAndPage.getPage() != null) {
            builder.setPage(toProtoPage(sortAndPage.getPage()));
        }

        // 转换排序列表
        if (sortAndPage.getSorts() != null) {
            for (cn.planka.api.card.request.Sort sort : sortAndPage.getSorts()) {
                builder.addSorts(toProtoSort(sort));
            }
        }

        return builder.build();
    }

    /**
     * 转换分页信息
     */
    private static Page toProtoPage(cn.planka.api.card.request.Page page) {
        if (page == null) {
            return Page.getDefaultInstance();
        }

        return Page.newBuilder()
                .setPageNum(page.getPageNum())
                .setPageSize(page.getPageSize())
                .build();
    }

    /**
     * 转换排序项
     */
    private static Sort toProtoSort(cn.planka.api.card.request.Sort sort) {
        if (sort == null) {
            return Sort.getDefaultInstance();
        }

        Sort.Builder builder = Sort.newBuilder();

        // 转换排序字段
        if (sort.getSortField() != null) {
            builder.setSortField(toProtoSortField(sort.getSortField()));
        }

        // 转换排序方式
        if (sort.getSortWay() != null) {
            builder.setSortWay(toProtoSortWay(sort.getSortWay()));
        }

        return builder.build();
    }

    /**
     * 转换排序字段
     * 注意：目前简化处理，仅支持内置字段排序
     */
    private static SortField toProtoSortField(cn.planka.api.card.request.SortField sortField) {
        if (sortField == null) {
            return SortField.getDefaultInstance();
        }

        SortField.Builder builder = SortField.newBuilder();

        // 根据字段类型转换
        // TODO 暂不支持非内置属性的排序
        String fieldId = sortField.getFieldId();
        if (fieldId != null) {
            // 判断是否是内置属性
            if (isInnerField(fieldId)) {
                builder.setInnerField(SortInnerField.newBuilder().setFieldId(fieldId).build());
            } else {
                log.debug("暂不支持非内置属性的排序，需要接入Schema查询功能后判断属性类型后才支持");
            }
        }

        return builder.build();
    }

    /**
     * 判断是否是内置字段
     */
    private static boolean isInnerField(String fieldId) {
        return fieldId != null && (fieldId.equals("title") ||
                fieldId.equals("code") ||
                fieldId.equals("cardType") ||
                fieldId.equals("createdAt") ||
                fieldId.equals("updatedAt") ||
                fieldId.equals("createdBy") ||
                fieldId.equals("updatedBy") ||
                fieldId.equals("discardedAt") ||
                fieldId.equals("archivedAt"));
    }

    /**
     * 转换排序方式
     */
    private static SortWay toProtoSortWay(cn.planka.api.card.request.SortWay sortWay) {
        if (sortWay == null) {
            return SortWay.Asc;
        }
        return switch (sortWay) {
            case ASC -> SortWay.Asc;
            case DESC -> SortWay.Desc;
        };
    }
}
