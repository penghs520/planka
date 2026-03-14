import { ref, onMounted, onUnmounted } from 'vue'

export interface InfiniteScrollOptions {
  /** 触发加载的距离阈值（像素） */
  threshold?: number
  /** 滚动容器选择器，默认为 window */
  container?: string
}

/**
 * 无限滚动 Hook
 */
export function useInfiniteScroll(
  loadMore: () => Promise<void>,
  options: InfiniteScrollOptions = {},
) {
  const { threshold = 100, container } = options

  const loading = ref(false)
  const noMore = ref(false)
  const error = ref<Error | null>(null)

  let containerEl: HTMLElement | Window = window

  async function handleScroll() {
    if (loading.value || noMore.value) return

    let scrollTop: number
    let scrollHeight: number
    let clientHeight: number

    if (containerEl === window) {
      scrollTop = document.documentElement.scrollTop || document.body.scrollTop
      scrollHeight = document.documentElement.scrollHeight
      clientHeight = document.documentElement.clientHeight
    } else {
      const el = containerEl as HTMLElement
      scrollTop = el.scrollTop
      scrollHeight = el.scrollHeight
      clientHeight = el.clientHeight
    }

    if (scrollHeight - scrollTop - clientHeight < threshold) {
      loading.value = true
      error.value = null
      try {
        await loadMore()
      } catch (e) {
        error.value = e as Error
      } finally {
        loading.value = false
      }
    }
  }

  function setNoMore(value: boolean) {
    noMore.value = value
  }

  function reset() {
    noMore.value = false
    error.value = null
  }

  onMounted(() => {
    if (container) {
      const el = document.querySelector(container)
      if (el) {
        containerEl = el as HTMLElement
      }
    }
    containerEl.addEventListener('scroll', handleScroll)
  })

  onUnmounted(() => {
    containerEl.removeEventListener('scroll', handleScroll)
  })

  return {
    loading,
    noMore,
    error,
    setNoMore,
    reset,
  }
}
