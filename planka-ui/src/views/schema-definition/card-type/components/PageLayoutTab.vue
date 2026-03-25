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

/** 详情页布局 / 新建页布局 */
const section = ref<'detail' | 'create'>('detail')

/** 详情模板编辑器工具栏（全屏/保存/撤销）Teleport 挂载点，与左侧切换同一行 */
const detailToolbarActionsEl = ref<HTMLElement | null>(null)
</script>

<template>
  <div class="page-layout-tab">
    <div class="page-layout-toolbar">
      <div class="page-layout-switch page-layout-segment">
        <a-radio-group v-model="section" type="button" size="small">
          <a-radio value="detail">
            {{ t('admin.cardType.pageLayout.tabs.detail') }}
          </a-radio>
          <a-radio value="create">
            {{ t('admin.cardType.pageLayout.tabs.create') }}
          </a-radio>
        </a-radio-group>
      </div>
      <!-- 与 TemplateEditorCore Teleport 对齐，消除中间大块空白 -->
      <div
        v-show="section === 'detail'"
        ref="detailToolbarActionsEl"
        class="page-layout-toolbar-actions"
      />
    </div>

    <div v-if="section === 'detail'" class="page-layout-detail">
      <DetailTemplateTab
        :card-type-id="cardTypeId"
        :card-type-name="cardTypeName"
        :toolbar-actions-target="detailToolbarActionsEl"
      />
    </div>

    <div v-if="section === 'create'" class="page-layout-create">
      <CreatePageTemplateTab
        :card-type-id="cardTypeId"
        :card-type-name="cardTypeName"
      />
    </div>
  </div>
</template>

<style scoped>
.page-layout-tab {
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  min-height: 0;
  min-width: 0;
  width: 100%;
  /* 切换条与下方编辑器（字段库 + 画布）留出呼吸间距 */
  gap: 10px;
}

.page-layout-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
  min-width: 0;
}

.page-layout-switch {
  flex-shrink: 0;
}

/* 仅选中项浅底高亮，未选透明（对称占位） */
.page-layout-segment :deep(.arco-radio-group-button) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 0;
}

.page-layout-segment :deep(.arco-radio-button) {
  margin: 0;
  border-radius: 6px;
  padding: 2px 8px;
  box-shadow: none;
  transition:
    color 0.15s ease,
    background-color 0.15s ease;
}

.page-layout-segment :deep(.arco-radio-button:not(.arco-radio-checked)) {
  background: transparent;
  color: var(--color-text-2);
}

.page-layout-segment :deep(.arco-radio-button:not(.arco-radio-checked):hover) {
  color: var(--color-text-1);
  background: transparent;
}

.page-layout-segment :deep(.arco-radio-button.arco-radio-checked) {
  color: rgb(var(--primary-6));
  background: rgb(var(--primary-1));
  font-weight: 500;
}

.page-layout-segment :deep(.arco-radio-button.arco-radio-checked:hover) {
  color: rgb(var(--primary-6));
  background: rgb(var(--primary-1));
}

.page-layout-segment :deep(.arco-radio-button::before) {
  display: none;
}

.page-layout-toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex: 1;
  min-width: 0;
}

.page-layout-detail {
  flex: 1;
  min-height: 0;
  min-width: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.page-layout-create {
  flex: 1;
  min-height: 0;
}
</style>
