package dev.planka.domain.schema.definition.permission;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.PermissionConfigId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 权限配置定义
 * <p>
 * 定义卡片类型级别的权限配置，包括操作权限、属性权限、附件权限等。
 * 支持组织级和空间级两层配置，运行时采用交集合并策略。
 * <p>
 * belongTo: 所属的卡片类型ID
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PermissionConfigDefinition extends AbstractSchemaDefinition<PermissionConfigId> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CARD_PERMISSION;
    }

    /**
     * 所属卡片类型ID
     */
    @Setter
    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    /**
     * 卡片操作权限列表
     */
    @Setter
    @JsonProperty("cardOperations")
    private List<PermissionConfig.CardOperationPermission> cardOperations;

    /**
     * 属性级别权限列表
     */
    @Setter
    @JsonProperty("fieldPermissions")
    private List<PermissionConfig.FieldPermission> fieldPermissions;

    /**
     * 附件权限列表
     */
    @Setter
    @JsonProperty("attachmentPermissions")
    private List<PermissionConfig.AttachmentPermission> attachmentPermissions;

    @JsonCreator
    public PermissionConfigDefinition(
            @JsonProperty("id") PermissionConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.CARD_PERMISSION;
    }

    /**
     * 权限配置属于某个卡片类型
     */
    @Override
    public SchemaId belongTo() {
        return cardTypeId;
    }

    /**
     * 二级索引
     */
    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of(cardTypeId);
    }

    @Override
    protected PermissionConfigId newId() {
        return PermissionConfigId.generate();
    }

    /**
     * 将定义转换为运行时配置对象
     */
    public PermissionConfig toPermissionConfig() {
        PermissionConfig config = new PermissionConfig();
        config.setCardOperations(this.cardOperations);
        config.setFieldPermissions(this.fieldPermissions);
        config.setAttachmentPermissions(this.attachmentPermissions);
        return config;
    }

    @Override
    public void validate() {
        super.validate();
        if (cardTypeId == null) {
            throw new IllegalArgumentException("cardTypeId 不能为空");
        }
    }
}
