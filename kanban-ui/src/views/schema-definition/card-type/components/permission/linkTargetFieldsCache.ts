import { fieldOptionsApi } from '@/api/field-options'
import type { FieldOption } from '@/types/field-option'

/**
 * Link 字段目标卡片类型字段缓存（按 linkFieldId 缓存 Promise，避免重复请求）
 */
const cache = new Map<string, Promise<FieldOption[]>>()

/**
 * 获取 Link 字段的目标卡片类型字段列表
 * @param linkFieldId Link 字段 ID
 * @returns 目标卡片类型的字段列表
 */
export function getLinkTargetFieldsCached(linkFieldId: string): Promise<FieldOption[]> {
  let cached = cache.get(linkFieldId)
  if (!cached) {
    cached = fieldOptionsApi.getFieldsByLinkFieldId(linkFieldId)
    cache.set(linkFieldId, cached)
  }
  return cached
}

/**
 * 清除指定 Link 字段的缓存
 */
export function clearLinkTargetFieldsCache(linkFieldId: string): void {
  cache.delete(linkFieldId)
}

/**
 * 清除所有缓存
 */
export function clearAllLinkTargetFieldsCache(): void {
  cache.clear()
}
