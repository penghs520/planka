import { defineStore } from 'pinia'
import { ref, shallowRef, watch, computed } from 'vue'
import type { CascadeRelationDefinition } from '@/types/cascade-relation'
import type { CascadeNodeDTO } from '@/api/cascade-field-options'
import { cascadeRelationApi } from '@/api/cascade-relation'
import { cascadeFieldOptionsApi } from '@/api/cascade-field-options'
import { sidebarPreferencesApi } from '@/api/sidebar-preferences'
import { orgApi } from '@/api/org'
import { useOrgStore } from '@/stores/org'
import type { CardDTO } from '@/types/card'
import { fetchProjectsForCascadeRelationNode } from '@/api/team'

function buildParentMap(
  nodes: CascadeNodeDTO[],
  parentId: string | null,
  into: Record<string, string | null>,
) {
  for (const n of nodes) {
    into[n.id] = parentId
    if (n.children?.length) {
      buildParentMap(n.children, n.id, into)
    }
  }
}

/**
 * 侧栏级联关系：目录、钉选（存后端）、树缓存
 */
export const useCascadeRelationNavStore = defineStore('cascadeRelationNav', () => {
  const orgStore = useOrgStore()
  const cascadeRelationCatalog = shallowRef<CascadeRelationDefinition[]>([])
  const pinnedCascadeRelationIds = ref<string[]>([])
  /** 组织级默认钉选；侧栏展示在用户个人钉选为空时使用 */
  const workspacePinnedCascadeRelationIds = ref<string[]>([])
  const trees = shallowRef<Record<string, CascadeNodeDTO[]>>({})
  const parentByCascadeRelation = shallowRef<Record<string, Record<string, string | null>>>({})
  const nodeProjects = shallowRef<Record<string, CardDTO[]>>({})
  const loadingCatalog = ref(false)
  const loadingTree = ref<Record<string, boolean>>({})

  function effectivePinnedCascadeRelationIds(): string[] {
    if (pinnedCascadeRelationIds.value.length > 0) {
      return pinnedCascadeRelationIds.value
    }
    return workspacePinnedCascadeRelationIds.value
  }

  const displayedCascadeRelations = computed(() => {
    const byId = new Map(cascadeRelationCatalog.value.map((s) => [String(s.id), s]))
    return effectivePinnedCascadeRelationIds()
      .map((id) => byId.get(id))
      .filter((s): s is CascadeRelationDefinition => s != null)
  })

  function clearCascadeRelationCaches() {
    cascadeRelationCatalog.value = []
    trees.value = {}
    parentByCascadeRelation.value = {}
    nodeProjects.value = {}
  }

  function clearTreesForPinnedSet(nextPinned: string[]) {
    const keep = new Set(nextPinned)
    const nextTrees = { ...trees.value }
    const nextParents = { ...parentByCascadeRelation.value }
    for (const k of Object.keys(nextTrees)) {
      if (!keep.has(k)) {
        delete nextTrees[k]
        delete nextParents[k]
      }
    }
    trees.value = nextTrees
    parentByCascadeRelation.value = nextParents
    nodeProjects.value = {}
  }

  async function fetchPinnedFromServer() {
    try {
      const dto = await sidebarPreferencesApi.get()
      pinnedCascadeRelationIds.value = [...(dto.pinnedCascadeRelationIds ?? [])]
    } catch {
      pinnedCascadeRelationIds.value = []
    }
  }

  async function fetchWorkspacePinnedFromServer() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      workspacePinnedCascadeRelationIds.value = []
      return
    }
    try {
      const dto = await orgApi.getSidebarWorkspacePreferences(orgId)
      workspacePinnedCascadeRelationIds.value = [...(dto.pinnedCascadeRelationIds ?? [])]
    } catch {
      workspacePinnedCascadeRelationIds.value = []
    }
  }

  async function loadCatalogOnly() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      cascadeRelationCatalog.value = []
      return
    }
    loadingCatalog.value = true
    try {
      const page = await cascadeRelationApi.list(1, 200)
      cascadeRelationCatalog.value = (page.content || []).filter(
        (s) => s.enabled !== false && s.levels && s.levels.length > 0,
      )
    } finally {
      loadingCatalog.value = false
    }
  }

  async function refreshCatalog() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      clearCascadeRelationCaches()
      return
    }
    await fetchWorkspacePinnedFromServer()
    await loadCatalogOnly()
    await prunePinnedToCatalogAndPersist()
    await pruneWorkspacePinnedToCatalogAndPersist()
  }

  async function prunePinnedToCatalogAndPersist() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      return
    }
    const allowed = new Set(cascadeRelationCatalog.value.map((s) => String(s.id)))
    const pruned = pinnedCascadeRelationIds.value.filter((id) => allowed.has(id))
    if (pruned.length === pinnedCascadeRelationIds.value.length) {
      return
    }
    pinnedCascadeRelationIds.value = pruned
    clearTreesForPinnedSet(effectivePinnedCascadeRelationIds())
    try {
      await sidebarPreferencesApi.update({ pinnedCascadeRelationIds: pruned })
    } catch {
      /* 拦截器已提示；本地仍以裁剪结果为准 */
    }
  }

  async function pruneWorkspacePinnedToCatalogAndPersist() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      return
    }
    const allowed = new Set(cascadeRelationCatalog.value.map((s) => String(s.id)))
    const pruned = workspacePinnedCascadeRelationIds.value.filter((id) => allowed.has(id))
    if (pruned.length === workspacePinnedCascadeRelationIds.value.length) {
      return
    }
    workspacePinnedCascadeRelationIds.value = pruned
    clearTreesForPinnedSet(effectivePinnedCascadeRelationIds())
    if (!orgStore.isAdmin) {
      return
    }
    try {
      await orgApi.updateSidebarWorkspacePreferences(orgId, { pinnedCascadeRelationIds: pruned })
    } catch {
      /* 拦截器已提示 */
    }
  }

  async function setPinnedCascadeRelationIds(ids: string[]) {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      return
    }
    const allowed = new Set(cascadeRelationCatalog.value.map((s) => String(s.id)))
    const next = ids.filter((id) => allowed.has(id))
    await sidebarPreferencesApi.update({ pinnedCascadeRelationIds: next })
    pinnedCascadeRelationIds.value = next
    clearTreesForPinnedSet(effectivePinnedCascadeRelationIds())
  }

  async function setWorkspacePinnedCascadeRelationIds(ids: string[]) {
    const orgId = orgStore.currentOrgId
    if (!orgId || !orgStore.isAdmin) {
      return
    }
    const allowed = new Set(cascadeRelationCatalog.value.map((s) => String(s.id)))
    const next = ids.filter((id) => allowed.has(id))
    await orgApi.updateSidebarWorkspacePreferences(orgId, { pinnedCascadeRelationIds: next })
    workspacePinnedCascadeRelationIds.value = next
    clearTreesForPinnedSet(effectivePinnedCascadeRelationIds())
  }

  async function bootstrapForOrg() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      pinnedCascadeRelationIds.value = []
      workspacePinnedCascadeRelationIds.value = []
      clearCascadeRelationCaches()
      return
    }
    await Promise.all([fetchPinnedFromServer(), fetchWorkspacePinnedFromServer()])
    await loadCatalogOnly()
    await prunePinnedToCatalogAndPersist()
    await pruneWorkspacePinnedToCatalogAndPersist()
  }

  async function ensureTree(cascadeRelationId: string) {
    const orgId = orgStore.currentOrgId
    if (!orgId || trees.value[cascadeRelationId]) {
      return
    }
    loadingTree.value = { ...loadingTree.value, [cascadeRelationId]: true }
    try {
      const tree = await cascadeFieldOptionsApi.queryOptions({ cascadeRelationId })
      trees.value = { ...trees.value, [cascadeRelationId]: tree }
      const pmap: Record<string, string | null> = {}
      buildParentMap(tree, null, pmap)
      parentByCascadeRelation.value = { ...parentByCascadeRelation.value, [cascadeRelationId]: pmap }
    } finally {
      const next = { ...loadingTree.value }
      delete next[cascadeRelationId]
      loadingTree.value = next
    }
  }

  function nodeCacheKey(cascadeRelationId: string, nodeId: string) {
    return `${cascadeRelationId}::${nodeId}`
  }

  async function ensureNodeProjects(cascadeRelationId: string, nodeId: string) {
    const orgId = orgStore.currentOrgId
    const mid = orgStore.currentMemberCardId
    const k = nodeCacheKey(cascadeRelationId, nodeId)
    if (!orgId || !mid || nodeProjects.value[k]) {
      return
    }
    const list = await fetchProjectsForCascadeRelationNode(orgId, mid, nodeId)
    nodeProjects.value = { ...nodeProjects.value, [k]: list }
  }

  function findNode(cascadeRelationId: string, nodeId: string): CascadeNodeDTO | undefined {
    const tree = trees.value[cascadeRelationId]
    if (!tree) return undefined
    function walk(nodes: CascadeNodeDTO[]): CascadeNodeDTO | undefined {
      for (const n of nodes) {
        if (n.id === nodeId) return n
        if (n.children?.length) {
          const found = walk(n.children)
          if (found) return found
        }
      }
      return undefined
    }
    return walk(tree)
  }

  function subtreeContains(cascadeRelationId: string, subtreeRootId: string, routeNodeId: string): boolean {
    if (routeNodeId === subtreeRootId) {
      return true
    }
    const pmap = parentByCascadeRelation.value[cascadeRelationId]
    if (!pmap) {
      return false
    }
    let cur: string | null | undefined = pmap[routeNodeId]
    while (cur) {
      if (cur === subtreeRootId) {
        return true
      }
      cur = pmap[cur] ?? null
    }
    return false
  }

  watch(
    () => orgStore.currentOrgId,
    () => {
      pinnedCascadeRelationIds.value = []
      workspacePinnedCascadeRelationIds.value = []
      clearCascadeRelationCaches()
      void bootstrapForOrg()
    },
    { immediate: true },
  )

  return {
    cascadeRelationCatalog,
    pinnedCascadeRelationIds,
    workspacePinnedCascadeRelationIds,
    displayedCascadeRelations,
    trees,
    nodeProjects,
    loadingCatalog,
    loadingTree,
    refreshCatalog,
    setPinnedCascadeRelationIds,
    setWorkspacePinnedCascadeRelationIds,
    ensureTree,
    ensureNodeProjects,
    subtreeContains,
    findNode,
    nodeCacheKey,
  }
})
