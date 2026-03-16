import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const GATEWAY = 'http://localhost:8000'

async function globalTeardown() {
  // 设置 E2E_KEEP_DATA=true 可跳过清理，方便调试
  if (process.env.E2E_KEEP_DATA) return

  const contextPath = path.join(__dirname, '.auth', 'e2e-context.json')
  if (!fs.existsSync(contextPath)) return

  const { orgId, token } = JSON.parse(fs.readFileSync(contextPath, 'utf-8'))

  // 删除 E2E 测试组织（级联清理所有数据）
  try {
    await fetch(`${GATEWAY}/api/v1/organizations/${orgId}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Org-Id': orgId,
      },
    })
  } catch {
    // 清理失败不阻塞
  }

  // 清理临时文件
  try {
    fs.unlinkSync(contextPath)
  } catch {
    // ignore
  }
}

export default globalTeardown
