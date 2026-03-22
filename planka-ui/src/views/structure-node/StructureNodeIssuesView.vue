<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { fetchIssuesForProjects, fetchProjectsForStructureNode } from '@/api/team'
import type { CardDTO } from '@/types/card'
import EntityCardTable from '@/views/workspace/components/EntityCardTable.vue'

const route = useRoute()
const orgStore = useOrgStore()
const cards = ref<CardDTO[]>([])
const loading = ref(false)

const nodeId = computed(() => route.params.nodeId as string)

async function load() {
  const orgId = orgStore.currentOrgId
  const mid = orgStore.currentMemberCardId
  const nid = nodeId.value
  if (!orgId || !mid || !nid) {
    cards.value = []
    return
  }
  loading.value = true
  try {
    const projects = await fetchProjectsForStructureNode(orgId, mid, nid)
    const pids = projects.map((p) => p.id)
    cards.value = await fetchIssuesForProjects(orgId, mid, pids)
  } finally {
    loading.value = false
  }
}

watch(() => [orgStore.currentOrgId, orgStore.currentMemberCardId, nodeId.value], load, {
  immediate: true,
})
</script>

<template>
  <div class="workspace-entity-page">
    <EntityCardTable
      :cards="cards"
      :loading="loading"
    />
  </div>
</template>

<style scoped>
.workspace-entity-page {
  padding: 20px 24px;
  height: 100%;
  overflow: auto;
  background: var(--color-main-panel);
}

</style>
