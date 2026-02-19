package dev.planka.api.schema;

import dev.planka.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;
import java.util.Set;

/**
 * 二级索引查询客户端
 * <p>
 * 用于服务启动时加载全量二级索引数据到本地内存。
 */
@FeignClient(name = "schema-service", contextId = "secondaryIndexQueryClient")
public interface SecondaryIndexQueryClient {

    /**
     * 获取全量二级索引数据
     * <p>
     * 返回结构: secondKeyType -> (secondKeyValue -> Set<schemaId>)
     * <p>
     * 例如: {"CARD_TYPE": {"cardTypeId1": ["fieldDefId1", "fieldDefId2"]}}
     *
     * @return 全量二级索引映射
     */
    @GetMapping("/api/v1/schemas/secondary-index/all")
    Result<Map<String, Map<String, Set<String>>>> getAllSecondaryIndexes();
}
