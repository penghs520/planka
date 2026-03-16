import request from './request'
import { schemaApi } from './schema'
import type { CardCreatePageTemplateDefinition, CreatePageTemplateListItemVO, CreatePageFormVO } from '@/types/card-create-page-template'
import type { CreateSchemaRequest, UpdateSchemaRequest } from '@/types/schema'

const TEMPLATE_URL = '/api/v1/schemas/card-create-page-templates'

/**
 * 卡片新建页模板 API
 */
export const cardCreatePageTemplateApi = {
    /**
     * 查询模板列表
     * @param cardTypeId 可选，按卡片类型筛选
     */
    list(cardTypeId?: string): Promise<CreatePageTemplateListItemVO[]> {
        return request.get(TEMPLATE_URL, { params: { cardTypeId } })
    },

    /**
     * 根据 ID 获取模板定义
     */
    getById(templateId: string): Promise<CardCreatePageTemplateDefinition> {
        return schemaApi.getById<CardCreatePageTemplateDefinition>(templateId)
    },

    /**
     * 根据卡片类型 ID 获取所有模板
     */
    getByCardType(cardTypeId: string): Promise<CardCreatePageTemplateDefinition[]> {
        return request.get(`${TEMPLATE_URL}/by-card-type/${cardTypeId}`)
    },

    /**
     * 获取卡片类型的默认新建页模板
     */
    getDefaultByCardType(cardTypeId: string): Promise<CardCreatePageTemplateDefinition | null> {
        return request.get(`${TEMPLATE_URL}/by-card-type/${cardTypeId}/default`)
    },

    /**
     * 创建模板
     */
    create(definition: CardCreatePageTemplateDefinition): Promise<CardCreatePageTemplateDefinition> {
        const req: CreateSchemaRequest = { definition }
        return schemaApi.create<CardCreatePageTemplateDefinition>(req)
    },

    /**
     * 更新模板
     */
    update(
        templateId: string,
        definition: CardCreatePageTemplateDefinition,
        expectedVersion?: number
    ): Promise<CardCreatePageTemplateDefinition> {
        const req: UpdateSchemaRequest = { definition, expectedVersion }
        return schemaApi.update<CardCreatePageTemplateDefinition>(templateId, req)
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
    copy(templateId: string, newName: string): Promise<CardCreatePageTemplateDefinition> {
        return request.post(`${TEMPLATE_URL}/${templateId}/copy`, { newName })
    },

    /**
     * 设置为默认模板
     */
    setDefault(templateId: string): Promise<void> {
        return request.post(`${TEMPLATE_URL}/${templateId}/set-default`)
    },

    /**
     * 获取运行时表单配置
     * @param cardTypeId 卡片类型 ID
     */
    getForm(cardTypeId: string): Promise<CreatePageFormVO> {
        return request.get(`${TEMPLATE_URL}/form`, { params: { cardTypeId } })
    },
}
