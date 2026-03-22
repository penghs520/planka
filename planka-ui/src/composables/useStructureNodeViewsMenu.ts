import { reactive, watch, provide, inject, type InjectionKey } from 'vue'
import { useRoute } from 'vue-router'
import type { RouteLocationRaw } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { menuNavApi } from '@/api/menu-nav'
import { fetchProjectsForStructureNode } from '@/api/team'
import type { MenuTreeNodeVO, MenuTreeVO } from '@/types/menu'

export interface StructureNodeViewsMenuState {
  loadingMenu: boolean
  menuTree: MenuTreeVO | null
  projectIds: string[]
}

export const structureNodeViewsMenuKey: InjectionKey<StructureNodeViewsMenuState> = Symbol(
  'structureNodeViewsMenu',
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
 * 架构节点页：在侧栏「视图」下展示与主侧栏一致的后台菜单树，并向子路由 provide 状态。
 */
export function useStructureNodeViewsMenu() {
  const route = useRoute()
  const orgStore = useOrgStore()

  const state = reactive<StructureNodeViewsMenuState>({
    loadingMenu: false,
    menuTree: null,
    projectIds: [],
  })

  provide(structureNodeViewsMenuKey, state)

  const nodeId = () => route.params.nodeId as string

  async function loadMenu() {
    state.loadingMenu = true
    try {
      const nid = nodeId()
      state.menuTree = await menuNavApi.nav(nid ? { structureNodeId: nid } : undefined)
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
    const projects = await fetchProjectsForStructureNode(orgId, mid, nid)
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

  /** 在当前架构节点页右栏打开视图（不跳转到全屏 /workspace） */
  function workspaceLinkForView(viewId: string): RouteLocationRaw {
    const sid = route.params.structureId as string
    const nid = route.params.nodeId as string
    const path = `/structure/${sid}/node/${nid}/views`
    const q: Record<string, string> = { viewId }
    if (state.projectIds.length > 0) {
      q.scopeProjectIds = state.projectIds.join(',')
    }
    return { path, query: q }
  }

  return { state, workspaceLinkForView, reloadMenu: loadMenu }
}

export function useStructureNodeViewsMenuInject(): StructureNodeViewsMenuState | undefined {
  return inject(structureNodeViewsMenuKey, undefined)
}
