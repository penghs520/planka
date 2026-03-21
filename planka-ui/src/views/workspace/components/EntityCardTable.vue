<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { CardDTO } from '@/types/card'
import { getCardTitle } from '@/types/card'

defineProps<{
  cards: CardDTO[]
  loading?: boolean
}>()

const { t } = useI18n()
</script>

<template>
  <a-spin :loading="loading" class="entity-card-table-spin">
    <a-table
      v-if="cards.length > 0"
      :data="cards"
      :pagination="false"
      row-key="id"
      size="small"
      :bordered="false"
    >
      <a-table-column :title="t('workspaceList.name')">
        <template #cell="{ record }">
          {{ getCardTitle(record as CardDTO) }}
        </template>
      </a-table-column>
      <a-table-column :title="t('workspaceList.cardId')" :width="200">
        <template #cell="{ record }">
          <span class="id-cell">{{ (record as CardDTO).id }}</span>
        </template>
      </a-table-column>
    </a-table>
    <a-empty
      v-else-if="!loading"
      :description="t('workspaceList.empty')"
    />
  </a-spin>
</template>

<style scoped>
.entity-card-table-spin {
  display: block;
  width: 100%;
  min-height: 120px;
}

.id-cell {
  font-size: 12px;
  color: var(--color-text-3);
  font-family: var(--font-mono, ui-monospace, monospace);
}
</style>
