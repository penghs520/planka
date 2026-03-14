<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { IconList, IconMindMapping } from '@arco-design/web-vue/es/icon'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const currentView = computed(() => {
  return route.name === 'LinkTypeGraph' ? 'graph' : 'list'
})

function switchView(value: string | number | boolean) {
  if (value === currentView.value) return
  if (value === 'graph') {
    router.push({ name: 'LinkTypeGraph' })
  } else {
    router.push({ name: 'LinkTypeList' })
  }
}
</script>

<template>
  <a-radio-group
    :model-value="currentView"
    type="button"
    size="small"
    @change="switchView"
  >
    <a-radio value="list">
      <IconList />
      {{ t('common.viewType.list') }}
    </a-radio>
    <a-radio value="graph">
      <IconMindMapping />
      {{ t('common.viewType.graph') }}
    </a-radio>
  </a-radio-group>
</template>

<style scoped>
:deep(.arco-radio-button-content) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
