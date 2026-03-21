<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { fetchIssuesForProject } from '@/api/team'
import type { CardDTO } from '@/types/card'
import EntityCardTable from '@/views/workspace/components/EntityCardTable.vue'

const { t } = useI18n()
const route = useRoute()
const orgStore = useOrgStore()
const cards = ref<CardDTO[]>([])
const loading = ref(false)

const projectId = computed(() => route.params.projectId as string)

async function load() {
  const orgId = orgStore.currentOrgId
  const mid = orgStore.currentMemberCardId
  const pid = projectId.value
  if (!orgId || !mid || !pid) {
    cards.value = []
    return
  }
  loading.value = true
  try {
    cards.value = await fetchIssuesForProject(orgId, mid, pid)
  } finally {
    loading.value = false
  }
}

watch(() => [orgStore.currentOrgId, orgStore.currentMemberCardId, projectId.value], load, {
  immediate: true,
})
</script>

<template>
  <div class="workspace-entity-page">
    <h1 class="page-title">{{ t('sidebar.issues') }}</h1>
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
