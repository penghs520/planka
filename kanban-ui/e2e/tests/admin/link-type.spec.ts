import { test, expect } from '../../fixtures/base'

test.describe('关联类型管理', () => {
  test('关联类型列表页正常加载', async ({ page }) => {
    await page.goto('/admin/link-type')
    await expect(page).toHaveURL(/\/admin\/link-type/)
    await page.waitForLoadState('networkidle')
  })

  test('切换到 ER 图视图', async ({ page }) => {
    await page.goto('/admin/link-type/graph')
    await expect(page).toHaveURL(/\/admin\/link-type\/graph/)
    await page.waitForLoadState('networkidle')
  })
})
