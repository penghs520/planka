# Schema 缓存使用规范

## 强制规则

1. **schema-service 之外的服务必须使用缓存查询 Schema**
2. **禁止直接使用 FeignClient 跨服务调用查询 Schema**

## 可用缓存查询类

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

## 正确使用方式

```java
// ✅ 正确 - 使用缓存查询
CardTypeDefinition cardType = cardTypeCacheQuery.getById(cardTypeId)
    .orElseThrow(() -> new BizException(CommonErrorCode.DATA_NOT_FOUND, "实体类型不存在"));

// ✅ 正确 - 使用 FieldConfigQueryService
FieldConfigListWithSource result = fieldConfigQueryService
    .getFieldConfigListWithSource(cardTypeId)
    .getData();
```

## 错误使用方式

```java
// ❌ 错误 - 禁止直接跨服务调用
// CardTypeDefinition cardType = cardServiceClient.getCardType(cardTypeId);

// ❌ 错误 - 禁止绕过缓存直接查询数据库
// cardTypeMapper.selectById(cardTypeId);
```

## 缓存更新

缓存由 schema-service 在数据变更时自动刷新，其他服务无需手动处理缓存更新。
