import type { Page, Locator } from '@playwright/test'
import { arco } from '../../helpers/selectors'

/**
 * 视图配置管理页 Page Object
 */
export class ViewConfigPage {
  readonly table: Locator
  readonly createButton: Locator

  constructor(private page: Page) {
    this.table = page.locator(arco.table)
    this.createButton = page.locator('.create-button, button:has-text("新建")')
  }

  async goto() {
    await this.page.goto('/admin/view-config')
  }

  async waitForLoaded() {
    await this.table.waitFor({ state: 'visible' })
  }
}
