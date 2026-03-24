package cn.planka.domain.schema.definition.view;

import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.ViewId;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
import cn.planka.domain.schema.definition.condition.Condition;
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
public abstract class AbstractViewDefinition extends AbstractSchemaDefinition<ViewId> implements NavVisibilitySubject {

    /** 是否为默认视图 */
    @JsonProperty("defaultView")
    protected boolean defaultView = false;

    /** 是否公开（其他用户可见） */
    @JsonProperty("shared")
    protected boolean shared = true;

    /** 可见性范围（用户ID列表，空表示所有人可见） */
    @JsonProperty("visibleTo")
    protected List<String> visibleTo;

    /**
     * 可见性范围基数（新模型，持久化推荐显式写入；未写时由 {@link #getEffectiveViewVisibilityScope()} 从 shared 推导）
     */
    @JsonProperty("viewVisibilityScope")
    protected ViewVisibilityScope viewVisibilityScope;

    /** 团队可见时的团队卡 ID 列表 */
    @JsonProperty("visibleTeamCardIds")
    protected List<String> visibleTeamCardIds;

    /** 架构节点可见时的节点 ID 列表 */
    @JsonProperty("visibleCascadeRelationNodeIds")
    protected List<String> visibleCascadeRelationNodeIds;

    /**
     * 受众条件（可选）；与列表数据过滤条件分离。
     * 当前服务端对非空根条件尚未实现求值，保存时将被拒绝。
     */
    @JsonProperty("visibilityAudienceCondition")
    protected Condition visibilityAudienceCondition;

    protected AbstractViewDefinition(ViewId id, String orgId, String name) {
        super(id, orgId, name);
    }

    /**
     * 有效可见性范围：新字段优先，否则按旧 shared 字段兼容推导。
     */
    public ViewVisibilityScope getEffectiveViewVisibilityScope() {
        if (viewVisibilityScope != null) {
            return viewVisibilityScope;
        }
        if (!shared) {
            return ViewVisibilityScope.PRIVATE;
        }
        return ViewVisibilityScope.WORKSPACE;
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
     * 子类实现以返回具体的视图类型标识（如 "LIST", "planka" 等）
     *
     * @return 视图类型字符串
     */
    public abstract String getViewType();


}
