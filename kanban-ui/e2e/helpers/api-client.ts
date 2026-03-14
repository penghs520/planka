import type { APIRequestContext } from '@playwright/test'

/**
 * 后端 API 直调客户端，用于测试数据的 setup/teardown
 */
export class ApiClient {
  constructor(
    private request: APIRequestContext,
    private baseURL = 'http://localhost:8000',
  ) {}

  private getHeaders(token: string, orgId: string) {
    return {
      Authorization: `Bearer ${token}`,
      'X-Org-Id': orgId,
    }
  }

  async createCardType(name: string, token: string, orgId: string) {
    return this.request.post(`${this.baseURL}/api/v1/schemas`, {
      data: { name, type: 'CARD_TYPE' },
      headers: this.getHeaders(token, orgId),
    })
  }

  async deleteSchema(id: string, token: string, orgId: string) {
    return this.request.delete(`${this.baseURL}/api/v1/schemas/${id}`, {
      headers: this.getHeaders(token, orgId),
    })
  }

  async createCard(data: Record<string, unknown>, token: string, orgId: string) {
    return this.request.post(`${this.baseURL}/api/v1/cards`, {
      data,
      headers: this.getHeaders(token, orgId),
    })
  }

  async deleteCard(id: string, token: string, orgId: string) {
    return this.request.delete(`${this.baseURL}/api/v1/cards/${id}`, {
      headers: this.getHeaders(token, orgId),
    })
  }
}
