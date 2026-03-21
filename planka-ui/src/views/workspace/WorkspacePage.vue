<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ListViewPanel } from './components/list'

const { t } = useI18n()
const route = useRoute()

const selectedViewId = ref<string>('')

watch(
  () => route.query.viewId,
  (viewId) => {
    if (viewId && typeof viewId === 'string') {
      selectedViewId.value = viewId
    } else {
      selectedViewId.value = ''
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="workspace-page">
    <template v-if="selectedViewId">
      <ListViewPanel
        :view-id="selectedViewId"
      />
    </template>
    <template v-else>
      <div class="empty-state">
        <a-empty :description="t('common.workspace.selectViewHint')" />
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.workspace-page {
  height: 100%;
  overflow: hidden;
  background: var(--color-main-panel);
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
