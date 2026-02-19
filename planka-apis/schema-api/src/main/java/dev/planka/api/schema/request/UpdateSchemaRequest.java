package dev.planka.api.schema.request;

import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新 Schema 请求
 */
@Getter
@Setter
public class UpdateSchemaRequest {

    /** Schema 定义（可选，不传则不更新定义内容） */
    private AbstractSchemaDefinition<?> definition;

    /** 期望的版本号（乐观锁） */
    private Integer expectedVersion;

}
