package dev.planka.schema.controller;

import dev.planka.common.result.Result;
import dev.planka.schema.mapper.SchemaIndexMapper;
import dev.planka.schema.model.SchemaIndexEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 二级索引查询控制器
 * <p>
 * 提供全量二级索引查询接口，供其他服务启动时加载到本地内存。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/schemas/secondary-index")
@RequiredArgsConstructor
public class SecondaryIndexController {

    private final SchemaIndexMapper schemaIndexMapper;

    /**
     * 获取全量二级索引数据
     * <p>
     * 返回结构: indexType -> (indexKey -> Set<schemaId>)
     *
     * @return 全量二级索引映射
     */
    @GetMapping("/all")
    public Result<Map<String, Map<String, Set<String>>>> getAllSecondaryIndexes() {
        log.info("Loading all secondary indexes...");

        List<SchemaIndexEntity> allIndexes = schemaIndexMapper.findAll();

        // 转换为嵌套 Map 结构: indexType -> (indexKey -> Set<schemaId>)
        Map<String, Map<String, Set<String>>> result = new HashMap<>();

        for (SchemaIndexEntity entity : allIndexes) {
            result
                    .computeIfAbsent(entity.getIndexType(), k -> new HashMap<>())
                    .computeIfAbsent(entity.getIndexKey(), k -> new HashSet<>())
                    .add(entity.getSchemaId());
        }

        log.info("Loaded {} index entries, {} index types",
                allIndexes.size(), result.size());

        return Result.success(result);
    }
}
