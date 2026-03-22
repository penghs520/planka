<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { IconList, IconApps } from '@arco-design/web-vue/es/icon'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const currentView = computed(() => {
  return route.name === 'CardTypeCard' ? 'card' : 'list'
})

function switchView(value: string | number | boolean) {
  if (value === currentView.value) return
  if (value === 'card') {
    router.push({ name: 'CardTypeCard' })
  } else {
    router.push({ name: 'CardTypeList' })
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
    <a-radio value="card">
      <IconApps />
      {{ t('common.viewType.card') }}
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
