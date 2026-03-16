import { test, expect } from '@playwright/test'

test.describe('登出流程', () => {
  test('登出后清除 token 并重定向到登录页', async ({ page }) => {
    await page.goto('/workspace')
    await page.waitForLoadState('networkidle')

    // 点击用户头像打开下拉菜单
    await page.locator('.user-avatar').click()
    // 点击登出选项
    await page.getByText('退出登录').click()

    // 应重定向到登录页
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 })

    // 验证 localStorage 中的 token 已清除
    const token = await page.evaluate(() => localStorage.getItem('token'))
    expect(token).toBeNull()

    const orgId = await page.evaluate(() => localStorage.getItem('orgId'))
    expect(orgId).toBeNull()
  })

  test('登出后访问受保护页面重定向到登录页', async ({ page }) => {
    await page.goto('/workspace')
    await page.waitForLoadState('networkidle')

    // 登出
    await page.locator('.user-avatar').click()
    await page.locator('.arco-dropdown-option:has(.arco-icon-poweroff), .arco-doption:has-text("退出")').click()
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 })

    // 尝试访问工作区
    await page.goto('/workspace')
    await expect(page).toHaveURL(/\/login/)
  })
})
