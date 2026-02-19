# 业务规则功能设计文档

---

## 1. 功能概述

「业务规则」是一种自动化机制，当卡片发生特定事件（如创建、状态变更、字段修改等）时，系统自动根据预定义的条件和动作执行相应的操作。

### 1.1 功能定位

- **配置层级**：卡片类型级别（每个规则隶属于某个卡片类型）
- **触发方式**：事件驱动，系统自动触发（区别于卡片动作的用户手动触发）
- **配置入口**：卡片类型配置页的「业务规则」标签页

### 1.2 与卡片动作的关系

| 特性 | 业务规则 | 卡片动作 |
|------|----------|----------|
| 触发方式 | 事件自动触发 | 用户手动点击 |
| 执行时机 | 后台异步执行 | 前端请求执行 |
| 动作执行器 | 共享同一套执行器框架 | 共享同一套执行器框架 |
| 字段赋值 | 共享 FieldAssignment 体系 | 共享 FieldAssignment 体系 |

---

## 2. 领域模型设计

### 2.1 核心类图

```
BizRuleDefinition
├── id: BizRuleId                      # 规则唯一标识
├── orgId: String                      # 所属组织ID
├── name: String                       # 规则名称
├── description: String                # 规则描述
├── cardTypeId: CardTypeId             # 所属卡片类型
├── triggerEvent: TriggerEvent         # 触发事件类型
├── listenFieldList: List<String>      # 监听字段列表（ON_FIELD_CHANGE时使用）
├── targetStatusId: StatusId           # 目标状态ID（ON_STATUS_MOVE/ROLLBACK时使用）
├── condition: Condition               # 触发条件（可选）
├── actions: List<RuleAction>          # 执行动作列表
├── scheduleConfig: ScheduleConfig     # 定时触发配置（ON_SCHEDULE时使用）
├── enabled: boolean                   # 是否启用（默认true）
├── priority: int                      # 优先级（数字越小越高，默认100）
└── retryConfig: RetryConfig           # 重试配置
```

### 2.2 触发事件枚举

```java
public enum TriggerEvent {
    ON_CREATE,          // 创建时
    ON_DISCARD,         // 丢弃时
    ON_ARCHIVE,         // 归档时
    ON_RESTORE,         // 还原时
    ON_STATUS_MOVE,     // 状态向前移动时
    ON_STATUS_ROLLBACK, // 状态向后回滚时
    ON_FIELD_CHANGE,    // 字段变更时
    ON_SCHEDULE,        // 定时触发
}
```

| 事件 | 说明 | 附加参数 |
|------|------|----------|
| ON_CREATE | 卡片创建时触发 | - |
| ON_DISCARD | 卡片丢弃时触发 | - |
| ON_ARCHIVE | 卡片归档时触发 | - |
| ON_RESTORE | 卡片还原时触发 | - |
| ON_STATUS_MOVE | 卡片状态向前流转时触发 | targetStatusId（可选，指定目标状态） |
| ON_STATUS_ROLLBACK | 卡片状态回滚时触发 | targetStatusId（可选，指定目标状态） |
| ON_FIELD_CHANGE | 字段值变更时触发 | listenFieldList（指定监听的字段列表） |
| ON_SCHEDULE | 定时触发 | scheduleConfig（定时配置） |

### 2.3 规则动作定义（sealed interface）

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "actionType")
@JsonSubTypes({
    @Type(value = DiscardCardAction.class, name = "DISCARD_CARD"),
    @Type(value = ArchiveCardAction.class, name = "ARCHIVE_CARD"),
    @Type(value = RestoreCardAction.class, name = "RESTORE_CARD"),
    @Type(value = MoveCardAction.class, name = "MOVE_CARD"),
    @Type(value = UpdateCardAction.class, name = "UPDATE_CARD"),
    @Type(value = CreateCardAction.class, name = "CREATE_CARD"),
    @Type(value = CreateLinkedCardAction.class, name = "CREATE_LINKED_CARD"),
    @Type(value = CommentCardAction.class, name = "COMMENT_CARD"),
    @Type(value = SendNotificationAction.class, name = "SEND_NOTIFICATION"),
    @Type(value = TrackUserBehaviorAction.class, name = "TRACK_USER_BEHAVIOR"),
    @Type(value = CallExternalApiAction.class, name = "CALL_EXTERNAL_API")
})
public sealed interface RuleAction
    permits DiscardCardAction, ArchiveCardAction, RestoreCardAction,
            MoveCardAction, UpdateCardAction, CreateCardAction,
            CreateLinkedCardAction, CommentCardAction, SendNotificationAction,
            TrackUserBehaviorAction, CallExternalApiAction {
    String getActionType();
    default int getSortOrder() { return 0; }
}
```

### 2.4 动作类型详解

| 动作类型 | 说明 | 关键属性 |
|----------|------|----------|
| DISCARD_CARD | 丢弃卡片 | target, reasonTemplate |
| ARCHIVE_CARD | 归档卡片 | target |
| RESTORE_CARD | 还原卡片 | target |
| MOVE_CARD | 移动卡片状态 | target, toStatusId |
| UPDATE_CARD | 更新卡片字段 | target, fieldAssignments |
| CREATE_CARD | 创建新卡片 | cardTypeId, titleTemplate, fieldAssignments |
| CREATE_LINKED_CARD | 创建关联卡片 | linkFieldId, cardTypeId, titleTemplate |
| COMMENT_CARD | 添加卡片评论 | target, contentTemplate |
| SEND_NOTIFICATION | 发送通知 | channels, titleTemplate, recipientSelector |
| TRACK_USER_BEHAVIOR | 记录用户行为 | behaviorType, properties |
| CALL_EXTERNAL_API | 调用外部API | urlTemplate, method, headers, bodyTemplate |

### 2.5 动作目标选择器

```java
public class ActionTargetSelector {
    private ActionTargetType targetType;      // 目标类型
    private Path linkPath;                    // 关联路径（LINKED_CARD时使用）
    private Condition filterCondition;        // 过滤条件
}

public enum ActionTargetType {
    CURRENT_CARD,   // 当前触发规则的卡片
    LINKED_CARD     // 通过关联路径找到的卡片
}
```

### 2.6 表达式模板

```java
public class ExpressionTemplate {
    private String template;  // 模板字符串，支持变量替换
}
```

支持的变量：
- `${card.id}` - 卡片ID
- `${card.title}` - 卡片标题
- `${card.fields.fieldId}` - 卡片字段值
- `${operator.id}` - 操作人ID
- `${operator.name}` - 操作人名称
- `${now}` - 当前时间

### 2.7 定时配置

```java
public class ScheduleConfig {
    private ScheduleType scheduleType;  // 调度类型
    private String cronExpression;      // CRON表达式（CRON类型）
    private String executeTime;         // 执行时间（HH:mm格式）
    private List<Integer> daysOfWeek;   // 周几（WEEKLY类型）
    private List<Integer> daysOfMonth;  // 几号（MONTHLY类型）
    private String timezone;            // 时区
}

public enum ScheduleType {
    CRON,     // CRON表达式
    DAILY,    // 每日
    WEEKLY,   // 每周
    MONTHLY   // 每月
}
```

### 2.8 重试配置

```java
public class RetryConfig {
    private int maxRetries = 3;             // 最大重试次数
    private long retryIntervalMs = 1000;    // 重试间隔（毫秒）
    private boolean exponentialBackoff;      // 是否指数退避
    private long maxRetryIntervalMs;         // 最大重试间隔
}
```

---

## 3. 系统架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         card-service                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────┐                                        │
│  │  BizRuleEventListener │  ◄─── Kafka: kanban-card-events     │
│  └──────────┬──────────┘                                        │
│             │ 事件分发                                            │
│             ▼                                                    │
│  ┌─────────────────────┐                                        │
│  │  BizRuleTriggerService │  规则匹配与触发                       │
│  └──────────┬──────────┘                                        │
│             │ 匹配规则                                            │
│             ▼                                                    │
│  ┌─────────────────────┐     ┌─────────────────────┐            │
│  │  BizRuleExecutionService │──▶│  ConditionEvaluator │           │
│  └──────────┬──────────┘     └─────────────────────┘            │
│             │ 执行动作                                            │
│             ▼                                                    │
│  ┌─────────────────────────┐                                     │
│  │  RuleActionExecutorRegistry │                                  │
│  └──────────┬──────────────┘                                     │
│             │ 分发到具体执行器                                      │
│             ▼                                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  DiscardCardActionExecutor  │  ArchiveCardActionExecutor │   │
│  │  MoveCardActionExecutor     │  UpdateCardActionExecutor  │   │
│  │  CreateCardActionExecutor   │  SendNotificationExecutor  │   │
│  │  CommentCardActionExecutor  │  CallExternalApiExecutor   │   │
│  │  ...                                                      │   │
│  └──────────────────────────────────────────────────────────┘   │
│             │ 记录日志                                            │
│             ▼                                                    │
│  ┌─────────────────────┐                                        │
│  │  RuleExecutionLogService │  分表存储                          │
│  └─────────────────────┘                                        │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 核心服务职责

#### BizRuleEventListener
- 监听 Kafka 卡片事件
- 将事件类型转换为规则触发调用
- 防止规则触发的事件形成循环

#### BizRuleTriggerService
- 从缓存加载卡片类型对应的规则
- 根据事件类型、目标状态、监听字段匹配规则
- 按优先级排序后调用执行服务

#### BizRuleExecutionService
- 评估规则条件
- 按顺序执行动作列表
- 异步执行，避免阻塞主流程
- 设置操作来源上下文（用于卡片变更记录）
- 记录执行日志

#### RuleActionExecutorRegistry
- 管理所有动作执行器
- 根据动作类型分发到对应执行器

---

## 4. 包结构设计

### 4.1 领域模型包

```
dev.planka.domain.schema.definition.rule/
├── BizRuleDefinition.java           # 规则定义主类
├── RuleExecutionLog.java            # 执行日志实体
├── RetryConfig.java                 # 重试配置
├── trigger/
│   └── ScheduleConfig.java          # 定时触发配置
└── action/
    ├── RuleAction.java              # 动作 sealed interface
    ├── DiscardCardAction.java       # 丢弃卡片动作
    ├── ArchiveCardAction.java       # 归档卡片动作
    ├── RestoreCardAction.java       # 还原卡片动作
    ├── MoveCardAction.java          # 移动卡片动作
    ├── UpdateCardAction.java        # 更新卡片动作
    ├── CreateCardAction.java        # 创建卡片动作
    ├── CreateLinkedCardAction.java  # 创建关联卡片动作
    ├── CommentCardAction.java       # 评论卡片动作
    ├── SendNotificationAction.java  # 发送通知动作
    ├── TrackUserBehaviorAction.java # 用户行为追踪动作
    ├── CallExternalApiAction.java   # 调用外部API动作
    ├── ActionTargetSelector.java    # 动作目标选择器
    ├── ExpressionTemplate.java      # 表达式模板
    └── RecipientSelector.java       # 通知接收者选择器
```

### 4.2 服务层包

```
dev.planka.card.service.rule/
├── trigger/
│   ├── BizRuleEventListener.java    # 事件监听器
│   └── BizRuleTriggerService.java   # 规则触发服务
├── executor/
│   ├── BizRuleExecutionService.java # 规则执行服务
│   ├── RuleActionExecutor.java      # 动作执行器接口
│   ├── RuleActionExecutorRegistry.java # 执行器注册表
│   ├── RuleExecutionContext.java    # 执行上下文
│   ├── RuleExecutionResult.java     # 执行结果
│   ├── ExpressionTemplateResolver.java # 表达式模板解析器
│   └── ActionTargetResolver.java    # 动作目标解析器
├── executor/impl/                   # 具体执行器实现
│   ├── DiscardCardActionExecutor.java
│   ├── ArchiveCardActionExecutor.java
│   ├── MoveCardActionExecutor.java
│   ├── UpdateCardActionExecutor.java
│   ├── CreateCardActionExecutor.java
│   ├── SendNotificationActionExecutor.java
│   └── CallExternalApiActionExecutor.java
├── log/
│   └── RuleExecutionLogService.java # 执行日志服务
└── config/
    └── BizRuleAsyncConfig.java      # 异步执行配置
```

---

## 5. 执行流程

### 5.1 事件触发流程

```
1. 卡片发生变更 → 发布 CardEvent 到 Kafka
                     ↓
2. BizRuleEventListener 接收事件
                     ↓
3. 检查是否为规则触发（防循环）
                     ↓ 否
4. 调用 BizRuleTriggerService.trigger()
                     ↓
5. 从缓存加载该卡片类型的所有规则
                     ↓
6. 过滤：启用 + 事件类型匹配 + 状态/字段匹配
                     ↓
7. 按优先级排序
                     ↓
8. 逐个调用 BizRuleExecutionService.execute()
```

### 5.2 规则执行流程

```
1. 检查规则是否启用 → 禁用则跳过
                     ↓
2. 评估触发条件 (ConditionEvaluator)
                     ↓ 满足
3. 设置 OperationSourceContext（标记变更来源）
                     ↓
4. 按 sortOrder 顺序执行动作列表
   ├── 获取动作执行器
   ├── 执行动作
   └── 收集执行结果
                     ↓
5. 记录执行日志 (RuleExecutionLog)
```

### 5.3 防循环机制

为防止规则执行导致的事件再次触发规则形成无限循环：

1. 规则执行时设置 `OperationSourceContext`，标记操作来源为业务规则
2. 事件中携带 `traceId`，以 `rule-` 前缀标识规则触发的事件
3. `BizRuleEventListener` 检查事件是否由规则触发，若是则跳过

---

## 6. API 设计

### 6.1 规则管理 API

规则的增删改操作通过 `SchemaCommonController` 统一处理：

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/v1/schemas` | 创建规则（type=BIZ_RULE） |
| PUT | `/api/v1/schemas/{id}` | 更新规则 |
| DELETE | `/api/v1/schemas/{id}` | 删除规则 |

### 6.2 规则查询 API

```
GET /api/v1/schemas/biz-rules/by-card-type/{cardTypeId}
```
根据卡片类型ID获取所有业务规则

```
GET /api/v1/schemas/biz-rules
Header: X-Org-Id
```
获取组织下所有业务规则

---

## 7. 执行日志

### 7.1 日志实体

```java
public class RuleExecutionLog {
    private String id;                            // 日志ID
    private BizRuleId ruleId;                     // 规则ID
    private String ruleName;                      // 规则名称
    private CardTypeId cardTypeId;                // 卡片类型ID（用于分表）
    private CardId cardId;                        // 触发卡片ID
    private TriggerEvent triggerEvent;            // 触发事件
    private String operatorId;                    // 操作人ID
    private LocalDateTime executionTime;          // 执行时间
    private long durationMs;                      // 执行耗时
    private ExecutionStatus status;               // 执行状态
    private List<String> affectedCardIds;         // 受影响的卡片ID
    private List<ActionResult> actionResults;     // 动作执行结果
    private String errorMessage;                  // 错误信息
    private String traceId;                       // 追踪ID
}
```

### 7.2 执行状态

```java
public enum ExecutionStatus {
    SUCCESS,   // 执行成功
    FAILED,    // 执行失败
    SKIPPED    // 跳过（条件不满足）
}
```

### 7.3 分表策略

执行日志按 `cardTypeId` 进行分表存储，便于按卡片类型查询和管理大量日志数据。

---

## 8. 操作来源追踪

### 8.1 设计目的

当业务规则执行动作修改卡片时，需要在卡片变更历史中记录该变更是由哪个规则触发的，便于问题排查和审计。

### 8.2 实现机制

```java
// 操作来源接口
public sealed interface OperationSource
    permits UserOperationSource, BizRuleOperationSource, CardActionOperationSource {
    String getType();
}

// 业务规则操作来源
public final class BizRuleOperationSource implements OperationSource {
    private String ruleId;    // 规则ID
    private String ruleName;  // 规则名称
}
```

### 8.3 使用方式

```java
BizRuleOperationSource source = new BizRuleOperationSource(rule.getId(), rule.getName());
try (AutoCloseable ignored = OperationSourceContext.with(source)) {
    // 在此范围内的所有卡片变更都会记录该来源
    executor.execute(action, context);
}
```

---

## 9. JSON 示例

### 9.1 完整规则定义示例

```json
{
  "id": "rule-001",
  "orgId": "org-001",
  "name": "需求完成时自动归档子任务",
  "description": "当需求移动到已完成状态时，自动归档所有关联的子任务",
  "schemaSubType": "BIZ_RULE",
  "cardTypeId": "ct-requirement",
  "triggerEvent": "ON_STATUS_MOVE",
  "targetStatusId": "status-done",
  "condition": {
    "type": "AND",
    "children": [
      {
        "type": "FIELD",
        "fieldId": "priority",
        "operator": "EQUALS",
        "value": "HIGH"
      }
    ]
  },
  "actions": [
    {
      "actionType": "ARCHIVE_CARD",
      "sortOrder": 1,
      "target": {
        "targetType": "LINKED_CARD",
        "linkPath": {
          "linkNodes": ["lt-parent-child:SOURCE"]
        }
      }
    },
    {
      "actionType": "SEND_NOTIFICATION",
      "sortOrder": 2,
      "channels": ["IN_APP", "EMAIL"],
      "titleTemplate": {
        "template": "需求 ${card.title} 已完成"
      },
      "contentTemplate": {
        "template": "需求 ${card.title} 已移动到完成状态，所有子任务已自动归档。"
      },
      "recipientSelector": {
        "selectorType": "FROM_FIELD",
        "fieldId": "assignee"
      }
    }
  ],
  "enabled": true,
  "priority": 50,
  "retryConfig": {
    "maxRetries": 3,
    "retryIntervalMs": 1000,
    "exponentialBackoff": true
  }
}
```

### 9.2 字段变更触发规则示例

```json
{
  "name": "优先级变更时通知负责人",
  "cardTypeId": "ct-task",
  "triggerEvent": "ON_FIELD_CHANGE",
  "listenFieldList": ["priority"],
  "actions": [
    {
      "actionType": "SEND_NOTIFICATION",
      "titleTemplate": {
        "template": "任务优先级已变更"
      },
      "contentTemplate": {
        "template": "任务 ${card.title} 的优先级已变更，请及时关注。"
      },
      "recipientSelector": {
        "selectorType": "FROM_FIELD",
        "fieldId": "assignee"
      }
    }
  ],
  "enabled": true
}
```

### 9.3 定时触发规则示例

```json
{
  "name": "每日检查超期任务",
  "cardTypeId": "ct-task",
  "triggerEvent": "ON_SCHEDULE",
  "scheduleConfig": {
    "scheduleType": "DAILY",
    "executeTime": "09:00",
    "timezone": "Asia/Shanghai"
  },
  "condition": {
    "type": "FIELD",
    "fieldId": "dueDate",
    "operator": "LESS_THAN",
    "value": "${now}"
  },
  "actions": [
    {
      "actionType": "UPDATE_CARD",
      "fieldAssignments": [
        {
          "assignmentType": "FIXED_VALUE",
          "fieldId": "isOverdue",
          "value": {
            "valueType": "TEXT",
            "text": "是"
          }
        }
      ]
    },
    {
      "actionType": "SEND_NOTIFICATION",
      "titleTemplate": {
        "template": "任务已超期"
      },
      "recipientSelector": {
        "selectorType": "FROM_FIELD",
        "fieldId": "assignee"
      }
    }
  ],
  "enabled": true
}
```

---

## 10. 前端组件设计

### 10.1 组件层级

```
CardTypeFormDrawer.vue
└── BizRulesTab.vue                      # 业务规则配置 Tab
    ├── BizRuleListPanel.vue             # 规则列表面板
    └── BizRuleEditDrawer.vue            # 规则编辑抽屉
        ├── TriggerEventSelector.vue     # 触发事件选择
        ├── ConditionEditor.vue          # 条件编辑器（复用）
        ├── RuleActionList.vue           # 动作列表
        │   └── RuleActionEditor.vue     # 单个动作编辑
        │       ├── ActionTargetEditor.vue
        │       ├── FieldAssignmentEditor.vue  # 复用卡片动作组件
        │       └── ExpressionTemplateEditor.vue
        └── ScheduleConfigEditor.vue     # 定时配置编辑器
```

### 10.2 类型定义

详见 `kanban-ui/src/types/biz-rule.ts`

---

## 11. 数据存储

业务规则作为 Schema 存储在 `schema_definition` 表：

```sql
INSERT INTO schema_definition (id, org_id, schema_type, name, content, belong_to, ...)
VALUES (
    'rule-001',
    'org-1',
    'BIZ_RULE',
    '需求完成时自动归档子任务',
    '{"schemaSubType":"BIZ_RULE","cardTypeId":"ct-requirement","triggerEvent":"ON_STATUS_MOVE",...}',
    'ct-requirement',
    ...
);
```

执行日志存储在按卡片类型分表的 `rule_execution_log_{cardTypeId}` 表中。

---

## 12. 注意事项

### 12.1 性能考虑

- 规则定义从缓存（`BizRuleCacheQuery`）加载，避免频繁数据库查询
- 统一异步执行，避免阻塞主流程
- 执行日志分表存储，支持大量日志数据

### 12.2 可靠性考虑

- 支持重试配置，应对临时性故障
- 完整的执行日志记录，便于问题排查
- 操作来源追踪，支持审计需求

### 12.3 安全考虑

- 防循环机制，避免规则互相触发导致无限循环
- 规则执行在事务外进行，单个规则失败不影响其他规则
- 动作执行失败时继续执行后续动作，确保最大化执行成功率
