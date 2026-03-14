package cn.agilean.kanban.schema.controller;

import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.schema.definition.SchemaDefinition;
import cn.agilean.kanban.schema.service.common.SchemaCommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schemas/debug")
@RequiredArgsConstructor
public class DebugController {

    private final SchemaCommonService schemaCommonService;

    @GetMapping("/{id}")
    public Result<SchemaDefinition<?>> getSchemaDefinitionById(
            @PathVariable("id") String id) {
        return schemaCommonService.getById(id);
    }
}
