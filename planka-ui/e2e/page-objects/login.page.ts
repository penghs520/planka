import type { Page, Locator } from '@playwright/test'

/**
 * 登录页 Page Object
 */
export class LoginPage {
  readonly emailInput: Locator
  readonly passwordInput: Locator
  readonly loginButton: Locator
  readonly formWrapper: Locator

  constructor(private page: Page) {
    this.formWrapper = page.locator('.login-form-wrapper')
    this.emailInput = this.formWrapper.locator('input').first()
    this.passwordInput = this.formWrapper.locator('input[type="password"]')
    this.loginButton = this.formWrapper.locator('button[type="submit"]')
  }

  async goto() {
    await this.page.goto('/login')
  }

  async login(email: string, password: string) {
    await this.emailInput.fill(email)
    await this.passwordInput.fill(password)
    await this.loginButton.click()
  }

  async waitForFormVisible() {
    await this.formWrapper.waitFor({ state: 'visible' })
  }
}
