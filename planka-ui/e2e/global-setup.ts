import { chromium, type FullConfig } from '@playwright/test'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const BASE_URL = 'http://localhost:3000'
const GATEWAY = 'http://localhost:8000'
const E2E_EMAIL = process.env.E2E_EMAIL || 'super@agilean.cn'
const E2E_PASSWORD = process.env.E2E_PASSWORD || 'changeme'

async function globalSetup(_config: FullConfig) {
  const authDir = path.join(__dirname, '.auth')
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true })
  }

  const browser = await chromium.launch()
  const context = await browser.newContext()
  const page = await context.newPage()

  // 1. 登录获取 token
  const loginRes = await page.request.post(`${GATEWAY}/api/v1/auth/login`, {
    data: { email: E2E_EMAIL, password: E2E_PASSWORD },
  })
  if (!loginRes.ok()) {
    throw new Error(`E2E 登录失败: ${loginRes.status()} ${await loginRes.text()}`)
  }
  const loginResult = await loginRes.json()
  if (!loginResult.success) {
    throw new Error(`E2E 登录业务失败: ${loginResult.message}`)
  }
  const token = loginResult.data.accessToken

  // 2. 创建新组织（每次运行唯一，自动创建成员卡片）
  const orgName = `E2E_${Date.now()}`
  const createOrgRes = await page.request.post(`${GATEWAY}/api/v1/organizations`, {
    data: { name: orgName, description: 'E2E 自动化测试组织' },
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!createOrgRes.ok()) {
    throw new Error(`E2E 创建组织失败: ${createOrgRes.status()} ${await createOrgRes.text()}`)
  }
  const createOrgResult = await createOrgRes.json()
  if (!createOrgResult.success) {
    throw new Error(`E2E 创建组织业务失败: ${createOrgResult.message}`)
  }
  const newOrgId = createOrgResult.data.id

  // 3. 切换到新组织（成员卡片已在创建时自动生成）
  const switchRes = await page.request.post(`${GATEWAY}/api/v1/auth/switch-organization`, {
    data: { orgId: newOrgId },
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!switchRes.ok()) {
    throw new Error(`E2E 切换组织失败: ${switchRes.status()} ${await switchRes.text()}`)
  }
  const switchResult = await switchRes.json()
  if (!switchResult.success) {
    throw new Error(`E2E 切换组织业务失败: ${switchResult.message}`)
  }
  const switchData = switchResult.data

  // 4. 写入 localStorage（与 src/stores/user.ts 中的 key 一致）
  await page.goto(BASE_URL)
  await page.evaluate((d) => {
    localStorage.setItem('token', d.accessToken)
    localStorage.setItem('refreshToken', d.refreshToken)
    localStorage.setItem('tokenExpiresAt', String(Date.now() + d.expiresIn * 1000))
    localStorage.setItem('orgId', d.orgId)
    localStorage.setItem('memberCardId', d.memberCardId)
  }, switchData)

  // 5. 保存 storageState
  await context.storageState({ path: path.join(authDir, 'user.json') })

  // 6. 保存上下文供 teardown 清理
  fs.writeFileSync(
    path.join(authDir, 'e2e-context.json'),
    JSON.stringify({ orgId: newOrgId, token: switchData.accessToken }),
  )

  await browser.close()
}

export default globalSetup
