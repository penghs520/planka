package dev.planka.api.schema.request;

import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建 Schema 请求
 */
@Getter
@Setter
public class CreateSchemaRequest {

    /** Schema 定义（类型安全） */
    @NotNull(message = "Schema定义不能为空")
    private AbstractSchemaDefinition<?> definition;

}
