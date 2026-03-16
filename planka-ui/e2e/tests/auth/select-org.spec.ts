import { test, expect } from '@playwright/test'

test.describe('选择组织', () => {
  test('未登录访问工作区重定向到登录页', async ({ page, context }) => {
    // 清除认证状态
    await context.clearCookies()
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())

    await page.goto('/workspace')
    await expect(page).toHaveURL(/\/login/)
  })
})
