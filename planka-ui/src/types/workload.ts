/**
 * 工时服务类型定义
 */

/**
 * 签到请求
 */
export interface ClockInRequest {
  memberCardId: string
  orgId: string
  clockInTime?: string
}

/**
 * 签退请求
 */
export interface ClockOutRequest {
  recordCardId: string
  memberCardId: string
  orgId: string
  clockOutTime?: string
}

/**
 * 签到响应
 */
export interface ClockInResponse {
  recordCardId: string
  clockInTime: string
  status: string
}

/**
 * 签退响应
 */
export interface ClockOutResponse {
  recordCardId: string
  clockInTime: string
  clockOutTime: string
  workDuration: number
  status: string
}

/**
 * 今日考勤响应
 */
export interface TodayAttendanceResponse {
  hasClockedIn: boolean
  hasClockedOut: boolean
  clockInTime?: string
  clockOutTime?: string
  workHours?: number
  recordCardId?: string
}

/**
 * 窗口期配置
 */
export interface WindowConfig {
  id?: string
  orgId: string
  createWindow: number
  updateWindow: number
  deleteWindow: number
  lockDay: number
  lockHour: number
  managerWhitelistEnable: boolean
  managerWhitelist?: string
  createTime?: string
  updateTime?: string
}

/**
 * 检查权限请求
 */
export interface CheckPermissionRequest {
  orgId: string
  memberId: string
  date: string
  operation: 'CREATE' | 'UPDATE' | 'DELETE'
}

/**
 * 检查权限响应
 */
export interface CheckPermissionResponse {
  allowed: boolean
  reason: string
  windowConfig: WindowConfig
}

/**
 * 工时类型
 */
export type WorklogType = 'NORMAL' | 'OVERTIME' | 'LEAVE'

/**
 * 创建工时请求
 */
export interface CreateWorklogRequest {
  orgId: string
  cardId: string
  memberId: string
  date: string
  hours: number
  type: WorklogType
  description?: string
}

/**
 * 创建工时响应
 */
export interface CreateWorklogResponse {
  worklogCardId: string
  date: string
  hours: number
  type: WorklogType
  status: string
}
