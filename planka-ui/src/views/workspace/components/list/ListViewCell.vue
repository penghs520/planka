<script setup lang="ts">
/**
 * 列表视图单元格组件
 * 
 * 性能优化策略：
 * - 完全移除编辑状态判断，单元格始终渲染显示内容
 * - 编辑器作为独立浮层覆盖在单元格上方，不影响单元格渲染
 * - 这样无论编辑哪个单元格，其他单元格都不会重新渲染
 */
import { computed } from 'vue'
import { IconEdit } from '@arco-design/web-vue/es/icon'
import type { CardDTO, FieldValue } from '@/types/card'
import type { ColumnMeta, StatusOption, FieldRenderConfig } from '@/types/view-data'
import { getFieldDisplayValue } from '@/types/view-data'
import { isBuiltinEnumField } from '@/utils/field-render'
import FieldDisplay from '@/components/field/display/FieldDisplay.vue'
import CardTitleDisplay from '@/components/common/CardTitleDisplay.vue'

const props = defineProps<{
  /** 卡片记录 */
  record: CardDTO
  /** 列配置 */
  column: ColumnMeta
  /** 所有列配置（用于获取显示值） */
  columns: ColumnMeta[]
  /** 状态选项（用于枚举显示） */
  statusOptions: StatusOption[]
}>()

const emit = defineEmits<{
  /** 触发编辑 */
  startEdit: [cardId: string, fieldId: string]
  /** 点击标题 */
  titleClick: [record: CardDTO]
}>()

// 从 record 获取卡片 ID
const cardId = computed(() => props.record.id || '')

// 判断是否为标题列
const isTitleColumn = computed(() => props.column.fieldId === 'title')

// 判断是否为枚举列
const isEnumColumn = computed(() => props.column.fieldType === 'ENUM')

// 获取渲染配置
const renderConfig = computed<FieldRenderConfig | undefined>(() => props.column.renderConfig)

// 获取字段值
const fieldValue = computed<FieldValue | undefined>(() => {
  return props.record.fieldValues?.[props.column.fieldId]
})

// 获取显示值
const displayValue = computed(() => {
  return getFieldDisplayValue(props.record, props.column.fieldId, props.columns, props.statusOptions)
})

// 是否可编辑
const editable = computed(() => props.column.editable !== false)

// 是否需要使用 FieldDisplay（内置枚举或普通枚举）
const useFieldDisplay = computed(() => {
  return isBuiltinEnumField(props.column.fieldId) || isEnumColumn.value
})

function handleStartEdit() {
  if (!editable.value) return
  emit('startEdit', cardId.value, props.column.fieldId)
}

function handleTitleClick() {
  emit('titleClick', props.record)
}
</script>

<template>
  <!-- 标题列特殊处理 -->
  <div v-if="isTitleColumn" class="cell-display title-cell">
    <span
      class="cell-link"
      :title="displayValue"
      @click="handleTitleClick"
    >
      <CardTitleDisplay :title="record.title" />
    </span>
    <span
      v-if="editable"
      class="title-edit-icon"
      @click.stop="handleStartEdit"
    >
      <IconEdit />
    </span>
  </div>
  
  <!-- 普通列 -->
  <div
    v-else
    class="cell-display"
    @dblclick="handleStartEdit"
  >
    <!-- 内置枚举字段或普通枚举字段：统一使用 FieldDisplay -->
    <FieldDisplay
      v-if="useFieldDisplay"
      :field-id="column.fieldId"
      :field-value="fieldValue"
      :render-config="renderConfig"
      :card="record"
      :status-options="statusOptions"
    />
    <span
      v-else
      class="cell-value"
      :title="displayValue"
    >
      {{ displayValue }}
    </span>
  </div>
</template>

<style scoped lang="scss">
.cell-display {
  width: 100%;
  min-height: 22px;
  cursor: default;

  .cell-value,
  .cell-link {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px; /* Ensure font consistency */
    line-height: 22px;
    color: var(--color-text-2);
  }

  // 标题列特殊样式
  &.title-cell {
    display: flex;
    align-items: center;
    gap: 4px;

    .cell-link {
      flex: 1;
      min-width: 0;
      color: var(--color-text-1);
      cursor: pointer;

      &:hover {
        color: rgb(var(--primary-6));
      }
    }
  }
}


// 标题编辑图标
.title-edit-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-4);
  opacity: 0;
  transition: opacity 0.2s, background-color 0.2s, color 0.2s;

  &:hover {
    background-color: rgb(var(--primary-1));
    color: rgb(var(--primary-6));
  }

  // 行 hover 时显示图标
  .arco-table-tr:hover & {
    opacity: 1;
  }
}
</style>
