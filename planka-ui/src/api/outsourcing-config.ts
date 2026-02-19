/**
 * 考勤配置 API
 */

import request from './request'
import type { OutsourcingConfig } from '@/types/outsourcing-config'

const BASE_URL = '/api/v1/outsourcing-config'

/**
 * 考勤配置 API 接口
 */
export const outsourcingConfigApi = {
  /**
   * 获取组织的考勤配置
   * @param orgId 组织ID
   * @returns 考勤配置（如果未配置则返回 null）
   */
  getByOrgId(orgId: string): Promise<OutsourcingConfig | null> {
    return request.get(`${BASE_URL}/${orgId}`)
  },

  /**
   * 保存或更新考勤配置
   * @param orgId 组织ID
   * @param config 考勤配置
   * @returns 保存后的考勤配置
   */
  saveOrUpdate(orgId: string, config: OutsourcingConfig): Promise<OutsourcingConfig> {
    return request.put(`${BASE_URL}/${orgId}`, config)
  }
}

export default outsourcingConfigApi
