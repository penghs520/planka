package dev.planka.domain.schema.definition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.schema.EntityState;
import dev.planka.domain.schema.SchemaId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Schema 定义抽象基类
 * <p>
 * 提供所有 Schema 定义的公共属性和方法。
 * 包含元数据（id, orgId, state, version等）和业务定义属性。
 *
 * @param <ID> Schema ID 类型，必须实现 SchemaId 接口
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractSchemaDefinition<ID extends SchemaId> implements SchemaDefinition<ID> {


    // ==================== 标识信息 ====================

    /**
     * Schema ID（设置后不应修改）
     */
    @JsonProperty("id")
    protected final ID id;

    /**
     * 组织ID（设置后不应修改）
     */
    @JsonProperty("orgId")
    protected String orgId;

    // ==================== 基本信息 ====================

    /**
     * 名称
     */
    @JsonProperty("name")
    protected String name;

    /**
     * 描述
     */
    @JsonProperty("description")
    protected String description;

    /**
     * 排序号
     */
    @JsonProperty("sortOrder")
    protected Integer sortOrder;

    /**
     * 是否启用
     */
    @JsonProperty("enabled")
    protected boolean enabled = true;

    // ==================== 状态信息 ====================

    /**
     * 实体状态
     */
    protected EntityState state;

    /**
     * 内容版本号（乐观锁，每次更新递增）
     */
    protected int contentVersion;

    /**
     * 结构版本号（语义版本）
     */
    protected String structureVersion;

    // ==================== 审计信息 ====================

    /**
     * 创建时间
     */
    protected LocalDateTime createdAt;

    /**
     * 创建人ID
     */
    protected String createdBy;

    /**
     * 更新时间
     */
    protected LocalDateTime updatedAt;

    /**
     * 更新人ID
     */
    protected String updatedBy;

    /**
     * 删除时间（软删除）
     */
    protected LocalDateTime deletedAt;

    // ==================== 构造函数 ====================

    /**
     * 完整构造函数（用于JSON反序列化或完整创建）
     *
     * @param id 为空则会新建ID
     */
    protected AbstractSchemaDefinition(ID id, String orgId, String name) {
        this.id = id == null ? newId() : id;
        this.orgId = orgId; //不再这里校验
        this.name = AssertUtils.requireNotBlank(name, "name不能为空");
        this.state = EntityState.ACTIVE;
        this.contentVersion = 1;
    }

    // ==================== 查询方法 ====================

    /**
     * 是否为活跃状态
     */
    public boolean isActive() {
        return state == EntityState.ACTIVE;
    }

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return state == EntityState.DELETED;
    }

    /**
     * 获取所属 Schema ID（组合关系的宿主）
     * <p>
     * 需要组合关系的子类（如 FieldConfig）返回其所属 Schema 的 ID，
     * 不需要组合关系的子类（如 CardTypeDefinition、ViewDefinition）返回 null。
     *
     * @return 所属 Schema ID，若无则返回 null
     */
    @JsonIgnore
    public abstract SchemaId belongTo();

    /**
     * 是否有所属关系
     */
    public boolean hasBelongTo() {
        return belongTo() != null;
    }

    /**
     * 获取业务二级索引列表
     * <p>
     * 由子类实现，返回用于快速检索的二级索引。
     * 例如：FieldDefinition 可返回关联的 CardTypeId 列表。
     *
     * @return 二级索引集合，若无则返回空集合
     */
    @JsonIgnore
    public abstract Set<SchemaId> secondKeys();


    // ==================== SchemaElement 接口实现 ====================

    @Override
    public ID getId() {
        return id;
    }

    @Override
    public String getOrgId() {
        return orgId;
    }

    @Override
    public EntityState getState() {
        return state;
    }


    // ==================== 验证方法 ====================

    @Override
    public void validate() {
        SchemaDefinition.super.validate();
        AssertUtils.notBlank(orgId,"orgId can't be blank");
    }

    protected abstract ID newId();
}
