package dev.planka.api.card.request;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.card.OrgId;
import dev.planka.domain.field.FieldValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 创建卡片请求
 *
 * @param orgId       组织ID
 * @param typeId      卡片类型ID
 * @param title       卡片标题
 * @param description 卡片描述
 * @param fieldValues 初始属性值
 * @param linkUpdates 关联属性 (可选，覆盖式更新)
 */
public record CreateCardRequest(OrgId orgId, CardTypeId typeId, CardTitle title, String description,
                                Map<String, FieldValue<?>> fieldValues,
                                List<LinkFieldUpdate> linkUpdates) {
    @JsonCreator
    public CreateCardRequest(@JsonProperty("orgId") OrgId orgId,
                             @JsonProperty("typeId") CardTypeId typeId,
                             @JsonProperty("title") CardTitle title,
                             @JsonProperty("description") String description,
                             @JsonProperty("fieldValues") Map<String, FieldValue<?>> fieldValues,
                             @JsonProperty("linkUpdates") List<LinkFieldUpdate> linkUpdates) {
        AssertUtils.notNull(orgId, "orgId can't be null");
        AssertUtils.notNull(typeId, "typeId can't be null");
        AssertUtils.notNull(title, "title can't be null");
        this.orgId = orgId;
        this.typeId = typeId;
        this.title = title;
        this.description = description;
        this.fieldValues = fieldValues;
        this.linkUpdates = linkUpdates;
    }

    /**
     * 兼容旧构造函数（不含关联属性）
     */
    public CreateCardRequest(OrgId orgId, CardTypeId typeId, CardTitle title, String description,
                             Map<String, FieldValue<?>> fieldValues) {
        this(orgId, typeId, title, description, fieldValues, null);
    }
}
