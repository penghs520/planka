/**
 * 考勤配置类型定义
 *
 * 与后端 Java 类型保持一致
 */

import type { Condition } from './condition'

// ==================== 枚举类型 ====================

/**
 * 时间统计单位
 */
export enum DurationUnit {
  HOUR = 'HOUR',
  MINUTE = 'MINUTE'
}

/**
 * 请假单位
 */
export enum LeaveUnit {
  HALF_DAY = 'HALF_DAY',
  DAY = 'DAY'
}

/**
 * 请假类型
 */
export enum LeaveType {
  SICK_LEAVE = 'SICK_LEAVE',
  TIME_OFF = 'TIME_OFF',
  ANNUAL_LEAVE = 'ANNUAL_LEAVE',
  PERSONAL_LEAVE = 'PERSONAL_LEAVE'
}

/**
 * 日期周期单位
 */
export enum DateUnit {
  DAY = 'DAY',
  WEEK = 'WEEK',
  MONTH = 'MONTH'
}

/**
 * 加班计算方式
 */
export enum OvertimeCalWay {
  ACTUAL_ATTENDANCE = 'ACTUAL_ATTENDANCE'
}

/**
 * 加班类型
 */
export enum OvertimeType {
  WORKDAY = 'WORKDAY',
  WEEKEND = 'WEEKEND',
  HOLIDAY = 'HOLIDAY'
}

/**
 * 窗口单位
 */
export enum WindowUnit {
  CALENDAR_DAY = 'CALENDAR_DAY',
  BUSINESS_DAY = 'BUSINESS_DAY'
}

/**
 * 结算方式
 */
export enum SettlementMethod {
  AUTO = 'AUTO',
  MANUAL = 'MANUAL'
}

// ==================== 主配置类型 ====================

/**
 * 考勤配置
 */
export interface OutsourcingConfig {
  /** 配置ID */
  id?: string
  /** 组织ID */
  orgId: string
  /** 配置名称 */
  name: string
  /** 描述 */
  description?: string
  /** Schema类型 */
  schemaType?: string
  /** Schema子类型 */
  schemaSubType?: string

  // 全局配置
  /** 时间统计折算单位 */
  durationUnit: DurationUnit
  /** 小数位数（≥0） */
  decimalScale: number
  /** 卡片考勤必填 */
  cardAttendanceRequired: boolean
  /** 成员卡片类型ID（可选） */
  memberCardTypeId?: string
  /** 成员筛选条件（可选） */
  memberFilter?: Condition

  // 子配置
  /** 签到配置 */
  attendanceConf?: AttendanceConf
  /** 请假配置 */
  leaveConf?: LeaveConf
  /** 加班配置 */
  overtimeConf?: OvertimeConf
  /** 补卡配置 */
  attendanceChangeConf?: AttendanceChangeConf
  /** 结算配置 */
  settlementConf?: SettlementConf

  // 元数据
  /** 是否启用 */
  enabled?: boolean
  /** 排序号 */
  sortOrder?: number
  /** 状态 */
  state?: string
  /** 内容版本号 */
  contentVersion?: number
  /** 结构版本号 */
  structureVersion?: string
  /** 创建时间 */
  createdAt?: string
  /** 创建人ID */
  createdBy?: string
  /** 更新时间 */
  updatedAt?: string
  /** 更新人ID */
  updatedBy?: string
  /** 删除时间 */
  deletedAt?: string
}

// ==================== 签到配置 ====================

/**
 * 签到配置
 */
export interface AttendanceConf {
  /** 每日工作开始时间（HH:mm） */
  workStart: string
  /** 每日工作结束时间（HH:mm） */
  workEnd: string
  /** 午休开始时间（HH:mm） */
  lunchStart: string
  /** 午休结束时间（HH:mm） */
  lunchEnd: string
  /** 正常出勤一天折算工作时长（小时） */
  workDuration: number
  /** 工作时间是否用于工时分配 */
  impactWm: boolean
  /** 可分配工作时长是否累计加班时长 */
  accumulatedOvertime: boolean
  /** 只有签入或只有签出时是否计入旷工 */
  absenceWhenNoSignInOrOut: boolean
}

// ==================== 请假配置 ====================

/**
 * 请假配置
 */
export interface LeaveConf {
  /** 请假限制规则列表 */
  limitRules?: LeaveLimitRule[]
  /** 最小请假单位 */
  leaveUnit: LeaveUnit
  /** 启用的请假类型列表 */
  enabledLeaveTypes?: LeaveType[]
}

/**
 * 请假限制规则
 */
export interface LeaveLimitRule {
  /** 周期范围 */
  range: DateUnit
  /** 请假类型 */
  leaveType: LeaveType
  /** 该周期内的请假上限天数（≥0） */
  limit: number
}

// ==================== 加班配置 ====================

/**
 * 加班配置
 */
export interface OvertimeConf {
  /** 计算方式 */
  calWay: OvertimeCalWay
  /** 加班起算时间（分钟，≥0） */
  startDuration: number
  /** 最小加班时间（分钟，≥0） */
  minDuration?: number
  /** 最大加班时长限制 */
  limitRules?: OvertimeLimitRule[]
  /** 非工作日加班设置 */
  nonWorkOvertime?: NonWorkOvertime
  /** 加班换算规则 */
  calRule?: OvertimeDurationCalRule
}

/**
 * 加班时长限制规则
 */
export interface OvertimeLimitRule {
  /** 周期范围 */
  range: DateUnit
  /** 时长上限（分钟） */
  limit: number
}

/**
 * 非工作日加班设置
 */
export interface NonWorkOvertime {
  /** 单日上限（分钟） */
  limit?: number
}

/**
 * 加班换算规则
 */
export interface OvertimeDurationCalRule {
  /** 换算规则列表 */
  rules?: CalRule[]
}

/**
 * 单条换算规则
 */
export interface CalRule {
  /** 加班类型 */
  type: OvertimeType
  /** 换算比例 */
  ratio: number
  /** 关联的请假类型ID */
  leaveItemId: string
}

// ==================== 补卡配置 ====================

/**
 * 补卡配置
 */
export interface AttendanceChangeConf {
  /** 补卡次数（≥0） */
  count: number
  /** 补卡窗口（≥0） */
  window: number
  /** 窗口单位 */
  windowUnit: WindowUnit
  /** 是否允许补非工作日的考勤 */
  allowWeekendOrHoliday: boolean
  /** 签入时间限制 */
  signIn?: TimeLimit
  /** 签出时间限制 */
  signOut?: TimeLimit
}

/**
 * 时间限制
 */
export interface TimeLimit {
  /** 开始时间（HH:mm） */
  start: string
  /** 结束时间（HH:mm） */
  end: string
}

// ==================== 结算配置 ====================

/**
 * 结算配置
 */
export interface SettlementConf {
  /** 结算方式 */
  method: SettlementMethod
  /** 实际缺勤时间抵扣系数（≥0） */
  absenteeismDeductionCoefficient: number
  /** 结算单位 */
  durationUnit: DurationUnit
  /** 结算结果保留小数位数（≥0） */
  decimalScale: number
  /** 需要单独计算的请假类型ID列表 */
  specialLeaveItemIds?: string[]
  /** 成员卡片类型ID列表 */
  vutIds: string[]
  /** 离职日期字段ID */
  leaveDateFieldId?: string
  /** 个人结算费用配置 */
  personalServiceFeeConf?: PersonalServiceFeeConf
  /** 维度分摊配置 */
  projectServiceFeeConf?: ProjectServiceFeeConf
}

/**
 * 个人服务费配置
 */
export interface PersonalServiceFeeConf {
  /** 基础费用字段ID */
  baseFeeFieldId?: string
  /** 加班费用字段ID */
  overtimeFeeFieldId?: string
  /** 补贴字段ID */
  subsidyFieldId?: string
}

/**
 * 项目服务费配置
 */
export interface ProjectServiceFeeConf {
  /** 分摊维度列配置 */
  columns?: Column[]
}

/**
 * 分摊维度列
 */
export interface Column {
  /** 维度列名称 */
  column: string
  /** 是否启用 */
  active: boolean
}

// ==================== 辅助类型 ====================

/**
 * 枚举选项（用于下拉框）
 */
export interface EnumOption<T = string> {
  label: string
  value: T
}

/**
 * 获取枚举选项列表
 */
export function getEnumOptions<T extends Record<string, string>>(
  enumObj: T,
  labelMap: Record<string, string>
): EnumOption<T[keyof T]>[] {
  return Object.values(enumObj).map(value => ({
    label: labelMap[value] || value,
    value: value as T[keyof T]
  }))
}
