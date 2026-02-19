import request from './request'
import type { FieldOption, CommonFieldOptionResponse } from '@/types/field-option'
import type { MatchingLinkFieldDTO } from '@/types/card-type'

const FIELD_OPTIONS_URL = '/api/v1/schemas/field-options'

/**
 * 属性选项 API
 * 提供卡片类型属性选项的查询接口，用于：
 * - 视图配置的列选择
 * - 筛选器的字段选择
 * - 排序字段选择
 * - 架构线配置的关联属性选择
 * - 其他需要属性列表的场景
 */
export const fieldOptionsApi = {
  /**
   * 获取单个卡片类型的字段列表（简化版，用于视图配置等场景）
   */
  async getFields(cardTypeId: string): Promise<FieldOption[]> {
    const response: CommonFieldOptionResponse = await request.post(`${FIELD_OPTIONS_URL}/common`, {
      cardTypeIds: [cardTypeId],
    })
    return response.fields
  },

  /**
   * 获取多个卡片类型的共同字段列表
   * @param cardTypeIds 卡片类型ID列表
   * @param fieldTypes 可选，属性类型过滤
   */
  getCommonFields(
    cardTypeIds: string[],
    fieldTypes?: string[]
  ): Promise<CommonFieldOptionResponse> {
    return request.post(`${FIELD_OPTIONS_URL}/common`, {
      cardTypeIds,
      fieldTypes,
    })
  },

  /**
   * 根据关联字段ID获取级联的目标卡片类型的共同属性选项
   * 当关联类型的目标端有多个卡片类型时，后端会返回这些卡片类型之间的共同属性
   * 返回精简的 FieldOption 列表，适用于属性选择场景
   */
  async getFieldsByLinkFieldId(linkFieldId: string): Promise<FieldOption[]> {
    try {
      const result: FieldOption[] = await request.get(
        `${FIELD_OPTIONS_URL}/by-link-field/${encodeURIComponent(linkFieldId)}`
      )
      return result || []
    } catch (e) {
      console.error('[FieldOptionsApi] Failed to fetch field options by link field id:', linkFieldId, e)
      return []
    }
  },

  /**
   * 获取源卡片类型和目标卡片类型之间可以匹配的关联属性
   * @param sourceCardTypeIds 源卡片类型ID列表（当前层级）
   * @param targetCardTypeIds 目标卡片类型ID列表（父层级）
   * @returns 只返回单选的关联属性（用于架构线配置，子节点只能有一个父节点）
   */
  getMatchingLinkFields(
    sourceCardTypeIds: string[],
    targetCardTypeIds: string[]
  ): Promise<{ fields: MatchingLinkFieldDTO[] }> {
    return request.post(`${FIELD_OPTIONS_URL}/matching-links`, {
      sourceCardTypeIds,
      targetCardTypeIds,
    })
  },
}
