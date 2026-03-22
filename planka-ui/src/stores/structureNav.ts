import { defineStore } from 'pinia'
import { ref, shallowRef, watch, computed } from 'vue'
import type { StructureDefinition } from '@/types/structure'
import type { StructureNodeDTO } from '@/api/structure-options'
import { structureApi } from '@/api/structure'
import { structureOptionsApi } from '@/api/structure-options'
import { sidebarPreferencesApi } from '@/api/sidebar-preferences'
import { orgApi } from '@/api/org'
import { useOrgStore } from '@/stores/org'
import type { CardDTO } from '@/types/card'
import { fetchProjectsForStructureNode } from '@/api/team'

function buildParentMap(
  nodes: StructureNodeDTO[],
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
 * 侧栏架构线：目录、钉选（存后端）、树缓存
 */
export const useStructureNavStore = defineStore('structureNav', () => {
  const orgStore = useOrgStore()
  const structureCatalog = shallowRef<StructureDefinition[]>([])
  const pinnedStructureIds = ref<string[]>([])
  /** 组织级默认钉选；侧栏展示在用户个人钉选为空时使用 */
  const workspacePinnedStructureIds = ref<string[]>([])
  const trees = shallowRef<Record<string, StructureNodeDTO[]>>({})
  const parentByStructure = shallowRef<Record<string, Record<string, string | null>>>({})
  const nodeProjects = shallowRef<Record<string, CardDTO[]>>({})
  const loadingCatalog = ref(false)
  const loadingTree = ref<Record<string, boolean>>({})

  function effectivePinnedStructureIds(): string[] {
    if (pinnedStructureIds.value.length > 0) {
      return pinnedStructureIds.value
    }
    return workspacePinnedStructureIds.value
  }

  const displayedStructures = computed(() => {
    const byId = new Map(structureCatalog.value.map((s) => [String(s.id), s]))
    return effectivePinnedStructureIds()
      .map((id) => byId.get(id))
      .filter((s): s is StructureDefinition => s != null)
  })

  function clearStructureCaches() {
    structureCatalog.value = []
    trees.value = {}
    parentByStructure.value = {}
    nodeProjects.value = {}
  }

  function clearTreesForPinnedSet(nextPinned: string[]) {
    const keep = new Set(nextPinned)
    const nextTrees = { ...trees.value }
    const nextParents = { ...parentByStructure.value }
    for (const k of Object.keys(nextTrees)) {
      if (!keep.has(k)) {
        delete nextTrees[k]
        delete nextParents[k]
      }
    }
    trees.value = nextTrees
    parentByStructure.value = nextParents
    nodeProjects.value = {}
  }

  async function fetchPinnedFromServer() {
    try {
      const dto = await sidebarPreferencesApi.get()
      pinnedStructureIds.value = [...(dto.pinnedStructureIds ?? [])]
    } catch {
      pinnedStructureIds.value = []
    }
  }

  async function fetchWorkspacePinnedFromServer() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      workspacePinnedStructureIds.value = []
      return
    }
    try {
      const dto = await orgApi.getSidebarWorkspacePreferences(orgId)
      workspacePinnedStructureIds.value = [...(dto.pinnedStructureIds ?? [])]
    } catch {
      workspacePinnedStructureIds.value = []
    }
  }

  async function loadCatalogOnly() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      structureCatalog.value = []
      return
    }
    loadingCatalog.value = true
    try {
      const page = await structureApi.list(1, 200)
      structureCatalog.value = (page.content || []).filter(
        (s) => s.enabled !== false && s.levels && s.levels.length > 0,
      )
    } finally {
      loadingCatalog.value = false
    }
  }

  async function refreshCatalog() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      clearStructureCaches()
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
    const allowed = new Set(structureCatalog.value.map((s) => String(s.id)))
    const pruned = pinnedStructureIds.value.filter((id) => allowed.has(id))
    if (pruned.length === pinnedStructureIds.value.length) {
      return
    }
    pinnedStructureIds.value = pruned
    clearTreesForPinnedSet(effectivePinnedStructureIds())
    try {
      await sidebarPreferencesApi.update({ pinnedStructureIds: pruned })
    } catch {
      /* 拦截器已提示；本地仍以裁剪结果为准 */
    }
  }

  async function pruneWorkspacePinnedToCatalogAndPersist() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      return
    }
    const allowed = new Set(structureCatalog.value.map((s) => String(s.id)))
    const pruned = workspacePinnedStructureIds.value.filter((id) => allowed.has(id))
    if (pruned.length === workspacePinnedStructureIds.value.length) {
      return
    }
    workspacePinnedStructureIds.value = pruned
    clearTreesForPinnedSet(effectivePinnedStructureIds())
    if (!orgStore.isAdmin) {
      return
    }
    try {
      await orgApi.updateSidebarWorkspacePreferences(orgId, { pinnedStructureIds: pruned })
    } catch {
      /* 拦截器已提示 */
    }
  }

  async function setPinnedStructureIds(ids: string[]) {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      return
    }
    const allowed = new Set(structureCatalog.value.map((s) => String(s.id)))
    const next = ids.filter((id) => allowed.has(id))
    await sidebarPreferencesApi.update({ pinnedStructureIds: next })
    pinnedStructureIds.value = next
    clearTreesForPinnedSet(effectivePinnedStructureIds())
  }

  async function setWorkspacePinnedStructureIds(ids: string[]) {
    const orgId = orgStore.currentOrgId
    if (!orgId || !orgStore.isAdmin) {
      return
    }
    const allowed = new Set(structureCatalog.value.map((s) => String(s.id)))
    const next = ids.filter((id) => allowed.has(id))
    await orgApi.updateSidebarWorkspacePreferences(orgId, { pinnedStructureIds: next })
    workspacePinnedStructureIds.value = next
    clearTreesForPinnedSet(effectivePinnedStructureIds())
  }

  async function bootstrapForOrg() {
    const orgId = orgStore.currentOrgId
    if (!orgId) {
      pinnedStructureIds.value = []
      workspacePinnedStructureIds.value = []
      clearStructureCaches()
      return
    }
    await Promise.all([fetchPinnedFromServer(), fetchWorkspacePinnedFromServer()])
    await loadCatalogOnly()
    await prunePinnedToCatalogAndPersist()
    await pruneWorkspacePinnedToCatalogAndPersist()
  }

  async function ensureTree(structureId: string) {
    const orgId = orgStore.currentOrgId
    if (!orgId || trees.value[structureId]) {
      return
    }
    loadingTree.value = { ...loadingTree.value, [structureId]: true }
    try {
      const tree = await structureOptionsApi.queryOptions({ structureId })
      trees.value = { ...trees.value, [structureId]: tree }
      const pmap: Record<string, string | null> = {}
      buildParentMap(tree, null, pmap)
      parentByStructure.value = { ...parentByStructure.value, [structureId]: pmap }
    } finally {
      const next = { ...loadingTree.value }
      delete next[structureId]
      loadingTree.value = next
    }
  }

  function nodeCacheKey(structureId: string, nodeId: string) {
    return `${structureId}::${nodeId}`
  }

  async function ensureNodeProjects(structureId: string, nodeId: string) {
    const orgId = orgStore.currentOrgId
    const mid = orgStore.currentMemberCardId
    const k = nodeCacheKey(structureId, nodeId)
    if (!orgId || !mid || nodeProjects.value[k]) {
      return
    }
    const list = await fetchProjectsForStructureNode(orgId, mid, nodeId)
    nodeProjects.value = { ...nodeProjects.value, [k]: list }
  }

  function findNode(structureId: string, nodeId: string): StructureNodeDTO | undefined {
    const tree = trees.value[structureId]
    if (!tree) return undefined
    function walk(nodes: StructureNodeDTO[]): StructureNodeDTO | undefined {
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

  function subtreeContains(structureId: string, subtreeRootId: string, routeNodeId: string): boolean {
    if (routeNodeId === subtreeRootId) {
      return true
    }
    const pmap = parentByStructure.value[structureId]
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
      pinnedStructureIds.value = []
      workspacePinnedStructureIds.value = []
      clearStructureCaches()
      void bootstrapForOrg()
    },
    { immediate: true },
  )

  return {
    structureCatalog,
    pinnedStructureIds,
    workspacePinnedStructureIds,
    displayedStructures,
    trees,
    nodeProjects,
    loadingCatalog,
    loadingTree,
    refreshCatalog,
    setPinnedStructureIds,
    setWorkspacePinnedStructureIds,
    ensureTree,
    ensureNodeProjects,
    subtreeContains,
    findNode,
    nodeCacheKey,
  }
})
