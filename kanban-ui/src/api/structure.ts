import { schemaApi } from './schema'
import { SchemaType, SchemaSubType } from '@/types/schema'
import type { PageResult } from '@/types/api'
import type { StructureDefinition, StructureDefinitionRequest } from '@/types/structure'

const ORG_ID_KEY = 'orgId'

function getOrgId(): string {
  return localStorage.getItem(ORG_ID_KEY) || ''
}

/**
 * 架构线 API
 */
export const structureApi = {
  /**
   * 获取架构线列表
   */
  list(page = 1, size = 100): Promise<PageResult<StructureDefinition>> {
    return schemaApi.listByType<StructureDefinition>(SchemaType.STRUCTURE_DEFINITION, page, size)
  },

  /**
   * 根据 ID 获取架构线
   */
  getById(id: string): Promise<StructureDefinition> {
    return schemaApi.getById<StructureDefinition>(id)
  },

  /**
   * 创建架构线
   */
  create(request: StructureDefinitionRequest): Promise<StructureDefinition> {
    return schemaApi.create<StructureDefinition>({
      definition: {
        orgId: getOrgId(),
        schemaSubType: SchemaSubType.STRUCTURE_DEFINITION,
        name: request.name,
        description: request.description,
        levels: request.levels,
        systemStructure: false,
      } as StructureDefinition,
    })
  },

  /**
   * 更新架构线
   */
  update(id: string, request: StructureDefinitionRequest, version: number): Promise<StructureDefinition> {
    return schemaApi.update<StructureDefinition>(id, {
      definition: {
        id,
        orgId: getOrgId(),
        schemaSubType: SchemaSubType.STRUCTURE_DEFINITION,
        name: request.name,
        description: request.description,
        levels: request.levels,
      } as StructureDefinition,
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
