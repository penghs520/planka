package cn.planka.card.controller.request;

import cn.planka.api.card.request.LinkFieldUpdate;
import cn.planka.domain.schema.definition.action.assignment.FixedValue;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 执行卡片动作请求
 */
@Getter
@Setter
public class ExecuteActionRequest {

    /**
     * 用户输入的字段值
     * key: fieldId, value: 用户输入的固定值
     */
    private Map<String, FixedValue> userInputs;

    /**
     * 创建关联卡片弹窗提交的新卡标题（非空时优先于标题模板解析）
     */
    private String linkedCardTitle;

    /**
     * 新建卡片上的关联属性更新（与创建卡片请求中的 linkUpdates 一致）
     */
    private List<LinkFieldUpdate> linkedCardLinkUpdates;

    /**
     * 弹窗提交的字段值（与写库 FieldValue JSON 一致，含 CASCADE 等），优先于 userInputs 的 FixedValue 合并
     */
    private Map<String, Object> linkedCardFieldValues;
}
