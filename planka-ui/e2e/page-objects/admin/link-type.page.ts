import type { Page, Locator } from '@playwright/test'
import { arco } from '../../helpers/selectors'

/**
 * 关联类型管理页 Page Object
 */
export class LinkTypePage {
  readonly table: Locator
  readonly createButton: Locator
  readonly listTab: Locator
  readonly erTab: Locator

  constructor(private page: Page) {
    this.table = page.locator(arco.table)
    this.createButton = page.locator('.create-button, button:has-text("新建")')
    this.listTab = page.locator(arco.tabPane('列表'))
    this.erTab = page.locator(arco.tabPane('ER'))
  }

  async goto() {
    await this.page.goto('/admin/link-type')
  }

  async gotoERDiagram() {
    await this.page.goto('/admin/link-type/graph')
  }

  async waitForLoaded() {
    await this.table.waitFor({ state: 'visible' })
  }

  async switchToER() {
    await this.erTab.click()
  }

  async switchToList() {
    await this.listTab.click()
  }
}
