<script setup lang="ts">
/* 与父级共用 listView 引用，直接改写字段 */
/* eslint-disable vue/no-mutating-props */
import { computed, ref, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ListViewDefinition } from '@/types/view'
import type { FieldOption } from '@/types/field-option'
import type { LinkTypeVO } from '@/types/link-type'
import ColumnConfigEditor from './ColumnConfigEditor.vue'
import ListViewVisibilityForm from './ListViewVisibilityForm.vue'
import ListViewSortPageForm from './ListViewSortPageForm.vue'
import ConditionEditor from '@/components/condition/ConditionEditor.vue'
import { useListViewGroupByOptions } from './composables/useListViewGroupByOptions'
import { useListViewFormValidation } from './composables/useListViewFormValidation'

const props = defineProps<{
  listView: ListViewDefinition
  mode: 'create' | 'edit'
  activeTab: string
  /** 新建且从架构节点打开时展示 */
  structureNodeContextId?: string
  cardTypeOptions: { value: string; label: string }[]
  loadingCardTypes: boolean
  availableFields: FieldOption[]
  loadingFields: boolean
  linkTypes: LinkTypeVO[]
  rootCardTypeName: string
}>()

const emit = defineEmits<{
  'update:activeTab': [value: string]
}>()

const { t } = useI18n()

const activeTabModel = computed({
  get: () => props.activeTab,
  set: (v: string) => emit('update:activeTab', v),
})

const { groupByFieldGroups } = useListViewGroupByOptions(toRef(props, 'availableFields'))

const formDataRef = computed(() => props.listView ?? null)
const { hasIncompleteSortRules } = useListViewFormValidation(formDataRef)

const conditionEditorRef = ref<InstanceType<typeof ConditionEditor> | null>(null)

/** 过滤条件未挂载（未打开过滤 Tab）时视为通过 */
function validateFilterCondition(): boolean {
  if (!conditionEditorRef.value) return true
  return conditionEditorRef.value.validate()
}

defineExpose({
  validateFilterCondition,
})
</script>

<template>
  <div class="list-view-edit-form">
    <a-alert
      v-if="mode === 'create' && structureNodeContextId"
      type="info"
      class="context-alert"
      :content="t('sidebar.createViewStructureScopeHint')"
    />
    <a-tabs
      v-model:active-key="activeTabModel"
      class="edit-tabs"
      lazy-load
      :header-padding="false"
    >
      <a-tab-pane key="basic" :title="t('viewForm.tabBasic')">
        <a-form :model="listView" layout="vertical" class="basic-info-form">
          <div class="basic-info-form-inner">
            <a-form-item>
              <template #label>
                <span class="required-field-label">
                  <span class="required-field-label__star">*</span>
                  {{ t('viewForm.viewName') }}
                </span>
              </template>
              <a-input
                v-model="listView.name"
                :placeholder="t('viewForm.viewNamePlaceholder')"
                :max-length="50"
              />
            </a-form-item>
            <a-form-item>
              <template #label>
                <span class="required-field-label">
                  <span class="required-field-label__star">*</span>
                  {{ t('viewForm.cardType') }}
                </span>
              </template>
              <a-select
                v-model="listView.cardTypeId"
                :placeholder="t('viewForm.validation.cardTypeRequired')"
                :loading="loadingCardTypes"
                allow-search
                :disabled="mode === 'edit'"
              >
                <a-option
                  v-for="option in cardTypeOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-option>
              </a-select>
            </a-form-item>

            <a-form-item :label="t('viewForm.groupBy')">
              <a-select
                v-model="listView.groupBy"
                allow-clear
                allow-search
                :loading="loadingFields"
                :disabled="!listView.cardTypeId"
                :placeholder="t('viewForm.groupByPlaceholder')"
              >
                <a-optgroup
                  v-for="g in groupByFieldGroups"
                  :key="g.key"
                  :label="g.label"
                >
                  <a-option
                    v-for="o in g.options"
                    :key="o.value"
                    :value="o.value"
                  >
                    {{ o.label }}
                  </a-option>
                </a-optgroup>
              </a-select>
            </a-form-item>

            <div
              v-if="listView.cardTypeId"
              class="basic-sort-block"
            >
              <a-spin :loading="loadingFields" style="width: 100%">
                <ListViewSortPageForm
                  :list-view="listView"
                  :field-options="availableFields"
                />
              </a-spin>
              <p
                v-if="hasIncompleteSortRules"
                class="basic-field-invalid-hint"
                role="alert"
              >
                {{ t('viewForm.validation.sortFieldRequired') }}
              </p>
            </div>
            <p
              v-else
              class="basic-card-type-hint"
            >
              {{ t('viewForm.selectCardTypeFirst') }}
            </p>

            <a-form-item :label="t('viewForm.description')">
              <a-textarea
                v-model="listView.description"
                :max-length="200"
                :auto-size="{ minRows: 2, maxRows: 4 }"
              />
            </a-form-item>
          </div>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="visibility" :title="t('viewForm.tabVisibility')">
        <ListViewVisibilityForm :list-view="listView" />
      </a-tab-pane>

      <a-tab-pane key="columns" :title="t('viewForm.tabColumns')">
        <ColumnConfigEditor
          v-if="listView.cardTypeId && activeTab === 'columns'"
          :model-value="listView.columnConfigs || []"
          :card-type-id="listView.cardTypeId"
          @update:model-value="listView.columnConfigs = $event"
        />
        <a-empty
          v-else-if="!listView.cardTypeId"
          :description="t('viewForm.selectCardTypeFirst')"
        />
      </a-tab-pane>

      <a-tab-pane key="filter" :title="t('viewForm.tabFilter')">
        <div v-if="listView.cardTypeId && activeTab === 'filter'" class="filter-tab">
          <a-spin :loading="loadingFields" style="width: 100%">
            <ConditionEditor
              ref="conditionEditorRef"
              v-model="listView.condition"
              :card-type-id="listView.cardTypeId"
              :available-fields="availableFields"
              :link-types="linkTypes"
              :root-card-type-name="rootCardTypeName"
            />
          </a-spin>
        </div>
        <a-empty
          v-else-if="!listView.cardTypeId"
          :description="t('viewForm.selectCardTypeFirst')"
        />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<style scoped lang="scss">
.list-view-edit-form {
  min-height: 0;
}

.context-alert {
  margin-bottom: 16px;
}

.edit-tabs {
  height: 100%;
}

.edit-tabs :deep(.arco-tabs-nav .arco-tabs-tab) {
  font-weight: 500;
}

.edit-tabs :deep(.arco-tabs-nav .arco-tabs-tab.arco-tabs-tab-active) {
  font-weight: 500;
}

.edit-tabs :deep(.arco-tabs-content) {
  padding-top: 16px;
}

.filter-tab {
  padding: 0;
}

.basic-info-form :deep(.arco-form-item-label) {
  font-weight: 500;
  color: var(--color-text-2);
}

.required-field-label__star {
  margin-right: 4px;
  color: rgb(var(--danger-6));
}

.basic-info-form-inner {
  width: 60%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.basic-sort-block {
  margin-top: 4px;
  margin-bottom: 16px;
}

.basic-field-invalid-hint {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-danger);
}

.basic-card-type-hint {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-3);
}
</style>
