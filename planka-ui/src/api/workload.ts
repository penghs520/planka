import request from './request'
import type {
  ClockInRequest,
  ClockOutRequest,
  ClockInResponse,
  ClockOutResponse,
  TodayAttendanceResponse,
  WindowConfig,
  CheckPermissionRequest,
  CheckPermissionResponse,
  CreateWorklogRequest,
  CreateWorklogResponse,
} from '@/types/workload'

/**
 * 工时服务 API
 */
export const workloadApi = {
  /**
   * 签到
   */
  async clockIn(data: ClockInRequest): Promise<ClockInResponse> {
    return request.post('/api/v1/workload/clock-in', data)
  },

  /**
   * 签退
   */
  async clockOut(data: ClockOutRequest): Promise<ClockOutResponse> {
    return request.post('/api/v1/workload/clock-out', data)
  },

  /**
   * 获取今日考勤
   */
  async getTodayAttendance(memberCardId: string, orgId: string): Promise<TodayAttendanceResponse> {
    return request.get('/api/v1/workload/today', {
      params: { memberCardId, orgId },
    })
  },

  /**
   * 获取考勤记录列表
   */
  async getAttendanceRecords(
    memberCardId: string,
    orgId: string,
    startDate?: string,
    endDate?: string,
  ): Promise<any[]> {
    return request.get('/api/v1/workload/attendance/records', {
      params: { memberCardId, orgId, startDate, endDate },
    })
  },

  /**
   * 获取窗口期配置
   */
  async getWindowConfig(orgId: string): Promise<WindowConfig> {
    return request.get(`/api/v1/workload/window/${orgId}`)
  },

  /**
   * 更新窗口期配置
   */
  async updateWindowConfig(orgId: string, config: WindowConfig): Promise<WindowConfig> {
    return request.put(`/api/v1/workload/window/${orgId}`, config)
  },

  /**
   * 检查工时填报权限
   */
  async checkPermission(data: CheckPermissionRequest): Promise<CheckPermissionResponse> {
    return request.post('/api/v1/workload/window/check', data)
  },

  /**
   * 创建工时记录
   */
  async createWorklog(data: CreateWorklogRequest): Promise<CreateWorklogResponse> {
    return request.post('/api/v1/workload/worklog', data)
  },
}
