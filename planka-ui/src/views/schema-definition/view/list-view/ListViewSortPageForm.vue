<script setup lang="ts">
/* listView 与 ViewEditForm 中 formData 同一引用，用于内联表单编辑 */
/* eslint-disable vue/no-mutating-props */
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { VueDraggable } from 'vue-draggable-plus'
import { IconDragDotVertical } from '@arco-design/web-vue/es/icon'
import CompactAddButton from '@/components/common/CompactAddButton.vue'
import type { ListViewDefinition, SortField } from '@/types/view'
import type { FieldOption } from '@/types/field-option'

const props = defineProps<{
  listView: ListViewDefinition
  fieldOptions: FieldOption[]
}>()

const { t } = useI18n()

/** 拖拽列表项 key（不写回 schema，仅用于 Vue 列表稳定渲染） */
const sortRowKeyMap = new WeakMap<SortField, string>()

function newSortRowUiKey(): string {
  const c = typeof globalThis !== 'undefined' ? globalThis.crypto : undefined
  if (c?.randomUUID) {
    return c.randomUUID()
  }
  return `sort-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

function getSortRowKey(row: SortField): string {
  let k = sortRowKeyMap.get(row)
  if (!k) {
    k = newSortRowUiKey()
    sortRowKeyMap.set(row, k)
  }
  return k
}

const fieldSelectOptions = computed(() =>
  props.fieldOptions.map((f) => ({
    value: f.id,
    label: f.name || f.id,
  })),
)

const dragOptions = {
  animation: 200,
  handle: '.sort-drag-handle',
  ghostClass: 'sort-row-ghost',
}

const sortsModel = computed({
  get(): SortField[] {
    return props.listView.sorts ?? []
  },
  set(val: SortField[]) {
    props.listView.sorts = val
  },
})

function ensureSorts() {
  if (!props.listView.sorts) {
    props.listView.sorts = []
  }
}

watch(
  () => props.listView,
  () => ensureSorts(),
  { immediate: true },
)

function addSortRow() {
  ensureSorts()
  props.listView.sorts!.push({ field: '', direction: 'ASC' })
}

function removeSortRow(index: number) {
  ensureSorts()
  props.listView.sorts!.splice(index, 1)
}
</script>

<template>
  <div class="sort-fields-form">
    <h4 class="section-title">{{ t('viewForm.sortRules') }}</h4>
    <VueDraggable
      v-model="sortsModel"
      v-bind="dragOptions"
      class="sort-rows-draggable"
      tag="div"
    >
      <div
        v-for="(row, index) in sortsModel"
        :key="getSortRowKey(row)"
        class="sort-row"
      >
        <span class="sort-drag-handle" :title="t('viewForm.sortDragHint')">
          <IconDragDotVertical />
        </span>
        <a-select
          v-model="row.field"
          allow-search
          :placeholder="t('viewForm.sortField')"
          class="sort-row-field"
        >
          <a-option
            v-for="opt in fieldSelectOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </a-option>
        </a-select>
        <a-select
          v-model="row.direction"
          class="sort-row-dir"
        >
          <a-option value="ASC">
            {{ t('viewForm.sortAsc') }}
          </a-option>
          <a-option value="DESC">
            {{ t('viewForm.sortDesc') }}
          </a-option>
        </a-select>
        <a-button
          type="text"
          status="danger"
          @click="removeSortRow(index)"
        >
          {{ t('viewForm.removeSort') }}
        </a-button>
      </div>
    </VueDraggable>
    <div class="sort-add-row">
      <CompactAddButton @click="addSortRow">
        {{ t('viewForm.addSort') }}
      </CompactAddButton>
    </div>
  </div>
</template>

<style scoped>
.sort-fields-form {
  max-width: 100%;
}

.section-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-2);
}

.sort-add-row {
  margin-top: 12px;
}

.sort-rows-draggable {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sort-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sort-drag-handle {
  display: inline-flex;
  flex-shrink: 0;
  cursor: grab;
  color: var(--color-text-3);
  line-height: 1;
}

.sort-drag-handle:active {
  cursor: grabbing;
}

.sort-row-ghost {
  opacity: 0.55;
}

.sort-row-field {
  flex: 1;
  min-width: 0;
}

.sort-row-dir {
  width: 120px;
}
</style>
