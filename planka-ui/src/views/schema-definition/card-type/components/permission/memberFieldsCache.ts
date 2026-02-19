import { fieldOptionsApi } from '@/api/field-options'
import type { FieldOption } from '@/types/field-option'

/**
 * 成员字段请求缓存（按 memberCardTypeId 缓存 Promise，避免重复请求）
 */
const cache = new Map<string, Promise<FieldOption[]>>()

export function getMemberFieldsCached(memberCardTypeId: string | null | undefined): Promise<FieldOption[]> {
  if (!memberCardTypeId) return Promise.resolve([])
  let cached = cache.get(memberCardTypeId)
  if (!cached) {
    cached = fieldOptionsApi.getFields(memberCardTypeId)
    cache.set(memberCardTypeId, cached)
  }
  return cached
}
