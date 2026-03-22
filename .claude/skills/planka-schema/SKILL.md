---
name: planka-schema
description: 开发 planka 的 Schema 定义（__PLANKA_EINST__、字段配置、视图、业务规则等）。当用户涉及 Schema 定义、__PLANKA_EINST__继承、字段配置、视图定义、业务规则时激活。
---

# planka Schema 开发 Skill

## Schema 类型

| 类型 | 类名 | 说明 |
|------|------|------|
| 属性配置 | FieldConfig | 字段配置 |
| __PLANKA_EINST__ | CardTypeDefinition | 特征类型 / __PLANKA_EINST__ |
| 关联类型 | LinkTypeDefinition | 卡片间关联 |
| 视图定义 | ListViewDefinition | 列表/看板视图 |
| 价值流 | ValueStreamDefinition | 状态流转 |
| 业务规则 | BizRuleDefinition | 自动化规则 |
| 卡片动作 | CardActionConfigDefinition | 按钮动作 |
| 公式定义 | AbstractFormulaDefinition | 计算公式 |

## 继承体系

```
自身配置 > 显式父类 > 任意卡特征类型
```

## 字段配置查询

使用 `FieldConfigQueryService` 获取完整字段列表（含继承解析）：

```java
FieldConfigListWithSource result = fieldConfigQueryService
    .getFieldConfigListWithSource(cardTypeId)
    .getData();
```

## 缓存查询类

| 查询类 | 适用场景 |
|--------|----------|
| CardTypeCacheQuery | __PLANKA_EINST__缓存查询 |
| ViewCacheQuery | 视图定义缓存查询 |
| CardFaceCacheQuery | 卡片面版模板缓存查询 |
| BizRuleCacheQuery | 业务规则缓存查询 |
| LinkTypeCacheQuery | 关联类型缓存查询 |
| ValueStreamCacheQuery | 价值流缓存查询 |
| CardActionCacheQuery | 卡片动作缓存查询 |
| CardPermissionCacheQuery | 卡片权限缓存查询 |

## 开发规范

1. **禁止使用 Serializable**，使用 Jackson
2. **Schema ID 使用 SchemaId 子类**
3. 单文件 < 600 行
4. 新增定义类型需在 schema-service 和缓存层同步支持
