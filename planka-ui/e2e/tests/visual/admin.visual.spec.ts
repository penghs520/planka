import { test, expect } from '@playwright/test'

test.describe('管理后台视觉回归', () => {
  test('卡片类型列表页', async ({ page }) => {
    await page.goto('/admin/card-type')
    await page.waitForLoadState('networkidle')
    await expect(page).toHaveScreenshot('admin-card-type-list.png')
  })

  test('关联类型 ER 图', async ({ page }) => {
    await page.goto('/admin/link-type/graph')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500) // 等待 Vue Flow 渲染
    await expect(page).toHaveScreenshot('admin-link-type-er.png', {
      maxDiffPixelRatio: 0.02,
    })
  })
})
