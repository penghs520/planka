import { computed, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FieldOption } from '@/types/field-option'

const GROUP_BY_ENUM_FIELD_TYPES = new Set(['ENUM', 'SINGLE_ENUM', 'MULTI_ENUM'])
const GROUP_BY_BUILTIN_ENUM_ORDER = ['$cardCycle', '$statusId'] as const

export interface GroupByFieldGroup {
  key: string
  label: string
  options: { value: string; label: string }[]
}

/** 列表视图「分组字段」下拉分组数据 */
export function useListViewGroupByOptions(availableFields: Ref<FieldOption[]>) {
  const { t } = useI18n()

  const groupByFieldGroups = computed<GroupByFieldGroup[]>(() => {
    const fields = availableFields.value
    const groups: GroupByFieldGroup[] = []

    const builtinIds = new Set<string>(GROUP_BY_BUILTIN_ENUM_ORDER)
    const enumOpts: { value: string; label: string }[] = []
    for (const id of GROUP_BY_BUILTIN_ENUM_ORDER) {
      const f = fields.find((x) => x.id === id)
      if (f && !GROUP_BY_ENUM_FIELD_TYPES.has(f.fieldType)) {
        continue
      }
      const label =
        id === '$cardCycle'
          ? f?.name?.trim() || t('common.systemField.cardCycle')
          : f?.name?.trim() || t('viewForm.groupByValueStreamStatusOption')
      enumOpts.push({ value: id, label })
    }
    for (const f of fields) {
      if (!GROUP_BY_ENUM_FIELD_TYPES.has(f.fieldType) || builtinIds.has(f.id)) {
        continue
      }
      enumOpts.push({ value: f.id, label: f.name || f.id })
    }

    groups.push({
      key: 'enum',
      label: t('viewForm.groupByCategoryEnum'),
      options: enumOpts,
    })

    const linkOpts = fields
      .filter((f) => f.fieldType === 'LINK')
      .map((f) => ({ value: f.id, label: f.name || f.id }))
    if (linkOpts.length > 0) {
      groups.push({
        key: 'link',
        label: t('viewForm.groupByCategoryLink'),
        options: linkOpts,
      })
    }

    return groups
  })

  return { groupByFieldGroups }
}
