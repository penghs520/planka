import request from './request'
import type { Condition } from '@/types/condition'
import type { EnumOption } from '@/types/field'

const CONDITION_URL = '/api/v1/conditions'

/**
 * 卡片显示信息
 */
export interface CardDisplayInfo {
  /** 卡片 ID */
  id: string
  /** 卡片编码 */
  code: string
  /** 卡片标题 */
  title: string
}

/**
 * 条件显示信息
 *
 * 包含条件中所有需要显示名称的 ID -> Name 映射
 */
export interface ConditionDisplayInfo {
  /** 字段名称映射：fieldId -> name */
  fieldNames: Record<string, string>
  /** 关联属性名称映射：linkFieldId -> name */
  linkFieldNames: Record<string, string>
  /** 枚举选项映射：fieldId -> options */
  enumOptions: Record<string, EnumOption[]>
  /** 卡片信息映射：cardId -> CardDisplayInfo */
  cards: Record<string, CardDisplayInfo>
  /** 状态名称映射：statusId -> name */
  statusNames: Record<string, string>
}

/**
 * 获取条件显示信息的请求参数
 */
export interface GetConditionDisplayInfoRequest {
  /** 条件定义 */
  condition: Condition
}

/**
 * 条件 API
 */
export const conditionApi = {
  /**
   * 获取条件的显示信息
   *
   * 批量解析 Condition 中所有需要显示名称的 ID，返回 ID -> Name 映射。
   * 包含：字段名称、关联属性名称、枚举选项、卡片信息、状态名称。
   *
   * @param condition 条件定义
   * @returns 条件显示信息
   */
  async getDisplayInfo(condition: Condition): Promise<ConditionDisplayInfo> {
    return request.post(`${CONDITION_URL}/display-info`, {
      condition,
    } as GetConditionDisplayInfoRequest)
  },
}
