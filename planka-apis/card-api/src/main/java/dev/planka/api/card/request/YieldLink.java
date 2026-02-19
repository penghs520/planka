package dev.planka.api.card.request;

import lombok.Data;

/**
 * 指定需要返回的关联卡
 */
@Data
public class YieldLink {
    /**
     * 关联属性ID
     * <p>
     * 格式为 "{linkTypeId}:{SOURCE|TARGET}"，如 "263671031548350464:SOURCE"
     */
    private String linkFieldId;

    /**
     * 关联卡的返回定义（递归）
     */
    private Yield targetYield;
}
