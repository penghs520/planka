package dev.planka.schema.service.common.lifecycle;

import dev.planka.domain.schema.definition.SchemaDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Schema 生命周期处理器注册中心
 * <p>
 * 负责管理和查找不同 Schema 类型对应的生命周期处理器。
 * 通过 Spring 自动注入所有实现了 {@link SchemaLifecycleHandler} 接口的 Bean。
 */
@Slf4j
@Component
public class SchemaLifecycleHandlerRegistry {

    private final Map<String, SchemaLifecycleHandler<?>> handlers;
    private final DefaultSchemaLifecycleHandler defaultHandler = new DefaultSchemaLifecycleHandler();

    public SchemaLifecycleHandlerRegistry(List<SchemaLifecycleHandler<?>> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        SchemaLifecycleHandler::getSchemaSubType,
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("Duplicate handler for schemaSubType: {}, using: {}",
                                    existing.getSchemaSubType(), replacement.getClass().getName());
                            return replacement;
                        }
                ));
        log.info("Registered {} schema lifecycle handlers: {}",
                handlers.size(), handlers.keySet());
    }

    /**
     * 获取指定 Schema 子类型的生命周期处理器
     *
     * @param schemaSubType Schema 子类型标识
     * @return 对应的处理器，如果没有找到则返回默认处理器
     */
    @SuppressWarnings("unchecked")
    public <T extends SchemaDefinition<?>> SchemaLifecycleHandler<T> getHandler(String schemaSubType) {
        return (SchemaLifecycleHandler<T>) handlers.getOrDefault(schemaSubType, defaultHandler);
    }
}
