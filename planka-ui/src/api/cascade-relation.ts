import { schemaApi } from './schema'
import { SchemaType, SchemaSubType } from '@/types/schema'
import type { PageResult } from '@/types/api'
import type { CascadeRelationDefinition, CascadeRelationDefinitionRequest } from '@/types/cascade-relation'

const ORG_ID_KEY = 'orgId'

function getOrgId(): string {
  return localStorage.getItem(ORG_ID_KEY) || ''
}

/**
 * 架构线 API
 */
export const cascadeRelationApi = {
  /**
   * 获取架构线列表
   */
  list(page = 1, size = 100): Promise<PageResult<CascadeRelationDefinition>> {
    return schemaApi.listByType<CascadeRelationDefinition>(SchemaType.CASCADE_RELATION_DEFINITION, page, size)
  },

  /**
   * 根据 ID 获取架构线
   */
  getById(id: string): Promise<CascadeRelationDefinition> {
    return schemaApi.getById<CascadeRelationDefinition>(id)
  },

  /**
   * 创建架构线
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
   * 更新架构线
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
   * 删除架构线
   */
  delete(id: string): Promise<void> {
    return schemaApi.delete(id)
  },

  /**
   * 启用架构线
   */
  activate(id: string): Promise<void> {
    return schemaApi.activate(id)
  },

  /**
   * 停用架构线
   */
  disable(id: string): Promise<void> {
    return schemaApi.disable(id)
  },
}
