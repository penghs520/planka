import { ref } from 'vue'

/**
 * 加载状态 Hook
 */
export function useLoading(initialValue = false) {
  const loading = ref(initialValue)

  function startLoading() {
    loading.value = true
  }

  function stopLoading() {
    loading.value = false
  }

  async function withLoading<T>(fn: () => Promise<T>): Promise<T> {
    try {
      startLoading()
      return await fn()
    } finally {
      stopLoading()
    }
  }

  return {
    loading,
    startLoading,
    stopLoading,
    withLoading,
  }
}
