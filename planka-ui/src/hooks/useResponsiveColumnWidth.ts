import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * 根据当前语言动态计算表格列宽
 * 英文翻译通常比中文长，需要更宽的列宽
 */
export function useResponsiveColumnWidth() {
  const { locale } = useI18n()

  const isEnglish = computed(() => locale.value === 'en-US')

  // 列宽配置：{ 中文宽度, 英文宽度 }
  const columnWidth = computed(() => ({
    // 系统内置列
    systemBuiltin: isEnglish.value ? 130 : 100,
    // 定义级别列
    definitionLevel: isEnglish.value ? 150 : 100,
    // 状态列
    status: isEnglish.value ? 90 : 80,
  }))

  // 表格滚动宽度增量（英文比中文多出的宽度）
  const scrollXExtra = computed(() => (isEnglish.value ? 150 : 0))

  return {
    isEnglish,
    columnWidth,
    scrollXExtra,
  }
}
