<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { fetchAllProjectsInOrg } from '@/api/team'
import type { CardDTO } from '@/types/card'
import EntityCardTable from './components/EntityCardTable.vue'

const { t } = useI18n()
const orgStore = useOrgStore()
const cards = ref<CardDTO[]>([])
const loading = ref(false)

async function load() {
  const orgId = orgStore.currentOrgId
  const mid = orgStore.currentMemberCardId
  if (!orgId || !mid) {
    cards.value = []
    return
  }
  loading.value = true
  try {
    cards.value = await fetchAllProjectsInOrg(orgId, mid)
  } finally {
    loading.value = false
  }
}

watch(() => [orgStore.currentOrgId, orgStore.currentMemberCardId], load)
onMounted(load)
</script>

<template>
  <div class="workspace-entity-page">
    <h1 class="page-title">{{ t('sidebar.projects') }}</h1>
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

.page-title {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-1);
}
</style>
