import { ref, computed, unref, type Ref, type ComputedRef } from 'vue'

export interface UseTableSearchOptions<T> {
  /** 数据源 */
  data: Ref<T[]> | ComputedRef<T[]>
  /** 搜索字段列表 */
  searchFields: (keyof T)[]
}

/**
 * 表格搜索 Hook
 *
 * @example
 * ```ts
 * const { searchKeyword, filteredData } = useTableSearch({
 *   data: items,
 *   searchFields: ['name', 'code', 'description']
 * })
 * ```
 */
export function useTableSearch<T>(options: UseTableSearchOptions<T>) {
  const searchKeyword = ref('')

  const filteredData = computed(() => {
    const data = unref(options.data)
    if (!searchKeyword.value) {
      return data
    }

    const keyword = searchKeyword.value.toLowerCase()
    return data.filter((item) =>
      options.searchFields.some((field) => {
        const value = item[field]
        return String(value ?? '').toLowerCase().includes(keyword)
      }),
    )
  })

  function clearSearch() {
    searchKeyword.value = ''
  }

  return {
    searchKeyword,
    filteredData,
    clearSearch,
  }
}
