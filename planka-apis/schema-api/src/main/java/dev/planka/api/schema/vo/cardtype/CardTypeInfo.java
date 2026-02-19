package dev.planka.api.schema.vo.cardtype;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 卡片类型信息
 */
@Data
@SuperBuilder
public class CardTypeInfo {
    private String id;
    private String name;
}
