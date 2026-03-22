import { computed, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ListViewDefinition } from '@/types/view'

/** 列表视图：排序行与主按钮禁用/tooltip 一致；名称与卡片类型仅在保存时 Message 提示（见 ViewEditForm.handleSave） */
export function useListViewFormValidation(formData: Ref<ListViewDefinition | null>) {
  const { t } = useI18n()

  const hasIncompleteSortRules = computed(() => {
    const sorts = formData.value?.sorts
    if (!sorts?.length) return false
    return sorts.some((s) => !s.field || String(s.field).trim() === '')
  })

  const saveDisabledTooltip = computed(() => {
    if (hasIncompleteSortRules.value) return t('viewForm.validation.sortFieldRequired')
    return ''
  })

  return {
    hasIncompleteSortRules,
    saveDisabledTooltip,
  }
}
