package cn.planka.schema.dto;

import cn.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import lombok.Data;

/**
 * 实体类型下「当前生效」的详情页模板：可能来自库表，也可能为未持久化的默认构建结果。
 */
@Data
public class EffectiveCardDetailTemplateVO {

    /** 模板定义 */
    private CardDetailTemplateDefinition definition;

    /**
     * 是否已持久化。为 {@code false} 时表示内存默认模板，首次保存应走创建接口。
     */
    private boolean persisted;
}
