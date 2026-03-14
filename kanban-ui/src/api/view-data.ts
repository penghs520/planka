import request from './request'
import type {
  ViewDataRequest,
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
   * 根据视图 ID 查询数据
   *
   * @param viewId 视图 ID
   * @param request 查询请求
   */
  queryByViewId(viewId: string, dataRequest?: ViewDataRequest): Promise<ViewDataResponse> {
    return request.post(`${VIEW_DATA_URL}/${viewId}`, dataRequest || {})
  },

  /**
   * 查询视图分组摘要
   *
   * @param viewId 视图 ID
   * @param request 查询请求
   */
  queryGroups(viewId: string, dataRequest?: ViewDataRequest): Promise<GroupedCardData[]> {
    return request.post(`${VIEW_DATA_URL}/${viewId}/groups`, dataRequest || {})
  },

  /**
   * 预览视图数据（不保存视图定义）
   *
   * @param request 预览请求
   */
  preview(previewRequest: ViewPreviewRequest): Promise<ViewDataResponse> {
    return request.post(`${VIEW_DATA_URL}/preview`, previewRequest)
  },
}
