<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  useCascadeRelationNodeViewsMenuInject,
  countMenuTreeViews,
} from '@/composables/useCascadeRelationNodeViewsMenu'
import { ListViewPanel } from '@/views/workspace/components/list'

const { t } = useI18n()
const route = useRoute()
const viewsMenu = useCascadeRelationNodeViewsMenuInject()

const selectedViewId = computed(() => {
  const v = route.query.viewId
  if (typeof v === 'string') {
    return v
  }
  if (Array.isArray(v) && typeof v[0] === 'string') {
    return v[0]
  }
  return ''
})

const cascadeRelationNodeId = computed(() => {
  const n = route.params.nodeId
  return typeof n === 'string' ? n : ''
})

const showHint = computed(() => {
  if (!viewsMenu || viewsMenu.loadingMenu) {
    return false
  }
  return countMenuTreeViews(viewsMenu.menuTree) > 0
})
</script>

<template>
  <div class="cascade-relation-node-views-root">
    <div
      v-if="selectedViewId"
      :key="selectedViewId"
      class="cascade-relation-node-views-panel-wrap"
    >
      <ListViewPanel
        :view-id="selectedViewId"
        :cascade-relation-node-id="cascadeRelationNodeId"
      />
    </div>
    <div
      v-else
      class="cascade-relation-node-views-hint-wrap"
    >
      <a-spin :loading="viewsMenu?.loadingMenu === true">
        <p
          v-if="showHint"
          class="placeholder hint"
        >
          {{ t('sidebar.cascadeRelationNodeViewsHint') }}
        </p>
      </a-spin>
    </div>
  </div>
</template>

<style scoped>
.cascade-relation-node-views-root {
  height: 100%;
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-main-panel);
}

.cascade-relation-node-views-panel-wrap {
  flex: 1;
  min-height: 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.cascade-relation-node-views-panel-wrap :deep(.view-data-panel) {
  flex: 1;
  min-height: 0;
}

.cascade-relation-node-views-hint-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 20px 24px;
}

.placeholder {
  margin: 0;
  max-width: 560px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-3);
}

.placeholder.hint {
  color: var(--color-text-2);
}
</style>
