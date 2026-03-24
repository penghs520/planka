import { reactive, watch, provide, inject, type InjectionKey } from 'vue'
import { useRoute } from 'vue-router'
import type { RouteLocationRaw } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { menuNavApi } from '@/api/menu-nav'
import { fetchProjectsForCascadeRelationNode } from '@/api/team'
import type { MenuTreeNodeVO, MenuTreeVO } from '@/types/menu'

export interface CascadeRelationNodeViewsMenuState {
  loadingMenu: boolean
  menuTree: MenuTreeVO | null
  projectIds: string[]
}

export const cascadeRelationNodeViewsMenuKey: InjectionKey<CascadeRelationNodeViewsMenuState> = Symbol(
  'cascadeRelationNodeViewsMenu',
)

function countViewsInNodes(nodes: MenuTreeNodeVO[]): number {
  let c = 0
  for (const n of nodes) {
    if (n.type === 'VIEW') {
      c++
    }
    if (n.children?.length) {
      c += countViewsInNodes(n.children)
    }
  }
  return c
}

export function countMenuTreeViews(tree: MenuTreeVO | null): number {
  if (!tree) {
    return 0
  }
  return countViewsInNodes(tree.roots) + countViewsInNodes(tree.ungroupedViews || [])
}

/**
 * 级联节点页：在侧栏「视图」下展示与主侧栏一致的后台菜单树，并向子路由 provide 状态。
 */
export function useCascadeRelationNodeViewsMenu() {
  const route = useRoute()
  const orgStore = useOrgStore()

  const state = reactive<CascadeRelationNodeViewsMenuState>({
    loadingMenu: false,
    menuTree: null,
    projectIds: [],
  })

  provide(cascadeRelationNodeViewsMenuKey, state)

  const nodeId = () => route.params.nodeId as string

  async function loadMenu() {
    state.loadingMenu = true
    try {
      const nid = nodeId()
      state.menuTree = await menuNavApi.nav(nid ? { cascadeRelationNodeId: nid } : undefined)
    } finally {
      state.loadingMenu = false
    }
  }

  async function loadProjects() {
    const orgId = orgStore.currentOrgId
    const mid = orgStore.currentMemberCardId
    const nid = nodeId()
    if (!orgId || !mid || !nid) {
      state.projectIds = []
      return
    }
    const projects = await fetchProjectsForCascadeRelationNode(orgId, mid, nid)
    state.projectIds = projects.map((p) => p.id)
  }

  watch(
    () => [orgStore.currentOrgId, orgStore.currentMemberCardId, route.params.nodeId],
    () => {
      void loadProjects()
    },
    { immediate: true },
  )

  watch(
    () => route.path.endsWith('/views'),
    (isViews) => {
      if (isViews) {
        void loadMenu()
      }
    },
    { immediate: true },
  )

  /** 在当前级联节点页右栏打开视图（不跳转到全屏 /workspace） */
  function workspaceLinkForView(viewId: string): RouteLocationRaw {
    const sid = route.params.cascadeRelationId as string
    const nid = route.params.nodeId as string
    const path = `/cascade-relation/${sid}/node/${nid}/views`
    const q: Record<string, string> = { viewId }
    if (state.projectIds.length > 0) {
      q.scopeProjectIds = state.projectIds.join(',')
    }
    return { path, query: q }
  }

  return { state, workspaceLinkForView, reloadMenu: loadMenu }
}

export function useCascadeRelationNodeViewsMenuInject(): CascadeRelationNodeViewsMenuState | undefined {
  return inject(cascadeRelationNodeViewsMenuKey, undefined)
}
