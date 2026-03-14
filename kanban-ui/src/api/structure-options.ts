import request from './request'

/**
 * 架构节点 DTO
 */
export interface StructureNodeDTO {
  /** 节点ID（卡片ID） */
  id: string
  /** 节点名称（卡片标题） */
  name: string
  /** 层级索引（0为根层级） */
  levelIndex: number
  /** 层级名称 */
  levelName: string
  /** 是否为叶子节点 */
  leaf: boolean
  /** 子节点列表 */
  children?: StructureNodeDTO[]
}

/**
 * 架构属性可选项查询请求
 */
export interface StructureOptionsRequest {
  /** 架构属性定义ID */
  structureFieldId: string
}

/**
 * 根据架构线ID查询可选项请求
 */
export interface StructureOptionsByIdRequest {
  /** 架构线定义ID */
  structureId: string
}

/**
 * 架构属性可选项 API
 */
export const structureOptionsApi = {
  /**
   * 查询架构属性的树形可选项
   */
  queryOptions(data: StructureOptionsRequest): Promise<StructureNodeDTO[]> {
    return request.post('/api/v1/structure-options', data)
  },

  /**
   * 根据架构线ID查询树形可选项
   */
  queryByStructureId(data: StructureOptionsByIdRequest): Promise<StructureNodeDTO[]> {
    return request.post('/api/v1/structure-options/by-structure', data)
  },
}
