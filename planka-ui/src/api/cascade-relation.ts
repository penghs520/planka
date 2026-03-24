import { schemaApi } from './schema'
import { SchemaType, SchemaSubType } from '@/types/schema'
import type { PageResult } from '@/types/api'
import type { CascadeRelationDefinition, CascadeRelationDefinitionRequest } from '@/types/cascade-relation'

const ORG_ID_KEY = 'orgId'

function getOrgId(): string {
  return localStorage.getItem(ORG_ID_KEY) || ''
}

/**
 * 级联关系 API
 */
export const cascadeRelationApi = {
  /**
   * 获取级联关系列表
   */
  list(page = 1, size = 100): Promise<PageResult<CascadeRelationDefinition>> {
    return schemaApi.listByType<CascadeRelationDefinition>(SchemaType.CASCADE_RELATION_DEFINITION, page, size)
  },

  /**
   * 根据 ID 获取级联关系
   */
  getById(id: string): Promise<CascadeRelationDefinition> {
    return schemaApi.getById<CascadeRelationDefinition>(id)
  },

  /**
   * 创建级联关系
   */
  create(request: CascadeRelationDefinitionRequest): Promise<CascadeRelationDefinition> {
    return schemaApi.create<CascadeRelationDefinition>({
      definition: {
        orgId: getOrgId(),
        schemaSubType: SchemaSubType.CASCADE_RELATION_DEFINITION,
        name: request.name,
        description: request.description,
        levels: request.levels,
        systemCascadeRelation: false,
      } as CascadeRelationDefinition,
    })
  },

  /**
   * 更新级联关系
   */
  update(id: string, request: CascadeRelationDefinitionRequest, version: number): Promise<CascadeRelationDefinition> {
    return schemaApi.update<CascadeRelationDefinition>(id, {
      definition: {
        id,
        orgId: getOrgId(),
        schemaSubType: SchemaSubType.CASCADE_RELATION_DEFINITION,
        name: request.name,
        description: request.description,
        levels: request.levels,
      } as CascadeRelationDefinition,
      expectedVersion: version,
    })
  },

  /**
   * 删除级联关系
   */
  delete(id: string): Promise<void> {
    return schemaApi.delete(id)
  },

  /**
   * 启用级联关系
   */
  activate(id: string): Promise<void> {
    return schemaApi.activate(id)
  },

  /**
   * 停用级联关系
   */
  disable(id: string): Promise<void> {
    return schemaApi.disable(id)
  },
}
