package dev.planka.schema.service.common.lifecycle;

import dev.planka.domain.schema.definition.SchemaDefinition;

/**
 * 默认的 Schema 生命周期处理器
 * <p>
 * 当没有为特定 Schema 类型注册专门的处理器时，使用此默认处理器。
 * 所有方法都是空实现，不执行任何操作。
 */
public class DefaultSchemaLifecycleHandler implements SchemaLifecycleHandler<SchemaDefinition<?>> {

    @Override
    public String getSchemaSubType() {
        return "__DEFAULT__";
    }
}
