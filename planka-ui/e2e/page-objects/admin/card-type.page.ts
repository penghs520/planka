import type { Page, Locator } from '@playwright/test'
import { arco } from '../../helpers/selectors'

/**
 * 卡片类型管理页 Page Object
 */
export class CardTypePage {
  readonly table: Locator
  readonly createButton: Locator
  readonly searchInput: Locator
  readonly formDrawer: Locator

  constructor(private page: Page) {
    this.table = page.locator(arco.table)
    this.createButton = page.locator('.create-button, button:has-text("新建")')
    this.searchInput = page.locator('.search-input input, input[placeholder*="搜索"]')
    this.formDrawer = page.locator(arco.drawer)
  }

  async goto() {
    await this.page.goto('/admin/card-type')
  }

  async waitForLoaded() {
    await this.table.waitFor({ state: 'visible' })
  }

  async clickCreate() {
    await this.createButton.click()
  }

  async waitForDrawer() {
    await this.formDrawer.waitFor({ state: 'visible' })
  }

  async getTableRow(name: string) {
    return this.page.locator(arco.tableRow(name))
  }

  async search(keyword: string) {
    await this.searchInput.fill(keyword)
  }
}
