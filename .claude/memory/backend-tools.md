# 后端通用工具

## 断言工具

| 工具 | 路径 | 适用场景 |
|------|------|----------|
| `AssertUtils` | `planka-common/util/AssertUtils.java` | 参数校验、前置条件检查 |

## 缓存工具

| 工具 | 路径 | 适用场景 |
|------|------|----------|
| `CardCacheService` | `planka-infra-card-cache/` | 卡片基础信息二级缓存 |
| `SchemaCacheService` | `planka-infra-schema-cache/` | Schema 定义二级缓存 |

## 缓存查询类

| 查询类 | 适用场景 |
|--------|----------|
| `CardTypeCacheQuery` | 实体类型缓存查询，支持按组织ID查询 |
| `ViewCacheQuery` | 视图定义缓存查询 |
| `CardFaceCacheQuery` | 卡片面版模板缓存查询，支持按实体类型查询 |
| `BizRuleCacheQuery` | 业务规则缓存查询，支持按实体类型查询 |
| `LinkTypeCacheQuery` | 关联类型缓存查询，支持按实体类型查询 |
| `ValueStreamCacheQuery` | 价值流缓存查询 |
| `CardActionCacheQuery` | 卡片动作缓存查询 |
| `CardPermissionCacheQuery` | 卡片权限缓存查询 |

## 其他工具

| 工具 | 路径 | 适用场景 |
|------|------|----------|
| `ConditionYieldBuilder` | `card-api/util/ConditionYieldBuilder.java` | 从条件中提取字段构建 Yield |
| `TextExpressionTemplateResolver` | `planka-infra-expression/TextExpressionTemplateResolver.java` | 解析模板变量生成动态文本 |
| `FieldConfigQueryService` | `schema-api/service/FieldConfigQueryService.java` | 获取实体类型字段配置（含继承解析） |
