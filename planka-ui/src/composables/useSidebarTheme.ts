import { ref, watchEffect } from 'vue'

export type SidebarTheme = 'dark' | 'light'

const STORAGE_KEY = 'sidebar-theme'

const theme = ref<SidebarTheme>(
  (localStorage.getItem(STORAGE_KEY) as SidebarTheme) || 'dark',
)

// 同步到 DOM 属性
watchEffect(() => {
  document.documentElement.setAttribute('data-sidebar-theme', theme.value)
})

export function useSidebarTheme() {
  function setTheme(value: SidebarTheme) {
    theme.value = value
    localStorage.setItem(STORAGE_KEY, value)
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  return {
    theme,
    setTheme,
    toggleTheme,
  }
}
