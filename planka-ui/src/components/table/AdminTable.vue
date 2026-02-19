<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { SortOrder } from '@/hooks/useTableSort'

const { t } = useI18n()

interface Props {
  /** 表格数据 */
  data: unknown[]
  /** 加载状态 */
  loading?: boolean
  /** 行唯一标识 */
  rowKey?: string | ((record: unknown) => string)
  /** 表格大小 */
  size?: 'mini' | 'small' | 'medium' | 'large'
  /** 是否显示边框 */
  bordered?: boolean
  /** 滚动配置 */
  scroll?: { x?: number | string; y?: number | string }
  /** 空状态文字 */
  emptyText?: string
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  rowKey: 'id',
  size: 'small',
  bordered: false,
  scroll: () => ({ x: 1200, y: '100%' }),
  emptyText: undefined,
})

const emit = defineEmits<{
  /** 排序变化事件 */
  (e: 'sorter-change', dataIndex: string, direction: SortOrder): void
  /** 行点击事件 */
  (e: 'row-click', record: unknown): void
}>()

function handleSorterChange(dataIndex: string, direction: SortOrder) {
  emit('sorter-change', dataIndex, direction)
}

function handleRowClick(record: unknown) {
  emit('row-click', record)
}
</script>

<template>
  <div class="admin-table-container">
    <a-spin :loading="props.loading" class="admin-table-spin">
      <a-table
        :data="props.data"
        :pagination="false"
        :bordered="props.bordered"
        :row-key="props.rowKey"
        :size="props.size"
        :scroll="props.scroll"
        @sorter-change="handleSorterChange"
        @row-click="handleRowClick"
      >
        <template #columns>
          <slot />
        </template>
        <template #empty>
          <slot name="empty">
            <a-empty :description="props.emptyText || t('common.state.empty')" />
          </slot>
        </template>
      </a-table>
    </a-spin>
  </div>
</template>

<style scoped>
.admin-table-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-table-spin {
  flex: 1;
  overflow: auto;
}

/* 表格内链接样式：默认黑色，hover 时蓝色 */
:deep(.arco-link) {
  color: var(--color-text-1);
  font-weight: normal;

  &:hover {
    color: rgb(var(--primary-6));
  }
}
</style>
