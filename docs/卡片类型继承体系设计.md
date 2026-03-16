# 卡片类型继承设计文档

## 1. 功能概述

EntityCardType（实体类型）支持从多个 AbstractCardType（属性集）继承，以获得：

1. **自定义属性** (FieldDefinition) - 全局属性定义，继承时直接合并
2. **关联关系** (LinkType) - 通过 cardTypeIds 间接继承
3. **属性配置** (CardTypeFieldConfig) - 卡片类型级别的属性配置，支持覆盖

### 1.1 设计原则

- **查询驱动**：卡片类型不维护属性/配置的 ID 列表，通过 `cardTypeId` 反向查询
- **自有优先**：EntityCardType 自有配置优先级最高，自动覆盖继承配置
- **冲突检测**：只有未被自有配置覆盖的 fieldId 才需要检测父类冲突

## 2. 模型设计

### 2.1 AbstractCardType（属性集）

```java
// 保持现状，无需添加额外字段
// 属性和配置通过 cardTypeId 查询获取
public final class AbstractCardType extends AbstractCardTypeDefinition {
    // 继承自 AbstractCardTypeDefinition 的基本属性
}
```

### 2.2 EntityCardType（实体类型）

```java
public final class EntityCardType extends AbstractCardTypeDefinition {

    // 继承的属性集ID列表（已存在）
    @JsonProperty("parentTypeIds")
    private Set<String> parentTypeIds;

    // 其他字段...
}
```

### 2.3 关联关系

| 实体 | 关联字段 | 说明 |
|------|----------|------|
| FieldDefinition | cardTypeId | 属性定义所属的卡片类型 |
| CardTypeFieldConfig | cardTypeId | 属性配置所属的卡片类型 |
| LinkTypeDefinition | sourceCardTypeIds, targetCardTypeIds | 关联类型约束的卡片类型 |

## 3. 继承规则

### 3.1 FieldDefinition 继承

- **规则**：直接合并所有父类和自有的 FieldDefinition
- **冲突处理**：无冲突（FieldDefinition 是全局定义，同一 fieldId 只有一个定义）

```
合并结果 = 父类1的属性 ∪ 父类2的属性 ∪ ... ∪ 自有属性
```

### 3.2 CardTypeFieldConfig 继承

- **规则**：自有配置优先级最高，覆盖继承配置
- **冲突处理**：有自有配置时无需处理冲突

```
合并优先级（从高到低）：
1. EntityCardType 自有配置
2. AbstractCardType 继承配置（无冲突时）
```

### 3.3 LinkType 继承

- **规则**：EntityCardType 自动适用于引用了其父类的 LinkType
- **实现**：查询时包含自身 ID 和所有 parentTypeIds

```java
Set<String> allCardTypeIds = {cardType.id} ∪ cardType.parentTypeIds
// 查询 sourceCardTypeIds 或 targetCardTypeIds 包含 allCardTypeIds 的 LinkType
```

## 4. 冲突检测

### 4.1 冲突定义

当满足以下**所有**条件时视为 CardTypeFieldConfig 冲突：

1. EntityCardType 继承多个 AbstractCardType
2. 多个父类对**同一 fieldId** 配置了不同的 CardTypeFieldConfig
3. EntityCardType **没有**该 fieldId 的自有配置

### 4.2 验证流程

```
保存 EntityCardType 时:
1. 查询自有的 CardTypeFieldConfig（cardTypeId = 当前类型ID）
2. 收集自有配置覆盖的 fieldId 集合（已解决冲突）
3. 查询所有父类的 CardTypeFieldConfig
4. 对于未覆盖的 fieldId，检测是否有多个父类配置冲突
5. 若存在未解决冲突，抛出 InheritanceConflictException
```

### 4.3 冲突解决

用户需要在 EntityCardType 上为冲突的 fieldId 创建自有配置，以覆盖继承配置。

## 5. API 说明

### 5.1 CardTypeInheritanceResolver

继承解析核心服务，提供以下功能：

```java
public class CardTypeInheritanceResolver {

    /**
     * 解析实体类型的完整配置（包含继承）
     * @param entityCardType 实体类型
     * @return 合并后的完整配置
     */
    public InheritedConfig resolveInheritedConfig(EntityCardType entityCardType);

    /**
     * 检测继承冲突
     * @param entityCardType 实体类型
     * @return 冲突列表（同一 fieldId 在多个父类有不同配置）
     */
    public List<InheritanceConflict> detectConflicts(EntityCardType entityCardType);

    /**
     * 验证继承配置是否合法
     * @param entityCardType 实体类型
     * @throws InheritanceConflictException 存在未解决冲突时抛出
     */
    public void validateInheritance(EntityCardType entityCardType);

    /**
     * 解析实体类型可用的关联类型
     * @param entityCardType 实体类型
     * @return 可用的关联类型列表
     */
    public List<LinkTypeDefinition> resolveLinkTypes(EntityCardType entityCardType);
}
```

### 5.2 InheritedConfig

合并后的配置结果：

```java
public class InheritedConfig {
    /** 所有生效的 FieldDefinition（继承 + 自有） */
    private List<FieldDefinition> fieldDefinitions;

    /** 所有生效的 CardTypeFieldConfig（按 fieldId 索引） */
    private Map<String, CardTypeFieldConfig> fieldConfigs;

    /** 配置来源追踪（fieldId -> 来源 cardTypeId） */
    private Map<String, String> configSources;
}
```

### 5.3 InheritanceConflict

冲突信息：

```java
public class InheritanceConflict {
    /** 冲突的 fieldId */
    private String fieldId;

    /** 冲突的配置列表（来自不同父类） */
    private List<CardTypeFieldConfig> conflictingConfigs;

    /** 冲突来源的父类 ID 列表 */
    private List<String> sourceCardTypeIds;
}
```

## 6. 使用示例

### 6.1 定义属性集

```json
// AbstractCardType: "工作项"
{
  "id": "abstract-workitem",
  "name": "工作项",
  "cardTypeKind": "ABSTRACT"
}

// 关联的 FieldDefinition
{
  "id": "field-priority",
  "cardTypeId": "abstract-workitem",
  "name": "优先级",
  "fieldType": "ENUM"
}

// 关联的 CardTypeFieldConfig
{
  "id": "config-priority",
  "cardTypeId": "abstract-workitem",
  "fieldId": "field-priority",
  "defaultOptionIds": ["high"]
}
```

### 6.2 定义实体类型

```json
// EntityCardType: "需求"
{
  "id": "concrete-requirement",
  "name": "需求",
  "cardTypeKind": "ENTITY",
  "parentTypeIds": ["abstract-workitem"]
}

// 覆盖继承的配置（可选）
{
  "id": "config-priority-override",
  "cardTypeId": "concrete-requirement",
  "fieldId": "field-priority",
  "defaultOptionIds": ["medium"]  // 覆盖为默认中优先级
}
```

### 6.3 运行时解析

```java
EntityCardType requirement = cardTypeRepository.findById("concrete-requirement");
InheritedConfig config = inheritanceResolver.resolveInheritedConfig(requirement);

// config.fieldDefinitions 包含 "优先级" 属性定义
// config.fieldConfigs.get("field-priority") 返回覆盖后的配置（defaultOptionIds = ["medium"]）
```

## 7. 注意事项

1. **属性集不支持继承**：只有 EntityCardType 可以继承 AbstractCardType
2. **继承关系不可循环**：parentTypeIds 引用的类型必须是 AbstractCardType
3. **保存时验证**：保存 EntityCardType 时会自动进行冲突检测
4. **运行时合并**：配置合并在运行时进行，数据库只存储自有配置
