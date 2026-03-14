import { test, expect } from '../../fixtures/base'

test.describe('组织切换', () => {
  test('访问组织选择页', async ({ page }) => {
    await page.goto('/select-org')
    await page.waitForLoadState('networkidle')
    // 已登录用户应能访问组织选择页
    await expect(page).toHaveURL(/\/select-org/)
  })

  test('组织选择页显示组织列表', async ({ page }) => {
    await page.goto('/select-org')
    await page.waitForLoadState('networkidle')
    // 页面应包含组织选择相关内容
    await expect(page.locator('.select-org-container')).toBeVisible()
  })
})
