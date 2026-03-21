<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { fetchProjectsForTeam, fetchIssuesForProjects } from '@/api/team'
import type { CardDTO } from '@/types/card'
import EntityCardTable from '@/views/workspace/components/EntityCardTable.vue'

const { t } = useI18n()
const route = useRoute()
const orgStore = useOrgStore()
const cards = ref<CardDTO[]>([])
const loading = ref(false)

const teamId = computed(() => route.params.teamId as string)

async function load() {
  const orgId = orgStore.currentOrgId
  const mid = orgStore.currentMemberCardId
  const tid = teamId.value
  if (!orgId || !mid || !tid) {
    cards.value = []
    return
  }
  loading.value = true
  try {
    const projects = await fetchProjectsForTeam(orgId, mid, tid)
    const pids = projects.map((p) => p.id)
    cards.value = await fetchIssuesForProjects(orgId, mid, pids)
  } finally {
    loading.value = false
  }
}

watch(() => [orgStore.currentOrgId, orgStore.currentMemberCardId, teamId.value], load, {
  immediate: true,
})
</script>

<template>
  <div class="workspace-entity-page">
    <h1 class="page-title">{{ t('sidebar.teamIssues') }}</h1>
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
  background: var(--color-bg-1);
}

.page-title {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-1);
}
</style>
