<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { menuApi } from '@/api/menu'
import { fetchProjectsForStructureNode } from '@/api/team'
import type { MenuTreeNodeVO } from '@/types/menu'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const orgStore = useOrgStore()

const loading = ref(false)
const projectIds = ref<string[]>([])
const viewRows = ref<{ id: string; name: string; cardTypeName?: string }[]>([])

const nodeId = computed(() => route.params.nodeId as string)

function flattenViews(nodes: MenuTreeNodeVO[], out: { id: string; name: string; cardTypeName?: string }[]) {
  for (const n of nodes) {
    if (n.type === 'VIEW' && n.id) {
      out.push({
        id: n.id,
        name: n.name,
        cardTypeName: n.cardTypeName,
      })
    }
    if (n.children?.length) {
      flattenViews(n.children, out)
    }
  }
}

async function loadMenu() {
  loading.value = true
  try {
    const tree = await menuApi.getMenuTree()
    const rows: { id: string; name: string; cardTypeName?: string }[] = []
    flattenViews(tree.roots || [], rows)
    flattenViews(tree.ungroupedViews || [], rows)
    viewRows.value = rows
  } finally {
    loading.value = false
  }
}

async function loadProjects() {
  const orgId = orgStore.currentOrgId
  const mid = orgStore.currentMemberCardId
  const nid = nodeId.value
  if (!orgId || !mid || !nid) {
    projectIds.value = []
    return
  }
  const projects = await fetchProjectsForStructureNode(orgId, mid, nid)
  projectIds.value = projects.map((p) => p.id)
}

function openView(viewId: string) {
  const q: Record<string, string> = { viewId }
  if (projectIds.value.length > 0) {
    q.scopeProjectIds = projectIds.value.join(',')
  }
  void router.push({ path: '/workspace', query: q })
}

onMounted(loadMenu)

watch(
  () => [orgStore.currentOrgId, orgStore.currentMemberCardId, nodeId.value],
  () => {
    void loadProjects()
  },
  { immediate: true },
)
</script>

<template>
  <div class="workspace-entity-page">
    <h1 class="page-title">{{ t('sidebar.teamViews') }}</h1>
    <p class="hint">{{ t('sidebar.structureViewsHint') }}</p>
    <a-spin :loading="loading">
      <div
        v-if="!loading && viewRows.length === 0"
        class="empty"
      >
        {{ t('sidebar.structureViewsEmpty') }}
      </div>
      <ul
        v-else
        class="view-list"
      >
        <li
          v-for="v in viewRows"
          :key="v.id"
        >
          <button
            type="button"
            class="view-link"
            @click="openView(v.id)"
          >
            <span class="view-name">{{ v.name }}</span>
            <span
              v-if="v.cardTypeName"
              class="view-meta"
            >{{ v.cardTypeName }}</span>
          </button>
        </li>
      </ul>
    </a-spin>
  </div>
</template>

<style scoped>
.workspace-entity-page {
  padding: 20px 24px;
  height: 100%;
  overflow: auto;
  background: var(--color-main-panel);
}

.page-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-1);
}

.hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--color-text-3);
  line-height: 1.5;
}

.empty {
  font-size: 14px;
  color: var(--color-text-3);
}

.view-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-width: 520px;
}

.view-link {
  display: flex;
  align-items: baseline;
  gap: 10px;
  width: 100%;
  margin-bottom: 4px;
  padding: 8px 12px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  background: var(--color-bg-2);
  cursor: pointer;
  text-align: left;
}

.view-link:hover {
  border-color: var(--color-primary-light-3);
  background: var(--color-fill-2);
}

.view-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
}

.view-meta {
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
