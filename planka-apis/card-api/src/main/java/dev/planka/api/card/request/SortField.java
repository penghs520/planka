package dev.planka.api.card.request;

import dev.planka.domain.link.Path;
import lombok.Data;

import java.util.Map;

/**
 * 排序字段
 */
@Data
public class SortField {


    private Path path;

    // 通用字段ID (结合path使用，当为path存在而fieldId为空时，表示使用关联卡的标题进行排序)
    private String fieldId;

    // 枚举选项排序的辅助信息
    private Map<String, Integer> enumOptionsOrderInfo;

}
