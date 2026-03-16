import request from './request'
import { schemaApi } from './schema'
import type {
  CardTypeDefinition,
  FieldConfigListWithSource,
  FieldConfig,
} from '@/types/card-type'
import type { FieldOption } from '@/types/field-option'

const CARD_TYPE_URL = '/api/v1/schemas/card-types'

/**
 * 卡片类型 API
 */
export const cardTypeApi = {
  /**
   * 获取卡片类型
   */
  getById(cardTypeId: string): Promise<CardTypeDefinition> {
    return schemaApi.getById<CardTypeDefinition>(cardTypeId)
  },

  /**
   * 批量获取卡片类型
   */
  getByIds(cardTypeIds: string[]): Promise<CardTypeDefinition[]> {
    return schemaApi.getByIds<CardTypeDefinition>(cardTypeIds)
  },

  /**
   * 创建卡片类型
   */
  create(definition: CardTypeDefinition): Promise<CardTypeDefinition> {
    return schemaApi.create<CardTypeDefinition>({ definition })
  },

  /**
   * 更新卡片类型
   */
  update(
    cardTypeId: string,
    definition: CardTypeDefinition,
    expectedVersion?: number,
  ): Promise<CardTypeDefinition> {
    return schemaApi.update<CardTypeDefinition>(cardTypeId, { definition, expectedVersion })
  },

  /**
   * 删除卡片类型
   */
  delete(cardTypeId: string): Promise<void> {
    return schemaApi.delete(cardTypeId)
  },

  /**
   * 启用卡片类型
   */
  activate(cardTypeId: string): Promise<void> {
    return schemaApi.activate(cardTypeId)
  },

  /**
   * 停用卡片类型
   */
  disable(cardTypeId: string): Promise<void> {
    return schemaApi.disable(cardTypeId)
  },

  /**
   * 查询卡片类型列表
   */
  list(): Promise<CardTypeDefinition[]> {
    return request.get(CARD_TYPE_URL)
  },

  /**
   * 查询卡片类型选项列表（用于下拉框）
   */
  listOptions(): Promise<{ id: string; name: string; icon?: string; schemaSubType: string }[]> {
    return request.get(`${CARD_TYPE_URL}/options`)
  },

  /**
   * 查询所有卡片类型
   */
  listAll(): Promise<CardTypeDefinition[]> {
    return request.get(CARD_TYPE_URL)
  },

  /**
   * 获取卡片类型的引用摘要
   */
  getReferenceSummary(cardTypeId: string) {
    return schemaApi.getReferenceSummary(cardTypeId)
  },

  /**
   * 获取卡片类型的变更历史
   */
  getChangelog(cardTypeId: string, limit = 50) {
    return schemaApi.getChangelog(cardTypeId, limit)
  },

  /**
   * 获取卡片类型的属性配置列表
   */
  getFieldConfigsWithSource(cardTypeId: string): Promise<FieldConfigListWithSource> {
    return request.get(`/api/v1/schemas/field-configs/with-source/${cardTypeId}`)
  },

  /**
   * 保存卡片类型的单个属性配置
   */
  saveFieldConfig(cardTypeId: string, config: FieldConfig): Promise<void> {
    return request.post(`/api/v1/schemas/field-configs/${cardTypeId}`, config)
  },

  /**
   * 删除属性配置（恢复为从定义继承）
   */
  deleteFieldConfig(cardTypeId: string, fieldConfigId: string): Promise<void> {
    return request.delete(`/api/v1/schemas/field-configs/${cardTypeId}/${fieldConfigId}`)
  },


  /**
   * 获取卡片类型的属性选项（用于过滤条件等场景）
   * 返回精简的属性信息，不包含完整的配置详情
   * @param cardTypeId 卡片类型ID
   */
  async getFieldOptions(cardTypeId: string): Promise<FieldOption[]> {
    try {
      // 使用 POST 接口避免 URL 编码问题（cardTypeId 可能包含冒号等特殊字符）
      const result: { fields: FieldOption[] } = await request.post(
        '/api/v1/schemas/field-options/common',
        {
          cardTypeIds: [cardTypeId],
          fieldTypes: null
        }
      )
      return result?.fields || []
    } catch (e) {
      console.error('[CardTypeApi] Failed to fetch field options:', cardTypeId, e)
      return []
    }
  },

  /**
   * 根据父类型ID查询继承它的卡片类型
   * @param parentTypeId 父类型ID（格式：schemaId:code）
   */
  getByParent(parentTypeId: string): Promise<CardTypeDefinition[]> {
    return request.get(`${CARD_TYPE_URL}/by-parent`, {
      params: { parentTypeId }
    })
  },
}
