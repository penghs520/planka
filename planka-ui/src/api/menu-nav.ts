import request from './request'
import type { MenuTreeVO } from '@/types/menu'

const URL = '/api/v1/schemas/menus'

function ensureChildren(nodes: MenuTreeVO['roots']): MenuTreeVO['roots'] {
  return (nodes ?? []).map((n) => ({
    ...n,
    children: ensureChildren(n.children ?? []),
  }))
}

/**
 * 侧栏菜单树（Schema 菜单分组 + 当前用户可见视图）
 */
export const menuNavApi = {
  nav(params?: { structureNodeId?: string }): Promise<MenuTreeVO> {
    return request.get(`${URL}/nav`, { params }).then((data) => {
      const d = data as unknown as MenuTreeVO
      return {
        roots: ensureChildren(d.roots ?? []),
        ungroupedViews: ensureChildren(d.ungroupedViews ?? []),
      }
    })
  },
}
