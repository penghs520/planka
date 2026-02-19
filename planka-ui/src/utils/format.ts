/**
 * 格式化日期时间 (YYYY/MM/DD HH:mm)
 * @param value 日期字符串或时间戳
 */
export function formatDateTime(value?: string | number): string {
  if (!value) return '-'
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}/${month}/${day} ${hour}:${minute}`
}

/**
 * 格式化日期 (YYYY/MM/DD)
 * @param value 日期字符串或时间戳
 */
export function formatDate(value?: string | number): string {
  if (!value) return '-'
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}`
}

/**
 * 格式化时间
 */
export function formatTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

/**
 * 格式化相对时间的时间单位配置
 * 使用翻译 key，在运行时通过 t() 函数获取实际文本
 */
export interface RelativeTimeResult {
  key: string
  value?: number
}

/**
 * 获取相对时间的翻译 key 和值
 * 返回对象，需要在运行时通过 t(key, { value }) 获取实际文本
 */
export function getRelativeTime(dateStr?: string): RelativeTimeResult {
  if (!dateStr) return { key: 'common.time.empty' }
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return { key: 'common.time.daysAgo', value: days }
  if (hours > 0) return { key: 'common.time.hoursAgo', value: hours }
  if (minutes > 0) return { key: 'common.time.minutesAgo', value: minutes }
  return { key: 'common.time.justNow' }
}

/**
 * 格式化日期时间含秒 (YYYY-MM-DD HH:mm:ss)
 * 用于审计日志等需要精确到秒的场景
 * @param value 日期字符串或时间戳
 */
export function formatDateTimeWithSeconds(value?: string | number): string {
  if (!value) return '-'
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}
