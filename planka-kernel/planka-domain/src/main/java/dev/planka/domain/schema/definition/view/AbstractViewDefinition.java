package dev.planka.domain.schema.definition.view;

import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.ViewId;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 视图定义抽象基类
 * <p>
 * 定义所有视图类型的通用属性和行为。具体视图类型（列表、看板、日历等）继承此类并添加各自特定的配置。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractViewDefinition extends AbstractSchemaDefinition<ViewId> {

    /** 是否为默认视图 */
    @JsonProperty("defaultView")
    protected boolean defaultView = false;

    /** 是否公开（其他用户可见） */
    @JsonProperty("shared")
    protected boolean shared = true;

    /** 可见性范围（用户ID列表，空表示所有人可见） */
    @JsonProperty("visibleTo")
    protected List<String> visibleTo;

    protected AbstractViewDefinition(ViewId id, String orgId, String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.VIEW;
    }

    @Override
    protected ViewId newId() {
        return ViewId.generate();
    }

    @Override
    public void validate() {
        super.validate();
    }

    /**
     * 获取视图类型字符串
     * 子类实现以返回具体的视图类型标识（如 "LIST", "KANBAN" 等）
     *
     * @return 视图类型字符串
     */
    public abstract String getViewType();


}
