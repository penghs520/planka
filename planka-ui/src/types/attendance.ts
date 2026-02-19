/**
 * 考勤相关类型定义
 */

/**
 * 考勤状态
 */
export type AttendanceStatus = 'NORMAL' | 'LATE' | 'EARLY_LEAVE' | 'ABSENT' | 'LEAVE' | 'OVERTIME'

/**
 * 审批状态
 */
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/**
 * 申请类型
 */
export type ApplicationType = 'LEAVE' | 'OVERTIME' | 'MAKEUP'

/**
 * 考勤记录
 */
export interface AttendanceRecord {
  /** 卡片ID */
  cardId: string
  /** 日期 */
  date: string
  /** 签到时间 */
  clockInTime?: string
  /** 签退时间 */
  clockOutTime?: string
  /** 工作时长（小时） */
  workHours?: number
  /** 考勤状态 */
  status: AttendanceStatus
  /** 成员卡片ID */
  memberCardId: string
  /** 备注 */
  remark?: string
}

/**
 * 申请记录（请假/加班/补卡）
 */
export interface ApplicationRecord {
  /** 卡片ID */
  cardId: string
  /** 申请类型 */
  type: ApplicationType
  /** 申请人卡片ID */
  applicantCardId: string
  /** 申请人姓名 */
  applicantName: string
  /** 申请时间 */
  applyTime: string
  /** 开始时间 */
  startTime: string
  /** 结束时间 */
  endTime: string
  /** 时长（小时） */
  duration?: number
  /** 申请原因 */
  reason: string
  /** 审批状态 */
  approvalStatus: ApprovalStatus
  /** 审批人卡片ID */
  approverCardId?: string
  /** 审批人姓名 */
  approverName?: string
  /** 审批时间 */
  approvalTime?: string
  /** 审批意见 */
  approvalComment?: string
  /** 标题 */
  title?: string
  /** 描述 */
  description?: string
  /** 请假类型 */
  leaveType?: string
}

/**
 * 考勤统计
 */
export interface AttendanceStats {
  /** 出勤天数 */
  attendanceDays: number
  /** 迟到次数 */
  lateDays: number
  /** 早退次数 */
  earlyLeaveDays: number
  /** 旷工天数 */
  absentDays: number
  /** 请假天数 */
  leaveDays: number
  /** 加班时长（小时） */
  overtimeHours: number
  /** 总工作时长（小时） */
  totalWorkHours: number
}

/**
 * 今日考勤状态
 */
export interface TodayAttendance {
  /** 是否已签到 */
  hasClockedIn: boolean
  /** 是否已签退 */
  hasClockedOut: boolean
  /** 签到时间 */
  clockInTime?: string
  /** 签退时间 */
  clockOutTime?: string
  /** 工作时长（小时） */
  workHours?: number
  /** 考勤记录卡片ID */
  recordCardId?: string
}
