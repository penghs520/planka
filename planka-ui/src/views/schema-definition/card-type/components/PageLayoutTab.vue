<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DetailTemplateTab from './DetailTemplateTab.vue'
import CreatePageTemplateTab from './CreatePageTemplateTab.vue'

defineProps<{
  cardTypeId: string
  cardTypeName?: string
}>()

const { t } = useI18n()

/** 详情页 / 新建页 子标签 */
const layoutSubTab = ref<'detail' | 'create'>('detail')
</script>

<template>
  <div class="page-layout-tab">
    <a-tabs
      v-model:active-key="layoutSubTab"
      type="line"
      class="page-layout-subtabs"
    >
      <a-tab-pane key="detail" :title="t('admin.cardType.pageLayout.tabs.detail')">
        <DetailTemplateTab
          v-if="layoutSubTab === 'detail'"
          :card-type-id="cardTypeId"
          :card-type-name="cardTypeName"
        />
      </a-tab-pane>
      <a-tab-pane key="create" :title="t('admin.cardType.pageLayout.tabs.create')">
        <CreatePageTemplateTab
          v-if="layoutSubTab === 'create'"
          :card-type-id="cardTypeId"
          :card-type-name="cardTypeName"
        />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<style scoped>
.page-layout-tab {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.page-layout-subtabs :deep(.arco-tabs-nav) {
  margin-bottom: 12px;
}

.page-layout-subtabs :deep(.arco-tabs-content) {
  padding-top: 0;
}
</style>
