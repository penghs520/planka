import request from './request'
import { schemaApi } from './schema'
import type { CardDetailTemplateDefinition, TemplateListItemVO } from '@/types/card-detail-template'
import type { CreateSchemaRequest, UpdateSchemaRequest } from '@/types/schema'

const TEMPLATE_URL = '/api/v1/schemas/card-detail-templates'

/**
 * 卡片详情页模板 API
 */
export const cardDetailTemplateApi = {
  /**
   * 查询模板列表
   * @param cardTypeId 可选，按卡片类型筛选
   */
  list(cardTypeId?: string): Promise<TemplateListItemVO[]> {
    return request.get(TEMPLATE_URL, { params: { cardTypeId } })
  },

  /**
   * 根据 ID 获取模板定义
   */
  getById(templateId: string): Promise<CardDetailTemplateDefinition> {
    return schemaApi.getById<CardDetailTemplateDefinition>(templateId)
  },

  /**
   * 根据卡片类型 ID 获取所有模板
   */
  getByCardType(cardTypeId: string): Promise<CardDetailTemplateDefinition[]> {
    return request.get(`${TEMPLATE_URL}/by-card-type/${cardTypeId}`)
  },

  /**
   * 创建模板
   */
  create(definition: CardDetailTemplateDefinition): Promise<CardDetailTemplateDefinition> {
    const req: CreateSchemaRequest = { definition }
    return schemaApi.create<CardDetailTemplateDefinition>(req)
  },

  /**
   * 更新模板
   */
  update(
    templateId: string,
    definition: CardDetailTemplateDefinition,
    expectedVersion?: number
  ): Promise<CardDetailTemplateDefinition> {
    const req: UpdateSchemaRequest = { definition, expectedVersion }
    return schemaApi.update<CardDetailTemplateDefinition>(templateId, req)
  },

  /**
   * 删除模板
   */
  delete(templateId: string): Promise<void> {
    return schemaApi.delete(templateId)
  },

  /**
   * 启用模板
   */
  activate(templateId: string): Promise<void> {
    return schemaApi.activate(templateId)
  },

  /**
   * 停用模板
   */
  disable(templateId: string): Promise<void> {
    return schemaApi.disable(templateId)
  },

  /**
   * 复制模板
   */
  copy(templateId: string, newName: string): Promise<CardDetailTemplateDefinition> {
    return request.post(`${TEMPLATE_URL}/${templateId}/copy`, { newName })
  },

  /**
   * 设置为默认模板
   */
  setDefault(templateId: string): Promise<void> {
    return request.post(`${TEMPLATE_URL}/${templateId}/set-default`)
  },
}
