package dev.planka.schema.service.common.lifecycle;

import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import org.springframework.stereotype.Component;

/**
 * 权限配置生命周期处理器
 * <p>
 * 校验规则：
 * <ul>
 *   <li>创建时必须指定 cardTypeId</li>
 *   <li>cardTypeId 创建后不可修改</li>
 * </ul>
 */
@Component
public class PermissionConfigLifecycleHandler implements SchemaLifecycleHandler<PermissionConfigDefinition> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CARD_PERMISSION;
    }

    @Override
    public void beforeCreate(PermissionConfigDefinition definition) {
        if (definition.getCardTypeId() == null) {
            throw new IllegalArgumentException("权限配置必须指定所属卡片类型ID");
        }
    }

    @Override
    public void beforeUpdate(PermissionConfigDefinition oldDefinition, PermissionConfigDefinition newDefinition) {
        // cardTypeId 创建后不可修改
        if (oldDefinition.getCardTypeId() != null && newDefinition.getCardTypeId() != null) {
            if (!oldDefinition.getCardTypeId().value().equals(newDefinition.getCardTypeId().value())) {
                throw new IllegalArgumentException("权限配置的所属卡片类型不可修改");
            }
        }
    }
}
