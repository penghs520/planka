/**
 * 国际化配置
 */
import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'

export type SupportedLocale = 'zh-CN' | 'en-US'

const LOCALE_STORAGE_KEY = 'planka-locale'

/**
 * 获取存储的语言设置
 */
function getStoredLocale(): SupportedLocale {
  const stored = localStorage.getItem(LOCALE_STORAGE_KEY)
  if (stored === 'zh-CN' || stored === 'en-US') {
    return stored
  }
  // 根据浏览器语言设置默认值
  const browserLang = navigator.language
  if (browserLang.startsWith('en')) {
    return 'en-US'
  }
  return 'zh-CN'
}

const i18n = createI18n({
  legacy: false, // 使用 Composition API
  locale: getStoredLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

/**
 * 切换语言
 */
export function setLocale(locale: SupportedLocale) {
  i18n.global.locale.value = locale
  localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  document.documentElement.setAttribute('lang', locale)
}

/**
 * 获取当前语言
 */
export function getLocale(): SupportedLocale {
  return i18n.global.locale.value as SupportedLocale
}

/**
 * 支持的语言列表
 */
export const supportedLocales: { value: SupportedLocale; label: string }[] = [
  { value: 'zh-CN', label: '简体中文' },
  { value: 'en-US', label: 'English' },
]

export default i18n
