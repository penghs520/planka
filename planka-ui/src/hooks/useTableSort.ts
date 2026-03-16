import { ref, computed, unref, type Ref, type ComputedRef } from 'vue'

export type SortOrder = 'ascend' | 'descend' | ''

/**
 * 表格排序 Hook
 *
 * @example
 * ```ts
 * const { sortedData, handleSortChange } = useTableSort(filteredData)
 *
 * // 在模板中：@sorter-change="handleSortChange"
 * ```
 */
export function useTableSort<T>(data: Ref<T[]> | ComputedRef<T[]>) {
  const sortField = ref('')
  const sortOrder = ref<SortOrder>('')

  const sortedData = computed(() => {
    const list = unref(data)
    if (!sortField.value || !sortOrder.value) {
      return list
    }

    return [...list].sort((a, b) => {
      const field = sortField.value as keyof T
      const aVal = a[field]
      const bVal = b[field]

      // 处理 undefined/null
      if (aVal == null && bVal == null) return 0
      if (aVal == null) return sortOrder.value === 'ascend' ? 1 : -1
      if (bVal == null) return sortOrder.value === 'ascend' ? -1 : 1

      // 字符串比较
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        const result = aVal.localeCompare(bVal)
        return sortOrder.value === 'ascend' ? result : -result
      }

      // 数字/日期比较
      const result = aVal < bVal ? -1 : aVal > bVal ? 1 : 0
      return sortOrder.value === 'ascend' ? result : -result
    })
  })

  function handleSortChange(field: string, order: SortOrder) {
    sortField.value = order ? field : ''
    sortOrder.value = order
  }

  function clearSort() {
    sortField.value = ''
    sortOrder.value = ''
  }

  return {
    sortField,
    sortOrder,
    sortedData,
    handleSortChange,
    clearSort,
  }
}
