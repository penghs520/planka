import { test, expect } from '../../fixtures/base'

test.describe('视图配置管理', () => {
  test('视图配置列表页正常加载', async ({ page }) => {
    await page.goto('/admin/view-config')
    await expect(page).toHaveURL(/\/admin\/view-config/)
    await page.waitForLoadState('networkidle')
  })
})
