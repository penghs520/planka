import { cardApi } from './card'
import { workloadApi } from './workload'
import request from './request'
import type { UpdateCardRequest } from '@/types/card'
import type {
  AttendanceRecord,
  ApplicationRecord,
  TodayAttendance,
} from '@/types/attendance'
import dayjs from 'dayjs'

/**
 * 考勤申请验证请求
 */
export interface AttendanceApplicationValidationRequest {
  orgId: string
  applicantId: string
  cardTypeId: string
  fieldValues: Record<string, any>
}

/**
 * 考勤申请验证响应
 */
export interface AttendanceApplicationValidationResponse {
  valid: boolean
  message?: string
}

/**
 * 考勤 API
 * 签到签退功能使用 workload-service
 * 其他功能基于卡片系统实现
 */
export const attendanceApi = {
  /**
   * 验证考勤申请
   */
  async validateApplication(
    req: AttendanceApplicationValidationRequest
  ): Promise<AttendanceApplicationValidationResponse> {
    const response = await request.post<AttendanceApplicationValidationResponse>(
      '/api/v1/attendance-validation/validate',
      req
    )
    return response.data
  },
  /**
   * 获取今日考勤记录
   */
  async getTodayAttendance(memberCardId: string, orgId: string): Promise<TodayAttendance> {
    try {
      // 使用 workload-service 的接口
      const response = await workloadApi.getTodayAttendance(memberCardId, orgId)
      return {
        hasClockedIn: response.hasClockedIn,
        hasClockedOut: response.hasClockedOut,
        clockInTime: response.clockInTime,
        clockOutTime: response.clockOutTime,
        workHours: response.workHours,
        recordCardId: response.recordCardId,
      }
    } catch (error) {
      console.error('Failed to get today attendance:', error)
      return {
        hasClockedIn: false,
        hasClockedOut: false,
      }
    }
  },

  /**
   * 签到（使用 workload-service）
   */
  async clockIn(memberCardId: string, orgId: string) {
    return await workloadApi.clockIn({
      memberCardId,
      orgId,
    })
  },

  /**
   * 签退（使用 workload-service）
   */
  async clockOut(recordCardId: string, memberCardId: string, orgId: string) {
    return await workloadApi.clockOut({
      recordCardId,
      memberCardId,
      orgId,
    })
  },

  /**
   * 获取考勤记录列表
   */
  async getRecords(
    memberCardId: string,
    orgId: string,
    startDate?: string,
    endDate?: string,
  ): Promise<AttendanceRecord[]> {
    try {
      // 使用 workload-service 的接口
      const records = await workloadApi.getAttendanceRecords(memberCardId, orgId, startDate, endDate)
      return records
    } catch (error) {
      console.error('Failed to get attendance records:', error)
      return []
    }
  },

  /**
   * 获取申请记录列表
   */
  async getApplications(
    applicantCardId: string,
    orgId: string,
    type?: 'LEAVE' | 'OVERTIME' | 'MAKEUP',
  ): Promise<ApplicationRecord[]> {
    try {
      console.log('[getApplications] 开始查询申请记录', { applicantCardId, orgId, type })

      // 确定要查询的卡片类型
      const cardTypes: Array<{ id: string; type: 'LEAVE' | 'OVERTIME' | 'MAKEUP' }> = []
      if (!type || type === 'LEAVE') {
        cardTypes.push({ id: `${orgId}:leave-application`, type: 'LEAVE' })
      }
      if (!type || type === 'OVERTIME') {
        cardTypes.push({ id: `${orgId}:overtime-application`, type: 'OVERTIME' })
      }
      if (!type || type === 'MAKEUP') {
        cardTypes.push({ id: `${orgId}:makeup-application`, type: 'MAKEUP' })
      }

      console.log('[getApplications] 要查询的卡片类型:', cardTypes)

      // 分别查询每种卡片类型（因为每种类型的 linkFieldId 不同）
      const allResults: ApplicationRecord[] = []

      for (const cardType of cardTypes) {
        const linkFieldId = `${cardType.id}:link:applicant:SOURCE`
        console.log('[getApplications] 查询卡片类型:', cardType.id, 'linkFieldId:', linkFieldId)

        const response = await cardApi.pageQuery({
          queryContext: {
            orgId,
            operatorId: applicantCardId,
          },
          queryScope: {
            cardTypeIds: [cardType.id],
            cardStyles: ['ACTIVE'],
          },
          condition: {
            type: 'LINK',
            linkFieldId,
            targetCardIds: [applicantCardId],
          },
          yield: {
            field: { allFields: true },
            links: [
              {
                linkFieldId: `${cardType.id}:link:applicant:SOURCE`,
                targetYield: { field: { allFields: false } },
              },
            ],
          },
          sortAndPage: {
            page: { pageNum: 0, pageSize: 100 },
            sort: [{ field: '$createdAt', order: 'DESC' }],
          },
        })

        console.log('[getApplications] 查询结果:', cardType.type, '数量:', response.content.length)

        // 转换数据并过滤只保留当前用户的申请
        const records = this.convertToApplicationRecords(response.content)
          .filter(record => record.applicantCardId === applicantCardId)
        allResults.push(...records)
      }

      console.log('[getApplications] 总共查询到记录数:', allResults.length)

      // 按创建时间倒序排序
      return allResults.sort((a, b) => {
        return dayjs(b.applyTime).valueOf() - dayjs(a.applyTime).valueOf()
      })
    } catch (error) {
      console.error('[getApplications] 查询失败:', error)
      return []
    }
  },

  /**
   * 转换卡片数据为 ApplicationRecord 格式
   */
  convertToApplicationRecords(cards: any[], applicantName?: string): ApplicationRecord[] {
    return cards.map((card) => {
      console.log('[convertToApplicationRecords] 卡片数据:', {
        id: card.id,
        typeId: card.typeId,
        fieldValues: card.fieldValues,
        fieldValuesKeys: card.fieldValues ? Object.keys(card.fieldValues) : [],
        linkedCards: card.linkedCards,
        linkedCardsKeys: card.linkedCards ? Object.keys(card.linkedCards) : [],
      })

      const cardType = card.typeId.includes('leave') ? 'LEAVE' : card.typeId.includes('overtime') ? 'OVERTIME' : 'MAKEUP'

      // 提取字段值
      const startDateField = card.fieldValues?.[`${card.typeId}:start-date`] || card.fieldValues?.[`${card.typeId}:overtime-date`] || card.fieldValues?.[`${card.typeId}:makeup-date`]
      const endDateField = card.fieldValues?.[`${card.typeId}:end-date`]
      const startTimeField = card.fieldValues?.[`${card.typeId}:start-time`]
      const endTimeField = card.fieldValues?.[`${card.typeId}:end-time`]
      const makeupTimeField = card.fieldValues?.[`${card.typeId}:makeup-time`]
      const durationField = card.fieldValues?.[`${card.typeId}:duration`]
      const approvalStatusField = card.fieldValues?.[`${card.typeId}:approval-status`]

      // 提取请假类型字段
      const leaveTypeField = card.fieldValues?.[`${card.typeId}:leave-type`]
      const leaveTypeValue = leaveTypeField?.value
      let leaveTypeLabel = '-'
      if (Array.isArray(leaveTypeValue) && leaveTypeValue.length > 0) {
        const typeCode = String(leaveTypeValue[0])
        const typeMap: Record<string, string> = {
          'sick': '病假',
          'time-off': '调休',
          'annual': '年假',
          'personal': '事假'
        }
        leaveTypeLabel = typeMap[typeCode] || typeCode
      }

      // 提取申请人字段
      const applicantLinkKey = `${card.typeId}:link:applicant:SOURCE`
      const applicantField = card.fieldValues?.[applicantLinkKey]
      console.log('[convertToApplicationRecords] 申请人字段:', {
        applicantLinkKey,
        applicantField,
      })

      // 格式化时间
      let startTime = '-'
      let endTime = '-'

      if (cardType === 'LEAVE') {
        startTime = startDateField?.value ? dayjs(startDateField.value as number).format('YYYY-MM-DD') : '-'
        endTime = endDateField?.value ? dayjs(endDateField.value as number).format('YYYY-MM-DD') : '-'
      } else if (cardType === 'OVERTIME') {
        const overtimeDate = startDateField?.value ? dayjs(startDateField.value as number).format('YYYY-MM-DD') : ''
        startTime = overtimeDate && startTimeField?.value ? `${overtimeDate} ${startTimeField.value}` : '-'
        endTime = overtimeDate && endTimeField?.value ? `${overtimeDate} ${endTimeField.value}` : '-'
      } else if (cardType === 'MAKEUP') {
        const makeupDate = startDateField?.value ? dayjs(startDateField.value as number).format('YYYY-MM-DD') : ''
        startTime = makeupDate && makeupTimeField?.value ? `${makeupDate} ${makeupTimeField.value}` : '-'
      }

      // 提取时长
      const duration = durationField?.value ? Number(durationField.value) : 0

      // 提取审批状态
      const approvalStatusValue = approvalStatusField?.value
      console.log('[convertToApplicationRecords] 审批状态原始值:', approvalStatusValue)

      let approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED' = 'PENDING'
      if (Array.isArray(approvalStatusValue) && approvalStatusValue.length > 0) {
        const statusStr = String(approvalStatusValue[0]).toUpperCase()
        if (statusStr === 'PENDING' || statusStr === 'APPROVED' || statusStr === 'REJECTED') {
          approvalStatus = statusStr as 'PENDING' | 'APPROVED' | 'REJECTED'
        }
      }

      console.log('[convertToApplicationRecords] 审批状态:', approvalStatus)

      // 提取申请人名称 - 优先使用传入的 applicantName，否则尝试从 linkedCards 获取
      let finalApplicantName = applicantName || '-'
      const applicantCards = card.linkedCards?.[applicantLinkKey]
      if (!applicantName && applicantCards?.[0]) {
        finalApplicantName = applicantCards[0].title?.displayValue || '-'
      }

      // 提取申请人ID（用于过滤）
      const extractedApplicantId = applicantCards?.[0]?.id || card.fieldValues?.[applicantLinkKey]?.value?.[0] || ''

      // 提取审批人信息
      const approverLinkKey = `${card.typeId}:link:approver:SOURCE`
      const approverCards = card.linkedCards?.[approverLinkKey]
      const extractedApproverId = approverCards?.[0]?.id || ''
      const extractedApproverName = approverCards?.[0]?.title?.displayValue || '-'

      // 提取标题和描述
      const title = card.title?.displayValue || card.title?.value || '-'
      const description = card.description?.value || '-'

      // 提取原因字段
      const reasonField = card.fieldValues?.[`${card.typeId}:reason`]
      const reason = reasonField?.value || '-'

      return {
        cardId: card.id,
        type: cardType,
        applicantCardId: extractedApplicantId,
        applicantName: finalApplicantName,
        applyTime: card.createdAt ? dayjs(card.createdAt).format('YYYY-MM-DD HH:mm') : '-',
        startTime,
        endTime,
        duration,
        reason,
        approvalStatus,
        title,
        description,
        approverCardId: extractedApproverId,
        approverName: extractedApproverName,
        leaveType: leaveTypeLabel,
      }
    })
  },

  /**
   * 获取待审批列表
   */
  async getPendingApprovals(approverCardId: string, orgId: string): Promise<ApplicationRecord[]> {
    try {
      console.log('[getPendingApprovals] 开始查询待审批记录', { approverCardId, orgId })

      const cardTypes = [
        { id: `${orgId}:leave-application`, type: 'LEAVE' as const },
        { id: `${orgId}:overtime-application`, type: 'OVERTIME' as const },
        { id: `${orgId}:makeup-application`, type: 'MAKEUP' as const },
      ]

      console.log('[getPendingApprovals] 要查询的卡片类型:', cardTypes)

      // 分别查询每种卡片类型
      const allResults: ApplicationRecord[] = []

      for (const cardType of cardTypes) {
        const linkFieldId = `${cardType.id}:link:approver:SOURCE`
        console.log('[getPendingApprovals] 查询卡片类型:', cardType.id, 'linkFieldId:', linkFieldId)

        const response = await cardApi.pageQuery({
          queryContext: {
            orgId,
            operatorId: approverCardId,
          },
          queryScope: {
            cardTypeIds: [cardType.id],
            cardStyles: ['ACTIVE'],
          },
          condition: {
            type: 'LINK',
            linkFieldId,
            targetCardIds: [approverCardId],
          },
          yield: {
            field: { allFields: true },
            links: [
              {
                linkFieldId: `${cardType.id}:link:applicant:SOURCE`,
                targetYield: { field: { allFields: false } },
              },
              {
                linkFieldId: `${cardType.id}:link:approver:SOURCE`,
                targetYield: { field: { allFields: false } },
              },
            ],
          },
          sortAndPage: {
            page: { pageNum: 0, pageSize: 100 },
            sort: [{ field: '$createdAt', order: 'DESC' }],
          },
        })

        console.log('[getPendingApprovals] 查询结果:', cardType.type, '数量:', response.content.length)

        // 转换结果并过滤：1) 待审批状态 2) 审批人是当前用户
        const pendingRecords = this.convertToApplicationRecords(response.content)
          .filter(record => record.approvalStatus === 'PENDING' && record.approverCardId === approverCardId)
        console.log('[getPendingApprovals] 待审批记录数:', pendingRecords.length)

        allResults.push(...pendingRecords)
      }

      console.log('[getPendingApprovals] 总共查询到待审批记录数:', allResults.length)

      // 按创建时间倒序排序
      return allResults.sort((a, b) => {
        return dayjs(b.applyTime).valueOf() - dayjs(a.applyTime).valueOf()
      })
    } catch (error) {
      console.error('[getPendingApprovals] 查询失败:', error)
      return []
    }
  },

  /**
   * 审批申请（批准或拒绝）
   */
  async approveApplication(
    cardId: string,
    orgId: string,
    approved: boolean,
    comment?: string,
    applicationType: 'LEAVE' | 'OVERTIME' | 'MAKEUP' = 'LEAVE',
  ): Promise<void> {
    let cardTypeId: string
    if (applicationType === 'LEAVE') {
      cardTypeId = `${orgId}:leave-application`
    } else if (applicationType === 'OVERTIME') {
      cardTypeId = `${orgId}:overtime-application`
    } else {
      cardTypeId = `${orgId}:makeup-application`
    }

    const now = dayjs().format('YYYY-MM-DD HH:mm:ss')

    const fieldValues: Record<string, any> = {
      [`${cardTypeId}:approval-status`]: {
        fieldId: `${cardTypeId}:approval-status`,
        readable: true,
        type: 'ENUM',
        value: [approved ? 'APPROVED' : 'REJECTED'],
      },
      [`${cardTypeId}:approval-time`]: {
        fieldId: `${cardTypeId}:approval-time`,
        readable: true,
        type: 'DATE',
        value: dayjs(now).valueOf(),
      },
    }

    if (comment) {
      fieldValues[`${cardTypeId}:approval-comment`] = {
        fieldId: `${cardTypeId}:approval-comment`,
        readable: true,
        type: 'TEXT',
        value: comment,
      }
    }

    const request: UpdateCardRequest = {
      cardId,
      fieldValues,
    }

    await cardApi.update(request)
  },
}

