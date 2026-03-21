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
 * 架构树查询请求（须且仅能指定其一）
 */
export type StructureOptionsRequest =
  | { structureFieldId: string; structureId?: undefined }
  | { structureId: string; structureFieldId?: undefined }

/**
 * 架构属性可选项 API
 */
export const structureOptionsApi = {
  /**
   * 查询架构树：按架构属性定义，或按架构线定义 ID
   */
  queryOptions(data: StructureOptionsRequest): Promise<StructureNodeDTO[]> {
    return request.post('/api/v1/structure-options', data)
  },
}
