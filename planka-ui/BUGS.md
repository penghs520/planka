# 待修复问题

## 2026-02-12 - 待审批列表数据过多

**问题描述**：
待审批列表显示的数据太多，应该只显示：
- 审批人 = 当前用户
- 审批状态 = 待审批（PENDING）

的记录。

**当前实现**：
- 文件：`planka-ui/src/api/attendance.ts`
- 方法：`getPendingApprovals()`
- 已实现：通过 LINK 条件过滤审批人=当前用户
- 已实现：前端过滤 `approvalStatus === 'PENDING'`

**可能原因**：
1. 审批状态字段值提取不正确
2. 默认值设置导致未设置状态的记录被显示
3. 需要检查实际返回的审批状态值格式

**下一步**：
- 查看 Console 日志中的审批状态原始值
- 确认审批状态字段的实际格式（pending/PENDING/其他）
- 可能需要在后端查询时就过滤审批状态，而不是在前端过滤

**相关代码**：
- `planka-ui/src/api/attendance.ts` - `getPendingApprovals()` 方法
- `planka-ui/src/api/attendance.ts` - `convertToApplicationRecords()` 方法
