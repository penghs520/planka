# 工时服务详细设计方案

> 基于 planka 卡片架构的工时服务设计
> 
> 文档版本: v1.0
> 创建日期: 2026-02-04
> 状态: 待评审

---

## 目录

1. [设计概述](#1-设计概述)
2. [架构适配分析](#2-架构适配分析)
3. [卡片类型设计](#3-卡片类型设计)
4. [数据模型设计](#4-数据模型设计)
5. [服务架构设计](#5-服务架构设计)
6. [API设计](#6-api设计)
7. [业务规则设计](#7-业务规则设计)
8. [事件设计](#8-事件设计)
9. [实施阶段规划](#9-实施阶段规划)
10. [技术难点与解决方案](#10-技术难点与解决方案)

---

## 1. 设计概述

### 1.1 设计目标

将原工时服务需求适配到 planka 的卡片架构中，实现：
- **工时数据以卡片形式存储**，而非传统MySQL表记录
- **考勤、打卡、请假数据均为卡片**，利用卡片关联关系建立数据联系
- **利用业务规则引擎**实现窗口期控制、自动计算等逻辑
- **利用事件驱动架构**实现数据同步和联动
- **保持原需求的核心功能**不变

### 1.2 核心设计原则

1. **卡片优先**：所有业务数据都以卡片形式存储
2. **Schema驱动**：通过Schema定义实现灵活配置
3. **事件驱动**：通过事件实现模块解耦
4. **规则引擎**：通过业务规则实现复杂逻辑
5. **零代码理念**：尽可能通过配置而非硬编码实现功能

### 1.3 架构对比

| 维度 | 原需求设计 | 新架构设计 |
|-----|----------|----------|
| 工时数据 | StBizMetrics宽表 | 工时卡片(WorklogCard) |
| 考勤数据 | DailyTime表 | 考勤卡片(AttendanceCard) |
| 打卡记录 | 考勤记录表 | 打卡卡片(ClockInCard) |
| 请假数据 | 无明确定义 | 请假卡片(LeaveCard) |
| 结算数据 | 结算记录表 | 结算卡片(SettlementCard) |
| 数据关联 | 外键关联 | 卡片关联(LinkCard) |
| 业务规则 | 硬编码 | 业务规则引擎(BizRule) |
| 数据校验 | Service层代码 | 业务规则+字段校验 |
| 窗口期控制 | 配置+代码 | 业务规则引擎 |
| 数据存储 | MySQL | zgraph图数据库 |

---

## 2. 架构适配分析

### 2.1 现有架构能力

**已具备的能力：**
1. ✅ 卡片CRUD能力（CardService）
2. ✅ 卡片关联能力（LinkCardService）
3. ✅ 业务规则引擎（BizRuleEngine）
4. ✅ 事件发布订阅（Kafka）
5. ✅ Schema定义体系（SchemaDefinition）
6. ✅ 属性值类型系统（FieldValue）
7. ✅ 操作历史追踪（OperationSource）
8. ✅ 价值流状态管理（ValueStream）

**需要扩展的能力：**
1. ⚠️ 时间窗口期校验规则
2. ⚠️ 工时聚合计算能力
3. ⚠️ 批量数据导入能力
4. ⚠️ 定时任务调度能力
5. ⚠️ 复杂查询统计能力

### 2.2 数据存储适配

**zgraph图数据库的优势：**
- 天然支持卡片关联关系查询
- 支持复杂的图遍历查询
- 支持自定义属性值存储
- 支持高性能并发读写

**需要注意的问题：**
- 聚合统计查询性能（需要缓存层）
- 批量数据导入性能（需要批处理优化）
- 时间范围查询优化（需要索引设计）

### 2.3 服务模块规划

建议新增 **workload-service** 模块，职责包括：
- 工时相关卡片类型的Schema定义管理
- 工时业务规则的执行和管理
- 工时数据的聚合计算和统计
- 考勤数据的批量导入
- 窗口期控制逻辑
- 结算流程编排

---

## 3. 卡片类型设计

### 3.1 工时卡片类型 (WorklogCard)

**卡片类型定义：**
```json
{
  "id": "worklog_card_type",
  "schemaType": "CARD_TYPE",
  "schemaSubType": "ENTITY_CARD_TYPE",
  "name": "工时卡片",
  "code": "WORKLOG",
  "icon": "icon-clock",
  "color": "#1890ff",
  "description": "记录成员在任务或项目上的工作时长",
  
  "fieldConfigs": [
    {
      "fieldId": "worklog_date",
      "fieldType": "DATE",
      "name": "工时日期",
      "required": true,
      "readonly": false,
      "description": "工时发生的日期"
    },
    {
      "fieldId": "worklog_hours",
      "fieldType": "NUMBER",
      "name": "工时数（小时）",
      "required": true,
      "numberConfig": {
        "precision": 2,
        "min": 0,
        "max": 24,
        "unit": "小时"
      }
    },
    {
      "fieldId": "worklog_member",
      "fieldType": "STRUCTURE",
      "name": "工时成员",
      "required": true,
      "structureConfig": {
        "structureType": "MEMBER",
        "multiSelect": false
      }
    },
    {
      "fieldId": "worklog_description",
      "fieldType": "MULTI_LINE_TEXT",
      "name": "工作内容",
      "required": false,
      "maxLength": 2000
    },
    {
      "fieldId": "worklog_type",
      "fieldType": "ENUM",
      "name": "工时类型",
      "required": true,
      "enumConfig": {
        "options": [
          {"value": "NORMAL", "label": "正常工时", "color": "#52c41a"},
          {"value": "OVERTIME", "label": "加班工时", "color": "#faad14"},
          {"value": "LEAVE", "label": "请假扣减", "color": "#f5222d"}
        ],
        "defaultValue": "NORMAL"
      }
    },
    {
      "fieldId": "worklog_status",
      "fieldType": "ENUM",
      "name": "审核状态",
      "required": true,
      "enumConfig": {
        "options": [
          {"value": "DRAFT", "label": "草稿", "color": "#d9d9d9"},
          {"value": "SUBMITTED", "label": "已提交", "color": "#1890ff"},
          {"value": "APPROVED", "label": "已审核", "color": "#52c41a"},
          {"value": "REJECTED", "label": "已驳回", "color": "#f5222d"}
        ],
        "defaultValue": "DRAFT"
      }
    },
    {
      "fieldId": "worklog_locked",
      "fieldType": "ENUM",
      "name": "锁定状态",
      "required": true,
      "enumConfig": {
        "options": [
          {"value": "UNLOCKED", "label": "未锁定", "color": "#52c41a"},
          {"value": "LOCKED", "label": "已锁定", "color": "#f5222d"}
        ],
        "defaultValue": "UNLOCKED"
      },
      "description": "锁定后不可修改"
    },
    {
      "fieldId": "worklog_created_at",
      "fieldType": "DATE",
      "name": "创建时间",
      "readonly": true,
      "systemField": true
    },
    {
      "fieldId": "worklog_updated_at",
      "fieldType": "DATE",
      "name": "更新时间",
      "readonly": true,
      "systemField": true
    }
  ],
  
  "linkConfigs": [
    {
      "linkTypeId": "worklog_to_task",
      "direction": "SOURCE",
      "required": true,
      "description": "工时关联的任务或项目"
    },
    {
      "linkTypeId": "worklog_to_attendance",
      "direction": "SOURCE",
      "required": false,
      "description": "工时关联的考勤记录"
    }
  ],
  
  "valueStreamId": "worklog_stream",
  "defaultStatusId": "worklog_draft"
}
```

**价值流定义：**
```json
{
  "id": "worklog_stream",
  "name": "工时审核流",
  "statuses": [
    {
      "id": "worklog_draft",
      "name": "草稿",
      "workType": "WAITING",
      "color": "#d9d9d9"
    },
    {
      "id": "worklog_submitted",
      "name": "待审核",
      "workType": "WAITING",
      "color": "#1890ff"
    },
    {
      "id": "worklog_approved",
      "name": "已审核",
      "workType": "WORKING",
      "color": "#52c41a"
    }
  ],
  "steps": [
    {
      "id": "step_submit",
      "name": "提交审核",
      "fromStatusId": "worklog_draft",
      "toStatusId": "worklog_submitted"
    },
    {
      "id": "step_approve",
      "name": "审核通过",
      "fromStatusId": "worklog_submitted",
      "toStatusId": "worklog_approved"
    },
    {
      "id": "step_reject",
      "name": "驳回",
      "fromStatusId": "worklog_submitted",
      "toStatusId": "worklog_draft"
    }
  ]
}
```

### 3.2 考勤卡片类型 (AttendanceCard)

**卡片类型定义：**
```json
{
  "id": "attendance_card_type",
  "schemaType": "CARD_TYPE",
  "schemaSubType": "ENTITY_CARD_TYPE",
  "name": "考勤卡片",
  "code": "ATTENDANCE",
  "icon": "icon-calendar",
  "color": "#52c41a",
  "description": "记录成员每日的考勤数据",
  
  "fieldConfigs": [
    {
      "fieldId": "attendance_date",
      "fieldType": "DATE",
      "name": "考勤日期",
      "required": true,
      "readonly": false
    },
    {
      "fieldId": "attendance_member",
      "fieldType": "STRUCTURE",
      "name": "考勤成员",
      "required": true,
      "structureConfig": {
        "structureType": "MEMBER",
        "multiSelect": false
      }
    },
    {
      "fieldId": "attendance_work_minutes",
      "fieldType": "NUMBER",
      "name": "工作时长（分钟）",
      "required": true,
      "numberConfig": {
        "precision": 0,
        "min": 0,
        "max": 1440,
        "unit": "分钟"
      }
    },
    {
      "fieldId": "attendance_work_hours",
      "fieldType": "NUMBER",
      "name": "工作时长（小时）",
      "readonly": true,
      "derived": true,
      "derivedFormula": "attendance_work_minutes / 60",
      "numberConfig": {
        "precision": 2,
        "unit": "小时"
      }
    },
    {
      "fieldId": "attendance_type",
      "fieldType": "ENUM",
      "name": "考勤类型",
      "required": true,
      "enumConfig": {
        "options": [
          {"value": "NORMAL", "label": "正常出勤", "color": "#52c41a"},
          {"value": "LEAVE", "label": "请假", "color": "#faad14"},
          {"value": "ABSENT", "label": "旷工", "color": "#f5222d"},
          {"value": "HOLIDAY", "label": "节假日", "color": "#1890ff"}
        ],
        "defaultValue": "NORMAL"
      }
    },
    {
      "fieldId": "attendance_batch",
      "fieldType": "TEXT",
      "name": "导入批次号",
      "readonly": true,
      "systemField": true
    },
    {
      "fieldId": "attendance_import_time",
      "fieldType": "DATE",
      "name": "导入时间",
      "readonly": true,
      "systemField": true
    },
    {
      "fieldId": "attendance_source",
      "fieldType": "ENUM",
      "name": "数据来源",
      "required": true,
      "enumConfig": {
        "options": [
          {"value": "IMPORT", "label": "批量导入"},
          {"value": "CLOCK_IN", "label": "打卡生成"},
          {"value": "MANUAL", "label": "手动创建"}
        ],
        "defaultValue": "IMPORT"
      }
    }
  ],
  
  "linkConfigs": [
    {
      "linkTypeId": "attendance_to_clockin",
      "direction": "TARGET",
      "required": false,
      "description": "考勤关联的打卡记录"
    },
    {
      "linkTypeId": "attendance_to_leave",
      "direction": "TARGET",
      "required": false,
      "description": "考勤关联的请假记录"
    }
  ],
  
  "uniqueConstraints": [
    {
      "fields": ["attendance_date", "attendance_member"],
      "errorMessage": "同一成员同一天只能有一条考勤记录"
    }
  ]
}
```

### 3.3 打卡卡片类型 (ClockInCard)

**卡片类型定义：**
```json
{
  "id": "clockin_card_type",
  "schemaType": "CARD_TYPE",
  "schemaSubType": "ENTITY_CARD_TYPE",
  "name": "打卡卡片",
  "code": "CLOCK_IN",
  "icon": "icon-check-circle",
  "color": "#13c2c2",
  "description": "记录外包人员的签入签出打卡",
  
  "fieldConfigs": [
    {
      "fieldId": "clockin_date",
      "fieldType": "DATE",
      "name": "打卡日期",
      "required": true,
      "readonly": true
    },
    {
      "fieldId": "clockin_member",
      "fieldType": "STRUCTURE",
      "name": "打卡成员",
      "required": true,
      "readonly": true,
      "structureConfig": {
        "structureType": "MEMBER",
        "multiSelect": false
      }
    },
    {
      "fieldId": "clockin_sign_in_time",
      "fieldType": "DATE",
      "name": "签入时间",
      "required": true,
      "readonly": true,
      "dateConfig": {
        "includeTime": true,
        "format": "YYYY-MM-DD HH:mm:ss"
      }
    },
    {
      "fieldId": "clockin_sign_out_time",
      "fieldType": "DATE",
      "name": "签出时间",
      "required": false,
      "readonly": true,
      "dateConfig": {
        "includeTime": true,
        "format": "YYYY-MM-DD HH:mm:ss"
      }
    },
    {
      "fieldId": "clockin_work_minutes",
      "fieldType": "NUMBER",
      "name": "工作时长（分钟）",
      "readonly": true,
      "derived": true,
      "derivedFormula": "(clockin_sign_out_time - clockin_sign_in_time) / 60000 - clockin_absent_minutes",
      "numberConfig": {
        "precision": 0,
        "min": 0,
        "unit": "分钟"
      }
    },
    {
      "fieldId": "clockin_absent_minutes",
      "fieldType": "NUMBER",
      "name": "旷工时长（分钟）",
      "readonly": true,
      "numberConfig": {
        "precision": 0,
        "min": 0,
        "unit": "分钟"
      },
      "description": "根据旷工规则计算"
    },
    {
      "fieldId": "clockin_overtime_minutes",
      "fieldType": "NUMBER",
      "name": "加班时长（分钟）",
      "readonly": true,
      "numberConfig": {
        "precision": 0,
        "min": 0,
        "unit": "分钟"
      },
      "description": "根据加班规则计算"
    },
    {
      "fieldId": "clockin_status",
      "fieldType": "ENUM",
      "name": "打卡状态",
      "required": true,
      "enumConfig": {
        "options": [
          {"value": "SIGNED_IN", "label": "已签入", "color": "#1890ff"},
          {"value": "SIGNED_OUT", "label": "已签出", "color": "#52c41a"},
          {"value": "ABNORMAL", "label": "异常", "color": "#f5222d"}
        ],
        "defaultValue": "SIGNED_IN"
      }
    }
  ],
  
  "linkConfigs": [
    {
      "linkTypeId": "clockin_to_attendance",
      "direction": "SOURCE",
      "required": false,
      "description": "打卡生成的考勤记录"
    }
  ]
}
```


### 3.4 请假卡片类型 (LeaveCard)

**卡片类型定义：**
```json
{
  "id": "leave_card_type",
  "schemaType": "CARD_TYPE",
  "schemaSubType": "ENTITY_CARD_TYPE",
  "name": "请假卡片",
  "code": "LEAVE",
  "icon": "icon-calendar-off",
  "color": "#faad14",
  "description": "记录成员的请假申请",
  
  "fieldConfigs": [
    {
      "fieldId": "leave_member",
      "fieldType": "STRUCTURE",
      "name": "请假人",
      "required": true,
      "structureConfig": {
        "structureType": "MEMBER",
        "multiSelect": false
      }
    },
    {
      "fieldId": "leave_type",
      "fieldType": "ENUM",
      "name": "请假类型",
      "required": true,
      "enumConfig": {
        "options": [
          {"value": "ANNUAL", "label": "年假", "color": "#52c41a"},
          {"value": "SICK", "label": "病假", "color": "#faad14"},
          {"value": "PERSONAL", "label": "事假", "color": "#1890ff"},
          {"value": "MATERNITY", "label": "产假", "color": "#eb2f96"},
          {"value": "PATERNITY", "label": "陪产假", "color": "#722ed1"},
          {"value": "OTHER", "label": "其他", "color": "#d9d9d9"}
        ]
      }
    },
    {
      "fieldId": "leave_start_date",
      "fieldType": "DATE",
      "name": "开始日期",
      "required": true,
      "dateConfig": {
        "includeTime": true
      }
    },
    {
      "fieldId": "leave_end_date",
      "fieldType": "DATE",
      "name": "结束日期",
      "required": true,
      "dateConfig": {
        "includeTime": true
      }
    },
    {
      "fieldId": "leave_days",
      "fieldType": "NUMBER",
      "name": "请假天数",
      "readonly": true,
      "derived": true,
      "derivedFormula": "(leave_end_date - leave_start_date) / 86400000",
      "numberConfig": {
        "precision": 1,
        "min": 0,
        "unit": "天"
      }
    },
    {
      "fieldId": "leave_reason",
      "fieldType": "MULTI_LINE_TEXT",
      "name": "请假原因",
      "required": true,
      "maxLength": 500
    },
    {
      "fieldId": "leave_status",
      "fieldType": "ENUM",
      "name": "审批状态",
      "required": true,
      "enumConfig": {
        "options": [
          {"value": "PENDING", "label": "待审批", "color": "#1890ff"},
          {"value": "APPROVED", "label": "已批准", "color": "#52c41a"},
          {"value": "REJECTED", "label": "已拒绝", "color": "#f5222d"},
          {"value": "CANCELLED", "label": "已取消", "color": "#d9d9d9"}
        ],
        "defaultValue": "PENDING"
      }
    },
    {
      "fieldId": "leave_approver",
      "fieldType": "STRUCTURE",
      "name": "审批人",
      "required": false,
      "structureConfig": {
        "structureType": "MEMBER",
        "multiSelect": false
      }
    },
    {
      "fieldId": "leave_approve_time",
      "fieldType": "DATE",
      "name": "审批时间",
      "readonly": true,
      "dateConfig": {
        "includeTime": true
      }
    },
    {
      "fieldId": "leave_attachment",
      "fieldType": "ATTACHMENT",
      "name": "附件",
      "required": false,
      "description": "病假证明等附件"
    }
  ],
  
  "linkConfigs": [
    {
      "linkTypeId": "leave_to_attendance",
      "direction": "SOURCE",
      "required": false,
      "description": "请假影响的考勤记录"
    }
  ],
  
  "valueStreamId": "leave_approval_stream"
}
```

### 3.5 结算卡片类型 (SettlementCard)

**卡片类型定义：**
```json
{
  "id": "settlement_card_type",
  "schemaType": "CARD_TYPE",
  "schemaSubType": "ENTITY_CARD_TYPE",
  "name": "结算卡片",
  "code": "SETTLEMENT",
  "icon": "icon-money-collect",
  "color": "#fa8c16",
  "description": "记录外包人员的月度结算",
  
  "fieldConfigs": [
    {
      "fieldId": "settlement_month",
      "fieldType": "TEXT",
      "name": "结算月份",
      "required": true,
      "pattern": "^\\d{4}-\\d{2}$",
      "placeholder": "格式：YYYY-MM"
    },
    {
      "fieldId": "settlement_member",
      "fieldType": "STRUCTURE",
      "name": "结算成员",
      "required": true,
      "structureConfig": {
        "structureType": "MEMBER",
        "multiSelect": false
      }
    },
    {
      "fieldId": "settlement_work_days",
      "fieldType": "NUMBER",
      "name": "工作天数",
      "readonly": true,
      "numberConfig": {
        "precision": 1,
        "min": 0,
        "unit": "天"
      }
    },
    {
      "fieldId": "settlement_work_hours",
      "fieldType": "NUMBER",
      "name": "工作时长",
      "readonly": true,
      "numberConfig": {
        "precision": 2,
        "min": 0,
        "unit": "小时"
      }
    },
    {
      "fieldId": "settlement_overtime_hours",
      "fieldType": "NUMBER",
      "name": "加班时长",
      "readonly": true,
      "numberConfig": {
        "precision": 2,
        "min": 0,
        "unit": "小时"
      }
    },
    {
      "fieldId": "settlement_absent_hours",
      "fieldType": "NUMBER",
      "name": "旷工时长",
      "readonly": true,
      "numberConfig": {
        "precision": 2,
        "min": 0,
        "unit": "小时"
      }
    },
    {
      "fieldId": "settlement_base_fee",
      "fieldType": "NUMBER",
      "name": "基础服务费",
      "readonly": true,
      "numberConfig": {
        "precision": 2,
        "min": 0,
        "unit": "元"
      }
    },
    {
      "fieldId": "settlement_overtime_fee",
      "fieldType": "NUMBER",
      "name": "加班费",
      "readonly": true,
      "numberConfig": {
        "precision": 2,
        "min": 0,
        "unit": "元"
      }
    },
    {
      "fieldId": "settlement_deduction",
      "fieldType": "NUMBER",
      "name": "旷工扣款",
      "readonly": true,
      "numberConfig": {
        "precision": 2,
        "min": 0,
        "unit": "元"
      }
    },
    {
      "fieldId": "settlement_total_fee",
      "fieldType": "NUMBER",
      "name": "总服务费",
      "readonly": true,
      "derived": true,
      "derivedFormula": "settlement_base_fee + settlement_overtime_fee - settlement_deduction",
      "numberConfig": {
        "precision": 2,
        "min": 0,
        "unit": "元"
      }
    },
    {
      "fieldId": "settlement_status",
      "fieldType": "ENUM",
      "name": "结算状态",
      "required": true,
      "enumConfig": {
        "options": [
          {"value": "DRAFT", "label": "草稿", "color": "#d9d9d9"},
          {"value": "CONFIRMED", "label": "已确认", "color": "#52c41a"},
          {"value": "REVOKED", "label": "已撤销", "color": "#f5222d"}
        ],
        "defaultValue": "DRAFT"
      }
    },
    {
      "fieldId": "settlement_confirm_time",
      "fieldType": "DATE",
      "name": "确认时间",
      "readonly": true,
      "dateConfig": {
        "includeTime": true
      }
    }
  ],
  
  "linkConfigs": [
    {
      "linkTypeId": "settlement_to_clockin",
      "direction": "TARGET",
      "required": false,
      "description": "结算关联的打卡记录"
    },
    {
      "linkTypeId": "settlement_to_worklog",
      "direction": "TARGET",
      "required": false,
      "description": "结算关联的工时记录"
    }
  ],
  
  "uniqueConstraints": [
    {
      "fields": ["settlement_month", "settlement_member"],
      "errorMessage": "同一成员同一月份只能有一条结算记录"
    }
  ]
}
```

### 3.6 关联类型定义

**工时关联任务：**
```json
{
  "id": "worklog_to_task",
  "code": "WORKLOG_TASK",
  "sourceName": "工时记录",
  "targetName": "关联任务",
  "sourceVisible": true,
  "targetVisible": true,
  "sourceCardTypeIds": ["worklog_card_type"],
  "targetCardTypeIds": ["task_card_type", "story_card_type", "bug_card_type"],
  "sourceMultiSelect": false,
  "targetMultiSelect": true,
  "systemLinkType": false
}
```

**工时关联考勤：**
```json
{
  "id": "worklog_to_attendance",
  "code": "WORKLOG_ATTENDANCE",
  "sourceName": "工时记录",
  "targetName": "考勤记录",
  "sourceVisible": true,
  "targetVisible": true,
  "sourceCardTypeIds": ["worklog_card_type"],
  "targetCardTypeIds": ["attendance_card_type"],
  "sourceMultiSelect": false,
  "targetMultiSelect": false,
  "systemLinkType": false
}
```

**打卡关联考勤：**
```json
{
  "id": "clockin_to_attendance",
  "code": "CLOCKIN_ATTENDANCE",
  "sourceName": "打卡记录",
  "targetName": "考勤记录",
  "sourceVisible": true,
  "targetVisible": true,
  "sourceCardTypeIds": ["clockin_card_type"],
  "targetCardTypeIds": ["attendance_card_type"],
  "sourceMultiSelect": false,
  "targetMultiSelect": false,
  "systemLinkType": false
}
```

**请假关联考勤：**
```json
{
  "id": "leave_to_attendance",
  "code": "LEAVE_ATTENDANCE",
  "sourceName": "请假记录",
  "targetName": "考勤记录",
  "sourceVisible": true,
  "targetVisible": true,
  "sourceCardTypeIds": ["leave_card_type"],
  "targetCardTypeIds": ["attendance_card_type"],
  "sourceMultiSelect": false,
  "targetMultiSelect": true,
  "systemLinkType": false
}
```

**结算关联打卡：**
```json
{
  "id": "settlement_to_clockin",
  "code": "SETTLEMENT_CLOCKIN",
  "sourceName": "结算记录",
  "targetName": "打卡记录",
  "sourceVisible": true,
  "targetVisible": true,
  "sourceCardTypeIds": ["settlement_card_type"],
  "targetCardTypeIds": ["clockin_card_type"],
  "sourceMultiSelect": false,
  "targetMultiSelect": true,
  "systemLinkType": false
}
```

---

## 4. 数据模型设计

### 4.1 卡片数据模型

所有工时相关数据都以卡片形式存储在 zgraph 图数据库中，遵循统一的卡片数据模型：

```protobuf
message Card {
    uint64 id = 1;                              // 卡片ID（雪花算法）
    string org_id = 2;                          // 组织ID
    int64 code_in_org = 3;                      // 组织内编号
    string custom_code = 4;                     // 自定义编号
    string type_id = 5;                         // 卡片类型ID
    
    Title title = 6;                            // 标题
    string description = 7;                     // 描述
    
    common.CardState state = 8;                 // 卡片状态
    string stream_id = 9;                       // 价值流ID
    string status_id = 10;                      // 状态ID
    
    map<string, zgraph.field.FieldValue> custom_field_value_map = 11;  // 自定义属性值
    map<string, CardList> link_card_map = 12;   // 关联卡片
    
    int64 created_at = 13;
    int64 updated_at = 14;
    int64 discarded_at = 15;
    int64 archived_at = 16;
}
```

### 4.2 配置数据模型（MySQL存储）

**工时配置表 (workload_config)：**
```sql
CREATE TABLE workload_config (
    id VARCHAR(64) PRIMARY KEY,
    org_id VARCHAR(64) NOT NULL,
    config_type VARCHAR(50) NOT NULL,          -- WINDOW/LOCK/LIMIT/ATTENDANCE
    config_content JSON NOT NULL,              -- 配置内容
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(64),
    updated_at DATETIME,
    updated_by VARCHAR(64),
    INDEX idx_org_type (org_id, config_type)
);
```

**窗口期配置示例：**
```json
{
  "configType": "WINDOW",
  "createWindow": {
    "left": -7,
    "right": 0,
    "disabled": false
  },
  "updateWindow": {
    "left": -30,
    "right": 0,
    "disabled": false
  },
  "deleteWindow": {
    "left": -7,
    "right": 0,
    "disabled": false
  }
}
```

**锁定日期配置示例：**
```json
{
  "configType": "LOCK",
  "lockDay": 5,
  "lockHour": 23,
  "enabled": true
}
```

**工时上限配置示例：**
```json
{
  "configType": "LIMIT",
  "maxHoursPerDay": 24,
  "maxHoursPerWeek": 168,
  "maxHoursPerMonth": 744
}
```

**考勤同步配置示例：**
```json
{
  "configType": "ATTENDANCE",
  "attendanceSync": true,
  "checkDailyTime": true,
  "checkOverflow": true,
  "uploadWindows": [1, 2, 3, 4, 5]
}
```

**外包配置表 (outsourcing_config)：**
```sql
CREATE TABLE outsourcing_config (
    id VARCHAR(64) PRIMARY KEY,
    org_id VARCHAR(64) NOT NULL,
    member_filter_id VARCHAR(64),              -- 外包人员过滤器ID
    attendance_config JSON,                    -- 考勤规则配置
    settlement_config JSON,                    -- 结算配置
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(64),
    updated_at DATETIME,
    updated_by VARCHAR(64),
    UNIQUE KEY uk_org_id (org_id)
);
```

**考勤规则配置示例：**
```json
{
  "standardWorkTime": 8.0,
  "overtimeRule": {
    "enabled": true,
    "startTime": "18:00",
    "rate": 1.5
  },
  "absenteeismRule": {
    "enabled": true,
    "lateThreshold": 30,
    "earlyLeaveThreshold": 30
  },
  "accumulatedOvertime": true
}
```

**结算配置示例：**
```json
{
  "personalServiceFeeConf": {
    "enabled": true,
    "baseFeePerDay": 500,
    "overtimeFeePerHour": 100,
    "deductionPerHour": 100
  },
  "projectServiceFeeConf": {
    "enabled": true,
    "allocationDimension": "PROJECT"
  }
}
```

### 4.3 批量导入记录表 (workload_import_record)

```sql
CREATE TABLE workload_import_record (
    id VARCHAR(64) PRIMARY KEY,
    batch VARCHAR(64) NOT NULL UNIQUE,         -- 批次号
    org_id VARCHAR(64) NOT NULL,
    title VARCHAR(255),
    import_type VARCHAR(50) NOT NULL,          -- ATTENDANCE/WORKLOG
    operator_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,               -- PROCESSING/SUCCESS/FAILED
    total_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    error_message TEXT,
    error_file_path VARCHAR(500),
    oss_file_id VARCHAR(64),
    created_at DATETIME NOT NULL,
    completed_at DATETIME,
    INDEX idx_org_batch (org_id, batch),
    INDEX idx_org_status (org_id, status)
);
```

### 4.4 工时统计缓存表 (workload_statistics_cache)

用于缓存聚合统计结果，提升查询性能：

```sql
CREATE TABLE workload_statistics_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id VARCHAR(64) NOT NULL,
    cache_key VARCHAR(255) NOT NULL,           -- 缓存键（包含查询条件）
    cache_type VARCHAR(50) NOT NULL,           -- MEMBER_DAILY/PROJECT_MONTHLY等
    dimension_value VARCHAR(255),              -- 维度值（成员ID/项目ID等）
    stat_date DATE,                            -- 统计日期
    stat_data JSON NOT NULL,                   -- 统计数据
    expired_at DATETIME NOT NULL,              -- 过期时间
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_cache_key (cache_key),
    INDEX idx_org_type_date (org_id, cache_type, stat_date),
    INDEX idx_expired (expired_at)
);
```


---

## 5. 服务架构设计

### 5.1 服务模块结构

```
kanban-services/
└── workload-service/
    ├── src/main/java/cn/agilean/kanban/workload/
    │   ├── WorkloadServiceApplication.java
    │   ├── controller/                        # REST API控制器
    │   │   ├── WorklogController.java
    │   │   ├── AttendanceController.java
    │   │   ├── ClockInController.java
    │   │   ├── LeaveController.java
    │   │   ├── SettlementController.java
    │   │   └── WorkloadConfigController.java
    │   ├── service/                           # 业务服务层
    │   │   ├── core/
    │   │   │   ├── WorklogService.java
    │   │   │   ├── AttendanceService.java
    │   │   │   ├── ClockInService.java
    │   │   │   ├── LeaveService.java
    │   │   │   └── SettlementService.java
    │   │   ├── validation/
    │   │   │   ├── WindowPeriodValidator.java
    │   │   │   ├── LockDateValidator.java
    │   │   │   └── AttendanceValidator.java
    │   │   ├── calculation/
    │   │   │   ├── WorklogCalculator.java
    │   │   │   ├── OvertimeCalculator.java
    │   │   │   ├── AbsenteeismCalculator.java
    │   │   │   └── SettlementCalculator.java
    │   │   ├── import/
    │   │   │   ├── AttendanceImportService.java
    │   │   │   ├── FileParser.java
    │   │   │   └── BatchProcessor.java
    │   │   ├── statistics/
    │   │   │   ├── WorklogStatisticsService.java
    │   │   │   └── StatisticsCacheService.java
    │   │   └── schedule/
    │   │       ├── DailyInitTask.java
    │   │       ├── LockDateTask.java
    │   │       └── SettlementTask.java
    │   ├── repository/                        # 数据访问层
    │   │   ├── WorkloadConfigRepository.java
    │   │   ├── OutsourcingConfigRepository.java
    │   │   ├── ImportRecordRepository.java
    │   │   └── StatisticsCacheRepository.java
    │   ├── model/                             # 数据模型
    │   │   ├── entity/
    │   │   │   ├── WorkloadConfigEntity.java
    │   │   │   ├── OutsourcingConfigEntity.java
    │   │   │   ├── ImportRecordEntity.java
    │   │   │   └── StatisticsCacheEntity.java
    │   │   ├── dto/
    │   │   │   ├── WorklogDTO.java
    │   │   │   ├── AttendanceDTO.java
    │   │   │   ├── ClockInDTO.java
    │   │   │   └── SettlementDTO.java
    │   │   ├── request/
    │   │   │   ├── CreateWorklogRequest.java
    │   │   │   ├── ImportAttendanceRequest.java
    │   │   │   └── SettlementRequest.java
    │   │   └── vo/
    │   │       ├── WorklogStatisticsVO.java
    │   │       └── SettlementDetailVO.java
    │   ├── event/                             # 事件处理
    │   │   ├── publisher/
    │   │   │   └── WorkloadEventPublisher.java
    │   │   └── listener/
    │   │       ├── CardEventListener.java
    │   │       └── StreamEventListener.java
    │   ├── rule/                              # 业务规则
    │   │   ├── WorklogRuleExecutor.java
    │   │   └── SettlementRuleExecutor.java
    │   └── config/
    │       ├── WorkloadConfiguration.java
    │       └── ScheduleConfiguration.java
    └── src/main/resources/
        ├── application.yml
        └── mapper/
            ├── WorkloadConfigMapper.xml
            └── ImportRecordMapper.xml
```

### 5.2 核心服务类设计

**WorklogService - 工时服务：**
```java
@Service
@RequiredArgsConstructor
public class WorklogService {
    private final CardRepository cardRepository;
    private final WindowPeriodValidator windowPeriodValidator;
    private final LockDateValidator lockDateValidator;
    private final AttendanceValidator attendanceValidator;
    private final WorklogCalculator worklogCalculator;
    private final WorkloadEventPublisher eventPublisher;
    
    /**
     * 创建工时记录
     */
    public Result<CardId> createWorklog(CreateWorklogRequest request, CardId operatorId) {
        // 1. 校验窗口期
        ValidationResult windowResult = windowPeriodValidator.validateCreate(
            request.getWorklogDate(), request.getOrgId(), operatorId
        );
        if (!windowResult.isValid()) {
            return Result.fail(windowResult.getMessage());
        }
        
        // 2. 校验锁定日期
        ValidationResult lockResult = lockDateValidator.validate(
            request.getWorklogDate(), request.getOrgId()
        );
        if (!lockResult.isValid()) {
            return Result.fail(lockResult.getMessage());
        }
        
        // 3. 校验考勤数据（如果启用）
        ValidationResult attendanceResult = attendanceValidator.validate(
            request.getMemberId(), request.getWorklogDate(), 
            request.getWorklogHours(), request.getOrgId()
        );
        if (!attendanceResult.isValid()) {
            return Result.fail(attendanceResult.getMessage());
        }
        
        // 4. 构建卡片实体
        CardEntity worklogCard = buildWorklogCard(request);
        
        // 5. 创建卡片
        CardId cardId = cardRepository.create(worklogCard);
        
        // 6. 发布事件
        eventPublisher.publishWorklogCreated(cardId, operatorId);
        
        return Result.success(cardId);
    }
    
    /**
     * 更新工时记录
     */
    public Result<Void> updateWorklog(UpdateWorklogRequest request, CardId operatorId) {
        // 1. 查询原工时记录
        Optional<CardDTO> cardOpt = cardRepository.findById(request.getCardId());
        if (cardOpt.isEmpty()) {
            return Result.fail("工时记录不存在");
        }
        
        CardDTO card = cardOpt.get();
        LocalDate worklogDate = extractWorklogDate(card);
        
        // 2. 校验窗口期
        ValidationResult windowResult = windowPeriodValidator.validateUpdate(
            worklogDate, request.getOrgId(), operatorId
        );
        if (!windowResult.isValid()) {
            return Result.fail(windowResult.getMessage());
        }
        
        // 3. 校验锁定状态
        if (isLocked(card)) {
            return Result.fail("工时记录已锁定，不可修改");
        }
        
        // 4. 更新卡片
        CardEntity updatedCard = buildUpdatedWorklogCard(card, request);
        cardRepository.update(updatedCard);
        
        // 5. 发布事件
        eventPublisher.publishWorklogUpdated(request.getCardId(), operatorId);
        
        return Result.success();
    }
    
    /**
     * 查询工时统计
     */
    public Result<WorklogStatisticsVO> queryStatistics(WorklogStatisticsRequest request) {
        // 1. 构建查询条件
        CardQueryRequest queryRequest = buildQueryRequest(request);
        
        // 2. 查询卡片数据
        PageResult<CardDTO> pageResult = cardRepository.query(queryRequest);
        
        // 3. 聚合计算统计数据
        WorklogStatisticsVO statistics = worklogCalculator.calculate(pageResult.getData());
        
        return Result.success(statistics);
    }
}
```

**AttendanceService - 考勤服务：**
```java
@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final CardRepository cardRepository;
    private final AttendanceImportService importService;
    private final ImportRecordRepository importRecordRepository;
    
    /**
     * 批量导入考勤数据
     */
    public Result<String> importAttendance(ImportAttendanceRequest request) {
        // 1. 生成批次号
        String batch = generateBatch();
        
        // 2. 创建导入记录
        ImportRecordEntity record = ImportRecordEntity.builder()
            .batch(batch)
            .orgId(request.getOrgId())
            .importType("ATTENDANCE")
            .operatorId(request.getOperatorId())
            .status("PROCESSING")
            .createdAt(LocalDateTime.now())
            .build();
        importRecordRepository.insert(record);
        
        // 3. 异步处理导入
        CompletableFuture.runAsync(() -> {
            try {
                importService.processImport(request, batch);
            } catch (Exception e) {
                log.error("导入考勤数据失败", e);
                importRecordRepository.updateStatus(batch, "FAILED", e.getMessage());
            }
        });
        
        return Result.success(batch);
    }
    
    /**
     * 查询导入记录
     */
    public Result<ImportRecordDTO> queryImportRecord(String batch) {
        ImportRecordEntity record = importRecordRepository.findByBatch(batch);
        if (record == null) {
            return Result.fail("导入记录不存在");
        }
        return Result.success(ImportRecordDTO.from(record));
    }
}
```

**ClockInService - 打卡服务：**
```java
@Service
@RequiredArgsConstructor
public class ClockInService {
    private final CardRepository cardRepository;
    private final OutsourcingConfigRepository configRepository;
    private final OvertimeCalculator overtimeCalculator;
    private final AbsenteeismCalculator absenteeismCalculator;
    
    /**
     * 签入打卡
     */
    public Result<CardId> signIn(CardId memberId, String orgId) {
        // 1. 验证是否为外包人员
        OutsourcingConfigEntity config = configRepository.findByOrgId(orgId);
        if (!isOutsourcingMember(memberId, config)) {
            return Result.fail("非外包人员，无法打卡");
        }
        
        // 2. 检查今天是否已签入
        Optional<CardDTO> todayClockIn = findTodayClockIn(memberId, orgId);
        if (todayClockIn.isPresent()) {
            return Result.fail("今天已签入，请勿重复打卡");
        }
        
        // 3. 创建打卡卡片
        CardEntity clockInCard = CardEntity.builder()
            .typeId(new CardTypeId("clockin_card_type"))
            .orgId(new OrgId(orgId))
            .title(new CardTitle("打卡-" + LocalDate.now()))
            .build();
        
        // 设置字段值
        clockInCard.setFieldValue("clockin_date", new DateFieldValue(LocalDate.now()));
        clockInCard.setFieldValue("clockin_member", new StructureFieldValue(memberId));
        clockInCard.setFieldValue("clockin_sign_in_time", new DateFieldValue(LocalDateTime.now()));
        clockInCard.setFieldValue("clockin_status", new EnumFieldValue("SIGNED_IN"));
        
        // 4. 保存卡片
        CardId cardId = cardRepository.create(clockInCard);
        
        return Result.success(cardId);
    }
    
    /**
     * 签出打卡
     */
    public Result<ClockInDetailVO> signOut(CardId clockInCardId, CardId memberId, String orgId) {
        // 1. 查询签入记录
        Optional<CardDTO> cardOpt = cardRepository.findById(clockInCardId);
        if (cardOpt.isEmpty()) {
            return Result.fail("签入记录不存在");
        }
        
        CardDTO card = cardOpt.get();
        
        // 2. 验证签入记录
        if (!validateSignInRecord(card, memberId)) {
            return Result.fail("签入记录验证失败");
        }
        
        // 3. 验证是否同一天
        LocalDate signInDate = extractSignInDate(card);
        if (!signInDate.equals(LocalDate.now())) {
            return Result.fail("签入和签出必须在同一天");
        }
        
        // 4. 计算工作时长
        LocalDateTime signInTime = extractSignInTime(card);
        LocalDateTime signOutTime = LocalDateTime.now();
        long workMinutes = Duration.between(signInTime, signOutTime).toMinutes();
        
        // 5. 计算旷工时长
        OutsourcingConfigEntity config = configRepository.findByOrgId(orgId);
        long absentMinutes = absenteeismCalculator.calculate(signInTime, signOutTime, config);
        
        // 6. 计算加班时长
        long overtimeMinutes = overtimeCalculator.calculate(signInTime, signOutTime, config);
        
        // 7. 更新打卡卡片
        CardEntity updatedCard = buildUpdatedClockInCard(card, signOutTime, 
            workMinutes - absentMinutes, absentMinutes, overtimeMinutes);
        cardRepository.update(updatedCard);
        
        // 8. 生成考勤卡片
        generateAttendanceCard(memberId, signInDate, workMinutes - absentMinutes, orgId);
        
        return Result.success(buildClockInDetailVO(updatedCard));
    }
}
```

**SettlementService - 结算服务：**
```java
@Service
@RequiredArgsConstructor
public class SettlementService {
    private final CardRepository cardRepository;
    private final SettlementCalculator settlementCalculator;
    private final OutsourcingConfigRepository configRepository;
    
    /**
     * 执行月度结算
     */
    public Result<CardId> executeSettlement(SettlementRequest request) {
        // 1. 检查是否已结算
        Optional<CardDTO> existingSettlement = findSettlement(
            request.getOrgId(), request.getMemberId(), request.getMonth()
        );
        if (existingSettlement.isPresent()) {
            return Result.fail("该月份已结算");
        }
        
        // 2. 查询当月打卡记录
        List<CardDTO> clockInCards = queryMonthClockInCards(
            request.getOrgId(), request.getMemberId(), request.getMonth()
        );
        
        // 3. 查询当月工时记录
        List<CardDTO> worklogCards = queryMonthWorklogCards(
            request.getOrgId(), request.getMemberId(), request.getMonth()
        );
        
        // 4. 获取结算配置
        OutsourcingConfigEntity config = configRepository.findByOrgId(request.getOrgId());
        
        // 5. 计算结算数据
        SettlementData data = settlementCalculator.calculate(
            clockInCards, worklogCards, config
        );
        
        // 6. 创建结算卡片
        CardEntity settlementCard = buildSettlementCard(request, data);
        CardId cardId = cardRepository.create(settlementCard);
        
        // 7. 建立关联关系
        linkClockInCards(cardId, clockInCards);
        linkWorklogCards(cardId, worklogCards);
        
        return Result.success(cardId);
    }
    
    /**
     * 撤销结算
     */
    public Result<Void> revokeSettlement(CardId settlementCardId, String orgId) {
        // 1. 查询结算记录
        Optional<CardDTO> cardOpt = cardRepository.findById(settlementCardId);
        if (cardOpt.isEmpty()) {
            return Result.fail("结算记录不存在");
        }
        
        // 2. 检查结算状态
        CardDTO card = cardOpt.get();
        if (!"CONFIRMED".equals(extractSettlementStatus(card))) {
            return Result.fail("只能撤销已确认的结算");
        }
        
        // 3. 更新结算状态
        CardEntity updatedCard = buildRevokedSettlementCard(card);
        cardRepository.update(updatedCard);
        
        return Result.success();
    }
}
```

### 5.3 校验器设计

**WindowPeriodValidator - 窗口期校验器：**
```java
@Component
@RequiredArgsConstructor
public class WindowPeriodValidator {
    private final WorkloadConfigRepository configRepository;
    
    public ValidationResult validateCreate(LocalDate worklogDate, String orgId, CardId operatorId) {
        // 1. 获取窗口期配置
        WorkloadConfigEntity config = configRepository.findByOrgIdAndType(orgId, "WINDOW");
        if (config == null) {
            return ValidationResult.valid();
        }
        
        WindowConfig windowConfig = parseWindowConfig(config);
        
        // 2. 检查是否为管理员白名单
        if (isManagerWhitelist(operatorId, orgId)) {
            return ValidationResult.valid();
        }
        
        // 3. 校验创建窗口期
        LocalDate today = LocalDate.now();
        LocalDate leftBoundary = today.plusDays(windowConfig.getCreateWindow().getLeft());
        LocalDate rightBoundary = today.plusDays(windowConfig.getCreateWindow().getRight());
        
        if (worklogDate.isBefore(leftBoundary) || worklogDate.isAfter(rightBoundary)) {
            return ValidationResult.invalid(
                String.format("工时日期必须在 %s 到 %s 之间", leftBoundary, rightBoundary)
            );
        }
        
        return ValidationResult.valid();
    }
    
    public ValidationResult validateUpdate(LocalDate worklogDate, String orgId, CardId operatorId) {
        // 类似逻辑，校验修改窗口期
        // ...
    }
    
    public ValidationResult validateDelete(LocalDate worklogDate, String orgId, CardId operatorId) {
        // 类似逻辑，校验删除窗口期
        // ...
    }
}
```

**LockDateValidator - 锁定日期校验器：**
```java
@Component
@RequiredArgsConstructor
public class LockDateValidator {
    private final WorkloadConfigRepository configRepository;
    
    public ValidationResult validate(LocalDate worklogDate, String orgId) {
        // 1. 获取锁定日期配置
        WorkloadConfigEntity config = configRepository.findByOrgIdAndType(orgId, "LOCK");
        if (config == null) {
            return ValidationResult.valid();
        }
        
        LockConfig lockConfig = parseLockConfig(config);
        if (!lockConfig.isEnabled()) {
            return ValidationResult.valid();
        }
        
        // 2. 计算锁定时间点
        LocalDateTime now = LocalDateTime.now();
        int lockDay = lockConfig.getLockDay();
        int lockHour = lockConfig.getLockHour();
        
        LocalDateTime lockDateTime = LocalDateTime.of(
            now.getYear(), now.getMonth(), lockDay, lockHour, 0
        );
        
        // 3. 如果当前时间已过锁定时间点，则不允许修改锁定日期之前的数据
        if (now.isAfter(lockDateTime)) {
            LocalDate lockDate = lockDateTime.toLocalDate();
            if (worklogDate.isBefore(lockDate) || worklogDate.isEqual(lockDate)) {
                return ValidationResult.invalid(
                    String.format("工时日期 %s 已被锁定，不可修改", worklogDate)
                );
            }
        }
        
        return ValidationResult.valid();
    }
}
```


**AttendanceValidator - 考勤校验器：**
```java
@Component
@RequiredArgsConstructor
public class AttendanceValidator {
    private final CardRepository cardRepository;
    private final WorkloadConfigRepository configRepository;
    
    public ValidationResult validate(CardId memberId, LocalDate worklogDate, 
                                     double worklogHours, String orgId) {
        // 1. 获取考勤同步配置
        WorkloadConfigEntity config = configRepository.findByOrgIdAndType(orgId, "ATTENDANCE");
        if (config == null) {
            return ValidationResult.valid();
        }
        
        AttendanceConfig attendanceConfig = parseAttendanceConfig(config);
        if (!attendanceConfig.isAttendanceSync()) {
            return ValidationResult.valid();
        }
        
        // 2. 查询考勤记录
        Optional<CardDTO> attendanceCard = findAttendanceCard(memberId, worklogDate, orgId);
        
        // 3. 检查是否有考勤数据
        if (attendanceConfig.isCheckDailyTime() && attendanceCard.isEmpty()) {
            return ValidationResult.invalid(
                String.format("日期 %s 没有考勤数据，无法填报工时", worklogDate)
            );
        }
        
        // 4. 检查工时是否超出考勤时长
        if (attendanceConfig.isCheckOverflow() && attendanceCard.isPresent()) {
            double attendanceHours = extractAttendanceHours(attendanceCard.get());
            double totalWorklogHours = calculateTotalWorklogHours(memberId, worklogDate, orgId);
            
            if (totalWorklogHours + worklogHours > attendanceHours) {
                return ValidationResult.invalid(
                    String.format("工时总和 %.2f 超出考勤时长 %.2f", 
                        totalWorklogHours + worklogHours, attendanceHours)
                );
            }
        }
        
        return ValidationResult.valid();
    }
}
```

### 5.4 计算器设计

**WorklogCalculator - 工时计算器：**
```java
@Component
public class WorklogCalculator {
    
    /**
     * 计算工时统计数据
     */
    public WorklogStatisticsVO calculate(List<CardDTO> worklogCards) {
        WorklogStatisticsVO statistics = new WorklogStatisticsVO();
        
        double totalHours = 0;
        double normalHours = 0;
        double overtimeHours = 0;
        int totalDays = 0;
        
        Map<String, Double> memberHoursMap = new HashMap<>();
        Map<String, Double> projectHoursMap = new HashMap<>();
        
        for (CardDTO card : worklogCards) {
            double hours = extractWorklogHours(card);
            String worklogType = extractWorklogType(card);
            String memberId = extractMemberId(card);
            String projectId = extractProjectId(card);
            
            totalHours += hours;
            
            if ("NORMAL".equals(worklogType)) {
                normalHours += hours;
            } else if ("OVERTIME".equals(worklogType)) {
                overtimeHours += hours;
            }
            
            memberHoursMap.merge(memberId, hours, Double::sum);
            if (projectId != null) {
                projectHoursMap.merge(projectId, hours, Double::sum);
            }
        }
        
        totalDays = (int) worklogCards.stream()
            .map(this::extractWorklogDate)
            .distinct()
            .count();
        
        statistics.setTotalHours(totalHours);
        statistics.setNormalHours(normalHours);
        statistics.setOvertimeHours(overtimeHours);
        statistics.setTotalDays(totalDays);
        statistics.setMemberHoursMap(memberHoursMap);
        statistics.setProjectHoursMap(projectHoursMap);
        
        return statistics;
    }
    
    /**
     * 计算剩余工时
     */
    public double calculateRemaining(CardId taskCardId) {
        // 1. 查询任务卡片
        Optional<CardDTO> taskCard = cardRepository.findById(taskCardId);
        if (taskCard.isEmpty()) {
            return 0;
        }
        
        // 2. 获取预估工时
        double estimatedHours = extractEstimatedHours(taskCard.get());
        
        // 3. 查询已填报工时
        List<CardDTO> worklogCards = queryWorklogCardsByTask(taskCardId);
        double usedHours = worklogCards.stream()
            .mapToDouble(this::extractWorklogHours)
            .sum();
        
        // 4. 计算剩余工时
        return Math.max(0, estimatedHours - usedHours);
    }
}
```

**SettlementCalculator - 结算计算器：**
```java
@Component
public class SettlementCalculator {
    
    public SettlementData calculate(List<CardDTO> clockInCards, 
                                    List<CardDTO> worklogCards,
                                    OutsourcingConfigEntity config) {
        SettlementData data = new SettlementData();
        
        // 1. 统计工作天数
        int workDays = (int) clockInCards.stream()
            .map(this::extractClockInDate)
            .distinct()
            .count();
        data.setWorkDays(workDays);
        
        // 2. 统计工作时长
        double totalWorkHours = clockInCards.stream()
            .mapToDouble(this::extractWorkMinutes)
            .sum() / 60.0;
        data.setWorkHours(totalWorkHours);
        
        // 3. 统计加班时长
        double totalOvertimeHours = clockInCards.stream()
            .mapToDouble(this::extractOvertimeMinutes)
            .sum() / 60.0;
        data.setOvertimeHours(totalOvertimeHours);
        
        // 4. 统计旷工时长
        double totalAbsentHours = clockInCards.stream()
            .mapToDouble(this::extractAbsentMinutes)
            .sum() / 60.0;
        data.setAbsentHours(totalAbsentHours);
        
        // 5. 计算服务费
        PersonalServiceFeeConfig feeConfig = config.getPersonalServiceFeeConf();
        
        double baseFee = workDays * feeConfig.getBaseFeePerDay();
        double overtimeFee = totalOvertimeHours * feeConfig.getOvertimeFeePerHour();
        double deduction = totalAbsentHours * feeConfig.getDeductionPerHour();
        double totalFee = baseFee + overtimeFee - deduction;
        
        data.setBaseFee(baseFee);
        data.setOvertimeFee(overtimeFee);
        data.setDeduction(deduction);
        data.setTotalFee(totalFee);
        
        return data;
    }
}
```

---

## 6. API设计

### 6.1 工时管理API

**检查允许填报：**
```
POST /api/v1/workload/worklog/allow
Content-Type: application/json

Request:
{
  "worklogDate": "2026-02-04",
  "orgId": "org_123",
  "operatorId": "member_456",
  "operationType": "CREATE"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "allowed": true,
    "reason": "允许创建工时"
  }
}
```

**创建工时：**
```
POST /api/v1/workload/worklog
Content-Type: application/json

Request:
{
  "orgId": "org_123",
  "worklogDate": "2026-02-04",
  "worklogHours": 8.0,
  "memberId": "member_456",
  "taskId": "task_789",
  "worklogType": "NORMAL",
  "description": "开发用户登录功能"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "cardId": "1234567890"
  }
}
```

**更新工时：**
```
PUT /api/v1/workload/worklog/{cardId}
Content-Type: application/json

Request:
{
  "worklogHours": 6.5,
  "description": "修改工时数"
}

Response:
{
  "code": 0,
  "message": "success"
}
```

**删除工时：**
```
DELETE /api/v1/workload/worklog/{cardId}

Response:
{
  "code": 0,
  "message": "success"
}
```

**查询工时统计：**
```
POST /api/v1/workload/worklog/statistics
Content-Type: application/json

Request:
{
  "orgId": "org_123",
  "startDate": "2026-02-01",
  "endDate": "2026-02-28",
  "memberIds": ["member_456"],
  "projectIds": ["project_001"],
  "groupBy": "MEMBER"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "totalHours": 160.0,
    "normalHours": 144.0,
    "overtimeHours": 16.0,
    "totalDays": 20,
    "memberStatistics": [
      {
        "memberId": "member_456",
        "memberName": "张三",
        "totalHours": 160.0,
        "normalHours": 144.0,
        "overtimeHours": 16.0
      }
    ]
  }
}
```

### 6.2 考勤管理API

**批量导入考勤：**
```
POST /api/v1/workload/attendance/import
Content-Type: multipart/form-data

Parameters:
- file: 考勤数据文件（Excel/CSV）
- orgId: 组织ID
- operatorId: 操作人ID

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "batch": "batch_20260204_001",
    "status": "PROCESSING"
  }
}
```

**查询导入记录：**
```
GET /api/v1/workload/attendance/import/{batch}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "batch": "batch_20260204_001",
    "orgId": "org_123",
    "title": "2月份考勤数据",
    "operatorId": "member_456",
    "status": "SUCCESS",
    "totalCount": 100,
    "successCount": 98,
    "failedCount": 2,
    "errorMessage": null,
    "errorFilePath": "/errors/batch_20260204_001_errors.xlsx",
    "createdAt": "2026-02-04T10:00:00",
    "completedAt": "2026-02-04T10:05:00"
  }
}
```

**查询考勤记录：**
```
GET /api/v1/workload/attendance?memberId={memberId}&date={date}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "cardId": "1234567890",
    "attendanceDate": "2026-02-04",
    "memberId": "member_456",
    "memberName": "张三",
    "workMinutes": 480,
    "workHours": 8.0,
    "attendanceType": "NORMAL",
    "source": "IMPORT",
    "batch": "batch_20260204_001"
  }
}
```

### 6.3 打卡管理API

**签入打卡：**
```
POST /api/v1/workload/clockin/sign-in
Content-Type: application/json

Request:
{
  "memberId": "member_456",
  "orgId": "org_123"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "cardId": "1234567890",
    "signInTime": "2026-02-04T09:00:00",
    "status": "SIGNED_IN"
  }
}
```

**签出打卡：**
```
PUT /api/v1/workload/clockin/{cardId}/sign-out
Content-Type: application/json

Request:
{
  "memberId": "member_456",
  "orgId": "org_123"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "cardId": "1234567890",
    "signInTime": "2026-02-04T09:00:00",
    "signOutTime": "2026-02-04T18:00:00",
    "workMinutes": 480,
    "workHours": 8.0,
    "absentMinutes": 0,
    "overtimeMinutes": 0,
    "status": "SIGNED_OUT"
  }
}
```

**查询打卡详情：**
```
GET /api/v1/workload/clockin?memberId={memberId}&date={date}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "cardId": "1234567890",
    "clockInDate": "2026-02-04",
    "memberId": "member_456",
    "memberName": "张三",
    "signInTime": "2026-02-04T09:00:00",
    "signOutTime": "2026-02-04T18:00:00",
    "workMinutes": 480,
    "workHours": 8.0,
    "absentMinutes": 0,
    "overtimeMinutes": 0,
    "status": "SIGNED_OUT"
  }
}
```

### 6.4 结算管理API

**执行结算：**
```
POST /api/v1/workload/settlement
Content-Type: application/json

Request:
{
  "orgId": "org_123",
  "memberId": "member_456",
  "month": "2026-02"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "cardId": "1234567890",
    "month": "2026-02",
    "memberId": "member_456",
    "memberName": "张三",
    "workDays": 20,
    "workHours": 160.0,
    "overtimeHours": 10.0,
    "absentHours": 0,
    "baseFee": 10000.00,
    "overtimeFee": 1000.00,
    "deduction": 0,
    "totalFee": 11000.00,
    "status": "CONFIRMED"
  }
}
```

**撤销结算：**
```
PUT /api/v1/workload/settlement/{cardId}/revoke

Response:
{
  "code": 0,
  "message": "success"
}
```

**查询结算状态：**
```
GET /api/v1/workload/settlement/status?orgId={orgId}&memberId={memberId}&month={month}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": true,
    "cardId": "1234567890",
    "month": "2026-02",
    "totalFee": 11000.00
  }
}
```

### 6.5 配置管理API

**获取窗口期配置：**
```
GET /api/v1/workload/config/window?orgId={orgId}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "createWindow": {
      "left": -7,
      "right": 0,
      "disabled": false
    },
    "updateWindow": {
      "left": -30,
      "right": 0,
      "disabled": false
    },
    "deleteWindow": {
      "left": -7,
      "right": 0,
      "disabled": false
    }
  }
}
```

**更新窗口期配置：**
```
PUT /api/v1/workload/config/window
Content-Type: application/json

Request:
{
  "orgId": "org_123",
  "createWindow": {
    "left": -7,
    "right": 0,
    "disabled": false
  },
  "updateWindow": {
    "left": -30,
    "right": 0,
    "disabled": false
  },
  "deleteWindow": {
    "left": -7,
    "right": 0,
    "disabled": false
  }
}

Response:
{
  "code": 0,
  "message": "success"
}
```

---

## 7. 业务规则设计

### 7.1 工时自动锁定规则

**规则定义：**
```json
{
  "id": "worklog_auto_lock_rule",
  "name": "工时自动锁定规则",
  "cardTypeId": "worklog_card_type",
  "triggerEvent": "ON_SCHEDULE",
  "scheduleConfig": {
    "cron": "0 0 23 5 * ?",
    "description": "每月5号23点执行"
  },
  "condition": {
    "type": "ALWAYS",
    "description": "总是执行"
  },
  "actions": [
    {
      "actionType": "UPDATE_CARD",
      "updateConfig": {
        "fieldUpdates": [
          {
            "fieldId": "worklog_locked",
            "value": "LOCKED"
          }
        ],
        "filter": {
          "type": "DATE_RANGE",
          "fieldId": "worklog_date",
          "operator": "LESS_THAN_OR_EQUAL",
          "value": "CURRENT_DATE"
        }
      }
    }
  ],
  "enabled": true,
  "priority": 1
}
```

### 7.2 考勤生成工时规则

**规则定义：**
```json
{
  "id": "attendance_generate_worklog_rule",
  "name": "考勤自动生成工时规则",
  "cardTypeId": "attendance_card_type",
  "triggerEvent": "ON_CREATE",
  "condition": {
    "type": "FIELD_EQUALS",
    "fieldId": "attendance_type",
    "operator": "EQUALS",
    "value": "NORMAL"
  },
  "actions": [
    {
      "actionType": "CREATE_CARD",
      "createConfig": {
        "cardTypeId": "worklog_card_type",
        "fieldMappings": [
          {
            "sourceFieldId": "attendance_date",
            "targetFieldId": "worklog_date"
          },
          {
            "sourceFieldId": "attendance_member",
            "targetFieldId": "worklog_member"
          },
          {
            "sourceFieldId": "attendance_work_hours",
            "targetFieldId": "worklog_hours"
          }
        ],
        "linkConfig": {
          "linkTypeId": "worklog_to_attendance",
          "direction": "SOURCE_TO_TARGET"
        }
      }
    }
  ],
  "enabled": false,
  "priority": 2,
  "description": "根据考勤数据自动生成工时记录（可选功能）"
}
```

### 7.3 打卡生成考勤规则

**规则定义：**
```json
{
  "id": "clockin_generate_attendance_rule",
  "name": "打卡自动生成考勤规则",
  "cardTypeId": "clockin_card_type",
  "triggerEvent": "ON_FIELD_CHANGE",
  "listenFieldList": ["clockin_status"],
  "condition": {
    "type": "FIELD_EQUALS",
    "fieldId": "clockin_status",
    "operator": "EQUALS",
    "value": "SIGNED_OUT"
  },
  "actions": [
    {
      "actionType": "CREATE_CARD",
      "createConfig": {
        "cardTypeId": "attendance_card_type",
        "fieldMappings": [
          {
            "sourceFieldId": "clockin_date",
            "targetFieldId": "attendance_date"
          },
          {
            "sourceFieldId": "clockin_member",
            "targetFieldId": "attendance_member"
          },
          {
            "sourceFieldId": "clockin_work_minutes",
            "targetFieldId": "attendance_work_minutes"
          }
        ],
        "staticFields": [
          {
            "fieldId": "attendance_type",
            "value": "NORMAL"
          },
          {
            "fieldId": "attendance_source",
            "value": "CLOCK_IN"
          }
        ],
        "linkConfig": {
          "linkTypeId": "clockin_to_attendance",
          "direction": "SOURCE_TO_TARGET"
        }
      }
    }
  ],
  "enabled": true,
  "priority": 1
}
```

### 7.4 工时审核通知规则

**规则定义：**
```json
{
  "id": "worklog_approval_notification_rule",
  "name": "工时审核通知规则",
  "cardTypeId": "worklog_card_type",
  "triggerEvent": "ON_STATUS_MOVE",
  "targetStatusId": "worklog_submitted",
  "condition": {
    "type": "ALWAYS"
  },
  "actions": [
    {
      "actionType": "SEND_NOTIFICATION",
      "notificationConfig": {
        "notificationType": "WORKLOG_APPROVAL",
        "recipientType": "ROLE",
        "recipientRole": "WORKLOG_APPROVER",
        "title": "工时审核通知",
        "content": "成员 {{worklog_member}} 提交了工时审核申请，请及时处理",
        "linkToCard": true
      }
    }
  ],
  "enabled": true,
  "priority": 3
}
```


---

## 8. 事件设计

### 8.1 工时事件

**WorklogEvent - 工时事件基类：**
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @Type(value = WorklogCreatedEvent.class, name = "worklog.created"),
    @Type(value = WorklogUpdatedEvent.class, name = "worklog.updated"),
    @Type(value = WorklogDeletedEvent.class, name = "worklog.deleted"),
    @Type(value = WorklogLockedEvent.class, name = "worklog.locked"),
    @Type(value = WorklogApprovedEvent.class, name = "worklog.approved")
})
public abstract class WorklogEvent extends DomainEvent {
    private final String worklogCardId;
    private final String memberId;
    private final LocalDate worklogDate;
    private final double worklogHours;
    
    private static final String TOPIC = "kanban-workload-events";
    
    @Override
    public String getTopic() {
        return TOPIC;
    }
}
```

**WorklogCreatedEvent - 工时创建事件：**
```java
public class WorklogCreatedEvent extends WorklogEvent {
    private final String taskId;
    private final String worklogType;
    
    @Override
    public String getKey() {
        return worklogCardId;
    }
}
```

**WorklogLockedEvent - 工时锁定事件：**
```java
public class WorklogLockedEvent extends WorklogEvent {
    private final LocalDateTime lockTime;
    private final String lockReason;
    
    @Override
    public String getKey() {
        return worklogCardId;
    }
}
```

### 8.2 考勤事件

**AttendanceEvent - 考勤事件基类：**
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @Type(value = AttendanceImportedEvent.class, name = "attendance.imported"),
    @Type(value = AttendanceGeneratedEvent.class, name = "attendance.generated")
})
public abstract class AttendanceEvent extends DomainEvent {
    private final String attendanceCardId;
    private final String memberId;
    private final LocalDate attendanceDate;
    private final int workMinutes;
    
    private static final String TOPIC = "kanban-workload-events";
    
    @Override
    public String getTopic() {
        return TOPIC;
    }
}
```

**AttendanceImportedEvent - 考勤导入事件：**
```java
public class AttendanceImportedEvent extends AttendanceEvent {
    private final String batch;
    private final String source;
    
    @Override
    public String getKey() {
        return attendanceCardId;
    }
}
```

### 8.3 打卡事件

**ClockInEvent - 打卡事件基类：**
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @Type(value = SignInEvent.class, name = "clockin.signin"),
    @Type(value = SignOutEvent.class, name = "clockin.signout")
})
public abstract class ClockInEvent extends DomainEvent {
    private final String clockInCardId;
    private final String memberId;
    private final LocalDate clockInDate;
    
    private static final String TOPIC = "kanban-workload-events";
    
    @Override
    public String getTopic() {
        return TOPIC;
    }
}
```

**SignOutEvent - 签出事件：**
```java
public class SignOutEvent extends ClockInEvent {
    private final LocalDateTime signInTime;
    private final LocalDateTime signOutTime;
    private final int workMinutes;
    private final int absentMinutes;
    private final int overtimeMinutes;
    
    @Override
    public String getKey() {
        return clockInCardId;
    }
}
```

### 8.4 结算事件

**SettlementEvent - 结算事件基类：**
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @Type(value = SettlementConfirmedEvent.class, name = "settlement.confirmed"),
    @Type(value = SettlementRevokedEvent.class, name = "settlement.revoked")
})
public abstract class SettlementEvent extends DomainEvent {
    private final String settlementCardId;
    private final String memberId;
    private final String month;
    private final double totalFee;
    
    private static final String TOPIC = "kanban-workload-events";
    
    @Override
    public String getTopic() {
        return TOPIC;
    }
}
```

**SettlementConfirmedEvent - 结算确认事件：**
```java
public class SettlementConfirmedEvent extends SettlementEvent {
    private final int workDays;
    private final double workHours;
    private final double overtimeHours;
    private final double baseFee;
    private final double overtimeFee;
    private final double deduction;
    
    @Override
    public String getKey() {
        return settlementCardId;
    }
}
```

### 8.5 事件监听器

**CardEventListener - 卡片事件监听器：**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CardEventListener {
    private final WorklogService worklogService;
    private final StatisticsCacheService cacheService;
    
    @KafkaListener(topics = "kanban-card-events", groupId = "workload-service")
    public void handleCardEvent(String message) {
        try {
            CardEvent event = objectMapper.readValue(message, CardEvent.class);
            
            if (event instanceof CardUpdatedEvent) {
                handleCardUpdated((CardUpdatedEvent) event);
            } else if (event instanceof CardMovedEvent) {
                handleCardMoved((CardMovedEvent) event);
            }
        } catch (Exception e) {
            log.error("处理卡片事件失败", e);
        }
    }
    
    private void handleCardUpdated(CardUpdatedEvent event) {
        // 如果是工时卡片更新，清除相关缓存
        if ("worklog_card_type".equals(event.getCardTypeId())) {
            cacheService.invalidateWorklogCache(event.getCardId());
        }
    }
    
    private void handleCardMoved(CardMovedEvent event) {
        // 如果工时卡片状态变更，触发相应逻辑
        if ("worklog_card_type".equals(event.getCardTypeId())) {
            worklogService.handleStatusChange(event.getCardId(), event.getToStatusId());
        }
    }
}
```

---

## 9. 实施阶段规划

### 9.1 阶段一：基础架构搭建（2周）

**目标：** 搭建工时服务的基础架构和核心模块

**任务清单：**
1. ✅ 创建 workload-service 模块
2. ✅ 配置数据库连接（MySQL）
3. ✅ 配置 zgraph 连接
4. ✅ 配置 Kafka 连接
5. ✅ 创建基础包结构
6. ✅ 创建配置表（workload_config, outsourcing_config, workload_import_record, workload_statistics_cache）
7. ✅ 实现配置管理功能（ConfigRepository, ConfigService）
8. ✅ 实现基础工具类（ValidationResult, OperateWindow, LockConfig等）

**交付物：**
- workload-service 模块可运行
- 配置管理API可用
- 数据库表创建完成

### 9.2 阶段二：卡片类型定义（1周）

**目标：** 定义所有工时相关的卡片类型和关联类型

**任务清单：**
1. ✅ 定义工时卡片类型（WorklogCard）
2. ✅ 定义考勤卡片类型（AttendanceCard）
3. ✅ 定义打卡卡片类型（ClockInCard）
4. ✅ 定义请假卡片类型（LeaveCard）
5. ✅ 定义结算卡片类型（SettlementCard）
6. ✅ 定义关联类型（worklog_to_task, worklog_to_attendance等）
7. ✅ 定义价值流（worklog_stream, leave_approval_stream）
8. ✅ 编写Schema初始化脚本

**交付物：**
- 所有卡片类型定义完成
- Schema初始化脚本可执行
- 可通过Schema API创建卡片

### 9.3 阶段三：工时核心功能（2周）

**目标：** 实现工时填报、查询、统计等核心功能

**任务清单：**
1. ✅ 实现 WorklogService
   - createWorklog()
   - updateWorklog()
   - deleteWorklog()
   - queryWorklog()
2. ✅ 实现校验器
   - WindowPeriodValidator
   - LockDateValidator
   - AttendanceValidator
3. ✅ 实现计算器
   - WorklogCalculator
4. ✅ 实现 WorklogController
   - POST /api/v1/workload/worklog
   - PUT /api/v1/workload/worklog/{cardId}
   - DELETE /api/v1/workload/worklog/{cardId}
   - POST /api/v1/workload/worklog/statistics
5. ✅ 实现工时事件发布
6. ✅ 编写单元测试

**交付物：**
- 工时CRUD功能可用
- 窗口期校验生效
- 锁定日期校验生效
- 工时统计功能可用

### 9.4 阶段四：考勤管理功能（1.5周）

**目标：** 实现考勤数据导入、查询等功能

**任务清单：**
1. ✅ 实现 AttendanceService
   - importAttendance()
   - queryImportRecord()
   - queryAttendance()
2. ✅ 实现 AttendanceImportService
   - parseFile()
   - validateData()
   - batchCreateCards()
3. ✅ 实现 FileParser（支持Excel、CSV）
4. ✅ 实现 BatchProcessor（批量处理优化）
5. ✅ 实现 AttendanceController
   - POST /api/v1/workload/attendance/import
   - GET /api/v1/workload/attendance/import/{batch}
   - GET /api/v1/workload/attendance
6. ✅ 实现考勤事件发布
7. ✅ 编写单元测试

**交付物：**
- 考勤批量导入功能可用
- 导入记录查询功能可用
- 考勤数据查询功能可用

### 9.5 阶段五：打卡管理功能（1.5周）

**目标：** 实现外包人员打卡、考勤生成等功能

**任务清单：**
1. ✅ 实现 ClockInService
   - signIn()
   - signOut()
   - queryClockIn()
2. ✅ 实现计算器
   - OvertimeCalculator
   - AbsenteeismCalculator
3. ✅ 实现 ClockInController
   - POST /api/v1/workload/clockin/sign-in
   - PUT /api/v1/workload/clockin/{cardId}/sign-out
   - GET /api/v1/workload/clockin
4. ✅ 实现打卡生成考勤规则
5. ✅ 实现打卡事件发布
6. ✅ 编写单元测试

**交付物：**
- 签入签出功能可用
- 工作时长自动计算
- 旷工时长自动计算
- 加班时长自动计算
- 考勤卡片自动生成

### 9.6 阶段六：结算管理功能（2周）

**目标：** 实现月度结算、服务费计算等功能

**任务清单：**
1. ✅ 实现 SettlementService
   - executeSettlement()
   - revokeSettlement()
   - querySettlement()
2. ✅ 实现 SettlementCalculator
   - calculate()
   - calculatePersonalServiceFee()
   - allocateProjectServiceFee()
3. ✅ 实现 SettlementController
   - POST /api/v1/workload/settlement
   - PUT /api/v1/workload/settlement/{cardId}/revoke
   - GET /api/v1/workload/settlement/status
4. ✅ 实现结算事件发布
5. ✅ 编写单元测试

**交付物：**
- 月度结算功能可用
- 服务费计算准确
- 结算撤销功能可用

### 9.7 阶段七：业务规则配置（1周）

**目标：** 配置所有业务规则，实现自动化流程

**任务清单：**
1. ✅ 配置工时自动锁定规则
2. ✅ 配置打卡生成考勤规则
3. ✅ 配置工时审核通知规则
4. ✅ 配置考勤生成工时规则（可选）
5. ✅ 测试规则执行
6. ✅ 优化规则性能

**交付物：**
- 所有业务规则配置完成
- 规则执行正常
- 规则日志可查询

### 9.8 阶段八：定时任务与优化（1周）

**目标：** 实现定时任务，优化性能和用户体验

**任务清单：**
1. ✅ 实现定时任务
   - DailyInitTask（每日初始化）
   - LockDateTask（锁定日期检查）
   - SettlementTask（月度结算提醒）
2. ✅ 实现统计缓存
   - StatisticsCacheService
   - 缓存失效策略
3. ✅ 性能优化
   - 批量查询优化
   - 索引优化
   - 缓存优化
4. ✅ 前端集成
   - 工时填报页面
   - 考勤管理页面
   - 打卡页面
   - 结算管理页面
5. ✅ 集成测试
6. ✅ 用户验收测试

**交付物：**
- 定时任务正常运行
- 统计查询性能优化
- 前端功能完整
- 系统整体可用

### 9.9 总体时间规划

| 阶段 | 任务 | 工期 | 依赖 |
|-----|------|------|------|
| 阶段一 | 基础架构搭建 | 2周 | 无 |
| 阶段二 | 卡片类型定义 | 1周 | 阶段一 |
| 阶段三 | 工时核心功能 | 2周 | 阶段二 |
| 阶段四 | 考勤管理功能 | 1.5周 | 阶段二 |
| 阶段五 | 打卡管理功能 | 1.5周 | 阶段二 |
| 阶段六 | 结算管理功能 | 2周 | 阶段五 |
| 阶段七 | 业务规则配置 | 1周 | 阶段三、四、五、六 |
| 阶段八 | 定时任务与优化 | 1周 | 阶段七 |
| **总计** | | **12周** | |

---

## 10. 技术难点与解决方案

### 10.1 难点一：卡片查询性能

**问题描述：**
- zgraph图数据库擅长关联查询，但聚合统计查询性能可能不如关系型数据库
- 工时统计需要按成员、项目、时间等多维度聚合
- 大数据量下查询可能较慢

**解决方案：**
1. **引入统计缓存表**
   - 在MySQL中创建 workload_statistics_cache 表
   - 定时或事件触发更新缓存
   - 查询时优先从缓存读取

2. **使用Redis缓存热点数据**
   - 缓存当月工时统计
   - 缓存成员工时汇总
   - 设置合理的过期时间

3. **优化查询策略**
   - 使用zgraph的批量查询接口
   - 减少不必要的字段查询
   - 使用分页查询

4. **异步计算**
   - 复杂统计使用异步任务
   - 提供进度查询接口
   - 完成后通知用户

### 10.2 难点二：批量导入性能

**问题描述：**
- 考勤数据可能有数千条记录
- 逐条创建卡片性能较差
- 需要保证数据一致性

**解决方案：**
1. **使用批量创建接口**
   - 使用 CardRepository.batchCreate()
   - 一次性创建多张卡片
   - 减少网络往返次数

2. **分批处理**
   - 将大文件拆分为多个批次
   - 每批处理100-500条记录
   - 批次间添加短暂延迟

3. **异步处理**
   - 导入任务放入队列异步处理
   - 立即返回批次号
   - 用户可查询导入进度

4. **错误处理**
   - 记录失败记录
   - 生成错误文件
   - 支持重新导入失败记录

### 10.3 难点三：窗口期校验复杂度

**问题描述：**
- 窗口期规则复杂（创建、修改、删除窗口不同）
- 锁定日期规则需要考虑月份边界
- 管理员白名单需要权限判断

**解决方案：**
1. **抽象校验器接口**
   - 定义统一的 Validator 接口
   - 每种规则独立实现
   - 支持规则组合

2. **配置化规则**
   - 规则参数存储在配置表
   - 支持动态调整
   - 无需重启服务

3. **缓存配置数据**
   - 配置数据缓存到Redis
   - 配置变更时清除缓存
   - 减少数据库查询

4. **提供校验预检接口**
   - 用户填报前先调用校验接口
   - 前端提前提示用户
   - 提升用户体验

### 10.4 难点四：工时与考勤数据一致性

**问题描述：**
- 工时数据和考勤数据需要保持一致
- 考勤数据变更时工时数据如何处理
- 工时总和不能超过考勤时长

**解决方案：**
1. **建立卡片关联**
   - 工时卡片关联考勤卡片
   - 通过关联关系查询
   - 保证数据可追溯

2. **事件驱动同步**
   - 考勤变更时发布事件
   - 工时服务监听事件
   - 自动更新相关工时数据

3. **校验规则**
   - 创建工时时校验考勤数据
   - 计算工时总和
   - 超出时拒绝创建

4. **数据修复工具**
   - 提供数据一致性检查工具
   - 发现不一致时生成报告
   - 支持批量修复

### 10.5 难点五：结算数据准确性

**问题描述：**
- 结算涉及多种数据源（打卡、工时、请假）
- 计算规则复杂（基础费、加班费、扣款）
- 结算后数据不可变更

**解决方案：**
1. **多数据源聚合**
   - 通过卡片关联查询所有相关数据
   - 统一计算逻辑
   - 记录计算过程

2. **预结算机制**
   - 提供预结算接口
   - 用户可预览结算结果
   - 确认无误后正式结算

3. **结算快照**
   - 结算时保存所有原始数据
   - 记录计算公式和参数
   - 支持结算数据审计

4. **撤销机制**
   - 支持结算撤销
   - 撤销后可重新结算
   - 记录撤销原因和操作人

### 10.6 难点六：业务规则引擎集成

**问题描述：**
- 业务规则引擎是现有系统能力
- 需要适配工时业务场景
- 规则执行性能需要保证

**解决方案：**
1. **规则模板化**
   - 定义工时领域的规则模板
   - 简化规则配置
   - 提供规则示例

2. **规则测试工具**
   - 提供规则测试接口
   - 模拟规则执行
   - 验证规则正确性

3. **规则性能优化**
   - 避免规则中的复杂查询
   - 使用缓存数据
   - 异步执行非关键规则

4. **规则监控**
   - 记录规则执行日志
   - 监控规则执行时间
   - 告警规则执行失败

---

## 11. 总结

### 11.1 设计亮点

1. **完全卡片化**：所有业务数据都以卡片形式存储，充分利用现有架构能力
2. **高度灵活**：通过Schema定义实现配置化，无需修改代码即可调整业务规则
3. **事件驱动**：通过事件实现模块解耦，易于扩展和维护
4. **规则引擎**：利用业务规则引擎实现复杂逻辑，降低代码复杂度
5. **性能优化**：通过缓存、批量处理等手段保证系统性能

### 11.2 与原需求的差异

| 维度 | 原需求 | 新设计 | 说明 |
|-----|-------|--------|------|
| 数据存储 | MySQL宽表 | zgraph卡片 | 适配新架构 |
| 数据关联 | 外键 | 卡片关联 | 更灵活 |
| 业务规则 | 硬编码 | 规则引擎 | 可配置化 |
| 窗口期控制 | 配置+代码 | 规则引擎 | 统一管理 |
| 考勤导入 | 直接入库 | 创建卡片 | 保持一致性 |
| 结算流程 | 存储记录 | 创建卡片 | 可追溯 |

### 11.3 后续扩展方向

1. **移动端支持**：开发移动端工时填报和打卡应用
2. **AI辅助**：利用AI预测工时、识别异常工时
3. **报表系统**：提供更丰富的工时报表和可视化
4. **第三方集成**：对接第三方考勤系统、财务系统
5. **多租户支持**：支持多组织独立配置和数据隔离

---

**文档结束**


---

## 12. Schema初始化方案

### 12.1 初始化架构设计

**核心原则：**
1. **Schema定义归属**：Schema定义文件存储在 workload-service 中
2. **Schema管理职责**：Schema的CRUD操作由 schema-service 统一管理
3. **初始化时机**：workload-service 启动时自动检查并初始化
4. **服务通信**：通过 schema-api（OpenFeign）调用 schema-service

**架构图：**
```
┌─────────────────────────────────────────────────────────┐
│                  workload-service                        │
│  ┌────────────────────────────────────────────────┐    │
│  │  WorkloadSchemaInitializer                      │    │
│  │  - 启动时检查Schema是否已初始化                  │    │
│  │  - 读取Schema定义文件（JSON/Java）              │    │
│  │  - 调用SchemaApiClient创建Schema                │    │
│  └────────────────┬───────────────────────────────┘    │
│                   │                                      │
│  ┌────────────────▼───────────────────────────────┐    │
│  │  resources/schema/                              │    │
│  │  - worklog-card-type.json                       │    │
│  │  - attendance-card-type.json                    │    │
│  │  - clockin-card-type.json                       │    │
│  │  - leave-card-type.json                         │    │
│  │  - settlement-card-type.json                    │    │
│  │  - link-types.json                              │    │
│  │  - value-streams.json                           │    │
│  │  - biz-rules.json                               │    │
│  └─────────────────────────────────────────────────┘    │
└───────────────────────┬─────────────────────────────────┘
                        │ OpenFeign
                        │ (schema-api)
┌───────────────────────▼─────────────────────────────────┐
│                  schema-service                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │  SchemaService                                   │   │
│  │  - createSchema()                                │   │
│  │  - updateSchema()                                │   │
│  │  - deleteSchema()                                │   │
│  │  - 校验系统内置Schema不可删除                    │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │  SchemaRepository (MySQL)                        │   │
│  │  - schema_definition表                           │   │
│  │  - system_built_in字段标识                       │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 12.2 模块依赖关系

**workload-service 的 pom.xml：**
```xml
<dependencies>
    <!-- 依赖 schema-api 进行服务调用 -->
    <dependency>
        <groupId>dev.planka</groupId>
        <artifactId>schema-api</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- 依赖 card-api 进行卡片操作 -->
    <dependency>
        <groupId>dev.planka</groupId>
        <artifactId>card-api</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- 依赖 domain 模块 -->
    <dependency>
        <groupId>dev.planka</groupId>
        <artifactId>kanban-domain</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Spring Cloud OpenFeign -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    
    <!-- 其他依赖... -->
</dependencies>
```

### 12.3 Schema定义文件

**目录结构：**
```
workload-service/
└── src/main/resources/
    └── schema/
        ├── card-types/
        │   ├── worklog-card-type.json
        │   ├── attendance-card-type.json
        │   ├── clockin-card-type.json
        │   ├── leave-card-type.json
        │   └── settlement-card-type.json
        ├── link-types/
        │   ├── worklog-to-task.json
        │   ├── worklog-to-attendance.json
        │   ├── clockin-to-attendance.json
        │   ├── leave-to-attendance.json
        │   └── settlement-to-clockin.json
        ├── value-streams/
        │   ├── worklog-stream.json
        │   └── leave-approval-stream.json
        └── biz-rules/
            ├── worklog-auto-lock-rule.json
            ├── clockin-generate-attendance-rule.json
            └── worklog-approval-notification-rule.json
```

**示例：worklog-card-type.json**
```json
{
  "schemaType": "CARD_TYPE",
  "schemaSubType": "ENTITY_CARD_TYPE",
  "id": "workload_worklog_card_type",
  "code": "WORKLOG",
  "name": "工时卡片",
  "icon": "icon-clock-circle",
  "color": "#1890ff",
  "description": "记录成员在任务或项目上的工作时长",
  "systemBuiltIn": true,
  "module": "WORKLOAD",
  "fieldConfigs": [
    {
      "fieldId": "worklog_date",
      "fieldType": "DATE",
      "name": "工时日期",
      "required": true,
      "readonly": false
    },
    {
      "fieldId": "worklog_hours",
      "fieldType": "NUMBER",
      "name": "工时数（小时）",
      "required": true,
      "numberConfig": {
        "precision": 2,
        "min": 0,
        "max": 24,
        "unit": "小时"
      }
    }
    // ... 其他字段
  ],
  "linkConfigs": [
    {
      "linkTypeId": "workload_worklog_to_task",
      "direction": "SOURCE",
      "required": true
    }
  ],
  "valueStreamId": "workload_worklog_stream",
  "defaultStatusId": "worklog_draft"
}
```

### 12.4 Schema初始化器实现

**WorkloadSchemaInitializer.java：**
```java
package dev.planka.workload.config;

import dev.planka.schema.api.SchemaApiClient;
import dev.planka.schema.api.dto.CreateSchemaRequest;
import result.dev.planka.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 工时服务Schema初始化器
 * 在服务启动时自动检查并初始化工时相关的Schema定义
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkloadSchemaInitializer implements ApplicationRunner {
    
    private final SchemaApiClient schemaApiClient;
    private final ObjectMapper objectMapper;
    private final WorkloadProperties workloadProperties;
    
    private static final String SCHEMA_BASE_PATH = "classpath:schema/";
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化工时服务Schema定义...");
        
        try {
            // 1. 检查是否需要初始化
            if (!needsInitialization()) {
                log.info("工时服务Schema已初始化，跳过");
                return;
            }
            
            // 2. 按顺序初始化Schema
            initializeCardTypes();
            initializeLinkTypes();
            initializeValueStreams();
            initializeBizRules();
            
            log.info("工时服务Schema初始化完成");
            
        } catch (Exception e) {
            log.error("工时服务Schema初始化失败", e);
            // 根据配置决定是否抛出异常导致服务启动失败
            if (workloadProperties.isFailOnInitError()) {
                throw new RuntimeException("Schema初始化失败", e);
            }
        }
    }
    
    /**
     * 检查是否需要初始化
     * 通过查询一个标志性的Schema来判断
     */
    private boolean needsInitialization() {
        try {
            Result<?> result = schemaApiClient.getSchemaById("workload_worklog_card_type");
            return !result.isSuccess();
        } catch (Exception e) {
            log.warn("检查Schema初始化状态失败，将执行初始化", e);
            return true;
        }
    }
    
    /**
     * 初始化卡片类型
     */
    private void initializeCardTypes() throws IOException {
        log.info("初始化卡片类型...");
        
        List<String> cardTypeFiles = List.of(
            "card-types/worklog-card-type.json",
            "card-types/attendance-card-type.json",
            "card-types/clockin-card-type.json",
            "card-types/leave-card-type.json",
            "card-types/settlement-card-type.json"
        );
        
        for (String file : cardTypeFiles) {
            createSchemaFromFile(file);
        }
        
        log.info("卡片类型初始化完成");
    }
    
    /**
     * 初始化关联类型
     */
    private void initializeLinkTypes() throws IOException {
        log.info("初始化关联类型...");
        
        List<String> linkTypeFiles = List.of(
            "link-types/worklog-to-task.json",
            "link-types/worklog-to-attendance.json",
            "link-types/clockin-to-attendance.json",
            "link-types/leave-to-attendance.json",
            "link-types/settlement-to-clockin.json"
        );
        
        for (String file : linkTypeFiles) {
            createSchemaFromFile(file);
        }
        
        log.info("关联类型初始化完成");
    }
    
    /**
     * 初始化价值流
     */
    private void initializeValueStreams() throws IOException {
        log.info("初始化价值流...");
        
        List<String> streamFiles = List.of(
            "value-streams/worklog-stream.json",
            "value-streams/leave-approval-stream.json"
        );
        
        for (String file : streamFiles) {
            createSchemaFromFile(file);
        }
        
        log.info("价值流初始化完成");
    }
    
    /**
     * 初始化业务规则
     */
    private void initializeBizRules() throws IOException {
        log.info("初始化业务规则...");
        
        List<String> ruleFiles = List.of(
            "biz-rules/worklog-auto-lock-rule.json",
            "biz-rules/clockin-generate-attendance-rule.json",
            "biz-rules/worklog-approval-notification-rule.json"
        );
        
        for (String file : ruleFiles) {
            createSchemaFromFile(file);
        }
        
        log.info("业务规则初始化完成");
    }
    
    /**
     * 从文件创建Schema
     */
    private void createSchemaFromFile(String filePath) throws IOException {
        String fullPath = SCHEMA_BASE_PATH + filePath;
        
        try {
            // 读取文件内容
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource(fullPath);
            
            if (!resource.exists()) {
                log.warn("Schema定义文件不存在: {}", fullPath);
                return;
            }
            
            // 解析JSON
            String content = new String(resource.getInputStream().readAllBytes());
            CreateSchemaRequest request = objectMapper.readValue(content, CreateSchemaRequest.class);
            
            // 调用schema-service创建Schema
            Result<?> result = schemaApiClient.createSchema(request);
            
            if (result.isSuccess()) {
                log.info("Schema创建成功: {}", filePath);
            } else {
                log.error("Schema创建失败: {}, 错误: {}", filePath, result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("创建Schema失败: {}", filePath, e);
            throw e;
        }
    }
}
```

**WorkloadProperties.java：**
```java
package dev.planka.workload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "workload")
public class WorkloadProperties {
    
    /**
     * Schema初始化失败时是否导致服务启动失败
     */
    private boolean failOnInitError = true;
    
    /**
     * 是否启用自动初始化
     */
    private boolean autoInitialize = true;
    
    /**
     * 工时服务模块标识
     */
    private String module = "WORKLOAD";
}
```

### 12.5 Schema API Client

**SchemaApiClient 接口（在 schema-api 模块中）：**
```java
package dev.planka.schema.api;

import result.dev.planka.common.Result;
import dev.planka.schema.api.dto.CreateSchemaRequest;
import dev.planka.schema.api.dto.SchemaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Schema服务API客户端
 */
@FeignClient(name = "schema-service", path = "/api/v1/schema")
public interface SchemaApiClient {
    
    /**
     * 创建Schema
     */
    @PostMapping
    Result<String> createSchema(@RequestBody CreateSchemaRequest request);
    
    /**
     * 根据ID查询Schema
     */
    @GetMapping("/{schemaId}")
    Result<SchemaDTO> getSchemaById(@PathVariable String schemaId);
    
    /**
     * 更新Schema
     */
    @PutMapping("/{schemaId}")
    Result<Void> updateSchema(@PathVariable String schemaId, 
                              @RequestBody CreateSchemaRequest request);
    
    /**
     * 删除Schema
     */
    @DeleteMapping("/{schemaId}")
    Result<Void> deleteSchema(@PathVariable String schemaId);
    
    /**
     * 查询组织的所有Schema
     */
    @GetMapping("/org/{orgId}")
    Result<List<SchemaDTO>> listSchemasByOrg(@PathVariable String orgId,
                                              @RequestParam(required = false) String schemaType,
                                              @RequestParam(required = false) String module);
}
```

### 12.6 Schema Service 保护逻辑

**在 schema-service 中增加系统内置Schema的保护：**

```java
package dev.planka.schema.service;

import result.dev.planka.common.Result;
import model.dev.planka.schema.SchemaEntity;
import repository.dev.planka.schema.SchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaService {
    
    private final SchemaRepository schemaRepository;
    
    /**
     * 删除Schema
     */
    @Transactional
    public Result<Void> deleteSchema(String schemaId, String operatorId) {
        // 1. 查询Schema
        SchemaEntity schema = schemaRepository.findById(schemaId);
        if (schema == null) {
            return Result.fail("Schema不存在");
        }
        
        // 2. 检查是否为系统内置Schema
        if (Boolean.TRUE.equals(schema.getSystemBuiltIn())) {
            log.warn("尝试删除系统内置Schema: {}, 操作人: {}", schemaId, operatorId);
            return Result.fail("系统内置配置不允许删除");
        }
        
        // 3. 检查是否有依赖
        if (hasReferences(schemaId)) {
            return Result.fail("该Schema被其他配置引用，无法删除");
        }
        
        // 4. 执行删除（软删除）
        schema.setDeletedAt(LocalDateTime.now());
        schemaRepository.update(schema);
        
        log.info("Schema删除成功: {}, 操作人: {}", schemaId, operatorId);
        return Result.success();
    }
    
    /**
     * 更新Schema
     */
    @Transactional
    public Result<Void> updateSchema(String schemaId, CreateSchemaRequest request, String operatorId) {
        // 1. 查询现有Schema
        SchemaEntity existing = schemaRepository.findById(schemaId);
        if (existing == null) {
            return Result.fail("Schema不存在");
        }
        
        // 2. 检查是否为系统内置Schema
        if (Boolean.TRUE.equals(existing.getSystemBuiltIn())) {
            // 系统内置Schema只允许修改特定字段
            return validateSystemSchemaUpdate(existing, request);
        }
        
        // 3. 执行更新
        SchemaEntity updated = buildUpdatedSchema(existing, request);
        updated.setUpdatedBy(operatorId);
        updated.setUpdatedAt(LocalDateTime.now());
        schemaRepository.update(updated);
        
        log.info("Schema更新成功: {}, 操作人: {}", schemaId, operatorId);
        return Result.success();
    }
    
    /**
     * 校验系统内置Schema的更新
     * 系统内置Schema只允许修改：描述、是否启用等非核心字段
     */
    private Result<Void> validateSystemSchemaUpdate(SchemaEntity existing, CreateSchemaRequest request) {
        // 不允许修改的字段
        if (!existing.getSchemaType().equals(request.getSchemaType())) {
            return Result.fail("系统内置Schema不允许修改类型");
        }
        if (!existing.getCode().equals(request.getCode())) {
            return Result.fail("系统内置Schema不允许修改编码");
        }
        
        // 允许修改的字段：描述、启用状态等
        log.info("更新系统内置Schema: {}", existing.getId());
        return Result.success();
    }
}
```

### 12.7 数据库Schema变更

**增加系统内置标识字段：**
```sql
-- V1.0__add_system_builtin_flag.sql

ALTER TABLE schema_definition 
ADD COLUMN system_built_in BOOLEAN DEFAULT FALSE COMMENT '是否系统内置',
ADD COLUMN module VARCHAR(50) COMMENT '所属模块（如：WORKLOAD、CRM等）';

-- 创建索引
CREATE INDEX idx_system_built_in ON schema_definition(org_id, system_built_in, module);
CREATE INDEX idx_module ON schema_definition(module);

-- 为已存在的系统内置Schema打标记（如果有）
UPDATE schema_definition 
SET system_built_in = TRUE, module = 'CORE'
WHERE id IN ('system_card_type_1', 'system_card_type_2');
```

### 12.8 配置文件

**application.yml：**
```yaml
workload:
  # Schema初始化配置
  auto-initialize: true
  fail-on-init-error: true
  module: WORKLOAD

# Feign配置
feign:
  client:
    config:
      schema-service:
        connect-timeout: 5000
        read-timeout: 10000
        logger-level: full

# 服务发现配置
spring:
  cloud:
    consul:
      discovery:
        service-name: workload-service
        health-check-path: /actuator/health
```

### 12.9 初始化流程图

```
┌─────────────────────────────────────────────────────────────┐
│  workload-service 启动                                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  WorkloadSchemaInitializer.run()                            │
│  (实现 ApplicationRunner 接口)                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  检查是否需要初始化                                          │
│  - 调用 schemaApiClient.getSchemaById("workload_worklog_card_type") │
│  - 如果返回成功，说明已初始化，跳过                          │
│  - 如果返回失败，说明未初始化，继续                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  按顺序初始化Schema                                          │
│  1. initializeCardTypes()                                   │
│     - worklog-card-type.json                                │
│     - attendance-card-type.json                             │
│     - clockin-card-type.json                                │
│     - leave-card-type.json                                  │
│     - settlement-card-type.json                             │
│  2. initializeLinkTypes()                                   │
│     - worklog-to-task.json                                  │
│     - clockin-to-attendance.json                            │
│     - ...                                                   │
│  3. initializeValueStreams()                                │
│     - worklog-stream.json                                   │
│     - leave-approval-stream.json                            │
│  4. initializeBizRules()                                    │
│     - worklog-auto-lock-rule.json                           │
│     - clockin-generate-attendance-rule.json                 │
│     - ...                                                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  对每个Schema文件：                                          │
│  1. 读取JSON文件内容                                         │
│  2. 解析为 CreateSchemaRequest                              │
│  3. 调用 schemaApiClient.createSchema(request)              │
│  4. 记录成功/失败日志                                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  schema-service 接收请求                                     │
│  1. 校验Schema定义                                           │
│  2. 设置 system_built_in = true                             │
│  3. 设置 module = "WORKLOAD"                                │
│  4. 保存到 schema_definition 表                             │
│  5. 返回成功/失败结果                                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  初始化完成                                                  │
│  - 记录日志                                                  │
│  - workload-service 正常启动                                │
└─────────────────────────────────────────────────────────────┘
```

### 12.10 多组织支持

**问题：** 不同组织可能需要不同的工时配置

**解决方案：**

1. **Schema定义不区分组织**
   - 卡片类型、关联类型等Schema定义是全局的
   - 所有组织共享相同的Schema定义
   - 通过 `system_built_in = true` 标识为系统内置

2. **组织级配置独立**
   - 窗口期配置、锁定日期配置等存储在 `workload_config` 表
   - 通过 `org_id` 区分不同组织
   - 每个组织可以独立配置

3. **组织启用工时服务**
   ```java
   @PostMapping("/api/v1/workload/enable")
   public Result<Void> enableWorkloadService(@RequestParam String orgId) {
       // 1. 检查Schema是否已初始化（全局检查）
       if (!isSchemaInitialized()) {
           return Result.fail("工时服务Schema未初始化，请联系管理员");
       }
       
       // 2. 为组织创建默认配置
       workloadConfigService.createDefaultConfig(orgId);
       
       // 3. 标记组织已启用工时服务
       orgModuleService.enableModule(orgId, "WORKLOAD");
       
       return Result.success();
   }
   ```

### 12.11 Schema版本管理

**问题：** Schema定义可能需要升级

**解决方案：**

1. **Schema版本号**
   ```json
   {
     "id": "workload_worklog_card_type",
     "version": "1.0.0",
     "systemBuiltIn": true,
     "module": "WORKLOAD"
   }
   ```

2. **版本升级检查**
   ```java
   private boolean needsUpgrade(String schemaId, String currentVersion) {
       Result<SchemaDTO> result = schemaApiClient.getSchemaById(schemaId);
       if (!result.isSuccess()) {
           return false;
       }
       
       SchemaDTO schema = result.getData();
       String existingVersion = schema.getVersion();
       
       return compareVersion(currentVersion, existingVersion) > 0;
   }
   ```

3. **增量升级**
   ```java
   private void upgradeSchema(String schemaId, String fromVersion, String toVersion) {
       // 读取升级脚本
       String upgradePath = String.format("schema/upgrades/%s_%s_to_%s.json", 
           schemaId, fromVersion, toVersion);
       
       // 执行升级
       // ...
   }
   ```

### 12.12 总结

**初始化方案的关键点：**

1. ✅ **Schema定义归属**：定义文件存储在 workload-service 的 resources 目录
2. ✅ **Schema管理职责**：CRUD操作由 schema-service 统一管理
3. ✅ **初始化时机**：workload-service 启动时自动检查并初始化
4. ✅ **服务通信**：通过 OpenFeign 调用 schema-service API
5. ✅ **系统内置标识**：通过 `system_built_in` 字段标识，防止误删
6. ✅ **模块标识**：通过 `module` 字段标识所属模块
7. ✅ **幂等性保证**：初始化前检查是否已存在，避免重复创建
8. ✅ **错误处理**：初始化失败可配置是否导致服务启动失败
9. ✅ **多组织支持**：Schema全局共享，配置按组织隔离
10. ✅ **版本管理**：支持Schema版本升级

**优势：**
- 职责清晰：Schema管理集中在 schema-service
- 业务自包含：workload-service 包含自己的Schema定义
- 易于维护：Schema定义以JSON文件形式存储，易于版本控制
- 灵活扩展：支持多组织、版本升级等场景

