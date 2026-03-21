import { ref, watchEffect } from 'vue'

const STORAGE_KEY = 'planka-ui-theme'
const LEGACY_STORAGE_KEY = 'sidebar-theme'

export const UI_THEME_IDS = ['pine-light', 'rose-pine-dawn', 'ash', 'notion-light'] as const

export type UiThemeId = (typeof UI_THEME_IDS)[number]

export const THEME_OPTIONS: { id: UiThemeId; labelKey: string }[] = [
  { id: 'pine-light', labelKey: 'sidebar.themePineLight' },
  { id: 'rose-pine-dawn', labelKey: 'sidebar.themeRosePineDawn' },
  { id: 'ash', labelKey: 'sidebar.themeAsh' },
  { id: 'notion-light', labelKey: 'sidebar.themeNotionLight' },
]

function isUiThemeId(value: string): value is UiThemeId {
  return (UI_THEME_IDS as readonly string[]).includes(value)
}

function readStoredTheme(): UiThemeId {
  try {
    const next = localStorage.getItem(STORAGE_KEY)
    if (next && isUiThemeId(next))
      return next
    if (next)
      localStorage.removeItem(STORAGE_KEY)

    const legacy = localStorage.getItem(LEGACY_STORAGE_KEY)
    if (legacy === 'dark' || legacy === 'light') {
      localStorage.removeItem(LEGACY_STORAGE_KEY)
      localStorage.setItem(STORAGE_KEY, 'pine-light')
      return 'pine-light'
    }
  }
  catch {
    /* ignore */
  }
  return 'pine-light'
}

const theme = ref<UiThemeId>(readStoredTheme())

if (typeof localStorage !== 'undefined') {
  try {
    if (!localStorage.getItem(STORAGE_KEY) && isUiThemeId(theme.value))
      localStorage.setItem(STORAGE_KEY, theme.value)
  }
  catch {
    /* ignore */
  }
}

watchEffect(() => {
  const el = document.documentElement
  el.removeAttribute('data-sidebar-theme')
  el.setAttribute('data-ui-theme', theme.value)
})

export function useSidebarTheme() {
  function setTheme(value: UiThemeId) {
    theme.value = value
    try {
      localStorage.setItem(STORAGE_KEY, value)
    }
    catch {
      /* ignore */
    }
  }

  return {
    theme,
    setTheme,
  }
}
