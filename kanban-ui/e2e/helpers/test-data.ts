import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/**
 * 从 global-setup 保存的上下文中读取 E2E 测试环境信息
 */
export function getE2EContext(): { orgId: string; token: string } {
  const contextPath = path.join(__dirname, '..', '.auth', 'e2e-context.json')
  return JSON.parse(fs.readFileSync(contextPath, 'utf-8'))
}

/**
 * 测试数据常量
 */
export const TEST_USERS = {
  admin: {
    email: process.env.E2E_EMAIL || 'super@agilean.cn',
    password: process.env.E2E_PASSWORD || 'changeme',
  },
} as const

/**
 * 生成唯一测试名称，避免并行测试冲突
 */
export function uniqueName(prefix: string): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
}
