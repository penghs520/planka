/**
 * 本地存储工具
 */

const PREFIX = 'kanban_'

/**
 * 设置存储项
 */
export function setItem<T>(key: string, value: T): void {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value))
  } catch {
    console.error(`Failed to set storage item: ${key}`)
  }
}

/**
 * 获取存储项
 */
export function getItem<T>(key: string, defaultValue?: T): T | undefined {
  try {
    const item = localStorage.getItem(PREFIX + key)
    if (item === null) return defaultValue
    return JSON.parse(item) as T
  } catch {
    console.error(`Failed to get storage item: ${key}`)
    return defaultValue
  }
}

/**
 * 移除存储项
 */
export function removeItem(key: string): void {
  localStorage.removeItem(PREFIX + key)
}

/**
 * 清空所有存储项
 */
export function clear(): void {
  const keys = Object.keys(localStorage).filter((k) => k.startsWith(PREFIX))
  keys.forEach((k) => localStorage.removeItem(k))
}
