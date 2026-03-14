# 前端开发工具文档

本文档记录项目中常用的前端开发工具和组件的详细使用说明。

---

## 文档说明

本文档仅作快速索引，**具体使用方式以源码为准**。各工具的详细用法请查看：
- 源码注释
- 单元测试
- 实际使用示例（通过 IDE 查找引用）

---

## CompactModal（紧凑型模态框）

### 概述

`CompactModal` 是一个基于 Arco Design 的紧凑型模态框工具，提供统一的样式和行为配置。

**优先使用场景**：所有需要强制用户操作的模态框场景

### 特性

- ✅ 标题左对齐
- ✅ 紧凑间距（减小 padding 和 margin）
- ✅ 蓝色主题按钮（`#3370FF`）
- ✅ 支持强制性弹窗（无法关闭、无法按 ESC、无法点击遮罩关闭）
- ✅ 样式已全局加载，无需额外引入

### 基本使用

```typescript
import { showCompactModal } from '@/utils/compactModal'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 基本使用（强制性弹窗）
showCompactModal({
  title: t('common.hint'),
  content: t('common.message'),
  okText: t('common.action.ok'),
  onOk: () => {
    // 处理确认操作
  },
})
```

### 高级用法

#### 可关闭的弹窗

```typescript
showCompactModal({
  title: t('common.hint'),
  content: t('common.message'),
  okText: t('common.action.ok'),
  hideCancel: false,      // 显示取消按钮
  closable: true,         // 显示关闭按钮
  maskClosable: true,     // 允许点击遮罩关闭
  escToClose: true,       // 允许 ESC 关闭
  cancelText: t('common.action.cancel'),
  onOk: () => {
    // 处理确认操作
  },
  onCancel: () => {
    // 处理取消操作
  },
})
```

#### 组织切换提示示例

```typescript
showCompactModal({
  title: t('common.hint'),
  content: t('common.org.switchConfirmMessage', { name: orgName }),
  okText: t('common.org.switchConfirmButton'),
  onOk: () => {
    window.location.reload()
  },
})
```

### 配置项

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `title` | `string` | 是 | - | 标题文本 |
| `content` | `string` | 是 | - | 内容文本 |
| `okText` | `string` | 是 | - | 确认按钮文本 |
| `onOk` | `() => void` | 否 | - | 点击确认按钮的回调 |
| `hideCancel` | `boolean` | 否 | `true` | 是否隐藏取消按钮 |
| `closable` | `boolean` | 否 | `false` | 是否显示右上角关闭按钮 |
| `maskClosable` | `boolean` | 否 | `false` | 点击遮罩是否关闭 |
| `escToClose` | `boolean` | 否 | `false` | 按 ESC 是否关闭 |
| `cancelText` | `string` | 否 | - | 取消按钮文本 |
| `onCancel` | `() => void` | 否 | - | 点击取消按钮的回调 |

### 样式定制

样式由 `CompactModal.vue` 组件提供，已在 `App.vue` 中全局导入。

**样式特性**：
- 外层 padding: 12px（默认 24px）
- 标题下方 margin: 8px
- 内容区 padding: 上 8px，下 16px
- 内容对齐: 左对齐
- 按钮颜色: 蓝色主题（#3370FF）
- 按钮悬停: #4E83FD

如需修改样式，请编辑 `kanban-ui/src/components/common/CompactModal.vue` 文件。

### 适用场景

- ✅ 组织切换提示
- ✅ 强制确认操作
- ✅ 重要通知提示
- ✅ 需要用户必须响应的场景
- ✅ 数据保存确认
- ✅ 危险操作警告

### 技术实现

**文件结构**：
- `kanban-ui/src/utils/compactModal.ts` - 工具函数和类型定义
- `kanban-ui/src/components/common/CompactModal.vue` - 全局样式组件
- `kanban-ui/src/App.vue` - 导入 CompactModal 组件以加载样式

**类型定义**：

```typescript
export interface CompactModalConfig {
  title: string
  content: string
  okText: string
  onOk?: () => void
  hideCancel?: boolean
  closable?: boolean
  maskClosable?: boolean
  escToClose?: boolean
  cancelText?: string
  onCancel?: () => void
}
```

### 注意事项

1. **国际化**：所有文本内容应使用 i18n，避免硬编码中文
2. **回调函数**：`onOk` 和 `onCancel` 回调是可选的，但建议提供
3. **强制性弹窗**：默认配置为强制性弹窗，如需可关闭弹窗，需显式设置相关参数
4. **样式一致性**：请勿直接使用 `Modal.info()`，统一使用 `showCompactModal()` 以保持样式一致

---

## TextExpressionTemplateEditor（文本表达式模板编辑器）

### 概述

基于 Tiptap 的富文本模板编辑器，支持 `${variable}` 语法插入字段变量，用于动态文本生成场景。

**使用场景**：
- 卡片标题模板配置
- 通知消息模板编辑
- 动态内容生成

### 基本使用

```vue
<script setup lang="ts">
import { TextExpressionTemplateEditor } from '@/components/common/text-expression-template'
import type { FieldProvider } from '@/components/common/text-expression-template/types'

const template = ref('')

const fieldProvider: FieldProvider = {
  // 获取当前卡片可用字段
  getCardFields: async () => [
    { id: 'title', name: '标题', type: 'TEXT' },
    { id: 'priority', name: '优先级', type: 'ENUM' }
  ],
  // 获取成员相关字段
  getMemberFields: async () => [
    { id: 'name', name: '姓名', type: 'TEXT' }
  ],
  // 根据关联字段ID获取关联卡片的字段
  getFieldsByLinkFieldId: async (linkFieldId) => [
    { id: 'title', name: '关联卡片标题', type: 'TEXT' }
  ]
}
</script>

<template>
  <TextExpressionTemplateEditor
    v-model="template"
    :field-provider="fieldProvider"
    placeholder="输入文本，使用 $ 插入变量"
  />
</template>
```

### 支持的变量类型

| 变量格式 | 示例 | 说明 |
|----------|------|------|
| `${card.fieldId}` | `${card.title}` | 当前卡片字段 |
| `${card.linkFieldId.fieldId}` | `${card.requirement.priority}` | 关联卡片字段（多级） |
| `${member.fieldId}` | `${member.name}` | 当前操作人字段 |
| `${system.xxx}` | `${system.currentTime}` | 系统变量 |

**系统变量**：`currentYear`, `currentMonth`, `currentDate`, `currentTime`

### 配置项

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `modelValue` | `string` | 是 | 模板内容（双向绑定） |
| `fieldProvider` | `FieldProvider` | 是 | 字段数据提供者 |
| `placeholder` | `string` | 否 | 占位提示文本 |
| `multiline` | `boolean` | 否 | 是否多行模式（默认单行） |

### 多行模式

设置 `multiline: true` 启用多行编辑，适合长文本模板：

```vue
<TextExpressionTemplateEditor
  v-model="template"
  :field-provider="fieldProvider"
  :multiline="true"
  placeholder="输入通知模板内容"
/>
```

### 注意事项

1. **FieldProvider 必须实现**：编辑器依赖 `fieldProvider` 获取可用字段列表
2. **异步加载**：字段列表是异步加载的，编辑器会显示加载状态
3. **变量格式**：使用 `$` 触发建议面板，选择变量后自动格式化为 `${xxx}`
4. **兼容性**：模板字符串与后端 `TextExpressionTemplateResolver` 兼容

---

## useLoading（加载状态管理）

### 概述

管理异步操作的加载状态，支持自动包裹异步函数。

**使用场景**：
- 表单提交按钮加载状态
- 页面数据加载指示
- 批量操作进度控制

### 基本使用

```typescript
import { useLoading } from '@/hooks/useLoading'

const { loading, startLoading, stopLoading, withLoading } = useLoading()

// 手动控制
const handleClick = async () => {
  startLoading()
  try {
    await saveData()
  } finally {
    stopLoading()
  }
}

// 自动管理（推荐）
const handleSubmit = () => withLoading(async () => {
  await saveData()
  await refreshList()
})
```

### 在模板中使用

```vue
<template>
  <a-button :loading="loading" @click="handleSubmit">
    保存
  </a-button>
</template>
```

### 初始状态

```typescript
// 设置初始加载状态为 true
const { loading } = useLoading(true)
```

---

## storage（本地存储工具）

### 概述

封装 `localStorage` 操作，自动添加 `kanban_` 前缀避免命名冲突，支持类型安全的存取。

**使用场景**：
- 用户偏好设置持久化
- 表单草稿临时存储
- 缓存数据本地保存

### 基本使用

```typescript
import { setItem, getItem, removeItem, clear } from '@/utils/storage'

// 存储数据（自动序列化）
setItem('userPreference', { theme: 'dark', sidebar: true })

// 读取数据（带类型推断）
const pref = getItem<{ theme: string, sidebar: boolean }>('userPreference')

// 带默认值
const pref = getItem('userPreference', { theme: 'light' })

// 删除指定项
removeItem('userPreference')

// 清空所有 kanban_ 前缀的存储项
clear()
```

### 存储键名

实际存储在 localStorage 中的键名会自动添加 `kanban_` 前缀：

```typescript
setItem('userPreference', data)  // 实际存储键: kanban_userPreference
```

### 错误处理

存储操作失败时会在控制台输出错误日志，不会抛出异常：

```typescript
// 存储失败时自动捕获错误并打印日志
try {
  localStorage.setItem('kanban_key', JSON.stringify(value))
} catch {
  console.error(`Failed to set storage item: ${key}`)
}
```

### 适用数据类型

| 类型 | 示例 | 说明 |
|------|------|------|
| 对象 | `{ theme: 'dark' }` | 自动 JSON 序列化 |
| 数组 | `['item1', 'item2']` | 自动 JSON 序列化 |
| 基础类型 | `'string', 123, true` | 自动 JSON 序列化 |

### 注意事项

1. **避免存储敏感信息**：localStorage 数据可被用户查看和修改
2. **容量限制**：通常约 5MB，大数据量考虑使用 IndexedDB
3. **序列化开销**：大对象存储前考虑压缩或分段

---

## AssertUtils - 参数校验断言

### 概述

`AssertUtils` 提供参数校验的断言方法，校验失败时抛出 `KanbanException`。用于替代传统的 if-throw 模式，使代码更简洁。

**使用场景**：
- 方法参数校验
- 前置条件检查
- 数据存在性校验

### 基本使用

```java
import cn.agilean.kanban.common.util.AssertUtils;

// 断言对象不为 null
AssertUtils.notNull(userId, "用户ID不能为空");

// 断言对象不为 null 并返回
User user = AssertUtils.requireNotNull(user, "用户不能为空");

// 断言字符串不为空
AssertUtils.notBlank(name, "名称不能为空");

// 断言字符串不为空并返回
String code = AssertUtils.requireNotBlank(code, "编码不能为空");

// 断言集合不为空
AssertUtils.notEmpty(items, "列表不能为空");

// 断言集合不为空并返回
List<Item> list = AssertUtils.requireNotEmpty(itemList, "列表不能为空");
```

### 条件断言

```java
// 断言条件为真
AssertUtils.isTrue(age >= 18, "年龄必须大于等于18岁");

// 断言条件为假
AssertUtils.isFalse(isDeleted, "数据已删除，无法操作");

// 断言两个对象相等
AssertUtils.equals(expected, actual, "数据不匹配");
```

### 数据存在性校验

```java
// 断言数据存在（常用于查询后校验）
User user = AssertUtils.requireFound(userRepository.findById(id), "用户");

// 等效于
User user = userRepository.findById(id)
    .orElseThrow(() -> new KanbanException(CommonErrorCode.DATA_NOT_FOUND, "用户"));
```

### 使用自定义错误码

```java
// 使用自定义错误码
AssertUtils.notNull(orgId, CommonErrorCode.DATA_NOT_FOUND, "组织ID");
AssertUtils.isTrue(hasPermission, CommonErrorCode.PERMISSION_DENIED, "无权限执行此操作");
```

### 注意事项

1. **异常类型**：所有断言失败都会抛出 `KanbanException`
2. **错误消息**：建议使用 i18n key 或清晰的错误描述
3. **性能考虑**：断言方法在参数合法时开销极小

---

## ConditionYieldBuilder - 条件 Yield 构建器

### 概述

`ConditionYieldBuilder` 从 Condition 定义中提取字段需求，构建卡片数据查询所需的 Yield 对象。用于支持复杂的权限条件和业务规则条件。

**使用场景**：
- 从权限条件中提取需要查询的卡片字段
- 构建动态的数据查询范围
- 支持多级关联卡片的数据获取

### 支持的数据源类型

| 类型 | 说明 |
|------|------|
| `CurrentCard` | 当前卡片（Subject 中 path 为空的字段） |
| `ParameterCard` | 参数卡片 |
| `Member` | 当前操作人 |
| `ContextualCard` | 上下文卡片 |

### 基本使用

```java
import cn.agilean.kanban.api.card.util.ConditionYieldBuilder;
import cn.agilean.kanban.api.card.request.Yield;

// 从单个条件中提取当前卡片需要的字段
Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

// 从多个条件中提取（自动合并）
Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition1, condition2);

// 从条件列表中提取
List<Condition> conditions = Arrays.asList(condition1, condition2, condition3);
Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(conditions);
```

### 提取不同类型的 Yield

```java
// 提取当前卡片需要的字段
Yield currentCardYield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

// 提取参数卡片需要的字段
Yield parameterCardYield = ConditionYieldBuilder.buildYieldForParameterCard(condition);

// 提取成员需要的字段
Yield memberYield = ConditionYieldBuilder.buildYieldForMemberCard(condition);

// 提取上下文卡片需要的字段
Yield contextualYield = ConditionYieldBuilder.buildYieldForContextualCard(condition);
```

### 使用示例

```java
@Service
public class PermissionCheckService {

    public boolean checkPermission(CardId cardId, Condition permissionCondition) {
        // 从权限条件中提取需要查询的字段
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(permissionCondition);

        // 查询卡片数据（只获取需要的字段）
        CardDTO card = cardRepository.findById(cardId, yield);

        // 评估条件
        return conditionEvaluator.evaluate(permissionCondition, card);
    }
}
```

### 注意事项

1. **Yield 合并**：多个条件的 Yield 会自动合并，避免重复查询
2. **关联字段**：支持多级关联路径（如 `${card.parent.status}`）
3. **性能优化**：只查询条件中实际需要的字段，减少数据传输

---

## TextExpressionTemplateResolver - 模板表达式解析器

### 概述

`TextExpressionTemplateResolver` 解析文本表达式模板中的占位符，将其替换为实际值。与前端 `TextExpressionTemplateEditor` 组件配套使用。

**使用场景**：
- 动态生成通知消息内容
- 生成卡片标题
- 动态错误提示

### 支持的表达式格式

| 表达式 | 说明 | 示例输出 |
|--------|------|----------|
| `${card}` | 当前卡片标题 | "需求-001" |
| `${card.title}` | 当前卡片标题 | "需求-001" |
| `${card.code}` | 当前卡片编号 | "REQ-001" |
| `${card.fieldId}` | 当前卡片字段值 | 取决于字段类型 |
| `${card.linkFieldId.fieldId}` | 关联卡片字段值 | "关联卡片标题" |
| `${member}` | 操作人卡片标题 | "张三" |
| `${member.name}` | 操作人字段值 | "张三" |
| `${system.currentTime}` | 当前时间 | "2024-01-15 10:30:00" |
| `${system.currentDate}` | 当前日期 | "2024-01-15" |
| `${system.currentYear}` | 当前年份 | "2024" |
| `${system.currentMonth}` | 当前月份 | "1" |

### 基本使用

```java
import cn.agilean.kanban.infra.expression.TextExpressionTemplateResolver;
import cn.agilean.kanban.domain.expression.TextExpressionTemplate;

@Service
public class NotificationService {
    private final TextExpressionTemplateResolver resolver;

    public String generateMessage(TextExpressionTemplate template, CardId cardId, CardId operatorId) {
        return resolver.resolve(template, cardId, operatorId);
    }
}
```

### 使用模板字符串

```java
// 直接解析模板字符串
String template = "卡片 ${card.title} 已分配给 ${member.name}";
String result = resolver.resolve(template, cardId, operatorId);
// 输出: "卡片 需求-001 已分配给 张三"
```

### 使用 TextExpressionTemplate 类型

```java
// TextExpressionTemplate 是值对象，自动序列化为字符串
TextExpressionTemplate template = TextExpressionTemplate.of("卡片 ${card.title} 状态已更新");
String result = resolver.resolve(template, cardId, operatorId);
```

### 关联卡片字段解析

```java
// 获取关联卡片的字段值
String template = "所属需求: ${card.requirement.title}, 优先级: ${card.requirement.priority}";
String result = resolver.resolve(template, cardId, operatorId);
```

### 懒加载机制

解析器会自动分析模板中需要的字段，并构建相应的 Yield 进行懒加载：

```java
// 只有包含 ${card.xxx} 时才会查询当前卡片
// 只有包含 ${member.xxx} 时才会查询操作人卡片
// 系统变量不需要查询卡片数据
```

### 注意事项

1. **空值处理**：字段值为空时返回空字符串，不会报错
2. **关联卡片**：关联字段支持多级路径，但只取第一个关联卡片
3. **性能**：解析器会自动优化 Yield，只查询必要的字段

---

## CardCacheService - 卡片缓存

### 概述

`CardCacheService` 提供卡片基础信息的二级缓存（L1: Caffeine 本地缓存，L2: Redis 分布式缓存）。用于高频查询场景下的性能优化。

**使用场景**：
- 查询卡片标题、状态等基础信息
- 批量获取卡片信息
- 关联卡片名称显示

### 基本使用

```java
import cn.agilean.kanban.infra.cache.card.CardCacheService;
import cn.agilean.kanban.infra.cache.card.model.CardBasicInfo;
import cn.agilean.kanban.domain.card.CardId;

@Service
public class CardQueryService {
    private final CardCacheService cardCacheService;

    public Optional<CardBasicInfo> getCardInfo(CardId cardId) {
        return cardCacheService.getBasicInfoById(cardId);
    }

    public Map<CardId, CardBasicInfo> batchGet(Set<CardId> cardIds) {
        return cardCacheService.getBasicInfoByIds(cardIds);
    }
}
```

### CardBasicInfo 结构

```java
public record CardBasicInfo(
    CardId cardId,              // 卡片ID
    OrgId orgId,                // 组织ID
    CardTypeId cardTypeId,      // 卡片类型ID
    CardTitle title,            // 卡片标题（CardTitle 类型，非 String）
    String code,                // 卡片编号
    CardCycle cardCycle,        // 生命周期（ACTIVE/ARCHIVED/DISCARDED）
    StreamId streamId,          // 价值流ID
    StatusId statusId           // 价值流状态ID
) {}
```

### 批量查询

```java
// 批量获取卡片信息（自动使用缓存）
Set<CardId> cardIds = Set.of(CardId.of("1"), CardId.of("2"), CardId.of("3"));
Map<CardId, CardBasicInfo> cardMap = cardCacheService.getBasicInfoByIds(cardIds);

// 获取卡片标题
Map<CardId, CardTitle> cardNames = cardCacheService.queryCardNames(cardIds);
```

### 缓存失效

```java
// 失效单个卡片的 L1 缓存（本地）
cardCacheService.evictL1(cardId);

// 失效单个卡片的 L2 缓存（Redis）
cardCacheService.evictL2(cardId);

// 失效单个卡片的所有缓存
cardCacheService.evict(cardId);

// 批量失效
cardCacheService.evictAll(cardIds);
```

### 注意事项

1. **缓存一致性**：通过 Kafka 事件监听自动处理缓存失效
2. **数据范围**：只缓存基础信息，不包含字段值
3. **缓存粒度**：按卡片 ID 缓存，单条失效不影响其他卡片

---

## SchemaCacheService - Schema 缓存

### 概述

`SchemaCacheService` 提供 Schema 定义的二级缓存（L1: Caffeine，L2: Redis），支持二级索引查询。用于高频访问的 Schema 配置数据。

**使用场景**：
- 查询卡片类型、视图、业务规则等 Schema 定义
- 通过卡片类型ID查询关联配置
- 组织级别的 Schema 列表查询

**必须遵守**：schema-service之外的服务都应该使用schema缓存，而不是跨服务调用查询schema。

### 基本使用

```java
import cn.agilean.kanban.infra.cache.schema.SchemaCacheService;

@Service
public class SchemaQueryService {
    private final SchemaCacheService schemaCacheService;

    public Optional<SchemaDefinition<?>> getSchema(String schemaId) {
        return schemaCacheService.getById(schemaId);
    }

    public Map<String, SchemaDefinition<?>> batchGet(Set<String> schemaIds) {
        return schemaCacheService.getByIds(schemaIds);
    }
}
```

### 二级索引查询

```java
// 通过卡片类型ID查询关联的字段配置
List<SchemaDefinition<?>> fieldConfigs = schemaCacheService.getBySecondaryIndex(
    new CardTypeId("cardTypeId"), SchemaType.FIELD_CONFIG);

// 通过卡片类型ID查询关联的卡片面版
List<SchemaDefinition<?>> cardFaces = schemaCacheService.getBySecondaryIndex(
    new CardTypeId("cardTypeId"), SchemaType.CARD_FACE);
```

### 具体缓存查询类

| 查询类 | 主要方法 | 适用场景 |
|--------|----------|----------|
| `CardTypeCacheQuery` | `getById()`, `getByOrgId()` | 卡片类型查询 |
| `ViewCacheQuery` | `getById()`, `getByOrgId()` | 视图定义查询 |
| `CardFaceCacheQuery` | `getById()`, `getByCardTypeId()` | 卡片面版模板查询 |
| `BizRuleCacheQuery` | `getById()`, `getByCardTypeId()` | 业务规则查询 |
| `LinkTypeCacheQuery` | `getById()`, `getByCardTypeId()` | 关联类型查询 |
| `ValueStreamCacheQuery` | `getById()`, `getByOrgId()` | 价值流查询 |
| `CardActionCacheQuery` | `getById()`, `getByCardTypeId()` | 卡片动作查询 |
| `CardPermissionCacheQuery` | `getById()`, `getByCardTypeId()` | 卡片权限查询 |

### 使用示例

```java
@Service
public class CardTypeService {
    private final CardTypeCacheQuery cardTypeCacheQuery;
    private final CardFaceCacheQuery cardFaceCacheQuery;
    private final BizRuleCacheQuery bizRuleCacheQuery;

    public CardTypeDefinition getCardType(CardTypeId id) {
        return cardTypeCacheQuery.getById(id)
            .orElseThrow(() -> new KanbanException(CommonErrorCode.DATA_NOT_FOUND, "卡片类型"));
    }

    public List<CardTypeDefinition> getOrgCardTypes(String orgId) {
        return cardTypeCacheQuery.getByOrgId(orgId);
    }

    public List<CardFaceDefinition> getCardFaces(CardTypeId cardTypeId) {
        return cardFaceCacheQuery.getByCardTypeId(cardTypeId);
    }

    public List<BizRuleDefinition> getBizRules(CardTypeId cardTypeId) {
        return bizRuleCacheQuery.getByCardTypeId(cardTypeId);
    }
}
```

### 缓存失效

```java
// 失效 L1 缓存（本地）
schemaCacheService.evictL1(schemaId);

// 失效 L2 缓存（Redis）
schemaCacheService.evictL2(schemaId);

// 失效所有缓存
schemaCacheService.evict(schemaId);

// 批量失效
schemaCacheService.evictAll(schemaIds);

// 清空所有缓存（慎用）
schemaCacheService.clearAll();
```

### 注意事项

1. **缓存一致性**：通过 Kafka 事件监听实现跨服务的缓存失效
2. **二级索引**：`getBySecondaryIndex` 会返回符合条件的所有 Schema
3. **类型转换**：具体查询类会自动进行类型过滤和转换

---

## FieldConfigQueryService - 字段配置查询

### 概述

`FieldConfigQueryService` 提供卡片类型的完整字段配置查询，自动解析继承关系（自身 > 显式父类 > 任意卡属性集）。

**使用场景**：
- 获取卡片类型的完整字段列表
- 了解字段的继承来源
- 字段配置冲突检测

### 继承优先级

从高到低：
1. **自身配置**：当前卡片类型直接定义的字段
2. **显式父类**：通过 `extends` 指定的父类
3. **任意卡属性集**：系统默认的任意卡字段

### 基本使用

```java
import cn.agilean.kanban.api.schema.service.FieldConfigQueryService;
import cn.agilean.kanban.api.schema.dto.inheritance.FieldConfigListWithSource;

@Service
public class CardTypeService {
    private final FieldConfigQueryService fieldConfigQueryService;

    // 获取带继承来源信息的字段配置
    public FieldConfigListWithSource getFieldConfigs(String cardTypeId) {
        return fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId).getData();
    }

    // 仅获取字段配置列表
    public List<FieldConfig> getFields(String cardTypeId) {
        return fieldConfigQueryService.getFieldConfigs(cardTypeId);
    }
}
```

### FieldConfigListWithSource 结构

```java
public class FieldConfigListWithSource {
    private String cardTypeId;                    // 卡片类型ID
    private String cardTypeName;                  // 卡片类型名称
    private List<FieldConfig> fields;             // 字段配置列表
    private Map<String, FieldSourceInfo> fieldSources; // 字段来源信息
}

public class FieldSourceInfo {
    private String definitionSourceCardTypeId;    // 定义来源卡片类型ID
    private String definitionSourceCardTypeName;  // 定义来源卡片类型名称
    private String configSourceCardTypeId;        // 配置来源卡片类型ID
    private String configSourceCardTypeName;      // 配置来源卡片类型名称
    private boolean definitionInherited;          // 是否继承的定义
    private boolean configInherited;              // 是否继承的配置
    private boolean fromLinkTypeDefinition;       // 是否来自关联类型定义
}
```

### 使用示例

```java
// 获取字段配置及来源信息
FieldConfigListWithSource result = fieldConfigQueryService
    .getFieldConfigListWithSource("cardTypeId")
    .getData();

// 遍历字段
for (FieldConfig field : result.getFields()) {
    FieldSourceInfo source = result.getFieldSources().get(field.getId());

    System.out.println("字段: " + field.getName());
    System.out.println("继承自: " + source.getDefinitionSourceCardTypeName());
    System.out.println("是否继承: " + source.isDefinitionInherited());
}
```

### 注意事项

1. **关联字段**：系统内置的创建人、归档人等关联字段会被视为普通字段
2. **来源信息**：`fromLinkTypeDefinition` 标识字段是否来自关联类型定义
3. **缓存**：查询结果会被缓存，数据变更时会自动失效

---

## 其他工具速查

以下工具暂未提供详细说明，请直接查看源码了解使用方法：

### 前端

| 工具 | 路径 | 用途 |
|------|------|------|
| usePagination | `src/hooks/usePagination.ts` | 分页状态管理 |
| usePermission | `src/hooks/usePermission.ts` | 权限检查 |
| CreateButton/SaveButton/CancelButton/DeleteButton | `src/components/common/` | 统一风格按钮 |

### 后端

| 工具 | 路径 | 用途 |
|------|------|------|
| StringUtils | `kanban-common/util/StringUtils.java` | UUID 生成、字符串转换 |
| SnowflakeIdGenerator | `kanban-common/util/SnowflakeIdGenerator.java` | 分布式 ID 生成 |
| KanbanException | `kanban-common/exception/KanbanException.java` | 业务异常基类 |
| Result | `kanban-common/result/Result.java` | 统一 API 响应 |
| PageResult | `kanban-common/result/PageResult.java` | 分页结果封装 |
