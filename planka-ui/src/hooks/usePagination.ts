import { ref, computed } from 'vue'
import type { PageResult } from '@/types/api'

export interface PaginationOptions {
  defaultPage?: number
  defaultSize?: number
}

/**
 * 分页 Hook
 */
export function usePagination<T>(options: PaginationOptions = {}) {
  const { defaultPage = 1, defaultSize = 20 } = options

  const page = ref(defaultPage)
  const size = ref(defaultSize)
  const total = ref(0)
  const data = ref<T[]>([]) as { value: T[] }
  const loading = ref(false)

  const totalPages = computed(() => Math.ceil(total.value / size.value))
  const hasNext = computed(() => page.value < totalPages.value)
  const hasPrevious = computed(() => page.value > 1)

  function setPage(newPage: number) {
    page.value = newPage
  }

  function setSize(newSize: number) {
    size.value = newSize
    page.value = 1
  }

  function updateFromResult(result: PageResult<T>) {
    data.value = result.content
    total.value = result.total
    page.value = result.page
    size.value = result.size
  }

  function reset() {
    page.value = defaultPage
    size.value = defaultSize
    total.value = 0
    data.value = []
  }

  return {
    page,
    size,
    total,
    data,
    loading,
    totalPages,
    hasNext,
    hasPrevious,
    setPage,
    setSize,
    updateFromResult,
    reset,
  }
}
