import { test, expect } from '../../fixtures/base'
import { WorkspacePage } from '../../page-objects/workspace.page'

test.describe('创建卡片', () => {
  let workspace: WorkspacePage

  test.beforeEach(async ({ page }) => {
    workspace = new WorkspacePage(page)
    await workspace.goto()
    await workspace.waitForLoaded()
  })

  test('打开创建卡片弹窗', async ({ page }) => {
    // 查找并点击新建按钮（工具栏中的添加按钮）
    const addButton = page.locator('button:has-text("新建"), .btn-add-row')
    if (await addButton.isVisible()) {
      await addButton.click()
      // 应弹出创建卡片弹窗
      await expect(page.locator('.arco-modal, .card-create-modal')).toBeVisible()
    }
  })
})
