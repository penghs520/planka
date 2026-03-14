import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  /** 全局加载状态 */
  const loading = ref(false)

  /** 侧边栏折叠状态 */
  const sidebarCollapsed = ref(false)

  function setLoading(value: boolean) {
    loading.value = value
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebarCollapsed(value: boolean) {
    sidebarCollapsed.value = value
  }

  return {
    loading,
    sidebarCollapsed,
    setLoading,
    toggleSidebar,
    setSidebarCollapsed,
  }
})
