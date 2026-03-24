<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import type { ListViewDefinition } from '@/types/view'
import { fetchAllTeamsInOrg } from '@/api/team'
import { cascadeRelationApi } from '@/api/cascade-relation'
import { cascadeFieldOptionsApi } from '@/api/cascade-field-options'
import type { CascadeNodeDTO } from '@/api/cascade-field-options'
import { getCardTitle } from '@/types/card'
import type { CardDTO } from '@/types/card'

/** TreeSelect 节点（含级联关系分组根，不可勾选） */
interface VisibilityTreeNode {
  key: string
  title: string
  levelName?: string
  isLeaf?: boolean
  selectable?: boolean
  disableCheckbox?: boolean
  checkable?: boolean
  children?: VisibilityTreeNode[]
}

interface CascadeRelationForest {
  cascadeRelationId: string
  cascadeRelationName: string
  roots: CascadeNodeDTO[]
}

const props = defineProps<{
  listView: ListViewDefinition
}>()

const { t } = useI18n()
const orgStore = useOrgStore()

const teamOptions = ref<{ value: string; label: string }[]>([])
const cascadeRelationForests = ref<CascadeRelationForest[]>([])
const loadingTeams = ref(false)
const loadingCascadeRelations = ref(false)

const scopeOptions = computed(() => [
  { value: 'PRIVATE', label: t('viewForm.visibilityPrivate') },
  { value: 'WORKSPACE', label: t('viewForm.visibilityWorkspace') },
  { value: 'TEAMS', label: t('viewForm.visibilityTeams') },
  { value: 'CASCADE_RELATION_NODE', label: t('viewForm.visibilityCascadeRelationNode') },
])

const visibilityScopeUi = computed({
  get() {
    const d = props.listView
    if (d.viewVisibilityScope) {
      return d.viewVisibilityScope
    }
    return d.shared === false ? 'PRIVATE' : 'WORKSPACE'
  },
  set(v: NonNullable<ListViewDefinition['viewVisibilityScope']>) {
    const d = props.listView
    d.viewVisibilityScope = v
    d.shared = v !== 'PRIVATE'
    if (v !== 'TEAMS') {
      d.visibleTeamCardIds = []
    }
    if (v !== 'CASCADE_RELATION_NODE') {
      d.visibleCascadeRelationNodeIds = []
    }
  },
})

function ensureIdArrays() {
  const d = props.listView
  if (!d.visibleTeamCardIds) {
    d.visibleTeamCardIds = []
  }
  if (!d.visibleCascadeRelationNodeIds) {
    d.visibleCascadeRelationNodeIds = []
  }
}

watch(
  () => props.listView,
  () => ensureIdArrays(),
  { immediate: true },
)

function mapDtoToTreeNodes(nodes: CascadeNodeDTO[]): VisibilityTreeNode[] {
  return nodes.map((n) => ({
    key: n.id,
    title: n.name,
    levelName: n.levelName,
    isLeaf: n.leaf,
    children: n.children?.length ? mapDtoToTreeNodes(n.children) : undefined,
  }))
}

const cascadeRelationVisibilityTreeData = computed<VisibilityTreeNode[]>(() =>
  cascadeRelationForests.value.map((sf) => ({
    key: `__sf__:${sf.cascadeRelationId}`,
    title: sf.cascadeRelationName || sf.cascadeRelationId,
    selectable: false,
    disableCheckbox: true,
    checkable: false,
    children: mapDtoToTreeNodes(sf.roots),
  })),
)

/** 节点 id → 「级联关系名 / … / 节点名」完整路径，用于多选回显 */
const cascadeRelationNodePathById = computed(() => {
  const map = new Map<string, string>()
  for (const sf of cascadeRelationForests.value) {
    const lineName = sf.cascadeRelationName || sf.cascadeRelationId
    const walk = (nodes: CascadeNodeDTO[], ancestors: string[]) => {
      for (const n of nodes) {
        const segments = [...ancestors, n.name]
        map.set(n.id, segments.join(' / '))
        if (n.children?.length) {
          walk(n.children, segments)
        }
      }
    }
    walk(sf.roots, [lineName])
  }
  return map
})

/** 级联树数据就绪后换 key，避免 TreeSelect 在「空 data → 异步有数据」时内部 patch 报错 */
const cascadeRelationTreeMountKey = computed(() => {
  const ids = cascadeRelationForests.value.map((f) => f.cascadeRelationId).sort()
  return `${orgStore.currentOrgId ?? ''}:${ids.join('|')}`
})

/**
 * TreeSelect 标签插槽入参为 { data: raw }，raw 含 value / label
 */
function cascadeRelationNodeTagText(data: { value?: string; label?: string } | undefined) {
  if (!data?.value) {
    return data?.label ?? ''
  }
  if (data.value === '__arco__more') {
    return data.label ?? ''
  }
  return cascadeRelationNodePathById.value.get(data.value) ?? data.label ?? data.value
}

/** 按节点标题 / 层级名搜索（默认按 key 搜对 UUID 不友好） */
function filterCascadeRelationTreeNode(searchKey: string, node: VisibilityTreeNode): boolean {
  const k = (searchKey || '').trim().toLowerCase()
  if (!k) {
    return true
  }
  const title = String(node.title ?? '').toLowerCase()
  const level = String(node.levelName ?? '').toLowerCase()
  return title.includes(k) || level.includes(k)
}

async function loadTeams() {
  const orgId = orgStore.currentOrgId
  const mid = orgStore.currentMemberCardId
  if (!orgId || !mid) {
    teamOptions.value = []
    return
  }
  loadingTeams.value = true
  try {
    const teams = await fetchAllTeamsInOrg(orgId, mid)
    teamOptions.value = teams.map((c: CardDTO) => ({
      value: c.id,
      label: getCardTitle(c) || c.id,
    }))
  } finally {
    loadingTeams.value = false
  }
}

async function loadCascadeRelationNodeOptions() {
  const orgId = orgStore.currentOrgId
  const mid = orgStore.currentMemberCardId
  if (!orgId || !mid) {
    cascadeRelationForests.value = []
    return
  }
  loadingCascadeRelations.value = true
  try {
    const page = await cascadeRelationApi.list(1, 100)
    const rows = page.content ?? []
    const forests: CascadeRelationForest[] = []
    for (const def of rows) {
      const sid = def.id
      if (!sid) {
        continue
      }
      try {
        const roots = await cascadeFieldOptionsApi.queryOptions({ cascadeRelationId: sid })
        forests.push({
          cascadeRelationId: sid,
          cascadeRelationName: def.name || sid,
          roots,
        })
      } catch {
        /* 单条级联关系失败时跳过 */
      }
    }
    cascadeRelationForests.value = forests
  } finally {
    loadingCascadeRelations.value = false
  }
}

onMounted(() => {
  void loadTeams()
  void loadCascadeRelationNodeOptions()
})

watch(
  () => [orgStore.currentOrgId, orgStore.currentMemberCardId],
  () => {
    void loadTeams()
    void loadCascadeRelationNodeOptions()
  },
)
</script>

<template>
  <div class="visibility-form">
    <a-alert
      type="warning"
      class="mb-3"
      :content="t('viewForm.visibilityAudienceNote')"
    />
    <a-form :model="listView" layout="vertical">
      <a-form-item :label="t('viewForm.visibilityScope')" required>
        <a-select v-model="visibilityScopeUi">
          <a-option
            v-for="opt in scopeOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </a-option>
        </a-select>
      </a-form-item>

      <a-form-item
        v-if="visibilityScopeUi === 'TEAMS'"
        :label="t('viewForm.visibilityTeamPick')"
      >
        <template #extra>
          <span class="form-extra">{{ t('viewForm.visibilityTeamsHint') }}</span>
        </template>
        <a-select
          v-model="listView.visibleTeamCardIds"
          multiple
          :loading="loadingTeams"
          allow-search
          :placeholder="t('viewForm.visibilityTeamPick')"
        >
          <a-option
            v-for="o in teamOptions"
            :key="o.value"
            :value="o.value"
          >
            {{ o.label }}
          </a-option>
        </a-select>
      </a-form-item>

      <a-form-item
        v-if="visibilityScopeUi === 'CASCADE_RELATION_NODE'"
        :label="t('viewForm.visibilityCascadeRelationNode')"
      >
        <template #extra>
          <span class="form-extra">{{ t('viewForm.visibilityCascadeRelationHint') }}</span>
        </template>
        <div v-if="loadingCascadeRelations" class="cascade-relation-tree-loading-wrap">
          <a-spin :loading="true" :tip="t('common.state.loading')" />
        </div>
        <a-tree-select
          v-else
          :key="cascadeRelationTreeMountKey"
          v-model="listView.visibleCascadeRelationNodeIds"
          class="cascade-relation-node-tree-select arco-select-tag-blue"
          :data="cascadeRelationVisibilityTreeData"
          :loading="false"
          multiple
          tree-checkable
          tree-check-strictly
          allow-search
          :filter-tree-node="filterCascadeRelationTreeNode"
          :max-tag-count="4"
          :dropdown-style="{ maxHeight: '360px' }"
          :placeholder="t('viewForm.visibilityCascadeRelationNode')"
        >
          <template #label="{ data }">
            <span
              class="cascade-relation-node-tag-text"
              :title="cascadeRelationNodeTagText(data)"
            >
              {{ cascadeRelationNodeTagText(data) }}
            </span>
          </template>
          <template #tree-slot-title="{ title, levelName }">
            <span class="node-title">
              {{ title }}
              <span v-if="levelName" class="node-level-name">{{ levelName }}</span>
            </span>
          </template>
        </a-tree-select>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
.visibility-form {
  max-width: 720px;
}

.mb-3 {
  margin-bottom: 12px;
}

.form-extra {
  font-size: 12px;
  color: var(--color-text-3);
  line-height: 1.4;
}

.cascade-relation-tree-loading-wrap {
  display: flex;
  align-items: center;
  min-height: 40px;
  padding: 4px 0;
}

.cascade-relation-node-tree-select {
  width: 100%;
}

.cascade-relation-node-tree-select :deep(.arco-tree-select) {
  width: 100%;
}

/*
 * 多选回显标签样式由全局 theme.css 的 .arco-select-tag-blue 提供（与 a-select 多选一致）。
 * TreeSelect 会把 class 透传到内部 SelectView / InputTag 根节点，需同时带 arco-select-tag-blue。
 */

.cascade-relation-node-tag-text {
  display: inline-block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.node-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.node-level-name {
  font-size: 12px;
  color: var(--color-text-3);
  padding: 0 4px;
  background: var(--color-fill-2);
  border-radius: 2px;
}
</style>
