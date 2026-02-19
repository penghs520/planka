import { describe, it, expect } from 'vitest'
import { formatDateTime, formatDate, getRelativeTime } from '../format'

describe('format utils', () => {
  it('formatDateTime formats timestamp correctly', () => {
    // 使用固定时间戳测试
    const date = new Date('2024-01-01T12:00:00')
    const formatted = formatDateTime(date.getTime())
    // formatDateTime 可能会根据本地时区格式化，这里只要验证格式是否包含日期
    expect(formatted).toContain('2024/01/01')
    expect(formatted).toContain('12:00')
  })

  it('formatDateTime returns dash for empty input', () => {
    expect(formatDateTime(undefined)).toBe('-')
    expect(formatDateTime(null as any)).toBe('-')
  })

  it('formatDate formats timestamp correctly', () => {
    const date = new Date('2024-01-01T12:00:00')
    const formatted = formatDate(date.getTime())
    expect(formatted).toContain('2024/01/01')
  })

  it('getRelativeTime calculates time difference correctly', () => {
    const now = new Date()
    // 3天前
    const threeDaysAgo = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000)

    // 注意：getRelativeTime 返回的是对象 { key, value } 用于 i18n
    const result = getRelativeTime(threeDaysAgo.toISOString())
    expect(result).toEqual({
      key: 'common.time.daysAgo',
      value: 3
    })
  })

  it('getRelativeTime returns just now for recent time', () => {
    const now = new Date()
    // 30秒前
    const justNow = new Date(now.getTime() - 30 * 1000)

    const result = getRelativeTime(justNow.toISOString())
    expect(result).toEqual({
      key: 'common.time.justNow',
      value: undefined
    })
  })
})
