<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { cardTypeApi } from '@/api'


interface SelectOption {
  value: string
  label: string
}

interface CardTypeOption {
  id: string
  name: string
  icon?: string
  schemaSubType: string
}

const props = withDefaults(
  defineProps<{
    modelValue?: string | string[]
    placeholder?: string
    schemaSubType?: string // 用于过滤选项，例如只显示特征类型
    multiple?: boolean // 是否多选
    options?: CardTypeOption[] // 外部传入的选项（优化性能，避免重复请求）
    limitConcreteSingle?: boolean // 是否限制实体类型单选
  }>(),
  {
    multiple: true,
    limitConcreteSingle: false,
  }
)

const emit = defineEmits<{
  /** 多选为 ID 数组；单选为单个 ID 字符串，清空为空串 */
  (e: 'update:modelValue', value: string | string[]): void
}>()

const selectOptions = ref<SelectOption[]>([])
const loading = ref(false)
const cardTypeMap = ref<Record<string, string>>({})
const rawOptions = ref<CardTypeOption[]>([])


// 多选模式下的值（数组）
const selectedValues = ref<string[]>(
  Array.isArray(props.modelValue) ? props.modelValue : (props.modelValue ? [props.modelValue] : [])
)

// 单选模式下的值（字符串）
const singleValue = ref<string>(
  Array.isArray(props.modelValue) ? (props.modelValue[0] || '') : (props.modelValue || '')
)

// 监听外部传入的值变化
watch(
  () => props.modelValue,
  (newVal) => {
    if (props.multiple) {
      selectedValues.value = Array.isArray(newVal) ? newVal : (newVal ? [newVal] : [])
    } else {
      singleValue.value = Array.isArray(newVal) ? (newVal[0] || '') : (newVal || '')
    }
  }
)

// 监听 multiple 变化，同步数据
watch(
  () => props.multiple,
  (isMultiple) => {
    if (isMultiple) {
      // 切换到多选：从单选值转换为数组
      selectedValues.value = singleValue.value ? [singleValue.value] : []
    } else {
      // 切换到单选：从数组取第一个值
      singleValue.value = selectedValues.value[0] || ''
    }
  }
)

// 多选模式：监听数组变化，向外发送始终为数组
watch(selectedValues, (newVal, oldVal) => {
  if (props.multiple) {
    if (props.limitConcreteSingle && newVal.length > (oldVal?.length || 0)) {
      const oldArr = oldVal || []
      const added = newVal.filter((id) => !oldArr.includes(id))
      
      if (added.length > 0) {
        // 取最后一个添加的项（通常一次只添加一项）
        const latest = added[added.length - 1]
        
        if (latest) {
          const latestType = cardTypeMap.value[latest]

          if (latestType === 'ENTITY_CARD_TYPE') {
            // 如果添加的是实体类型，则清除其他所有选项，只保留当前这一个
            if (newVal.length > 1) {
              // 只有当有其他选项时才提示并清理，避免不必要的干扰
              selectedValues.value = [latest]
              return // 这里的 return 很重要，会在 selectedValues 更新后再次触发 watch
            }
          } else {
            // 如果添加的是特征类型，检查之前是否选中了实体类型
            const hasConcrete = oldArr.some(
              (id) => cardTypeMap.value[id] === 'ENTITY_CARD_TYPE'
            )
            if (hasConcrete) {
              selectedValues.value = [latest]
              return
            }
          }
        }
      }
    }
    emit('update:modelValue', newVal)
  }
})

// 单选模式：向外发送单个 ID 字符串（清空为空串）
watch(singleValue, (newVal) => {
  if (!props.multiple) {
    emit('update:modelValue', newVal || '')
  }
})

// 构建下拉选项（从 CardTypeOption[] 转换为分组的 SelectOption[]）
function buildSelectOptions(cardTypes: CardTypeOption[]) {
  // 构建类型映射
  const map: Record<string, string> = {}
  cardTypes.forEach((t) => (map[t.id] = t.schemaSubType))
  cardTypeMap.value = map

  // 根据 schemaSubType 过滤选项
  const filteredList = props.schemaSubType
    ? cardTypes.filter((item) => item.schemaSubType === props.schemaSubType)
    : cardTypes

  // 分组：特征类型和实体类型
  const abstractTypes = filteredList
    .filter((item) => item.schemaSubType === 'TRAIT_CARD_TYPE')
    .map((item) => ({
      value: item.id,
      label: item.name,
    }))

  const concreteTypes = filteredList
    .filter((item) => item.schemaSubType === 'ENTITY_CARD_TYPE')
    .map((item) => ({
      value: item.id,
      label: item.name,
    }))

  // 构建分组选项
  const groupedOptions: any[] = []

  if (abstractTypes.length > 0) {
    groupedOptions.push({
      isGroup: true,
      label: '特征类型',
      options: abstractTypes,
    })
  }

  if (concreteTypes.length > 0) {
    groupedOptions.push({
      isGroup: true,
      label: '实体类型',
      options: concreteTypes,
    })
  }

  return groupedOptions
}

async function fetchCardTypes() {
  // 如果外部传入了 options，直接使用
  if (props.options && props.options.length > 0) {
    rawOptions.value = props.options
    return
  }

  // 否则从 API 获取
  loading.value = true
  try {
    const list = await cardTypeApi.listOptions()
    rawOptions.value = list
  } catch (error) {
    console.error('Failed to fetch card types:', error)
  } finally {
    loading.value = false
  }
}

// 监听外部传入的 options 变化
watch(
  () => props.options,
  (newOptions) => {
    if (newOptions && newOptions.length > 0) {
      rawOptions.value = newOptions
    }
  },
  { immediate: true }
)

// 监听数据源或配置变化，重新构建选项
watch(
  [rawOptions, () => props.limitConcreteSingle, () => props.schemaSubType],
  () => {
    if (rawOptions.value.length > 0) {
      selectOptions.value = buildSelectOptions(rawOptions.value)
    } else {
      selectOptions.value = []
    }
  },
  { immediate: true }
)

onMounted(() => {
  fetchCardTypes()
})
</script>

<template>
  <a-select
    v-if="multiple"
    v-model="selectedValues"
    :options="selectOptions"
    :placeholder="placeholder || '选择实体类型'"
    :loading="loading"
    multiple
    allow-clear
    allow-search
    class="arco-select-tag-blue"
  />
  <a-select
    v-else
    v-model="singleValue"
    :options="selectOptions"
    :placeholder="placeholder || '选择实体类型'"
    :loading="loading"
    allow-clear
    allow-search
  />
</template>

<style scoped>
.arco-select-tag-blue {
  width: 100%;
}
</style>
