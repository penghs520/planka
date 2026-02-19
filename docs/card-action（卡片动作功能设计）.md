# 卡片动作功能设计文档

---

## 1. 功能概述

「卡片动作」是在卡片类型级别配置的可执行操作，用户可在卡片详情页点击触发执行。

### 1.1 功能定位

- **配置层级**：卡片类型级别（单层配置，直接挂载在卡片类型下）
- **触发位置**：卡片详情页（暂不支持卡面，看板视图尚未开发）
- **配置入口**：卡片类型配置页的「卡片动作」标签页

### 1.2 动作分类

| 类别 | 说明 | 示例 |
|------|------|------|
| 生命周期动作 | 固定显示，控制卡片生命周期 | 丢弃、归档、还原 |
| 切换类动作 | 根据卡片状态自动决定显示文案和行为 | 阻塞/解阻、点亮/暂停 |
| 自定义动作 | 用户配置的业务操作 | 更新字段、调用接口等 |

---

## 2. 领域模型设计

### 2.1 核心类图

```
CardActionConfigDefinition
├── id: CardActionId
├── cardTypeId: CardTypeId          # 所属卡片类型
├── name: String                    # 动作名称
├── actionCategory: ActionCategory  # 动作类别
├── builtIn: boolean               # 是否内置动作
├── builtInActionType: BuiltInActionType  # 内置动作类型
├── icon: String                   # 图标
├── color: String                  # 颜色
├── executionType: ActionExecutionType  # 执行类型（自定义动作）
├── visibilityConditions: List<Condition>  # 可见性条件
├── executionConditions: List<Condition>   # 执行条件
├── sortOrder: Integer             # 排序号
├── confirmMessage: String         # 确认提示
└── successMessage: String         # 成功提示
```

### 2.2 动作类别枚举

```java
public enum ActionCategory {
    LIFECYCLE,      // 生命周期操作
    STATE_TOGGLE,   // 状态切换操作
    CUSTOM          // 自定义操作
}
```

### 2.3 内置动作类型枚举

```java
public enum BuiltInActionType {
    DISCARD,          // 丢弃
    ARCHIVE,          // 归档
    RESTORE,          // 还原
    BLOCK_TOGGLE,     // 阻塞/解阻（切换）
    HIGHLIGHT_TOGGLE  // 点亮/暂停（切换）
}
```

### 2.4 执行类型多态设计（sealed interface）

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @Type(value = UpdateCardExecution.class, name = "UPDATE_CARD"),
    @Type(value = CallExternalApiExecution.class, name = "CALL_EXTERNAL_API"),
    @Type(value = NavigateToPageExecution.class, name = "NAVIGATE_TO_PAGE"),
    @Type(value = TriggerBuiltInExecution.class, name = "TRIGGER_BUILT_IN")
})
public sealed interface ActionExecutionType
    permits UpdateCardExecution, CallExternalApiExecution,
            NavigateToPageExecution, TriggerBuiltInExecution {
    String getType();
}
```

#### 2.4.1 UpdateCardExecution - 更新卡片

```java
public final class UpdateCardExecution implements ActionExecutionType {
    private List<FieldAssignment> fieldAssignments;  // 字段赋值列表
    private String targetStatusId;                   // 目标状态ID
    private CreateLinkedCardConfig createLinkedCard; // 新建关联卡片配置
}
```

#### 2.4.2 CallExternalApiExecution - 调用外部接口

```java
public final class CallExternalApiExecution implements ActionExecutionType {
    private String url;                      // 请求URL
    private HttpMethod method;               // HTTP方法
    private Map<String, String> headers;     // 请求头
    private String bodyTemplate;             // 请求体模板（支持变量替换）
    private int timeoutMs;                   // 超时时间
    private boolean waitForResponse;         // 是否等待响应
}
```

#### 2.4.3 NavigateToPageExecution - 跳转页面

```java
public final class NavigateToPageExecution implements ActionExecutionType {
    private String targetUrl;                // 目标URL（支持变量替换）
    private Map<String, String> urlParams;   // URL参数
    private boolean openInNewWindow;         // 是否新窗口打开
}
```

#### 2.4.4 TriggerBuiltInExecution - 触发内置操作

```java
public final class TriggerBuiltInExecution implements ActionExecutionType {
    private BuiltInActionType builtInActionType;  // 内置动作类型
}
```

### 2.5 字段赋值策略（sealed interface）

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "assignmentType")
@JsonSubTypes({
    @Type(value = UserInputAssignment.class, name = "USER_INPUT"),
    @Type(value = FixedValueAssignment.class, name = "FIXED_VALUE"),
    @Type(value = ReferenceFieldAssignment.class, name = "REFERENCE_FIELD"),
    @Type(value = CurrentTimeAssignment.class, name = "CURRENT_TIME"),
    @Type(value = ClearValueAssignment.class, name = "CLEAR_VALUE"),
    @Type(value = IncrementAssignment.class, name = "INCREMENT")
})
public sealed interface FieldAssignment
    permits UserInputAssignment, FixedValueAssignment, ReferenceFieldAssignment, CurrentTimeAssignment,
            ClearValueAssignment, IncrementAssignment {
    String getFieldId();
    String getAssignmentType();
}
```

| 赋值类型 | 说明 | 适用场景 |
|----------|------|----------|
| USER_INPUT | 用户输入 | 执行时弹窗让用户输入值（默认选项） |
| FIXED_VALUE | 固定值 | 设置预定义的常量值 |
| REFERENCE_FIELD | 字段引用 | 引用当前用户、当前卡片字段、关联卡片字段（支持级联） |
| CURRENT_TIME | 当前时间 | 设置为执行时的当前时间（支持偏移） |
| CLEAR_VALUE | 清空值 | 将字段置空 |
| INCREMENT | 数值增量 | 数字字段加减运算 |

#### 2.5.0 UserInputAssignment - 用户输入

```java
public final class UserInputAssignment implements FieldAssignment {
    private String fieldId;      // 目标字段ID
    private String placeholder;  // 输入提示文字（可选）
    private Boolean required;    // 是否必填（默认 true）
}
```

执行动作时，如果存在 USER_INPUT 类型的字段赋值，前端会弹出弹窗让用户输入对应字段的值，然后将用户输入的值通过 `userInputs` 参数传递给后端执行接口。

#### 2.5.1 FixedValue - 固定值多态类型

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "valueType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextValue.class, name = "TEXT"),
    @JsonSubTypes.Type(value = NumberValue.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = DateValue.class, name = "DATE"),
    @JsonSubTypes.Type(value = EnumValue.class, name = "ENUM"),
    @JsonSubTypes.Type(value = LinkValue.class, name = "LINK")
})
public sealed interface FixedValue
    permits TextValue, NumberValue, DateValue,
            EnumValue, LinkValue {
    String getValueType();
}
```

#### 2.5.2 字段类型兼容性矩阵

| 源字段类型 | 目标字段类型 | 是否兼容 | 转换规则 |
|-----------|-------------|----------|----------|
| 文本 | 文本 | ✅ | 直接复制 |
| 数字 | 数字 | ✅ | 直接复制 |
| 数字 | 文本 | ✅ | 转换为字符串 |
| 日期 | 日期 | ✅ | 直接复制 |
| 日期 | 文本 | ✅ | 按目标格式转换 |
| 枚举 | 枚举 | ⚠️ | 枚举值ID必须匹配 |
| 枚举 | 文本 | ✅ | 取枚举值名称 |
| 关联(ID) | 文本 | ✅ | 复制卡片ID |
| 关联(title) | 文本 | ✅ | 复制卡片标题 |
| 多选 | 单选 | ⚠️ | 取第一个值 |
| 单选 | 多选 | ✅ | 包装为单元素列表 |
| 当前用户 | 人员 | ✅ | 设置用户ID |
| 当前时间 | 日期 | ✅ | 设置时间戳 |

---

## 3. 包结构设计

```
dev.planka.domain.schema.definition.action/
├── CardActionConfigDefinition.java    # 主配置类
├── CardActionId.java                  # 动作ID值对象
├── ActionCategory.java                # 动作类别枚举
├── BuiltInActionType.java             # 内置动作类型枚举
├── ActionExecutionType.java           # 执行类型 sealed interface
├── UpdateCardExecution.java           # 更新卡片执行
├── CallExternalApiExecution.java      # 调用外部接口执行
├── NavigateToPageExecution.java       # 跳转页面执行
├── TriggerBuiltInExecution.java       # 触发内置操作执行
├── CreateLinkedCardConfig.java        # 新建关联卡片配置
└── assignment/                        # 字段赋值策略
    ├── FieldAssignment.java           # sealed interface
    ├── FixedValueAssignment.java      # 固定值赋值
    ├── ReferenceFieldAssignment.java  # 字段引用赋值（含 ReferenceSource 枚举）
    ├── CurrentTimeAssignment.java     # 当前时间赋值
    ├── ClearValueAssignment.java      # 清空值赋值
    ├── IncrementAssignment.java       # 数值增量赋值
    ├── FixedValue.java                # 固定值 sealed interface
    ├── TextValue.java                 # 文本值
    ├── NumberValue.java               # 数字值
    ├── DateValue.java                 # 日期值
    ├── EnumValue.java                 # 枚举值
    └── LinkValue.java                 # 关联值（含人员）
```

> **注意**：
> - `FixedValue` 与 `field.dev.planka.domain.FieldValue` 区分，这里是用于动作赋值的固定值配置
> - `ReferenceFieldAssignment` 复用现有的 `link.dev.planka.domain.Path` 类
> - `LinkValue` 同时用于卡片关联和人员字段（人员也是一种关联）

---

## 4. API 设计

### 4.1 配置管理 API

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/schemas/card-actions/by-card-type/{cardTypeId}` | 查询卡片类型的所有动作配置 |
| GET | `/api/v1/schemas/card-actions/{id}` | 获取单个动作配置 |
| POST | `/api/v1/schemas/card-actions` | 创建动作配置 |
| PUT | `/api/v1/schemas/card-actions/{id}` | 更新动作配置 |
| DELETE | `/api/v1/schemas/card-actions/{id}` | 删除动作配置 |

### 4.2 动作执行 API

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/v1/cards/actions/{actionId}/execute?cardId={cardId}` | 执行卡片动作 |
| GET | `/api/v1/cards/{cardId}/available-actions` | 获取卡片可用的动作列表 |

### 4.3 执行结果格式

```typescript
interface ActionExecutionResult {
  type: 'SUCCESS' | 'NAVIGATE' | 'ERROR'
  message?: string
  data?: any
  navigateUrl?: string      // type=NAVIGATE 时返回
  openInNewWindow?: boolean // type=NAVIGATE 时返回
}
```

---

## 5. 前端组件设计

### 5.1 组件层级

```
CardTypeFormDrawer.vue
└── CardActionsTab.vue                    # 卡片动作配置 Tab
    ├── ActionListPanel.vue               # 动作列表面板（按类别分组）
    └── CardActionEditDrawer.vue          # 动作编辑抽屉
        ├── UpdateCardExecutionForm.vue   # 更新卡片配置表单
        ├── CallExternalApiExecutionForm.vue
        ├── NavigateToPageExecutionForm.vue
        ├── TriggerBuiltInExecutionForm.vue
        └── FieldAssignmentEditor.vue     # 字段赋值编辑器
```

### 5.2 卡片详情页动作触发

```
CardDetailPage.vue
└── CardActionButtons.vue                 # 动作按钮组
    ├── 内置动作按钮（根据卡片状态显示/隐藏）
    └── 自定义动作按钮（根据可见性条件显示）
```

---

## 6. 内置动作行为定义

### 6.1 固定显示动作

| 动作 | 显示条件 | 执行行为 |
|------|----------|----------|
| 丢弃 | 卡片状态为 ACTIVE | 设置 cardStyle=DISCARDED |
| 归档 | 卡片状态为 ACTIVE | 设置 cardStyle=ARCHIVED |
| 还原 | 卡片状态为 DISCARDED 或 ARCHIVED | 设置 cardStyle=ACTIVE |

### 6.2 切换类动作

| 动作 | 显示文案逻辑 | 执行行为 |
|------|-------------|----------|
| 阻塞/解阻 | 已阻塞显示"解阻"，否则显示"阻塞" | 切换阻塞状态 |
| 点亮/暂停 | 已点亮显示"暂停"，否则显示"点亮" | 切换点亮状态 |

---

## 7. 数据存储

卡片动作配置作为 Schema 存储在 `schema_definition` 表：

```sql
-- 示例数据
INSERT INTO schema_definition (id, org_id, schema_type, name, content, belong_to, ...)
VALUES (
    'action-1',
    'org-1',
    'CARD_ACTION',
    '丢弃',
    '{"schemaSubType":"CARD_ACTION_CONFIG","builtIn":true,"builtInActionType":"DISCARD",...}',
    'card-type-1',
    ...
);
```

---

## 8. JSON 示例

### 8.1 固定值赋值示例

**文本值**:
```json
{
  "assignmentType": "FIXED_VALUE",
  "fieldId": "description",
  "value": {
    "valueType": "TEXT",
    "text": "默认描述内容"
  }
}
```

**数字值**:
```json
{
  "assignmentType": "FIXED_VALUE",
  "fieldId": "storyPoints",
  "value": {
    "valueType": "NUMBER",
    "number": 5
  }
}
```

**日期值 - 绝对日期**:
```json
{
  "assignmentType": "FIXED_VALUE",
  "fieldId": "dueDate",
  "value": {
    "valueType": "DATE",
    "mode": "ABSOLUTE",
    "absoluteDate": "2025-12-31T23:59:59"
  }
}
```

**日期值 - 相对日期**:
```json
{
  "assignmentType": "FIXED_VALUE",
  "fieldId": "dueDate",
  "value": {
    "valueType": "DATE",
    "mode": "RELATIVE",
    "offsetDays": 7
  }
}
```

**枚举值**:
```json
{
  "assignmentType": "FIXED_VALUE",
  "fieldId": "priority",
  "value": {
    "valueType": "ENUM",
    "enumValueIds": ["HIGH"]
  }
}
```

**关联值（人员）**:
```json
{
  "assignmentType": "FIXED_VALUE",
  "fieldId": "reviewers",
  "value": {
    "valueType": "LINK",
    "ids": ["user-001", "user-002"]
  }
}
```

### 8.2 字段引用赋值示例

**引用当前用户**:
```json
{
  "assignmentType": "REFERENCE_FIELD",
  "fieldId": "assignee",
  "source": "CURRENT_USER",
  "sourceFieldId": null,
  "path": null,
  "appendMode": false
}
```

**引用父卡片字段**:
```json
{
  "assignmentType": "REFERENCE_FIELD",
  "fieldId": "inheritedPriority",
  "source": "CURRENT_CARD",
  "sourceFieldId": "priority",
  "path": {
    "linkNodes": ["lt-parent-child:TARGET"]
  },
  "appendMode": false
}
```

### 8.3 当前时间赋值示例

```json
{
  "assignmentType": "CURRENT_TIME",
  "fieldId": "completedAt",
  "offsetDays": 0
}
```

### 8.4 数值增量赋值示例

```json
{
  "assignmentType": "INCREMENT",
  "fieldId": "storyPoints",
  "incrementValue": 1,
  "allowNegative": false
}
```

### 8.5 新建关联卡片配置示例

```json
{
  "linkTypeId": "link-parent-child",
  "targetCardTypeId": "ct-subtask",
  "fieldAssignments": [
    {
      "assignmentType": "REFERENCE_FIELD",
      "fieldId": "priority",
      "source": "CURRENT_CARD",
      "sourceFieldId": "priority"
    }
  ],
  "showCreateDialog": true
}
```
