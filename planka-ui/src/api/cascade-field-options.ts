import request from './request'

/**
 * 级联节点 DTO
 */
export interface CascadeNodeDTO {
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
  children?: CascadeNodeDTO[]
}

/**
 * 级联节点树查询请求（须且仅能指定其一）
 */
export type CascadeFieldOptionsRequest =
  | { cascadeFieldId: string; cascadeRelationId?: undefined }
  | { cascadeRelationId: string; cascadeFieldId?: undefined }

/**
 * 级联属性可选项 API
 */
export const cascadeFieldOptionsApi = {
  /**
   * 查询级联树：按级联属性定义，或按级联关系定义 ID
   */
  queryOptions(data: CascadeFieldOptionsRequest): Promise<CascadeNodeDTO[]> {
    return request.post('/api/v1/cascade-field-options', data)
  },
}
