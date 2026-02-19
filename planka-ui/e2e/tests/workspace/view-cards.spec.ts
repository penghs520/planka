import { test, expect } from '../../fixtures/base'
import { WorkspacePage } from '../../page-objects/workspace.page'

test.describe('工作区视图', () => {
  let workspace: WorkspacePage

  test.beforeEach(async ({ page }) => {
    workspace = new WorkspacePage(page)
    await workspace.goto()
    await workspace.waitForLoaded()
  })

  test('工作区页面正常加载', async ({ page }) => {
    // 验证 header 可见
    await expect(workspace.header).toBeVisible()
    // 验证侧边栏可见
    await expect(workspace.sidebar).toBeVisible()
    // URL 应为 /workspace
    await expect(page).toHaveURL(/\/workspace/)
  })

  test('侧边栏菜单搜索', async ({ page }) => {
    await workspace.searchMenu('测试')
    // 搜索后菜单应过滤
    await page.waitForTimeout(300) // 等待搜索防抖
  })
})
