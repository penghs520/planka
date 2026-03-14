import i18n from '@/i18n'
import { getRelativeTime } from './format'

/**
 * 格式化相对时间（返回可读字符串）
 * @param dateStr 日期字符串
 */
export function formatRelativeTime(dateStr?: string): string {
  const result = getRelativeTime(dateStr)
  const t = i18n.global.t
  if (result.value !== undefined) {
    return t(result.key, { value: result.value })
  }
  return t(result.key)
}
