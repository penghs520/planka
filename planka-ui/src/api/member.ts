import request from './request'
import type { PageResult } from '@/types/api'
import type { MemberDTO, MemberOptionDTO, AddMemberRequest, UpdateMemberRoleRequest, MemberCardTypeOption } from '@/types/member'

const BASE_URL = '/api/v1/members'
const SCHEMA_BASE_URL = '/api/v1/schemas/card-types'

/**
 * 成员相关 API
 */
export const memberApi = {
  /**
   * 获取组织成员列表
   */
  list(page: number = 1, size: number = 20): Promise<PageResult<MemberDTO>> {
    return request.get(BASE_URL, { params: { page, size } })
  },

  /**
   * 获取成员卡片选项列表
   * 用于下拉选择器等场景，支持按成员名称关键字搜索
   * @param keyword 搜索关键字（可选，匹配成员卡片名称）
   */
  getOptions(page: number = 1, size: number = 50, keyword?: string): Promise<PageResult<MemberOptionDTO>> {
    return request.get(`${BASE_URL}/options`, { params: { page, size, keyword } })
  },

  /**
   * 添加成员
   */
  add(data: AddMemberRequest): Promise<MemberDTO> {
    return request.post(BASE_URL, data)
  },

  /**
   * 获取成员详情
   */
  getById(memberId: string): Promise<MemberDTO> {
    return request.get(`${BASE_URL}/${memberId}`)
  },

  /**
   * 移除成员
   */
  remove(memberId: string): Promise<void> {
    return request.delete(`${BASE_URL}/${memberId}`)
  },

  /**
   * 修改成员角色
   */
  changeRole(memberId: string, data: UpdateMemberRoleRequest): Promise<MemberDTO> {
    return request.put(`${BASE_URL}/${memberId}/role`, data)
  },

  /**
   * 获取成员卡片类型选项（用于添加成员时选择）
   * 直接调用 schema-service 的 /by-parent 接口
   * @param parentTypeId 父类型ID（成员属性集）
   */
  getMemberCardTypes(parentTypeId: string): Promise<MemberCardTypeOption[]> {
    return request.get(`${SCHEMA_BASE_URL}/by-parent`, { params: { parentTypeId } })
  },
}
