# Workload 工时服务需求文档

## 1. 系统概述

### 1.1 服务简介
Workload工时服务是一个企业级工时管理系统，提供工时数据的采集、计算、统计和管理功能。系统支持考勤数据导入、工时填报、工时审核、外包人员管理等核心业务场景。

### 1.2 技术架构
- **开发语言**: Java
- **框架**: Spring Boot + Spring WebFlux (响应式编程)
- **数据库**: JPA/Hibernate
- **消息队列**: Kafka
- **API风格**: RESTful API

### 1.3 核心模块
1. 工时数据管理模块
2. 考勤数据管理模块
3. 外包人员管理模块
4. 工时计算与统计模块
5. 窗口期控制模块
6. 数据修复与同步模块

---

## 2. 核心功能模块

### 2.1 工时数据管理

#### 2.1.1 工时填报
**功能描述**: 用户可以填报每日工时数据，系统会根据配置的窗口期和规则进行校验。

**业务规则**:
- 支持新建、修改、删除工时数据
- 受窗口期限制（创建窗口、修改窗口、删除窗口）
- 受锁定日期限制（每月指定日期后不可修改历史数据）
- 支持工时管理员白名单（管理员不受窗口期限制）
- 支持工时上限检查（每日工时不超过配置的最大值）

**涉及接口**:
- `POST /api/v1/wl/allow` - 检查是否允许填报工时
- 工时数据通过指标服务进行CRUD操作

#### 2.1.2 工时数据校验
**功能描述**: 在提交工时数据时进行多维度校验。

**校验项**:
1. **窗口期校验**: 
   - 创建窗口期（createWindow）：控制可以新建工时的时间范围
   - 修改窗口期（updateWindow）：控制可以修改工时的时间范围
   - 删除窗口期（deleteWindow）：控制可以删除工时的时间范围

2. **锁定日期校验**:
   - 每月指定日期（lockDay）和时间（lockHour）后，不允许修改该日期之前的工时数据
   - 例如：每月5号23点锁定，则5号23点后不能修改5号及之前的工时

3. **考勤数据校验**:
   - 如果启用考勤同步，需要检查是否有对应日期的考勤数据
   - 工时总和不能超过当天考勤时长

4. **工时管理员白名单**:
   - 配置的工时管理员可以不受窗口期限制
   - 需要开启管理员白名单功能（managerWhiteListEnable）

#### 2.1.3 工时数据修复
**功能描述**: 针对工时指标新增字段或数据异常，提供批量修复功能。

**涉及接口**:
- `PUT /api/v1/wl/fix` - 修复指定组织、时间范围、字段的工时数据
- `GET /api/v1/wl/forceUpdate/fix` - 强制更新指定范围的工时数据

**参数说明**:
- orgId: 组织ID
- columns: 需要更新的列（逗号分隔）
- since/until: 时间范围
- comp: 比较字段（默认为inner_create_time）


### 2.2 考勤数据管理

#### 2.2.1 考勤数据导入
**功能描述**: 支持通过文件批量导入考勤数据。

**涉及接口**:
- `POST /api/v1/wl/upload` - 上传考勤数据文件

**参数说明**:
- file: 考勤数据文件（MultipartFile）
- orgId: 组织ID
- memberId: 操作人ID

**业务流程**:
1. 上传文件到临时目录
2. 解析文件内容
3. 校验数据格式
4. 批量导入数据库
5. 生成导入记录（batch批次号）
6. 返回导入结果

**导入记录状态**:
- 0: 导入中
- 1: 成功
- 2: 失败

#### 2.2.2 考勤记录查询
**功能描述**: 查询考勤数据导入记录。

**涉及接口**:
- `GET /api/v1/wl/records?orgId={orgId}` - 查询组织的所有导入记录
- `GET /api/v1/wl/records/{batch}` - 查询指定批次的导入记录

#### 2.2.3 考勤数据清理
**功能描述**: 删除指定批次的考勤数据。

**涉及接口**:
- `DELETE /api/v1/wl?batch={batch}` - 清理指定批次的考勤数据

#### 2.2.4 Mock考勤数据
**功能描述**: 用于测试环境生成模拟考勤数据。

**涉及接口**:
- `POST /api/v1/wl/mock` - 生成Mock考勤数据

### 2.3 外包人员管理模块

#### 2.3.1 考勤打卡
**功能描述**: 外包人员可以进行签入签出打卡，系统自动计算工作时长、旷工时长、加班时长等。

**涉及接口**:
- `POST /api/v1/attendances/sign-in?memberId={memberId}` - 签入打卡
- `PUT /api/v1/attendances/{recordId}/sign-out?memberId={memberId}` - 签出打卡
- `GET /api/v1/attendances?memberId={memberId}` - 查询当天考勤详情

**业务规则**:
1. **签入规则**:
   - 验证人员是否在外包人员白名单中
   - 记录签入时间
   - 生成考勤记录ID

2. **签出规则**:
   - 验证签入记录是否存在
   - 验证签入和签出是否在同一天
   - 计算工作时长（扣除旷工时长）
   - 计算旷工时长（根据配置的旷工规则）
   - 计算加班时长（根据配置的加班规则）

3. **时长计算**:
   - 工作时长 = 签出时间 - 签入时间 - 旷工时长
   - 旷工时长：工作日内未打卡或迟到早退的时长
   - 加班时长：超出标准工作时间的时长

#### 2.3.2 考勤数据重算
**功能描述**: 运维接口，用于手动触发重新计算指定日期的考勤数据。

**涉及接口**:
- `GET /api/v1/attendances/manual/re-cal?orgId={orgId}&date={date}&run={run}` - 重新计算考勤数据

**参数说明**:
- orgId: 组织ID
- date: 日期（格式：yyyy-MM-dd）
- run: 是否执行（不传则为DEBUG模式，只打印不执行）


#### 2.3.3 结算管理
**功能描述**: 对外包人员进行月度结算，计算应付服务费。

**涉及接口**:
- `POST /api/v1/attendances/settlement/status` - 查询结算状态
- `POST /api/v1/attendances/settlement` - 执行结算
- `PUT /api/v1/attendances/revoke-settlement` - 撤销结算

**业务流程**:
1. 查询指定月份的考勤数据
2. 根据配置的计费规则计算服务费
3. 生成结算记录
4. 支持撤销重新结算

#### 2.3.4 个人服务费管理
**功能描述**: 计算每个外包人员的个人服务费。

**涉及接口**:
- `POST /api/v1/attendances/personal-service-fee/status` - 查询个人服务费状态
- `POST /api/v1/attendances/personal-service-fee` - 计算个人服务费
- `PUT /api/v1/attendances/revoke-personal-service-fee` - 撤销个人服务费

#### 2.3.5 项目服务费分摊
**功能描述**: 将服务费按照工时分摊到各个项目。

**涉及接口**:
- `POST /api/v1/attendances/allocation/status` - 查询分摊状态
- `POST /api/v1/attendances/allocation` - 执行分摊
- `PUT /api/v1/attendances/revoke-allocation` - 撤销分摊

**业务规则**:
- 需要配置分摊维度（通常是项目维度）
- 根据工时数据中的项目信息进行分摊
- 分摊比例 = 项目工时 / 总工时

#### 2.3.6 旷工处理
**功能描述**: 自动计算和处理旷工情况。

**涉及接口**:
- `GET /api/v1/attendances/absenteeism/manual` - 手动触发旷工计算

**业务规则**:
- 根据配置的标准工作时间判断是否旷工
- 计算旷工时长
- 旷工时长会影响工作时长和服务费计算

#### 2.3.7 预结算
**功能描述**: 在正式结算前进行预结算，用于数据核对。

**涉及接口**:
- `GET /api/v1/attendances/pre-settlement/manual` - 手动触发预结算

### 2.4 工时计算与统计模块

#### 2.4.1 自动计算工时数据
**功能描述**: 定时任务自动计算和初始化工时数据。

**涉及接口**:
- `GET /api/v1/wl/manual/init?date={date}` - 手动初始化工时数据
- `GET /api/v1/wl/manual/cal?date={date}&orgId={orgId}` - 手动计算工时数据

**业务流程**:
1. 初始化指定日期的工时数据结构
2. 根据配置的派生字段规则自动计算
3. 更新工时指标数据

#### 2.4.2 工时剩余量查询
**功能描述**: 查询任务或项目的剩余工时。

**涉及接口**:
- `POST /api/v1/wl/getRemaining` - 查询剩余工时

**应用场景**:
- 项目进度跟踪
- 资源分配决策
- 工时预警

#### 2.4.3 每日数据同步
**功能描述**: 同步指定时间范围的每日工时数据。

**涉及接口**:
- `GET /api/v1/wl/syncDailyData?start={start}&end={end}` - 同步每日数据

### 2.5 窗口期控制模块

#### 2.5.1 窗口期配置
**功能描述**: 配置工时填报的时间窗口限制。

**配置项**:
1. **创建窗口（createWindow）**:
   - left: 可以创建工时的最早日期（相对当前日期）
   - right: 可以创建工时的最晚日期（相对当前日期）
   - 例如：left=-7, right=0 表示可以创建7天前到今天的工时

2. **修改窗口（updateWindow）**:
   - 控制可以修改工时的时间范围

3. **删除窗口（deleteWindow）**:
   - 控制可以删除工时的时间范围

4. **锁定日期（lockDay + lockHour）**:
   - lockDay: 每月锁定日期（1-31）
   - lockHour: 锁定时间点（0-23）
   - 例如：lockDay=5, lockHour=23 表示每月5号23点锁定

#### 2.5.2 窗口期校验
**功能描述**: 在工时操作时校验是否在允许的窗口期内。

**涉及接口**:
- `POST /api/v1/wl/allow` - 查询是否允许操作

**校验逻辑**:
```
if (当前日期 > 锁定日期) {
    不允许修改锁定日期之前的数据
}
if (操作类型 == 新建 && !在创建窗口内) {
    不允许新建
}
if (操作类型 == 修改 && !在修改窗口内) {
    不允许修改
}
if (操作类型 == 删除 && !在删除窗口内) {
    不允许删除
}
```


#### 2.5.3 工作日判断
**功能描述**: 判断指定日期是否为工作日。

**涉及接口**:
- `GET /api/v1/wl/skip?date={date}` - 判断是否跳过（非工作日）

**应用场景**:
- 考勤计算时区分工作日和非工作日
- 旷工判断时只计算工作日

### 2.6 数据修复与同步模块

#### 2.6.1 强制更新
**功能描述**: 强制更新指定范围的工时数据，用于数据修复或批量更新。

**涉及接口**:
- `GET /api/v1/wl/forceUpdate/fix?columns={columns}&start={start}&end={end}` - 按范围强制更新

**特点**:
- 支持批量更新
- 支持指定更新字段
- 支持时间范围过滤
- 自动处理派生字段

#### 2.6.2 定时任务
**功能描述**: 系统提供多个定时任务自动处理数据。

**任务类型**:
1. **每日工时初始化**: 每天自动初始化当天的工时数据结构
2. **考勤数据同步**: 定期同步考勤数据
3. **旷工计算**: 定期计算旷工情况
4. **预结算**: 月末自动触发预结算
5. **强制更新**: 定期更新派生字段

---

## 3. 数据结构设计

### 3.1 核心实体

#### 3.1.1 StBizMetrics（工时指标数据）
**表名**: st_biz_metrics

**字段说明**:
```java
- id: String(32) - 主键，UUID
- orgId: String - 组织ID
- metricsId: String - 指标定义ID
- dttm: Date - 业务日期
- numberVal1~20: Double - 数值字段1-20（用于存储工时、金额等数值）
- strVal1~20: String - 字符串字段1-20（用于存储人员、项目等信息）
- dttm1~5: Date - 日期字段1-5（用于存储时间点）
- text1~2: Text - 大文本字段1-2（用于存储备注等长文本）
- deleted: int - 删除标记（0-未删除，1-已删除）
```

**设计说明**:
- 采用宽表设计，通过配置映射字段含义
- numberVal字段可以映射为：工时、加班时长、服务费等
- strVal字段可以映射为：人员ID、项目ID、任务ID等
- 灵活性高，可以通过配置适配不同的业务场景

**常见字段映射**:
- numberVal1: 工时数（小时）
- strVal1: 成员ID
- strVal2: 任务/项目ID
- dttm1: 工时日期

#### 3.1.2 DailyTime（每日考勤数据）
**表名**: wl_daily_time

**字段说明**:
```java
- id: String - 主键
- userId: String(32) - 用户ID
- userName: String - 用户名
- date: LocalDate - 考勤日期
- workingMinutes: Integer - 工作时长（分钟）
- status: Integer - 状态（0-正常，1-已废弃）
- batch: String - 导入批次号
- batchDttm: LocalDateTime - 导入时间
```

**业务规则**:
- 每个用户每天只能有一条有效记录
- 通过batch批次号关联导入记录
- status=1表示该记录已被新批次覆盖

#### 3.1.3 UploadRecord（导入记录）
**表名**: 通过Redis或数据库存储

**字段说明**:
```java
- batch: String - 批次号（唯一标识）
- orgId: String - 组织ID
- title: String - 导入标题
- userId: String - 操作人ID
- dttm: long - 导入时间戳
- status: int - 状态（0-导入中，1-成功，2-失败）
- total: long - 总记录数
- success: long - 成功记录数
- failed: long - 失败记录数
- errorMsg: String - 错误信息
- errorFile: String - 错误文件路径
- ossId: String - OSS文件ID
- lastSyncTime: long - 最后同步时间
```

**状态流转**:
```
导入中(0) -> 成功(1)
导入中(0) -> 失败(2)
```


#### 3.1.4 WorkingStatusChangeEvent（工作状态变更事件）
**表名**: working_status_change_event

**用途**: 记录价值单元（任务/项目）的状态变更事件，用于触发工时数据的自动更新。

**字段说明**:
```java
- id: String - 事件ID
- orgId: String - 组织ID
- vuId: String - 价值单元ID
- oldStatus: String - 旧状态
- newStatus: String - 新状态
- changeTime: Date - 变更时间
```

### 3.2 配置数据结构

#### 3.2.1 WorkMetrics（工时配置）
**配置项**:
```java
- id: String - 指标定义ID
- orgId: String - 组织ID
- vuColumn: String - 工作卡列（主键列）
- memberColumn: String - 成员列
- numberColumn: String - 工时数值列
- lockDay: Integer - 锁定日期（1-31）
- lockHour: Integer - 锁定小时（0-23）
- createWindow: OperateWindow - 创建窗口
- updateWindow: OperateWindow - 修改窗口
- deleteWindow: OperateWindow - 删除窗口
- attendanceSync: boolean - 是否启用考勤同步
- global: WorkMetricsGlobal - 全局配置
```

**OperateWindow（操作窗口）**:
```java
- left: Integer - 左边界（相对天数，负数表示过去）
- right: Integer - 右边界（相对天数，0表示今天）
- disabled: boolean - 是否禁用该操作
```

**WorkMetricsGlobal（全局配置）**:
```java
- maxCount: double - 每日工时上限（小时）
- managerWhiteListEnable: boolean - 是否启用管理员白名单
- attendance: AttendanceConfig - 考勤配置
```

**AttendanceConfig（考勤配置）**:
```java
- dailyTime: boolean - 是否检查每日考勤
- overFlow: boolean - 是否检查工时超出考勤
- uploadWindows: List<Integer> - 允许上传考勤的窗口期
```

#### 3.2.2 OutsourcingConfig（外包配置）
**配置项**:
```java
- orgId: String - 组织ID
- memberFilterId: String - 外包人员过滤器ID
- attendance: Attendance - 考勤规则配置
- settlementConf: Settlement - 结算配置
```

**Attendance（考勤规则）**:
```java
- standardWorkTime: double - 标准工作时长（小时）
- overtimeRule: OvertimeRule - 加班规则
- absenteeismRule: AbsenteeismRule - 旷工规则
- accumulatedOvertime: boolean - 是否累计加班时长
```

**Settlement（结算配置）**:
```java
- personalServiceFeeConf: PersonalServiceFeeConf - 个人服务费配置
- projectServiceFeeConf: ProjectServiceFeeConf - 项目服务费配置
```

### 3.3 外包相关数据结构

#### 3.3.1 AttendanceColumn（考勤列枚举）
**字段映射**:
```java
ATTENDANCE_DTTM - 考勤日期
ATTENDANCE_OPERATOR - 操作人（外包人员ID）
ATTENDANCE_SIGN_IN_TIME - 签入时间
ATTENDANCE_SIGN_OUT_TIME - 签出时间
ATTENDANCE_WORK_TIME - 工作时长（小时）
ATTENDANCE_ABSENTEEISM_TIME - 旷工时长（小时）
ATTENDANCE_OVERTIME_TIME - 加班时长（小时）
```

#### 3.3.2 AttendanceDetail（考勤详情VO）
**字段说明**:
```java
- recordId: String - 考勤记录ID
- signInTime: Long - 签入时间戳
- signOutTime: Long - 签出时间戳
- workDuration: String - 工作时长（格式化字符串，如"8小时30分钟"）
```

---

## 4. API接口说明

### 4.1 工时管理接口

#### 4.1.1 检查允许填报
```
POST /api/v1/wl/allow
Content-Type: application/json

Request Body:
{
  "metricsId": "指标ID",
  "operate": "操作类型(CREATE/UPDATE/DELETE)"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "allowed": true,
    "reason": "允许原因或拒绝原因"
  }
}
```

#### 4.1.2 上传考勤数据
```
POST /api/v1/wl/upload
Content-Type: multipart/form-data

Parameters:
- file: 考勤数据文件
- orgId: 组织ID
- memberId: 操作人ID

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "batch": "批次号",
    "total": 100,
    "success": 98,
    "failed": 2,
    "errorFile": "错误文件路径"
  }
}
```


#### 4.1.3 查询导入记录
```
GET /api/v1/wl/records?orgId={orgId}

Response:
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "batch": "批次号",
      "title": "导入标题",
      "dttm": 1234567890,
      "status": 1,
      "total": 100,
      "success": 98,
      "failed": 2
    }
  ]
}
```

#### 4.1.4 查询指定批次
```
GET /api/v1/wl/records/{batch}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "batch": "批次号",
    "orgId": "组织ID",
    "title": "导入标题",
    "userId": "操作人ID",
    "dttm": 1234567890,
    "status": 1,
    "total": 100,
    "success": 98,
    "failed": 2,
    "errorMsg": "错误信息",
    "errorFile": "错误文件路径"
  }
}
```

#### 4.1.5 清理考勤数据
```
DELETE /api/v1/wl?batch={batch}

Response:
{
  "code": 0,
  "message": "success"
}
```

#### 4.1.6 手动初始化工时数据
```
GET /api/v1/wl/manual/init?date={date}

Parameters:
- date: 日期（可选，格式：yyyy-MM-dd，默认今天）

Response:
{
  "code": 0,
  "message": "success"
}
```

#### 4.1.7 手动计算工时数据
```
GET /api/v1/wl/manual/cal?date={date}&orgId={orgId}

Parameters:
- date: 日期（可选，格式：yyyy-MM-dd）
- orgId: 组织ID（可选）

Response:
{
  "code": 0,
  "message": "success"
}
```

#### 4.1.8 修复工时数据
```
PUT /api/v1/wl/fix?orgId={orgId}&columns={columns}&since={since}&until={until}&comp={comp}

Parameters:
- orgId: 组织ID（必填）
- columns: 需要更新的列，逗号分隔（必填）
- since: 开始日期（可选，格式：yyyy-MM-dd）
- until: 结束日期（可选，格式：yyyy-MM-dd）
- comp: 比较字段（可选，默认：inner_create_time）

Response:
{
  "code": 0,
  "message": "success"
}
```

#### 4.1.9 强制更新
```
GET /api/v1/wl/forceUpdate/fix?columns={columns}&start={start}&end={end}

Headers:
- org-identity: 组织ID

Parameters:
- columns: 需要更新的列，逗号分隔
- start: 开始日期（格式：yyyy-MM-dd）
- end: 结束日期（可选，格式：yyyy-MM-dd）

Response:
{
  "code": 0,
  "message": "success"
}
```

#### 4.1.10 同步每日数据
```
GET /api/v1/wl/syncDailyData?start={start}&end={end}

Parameters:
- start: 开始日期（格式：yyyy-MM-dd）
- end: 结束日期（格式：yyyy-MM-dd）

Response:
{
  "code": 0,
  "message": "success"
}
```

#### 4.1.11 查询剩余工时
```
POST /api/v1/wl/getRemaining
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "vuIds": ["价值单元ID1", "价值单元ID2"],
  "date": "查询日期"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "vuId1": {
      "remaining": 40.5,
      "total": 100,
      "used": 59.5
    }
  }
}
```

#### 4.1.12 判断是否跳过
```
GET /api/v1/wl/skip?date={date}

Parameters:
- date: 日期（可选，格式：yyyy-MM-dd，默认今天）

Response:
{
  "code": 0,
  "message": "success",
  "data": true  // true表示跳过（非工作日），false表示不跳过（工作日）
}
```

#### 4.1.13 Mock数据
```
POST /api/v1/wl/mock?orgId={orgId}&memberId={memberId}
Content-Type: application/json

Request Body:
{
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "userIds": ["user1", "user2"],
  "workingMinutes": 480
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "batch": "批次号",
    "total": 60
  }
}
```

### 4.2 考勤打卡接口

#### 4.2.1 签入打卡
```
POST /api/v1/attendances/sign-in?memberId={memberId}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": "考勤记录ID",
    "signInTime": 1234567890,
    "signOutTime": null,
    "workDuration": null
  }
}
```

#### 4.2.2 签出打卡
```
PUT /api/v1/attendances/{recordId}/sign-out?memberId={memberId}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": "考勤记录ID",
    "signInTime": 1234567890,
    "signOutTime": 1234598890,
    "workDuration": "8小时30分钟"
  }
}
```

#### 4.2.3 查询考勤详情
```
GET /api/v1/attendances?memberId={memberId}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": "考勤记录ID",
    "signInTime": 1234567890,
    "signOutTime": 1234598890,
    "workDuration": "8小时30分钟"
  }
}
```

#### 4.2.4 重新计算考勤
```
GET /api/v1/attendances/manual/re-cal?orgId={orgId}&date={date}&run={run}

Parameters:
- orgId: 组织ID
- date: 日期（格式：yyyy-MM-dd）
- run: 是否执行（可选，不传则为DEBUG模式）

Response:
{
  "code": 0,
  "message": "success"
}
```


### 4.3 结算管理接口

#### 4.3.1 查询结算状态
```
POST /api/v1/attendances/settlement/status
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "date": "2024-01"  // 格式：yyyy-MM
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": true,
    "date": "2024-01",
    "totalAmount": 150000.00,
    "recordCount": 50
  }
}
```

#### 4.3.2 执行结算
```
POST /api/v1/attendances/settlement
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "date": "2024-01"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": true,
    "date": "2024-01",
    "totalAmount": 150000.00,
    "recordCount": 50
  }
}
```

#### 4.3.3 撤销结算
```
PUT /api/v1/attendances/revoke-settlement
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "date": "2024-01"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": false,
    "date": "2024-01"
  }
}
```

#### 4.3.4 查询个人服务费状态
```
POST /api/v1/attendances/personal-service-fee/status
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "date": "2024-01"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": true,
    "date": "2024-01",
    "recordCount": 50
  }
}
```

#### 4.3.5 计算个人服务费
```
POST /api/v1/attendances/personal-service-fee
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "date": "2024-01"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": true,
    "date": "2024-01",
    "recordCount": 50
  }
}
```

#### 4.3.6 撤销个人服务费
```
PUT /api/v1/attendances/revoke-personal-service-fee
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "date": "2024-01"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": false,
    "date": "2024-01"
  }
}
```

#### 4.3.7 查询分摊状态
```
POST /api/v1/attendances/allocation/status
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "date": "2024-01-15"  // 格式：yyyy-MM-dd
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": true,
    "date": "2024-01-15"
  }
}
```

#### 4.3.8 执行分摊
```
POST /api/v1/attendances/allocation
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "date": "2024-01-15"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": true,
    "date": "2024-01-15"
  }
}
```

#### 4.3.9 撤销分摊
```
PUT /api/v1/attendances/revoke-allocation
Content-Type: application/json

Request Body:
{
  "orgId": "组织ID",
  "date": "2024-01-15"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "confirmed": false,
    "date": "2024-01-15"
  }
}
```

#### 4.3.10 手动触发旷工计算
```
GET /api/v1/attendances/absenteeism/manual

Response:
{
  "code": 0,
  "message": "success"
}
```

#### 4.3.11 手动触发预结算
```
GET /api/v1/attendances/pre-settlement/manual

Response:
{
  "code": 0,
  "message": "success"
}
```

---

## 5. 业务流程

### 5.1 工时填报流程

```
用户填报工时
    ↓
检查是否允许填报（窗口期、锁定日期）
    ↓
[是] → 检查是否有考勤数据（如果启用考勤同步）
    ↓
[是] → 检查工时总和是否超过考勤时长
    ↓
[否] → 保存工时数据
    ↓
触发派生字段计算
    ↓
更新相关统计数据
    ↓
发送Kafka消息通知
    ↓
完成
```

### 5.2 考勤数据导入流程

```
上传考勤文件
    ↓
生成批次号
    ↓
创建导入记录（状态：导入中）
    ↓
解析文件内容
    ↓
校验数据格式
    ↓
批量插入数据库
    ↓
更新导入记录（状态：成功/失败）
    ↓
如果有错误，生成错误文件
    ↓
返回导入结果
```

### 5.3 外包人员打卡流程

```
【签入流程】
外包人员签入
    ↓
验证人员是否在白名单
    ↓
[是] → 创建考勤记录
    ↓
记录签入时间
    ↓
返回考勤记录ID
    ↓
完成

【签出流程】
外包人员签出
    ↓
查询签入记录
    ↓
验证签入记录是否存在
    ↓
验证签入签出是否同一天
    ↓
计算工作时长
    ↓
计算旷工时长
    ↓
计算加班时长
    ↓
更新考勤记录
    ↓
返回考勤详情
    ↓
完成
```

### 5.4 月度结算流程

```
触发月度结算
    ↓
查询当月所有考勤记录
    ↓
按人员汇总工作时长
    ↓
根据计费规则计算服务费
    ↓
生成结算记录
    ↓
【可选】计算个人服务费
    ↓
查询当月工时数据
    ↓
按项目维度分摊服务费
    ↓
生成项目服务费记录
    ↓
发送消息通知
    ↓
完成
```


### 5.5 工时数据修复流程

```
触发数据修复
    ↓
指定组织、时间范围、字段
    ↓
查询需要修复的工时数据
    ↓
遍历每条数据
    ↓
重新计算派生字段
    ↓
批量更新数据库
    ↓
记录修复日志
    ↓
完成
```

### 5.6 强制更新流程

```
定时任务触发 / 手动触发
    ↓
计算更新范围（根据配置的批次大小）
    ↓
查询需要更新的价值单元
    ↓
按批次处理
    ↓
查询每个价值单元的工时数据
    ↓
重新计算派生字段
    ↓
批量更新数据库
    ↓
记录更新日志
    ↓
完成
```

### 5.7 窗口期校验流程

```
用户操作工时数据
    ↓
获取工时配置
    ↓
判断是否为工时管理员
    ↓
[是] → 跳过窗口期检查（如果启用白名单）
    ↓
[否] → 检查锁定日期
    ↓
[通过] → 检查操作窗口期
    ↓
[通过] → 允许操作
    ↓
[不通过] → 返回错误信息
```

---

## 6. 关键技术点

### 6.1 响应式编程
系统使用Spring WebFlux实现响应式编程，所有API接口返回`Publisher<ApplicationResult>`类型，支持异步非阻塞处理。

**优势**:
- 提高系统吞吐量
- 降低资源消耗
- 支持背压机制

### 6.2 宽表设计
工时指标数据采用宽表设计（StBizMetrics），通过配置映射字段含义，提供高度灵活性。

**优势**:
- 无需频繁修改表结构
- 支持多种业务场景
- 查询性能好

**劣势**:
- 字段语义不直观
- 需要配置管理
- 数据迁移复杂

### 6.3 派生字段自动计算
系统支持配置派生字段规则，在数据变更时自动计算相关字段。

**示例**:
- 工时总和 = 各子任务工时之和
- 剩余工时 = 预估工时 - 已用工时
- 进度百分比 = 已用工时 / 预估工时 * 100

### 6.4 事件驱动架构
系统通过Kafka消息队列实现事件驱动，解耦各模块之间的依赖。

**事件类型**:
- 工时数据变更事件
- 价值单元状态变更事件
- 考勤数据变更事件
- 结算完成事件

### 6.5 分布式锁
在并发场景下使用分布式锁保证数据一致性。

**应用场景**:
- 月度结算（防止重复结算）
- 考勤数据导入（防止并发导入）
- 强制更新（防止重复更新）

### 6.6 定时任务
系统提供多个定时任务自动处理数据。

**任务调度**:
- 使用Spring @Scheduled注解
- 支持Cron表达式配置
- 支持分布式任务调度

---

## 7. 非功能需求

### 7.1 性能要求
- API响应时间：P95 < 500ms
- 批量导入：支持10000条/分钟
- 并发用户：支持1000+在线用户
- 数据库查询：单表查询 < 100ms

### 7.2 可用性要求
- 系统可用性：99.9%
- 故障恢复时间：< 5分钟
- 数据备份：每日备份
- 灾难恢复：支持异地容灾

### 7.3 安全要求
- 身份认证：支持SSO单点登录
- 权限控制：基于角色的访问控制（RBAC）
- 数据加密：敏感数据加密存储
- 审计日志：记录所有关键操作

### 7.4 可扩展性要求
- 水平扩展：支持多实例部署
- 数据分片：支持按组织分片
- 插件化：支持自定义派生字段规则
- 配置化：核心业务规则可配置

### 7.5 可维护性要求
- 代码规范：遵循阿里巴巴Java开发规范
- 日志规范：统一日志格式和级别
- 监控告警：关键指标监控和告警
- 文档完善：API文档、部署文档、运维文档

---

## 8. 部署架构

### 8.1 服务部署
```
┌─────────────────────────────────────────┐
│          Nginx (负载均衡)                │
└─────────────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
┌───────▼────────┐    ┌────────▼────────┐
│ Workload实例1  │    │ Workload实例2   │
└────────────────┘    └─────────────────┘
        │                       │
        └───────────┬───────────┘
                    │
        ┌───────────▼───────────┐
        │    MySQL数据库集群     │
        └───────────────────────┘
```

### 8.2 依赖服务
- **数据库**: MySQL 5.7+
- **缓存**: Redis 5.0+
- **消息队列**: Kafka 2.0+
- **配置中心**: Apollo
- **服务注册**: Consul/Eureka

### 8.3 监控体系
- **应用监控**: Spring Boot Actuator + Prometheus
- **日志收集**: ELK (Elasticsearch + Logstash + Kibana)
- **链路追踪**: Zipkin/Skywalking
- **告警通知**: 钉钉/企业微信

---

## 9. 数据字典

### 9.1 工时状态
| 状态码 | 状态名称 | 说明 |
|-------|---------|------|
| 0 | 正常 | 工时数据正常 |
| 1 | 已删除 | 工时数据已删除（软删除） |

### 9.2 导入状态
| 状态码 | 状态名称 | 说明 |
|-------|---------|------|
| 0 | 导入中 | 正在导入数据 |
| 1 | 成功 | 导入成功 |
| 2 | 失败 | 导入失败 |

### 9.3 操作类型
| 操作类型 | 说明 |
|---------|------|
| CREATE | 新建工时 |
| UPDATE | 修改工时 |
| DELETE | 删除工时 |

### 9.4 窗口期配置示例
| 配置项 | 示例值 | 说明 |
|-------|--------|------|
| createWindow.left | -7 | 可以创建7天前的工时 |
| createWindow.right | 0 | 可以创建到今天的工时 |
| updateWindow.left | -30 | 可以修改30天前的工时 |
| updateWindow.right | 0 | 可以修改到今天的工时 |
| deleteWindow.left | -7 | 可以删除7天前的工时 |
| deleteWindow.right | 0 | 可以删除到今天的工时 |
| lockDay | 5 | 每月5号锁定 |
| lockHour | 23 | 23点锁定 |

---

## 10. 常见问题

### 10.1 为什么工时填报被拒绝？
**可能原因**:
1. 不在创建/修改/删除窗口期内
2. 已过锁定日期
3. 没有对应日期的考勤数据（如果启用考勤同步）
4. 工时总和超过考勤时长
5. 超过每日工时上限

**解决方案**:
- 检查窗口期配置
- 联系工时管理员调整配置
- 确保有考勤数据
- 调整工时数值

### 10.2 如何修改历史工时？
**方案**:
1. 如果在修改窗口期内，直接修改
2. 如果已过窗口期，联系工时管理员
3. 工时管理员可以不受窗口期限制（需要启用白名单）

### 10.3 考勤数据导入失败怎么办？
**排查步骤**:
1. 检查文件格式是否正确
2. 检查数据是否有重复
3. 检查日期格式是否正确
4. 查看错误文件了解具体错误
5. 联系技术支持

### 10.4 如何撤销结算？
**操作步骤**:
1. 调用撤销结算接口
2. 系统会删除结算记录
3. 如果已生成个人服务费，需要先撤销个人服务费
4. 如果已生成项目服务费分摊，需要先撤销分摊

### 10.5 强制更新会影响什么？
**影响范围**:
- 重新计算指定字段的派生值
- 更新工时指标数据
- 触发相关统计数据更新
- 不会影响原始填报数据

---

## 11. 附录

### 11.1 相关文档
- [工时服务API文档](./workload-api.md)
- [工时服务部署文档](./workload-deployment.md)
- [工时服务运维手册](./workload-operations.md)

### 11.2 版本历史
| 版本号 | 日期 | 变更内容 | 作者 |
|-------|------|---------|------|
| 1.0.0 | 2024-01-01 | 初始版本 | - |

### 11.3 术语表
| 术语 | 英文 | 说明 |
|-----|------|------|
| 工时 | Workload | 工作时长，单位：小时 |
| 考勤 | Attendance | 上下班打卡记录 |
| 窗口期 | Window | 允许操作的时间范围 |
| 锁定日期 | Lock Date | 不允许修改历史数据的截止日期 |
| 派生字段 | Derived Field | 根据其他字段自动计算的字段 |
| 价值单元 | Value Unit | 任务、项目等工作单元 |
| 指标 | Metrics | 统计数据 |
| 结算 | Settlement | 月度费用结算 |
| 分摊 | Allocation | 费用按比例分配 |

---

**文档生成时间**: 2026-02-04  
**文档版本**: v1.0  
**文档状态**: 待评审
