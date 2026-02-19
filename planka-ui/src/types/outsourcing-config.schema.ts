/**
 * 考勤配置 Zod 校验 Schema
 *
 * 提供前端数据校验
 */

import { z } from 'zod'
import {
  DurationUnit,
  LeaveUnit,
  LeaveType,
  DateUnit,
  OvertimeCalWay,
  OvertimeType,
  WindowUnit,
  SettlementMethod
} from './outsourcing-config'

// ==================== 辅助校验函数 ====================

/**
 * 时间格式校验（HH:mm）
 */
const timeStringSchema = z
  .string()
  .regex(/^([01]\d|2[0-3]):([0-5]\d)$/, {
    message: '时间格式必须为 HH:mm'
  })

/**
 * 比较两个时间字符串
 * @returns true 如果 time1 < time2
 */
function isTimeBefore(time1: string, time2: string): boolean {
  const [h1, m1] = time1.split(':').map(Number)
  const [h2, m2] = time2.split(':').map(Number)
  return (h1 ?? 0) < (h2 ?? 0) || ((h1 ?? 0) === (h2 ?? 0) && (m1 ?? 0) < (m2 ?? 0))
}

// ==================== 时间限制 Schema ====================

export const timeLimitSchema = z
  .object({
    start: timeStringSchema,
    end: timeStringSchema
  })
  .refine(
    (data: { start: string; end: string }) => isTimeBefore(data.start, data.end),
    {
      message: '结束时间必须晚于开始时间',
      path: ['end']
    }
  )

// ==================== 签到配置 Schema ====================

export const attendanceConfSchema = z
  .object({
    workStart: timeStringSchema,
    workEnd: timeStringSchema,
    lunchStart: timeStringSchema,
    lunchEnd: timeStringSchema,
    workDuration: z.number().positive({ message: '工作时长必须大于 0' }),
    impactWm: z.boolean(),
    accumulatedOvertime: z.boolean(),
    absenceWhenNoSignInOrOut: z.boolean()
  })
  .refine(
    (data: { workStart: string; workEnd: string; lunchStart: string; lunchEnd: string; workDuration: number; impactWm: boolean; accumulatedOvertime: boolean; absenceWhenNoSignInOrOut: boolean }) => isTimeBefore(data.workStart, data.workEnd),
    {
      message: '工作结束时间必须晚于工作开始时间',
      path: ['workEnd']
    }
  )
  .refine(
    (data: { workStart: string; workEnd: string; lunchStart: string; lunchEnd: string; workDuration: number; impactWm: boolean; accumulatedOvertime: boolean; absenceWhenNoSignInOrOut: boolean }) => isTimeBefore(data.lunchStart, data.lunchEnd),
    {
      message: '午休结束时间必须晚于午休开始时间',
      path: ['lunchEnd']
    }
  )
  .refine(
    (data: { workStart: string; workEnd: string; lunchStart: string; lunchEnd: string; workDuration: number; impactWm: boolean; accumulatedOvertime: boolean; absenceWhenNoSignInOrOut: boolean }) => {
      // 午休时间必须在工作时间段内
      return (
        !isTimeBefore(data.lunchStart, data.workStart) &&
        !isTimeBefore(data.workEnd, data.lunchEnd)
      )
    },
    {
      message: '午休时间必须在工作时间段内',
      path: ['lunchStart']
    }
  )

// ==================== 请假配置 Schema ====================

export const leaveLimitRuleSchema = z.object({
  range: z.nativeEnum(DateUnit, { message: '请选择周期范围' }),
  leaveType: z.nativeEnum(LeaveType, { message: '请选择请假类型' }),
  limit: z.number().int().min(0, { message: '上限天数必须 ≥ 0' })
})

export const leaveConfSchema = z.object({
  limitRules: z.array(leaveLimitRuleSchema).nullish(),
  leaveUnit: z.nativeEnum(LeaveUnit, { message: '请选择请假单位' }),
  enabledLeaveTypes: z.array(z.nativeEnum(LeaveType)).nullish()
})

// ==================== 加班配置 Schema ====================

export const overtimeLimitRuleSchema = z.object({
  range: z.nativeEnum(DateUnit, { message: '请选择周期范围' }),
  limit: z.number().int().min(0, { message: '时长上限必须 ≥ 0' })
})

export const nonWorkOvertimeSchema = z.object({
  limit: z.number().min(0, { message: '单日上限必须 ≥ 0' }).nullish()
})

export const calRuleSchema = z.object({
  type: z.nativeEnum(OvertimeType, { message: '请选择加班类型' }),
  ratio: z.number().positive({ message: '换算比例必须 > 0' }),
  leaveItemId: z.string().min(1, { message: '请选择关联的请假类型' })
})

export const overtimeDurationCalRuleSchema = z.object({
  rules: z.array(calRuleSchema).nullish()
})

export const overtimeConfSchema = z.object({
  calWay: z.nativeEnum(OvertimeCalWay),
  startDuration: z.number().int().min(0, { message: '加班起算时间必须 ≥ 0' }),
  minDuration: z.number().int().min(0, { message: '最小加班时间必须 ≥ 0' }).nullish(),
  limitRules: z.array(overtimeLimitRuleSchema).nullish(),
  nonWorkOvertime: nonWorkOvertimeSchema.nullish(),
  calRule: overtimeDurationCalRuleSchema.nullish()
})

// ==================== 补卡配置 Schema ====================

export const attendanceChangeConfSchema = z.object({
  count: z.number().int().min(0, { message: '补卡次数必须 ≥ 0' }),
  window: z.number().int().min(0, { message: '补卡窗口必须 ≥ 0' }),
  windowUnit: z.nativeEnum(WindowUnit, { message: '请选择窗口单位' }),
  allowWeekendOrHoliday: z.boolean(),
  signIn: timeLimitSchema.nullish(),
  signOut: timeLimitSchema.nullish()
})

// ==================== 结算配置 Schema ====================

export const columnSchema = z.object({
  column: z.string().min(1, { message: '维度列名称不能为空' }),
  active: z.boolean()
})

export const personalServiceFeeConfSchema = z.object({
  baseFeeFieldId: z.string().nullish(),
  overtimeFeeFieldId: z.string().nullish(),
  subsidyFieldId: z.string().nullish()
})

export const projectServiceFeeConfSchema = z.object({
  columns: z.array(columnSchema).nullish()
})

export const settlementConfSchema = z.object({
  method: z.nativeEnum(SettlementMethod, { message: '请选择结算方式' }),
  absenteeismDeductionCoefficient: z
    .number()
    .int()
    .min(0, { message: '缺勤抵扣系数必须 ≥ 0' }),
  durationUnit: z.nativeEnum(DurationUnit, { message: '请选择结算单位' }),
  decimalScale: z.number().int().min(0, { message: '小数位数必须 ≥ 0' }),
  specialLeaveItemIds: z.array(z.string()).nullish(),
  vutIds: z.array(z.string()).min(1, { message: '成员卡片类型列表不能为空' }),
  leaveDateFieldId: z.string().nullish(),
  personalServiceFeeConf: personalServiceFeeConfSchema.nullish(),
  projectServiceFeeConf: projectServiceFeeConfSchema.nullish()
})

// ==================== 主配置 Schema ====================

export const outsourcingConfigSchema = z.object({
  id: z.string().nullish(),
  orgId: z.string().min(1, { message: '组织ID不能为空' }),
  name: z.string().min(1, { message: '配置名称不能为空' }),
  description: z.string().nullish(),

  // 全局配置
  durationUnit: z.nativeEnum(DurationUnit, { message: '请选择时间单位' }),
  decimalScale: z.number().int().min(0, { message: '小数位数必须 ≥ 0' }),
  cardAttendanceRequired: z.boolean(),
  memberCardTypeId: z.string().nullish(),
  memberFilter: z.any().nullish(), // Condition 类型，使用 any 避免循环依赖

  // 子配置
  attendanceConf: attendanceConfSchema.nullish(),
  leaveConf: leaveConfSchema.nullish(),
  overtimeConf: overtimeConfSchema.nullish(),
  attendanceChangeConf: attendanceChangeConfSchema.nullish(),
  settlementConf: settlementConfSchema.nullish(),

  // 元数据（可选）
  enabled: z.boolean().nullish(),
  sortOrder: z.number().nullish(),
  state: z.string().nullish(),
  contentVersion: z.number().nullish(),
  structureVersion: z.string().nullish(),
  createdAt: z.string().nullish(),
  createdBy: z.string().nullish(),
  updatedAt: z.string().nullish(),
  updatedBy: z.string().nullish(),
  deletedAt: z.string().nullish()
})

// ==================== 类型导出 ====================

export type OutsourcingConfigInput = z.infer<typeof outsourcingConfigSchema>
export type AttendanceConfInput = z.infer<typeof attendanceConfSchema>
export type LeaveConfInput = z.infer<typeof leaveConfSchema>
export type OvertimeConfInput = z.infer<typeof overtimeConfSchema>
export type AttendanceChangeConfInput = z.infer<typeof attendanceChangeConfSchema>
export type SettlementConfInput = z.infer<typeof settlementConfSchema>
