import request from './request'
import type {
  ViewPreviewRequest,
  ViewDataResponse,
  GroupedCardData,
} from '@/types/view-data'

const VIEW_DATA_URL = '/api/v1/view-data'

/**
 * 视图数据 API
 */
export const viewDataApi = {
  /**
   * 根据视图 ID 查询数据（body 与后端 ViewDataRequest 对齐）
   */
  queryByViewId(
    viewId: string,
    body: Record<string, unknown> = {},
    options?: { structureNodeId?: string },
  ): Promise<ViewDataResponse> {
    return request.post(`${VIEW_DATA_URL}/${viewId}`, body, { params: options })
  },

  /**
   * 查询视图分组摘要
   *
   * @param viewId 视图 ID
   * @param request 查询请求
   */
  queryGroups(viewId: string, body: Record<string, unknown> = {}): Promise<GroupedCardData[]> {
    return request.post(`${VIEW_DATA_URL}/${viewId}/groups`, body)
  },

  /**
   * 预览视图数据（不保存视图定义）
   *
   * @param request 预览请求
   */
  preview(
    previewRequest: ViewPreviewRequest,
    options?: { structureNodeId?: string },
  ): Promise<ViewDataResponse> {
    return request.post(`${VIEW_DATA_URL}/preview`, previewRequest, { params: options })
  },
}
