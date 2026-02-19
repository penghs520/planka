<template>
  <div class="condition-editor">
    <!-- 条件编辑器 -->
    <div v-if="localCondition?.root" class="condition-editor-wrapper">
      <ConditionGroupEditor
        v-if="isConditionGroup(localCondition.root)"
        :model-value="localCondition.root"
        :depth="0"
        :available-fields="availableFields"
        :link-types="linkTypes"
        :any-trait-card-type-name="rootCardTypeName"
        :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
        @update:model-value="handleRootUpdate"
        @clear-all="clearAllConditions"
      />

      <!-- 如果root是单个条件项，包装成条件组 -->
      <ConditionGroupEditor
        v-else
        :model-value="wrapAsGroup(localCondition.root)"
        :depth="0"
        :available-fields="availableFields"
        :link-types="linkTypes"
        :any-trait-card-type-name="rootCardTypeName"
        :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
        @update:model-value="handleRootUpdate"
        @clear-all="clearAllConditions"
      />
    </div>

    <!-- 空状态提示 -->
    <div v-if="!localCondition?.root" class="empty-condition-state">
      <div class="empty-hint">{{ t('common.condition.emptyHint') }}</div>
      <a-button type="outline" class="add-condition-btn" @click="initCondition">
        <template #icon><IconPlus /></template>
        {{ t('common.condition.addCondition') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, provide, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import type { Condition, ConditionNode, ConditionGroup } from '@/types/condition'
import { isConditionGroup } from '@/types/condition'
import ConditionGroupEditor from './ConditionGroupEditor.vue'
import type { FieldOption } from '@/types/field-option'
import type { LinkTypeVO } from '@/types/link-type'
import { isConditionComplete, getIncompleteConditionCount } from '@/utils/condition-factory'
import { useConditionDisplayInfo, CONDITION_DISPLAY_INFO_KEY } from './useConditionDisplayInfo'

const { t } = useI18n()

/**
 * Props定义
 */
const props = withDefaults(
  defineProps<{
    /** 条件值（v-model绑定） */
    modelValue?: Condition | null

    /** 卡片类型ID（用于获取可用字段列表） */
    cardTypeId?: string

    /** 卡片类型ID列表（用于获取条件显示信息） */
    cardTypeIds?: string[]

    /** 可用字段列表（如果不提供，则根据cardTypeId自动获取） */
    availableFields?: FieldOption[]

    /** 关联类型列表 */
    linkTypes?: LinkTypeVO[]

    /** 根卡片类型名称（用于路径面包屑显示） */
    rootCardTypeName?: string

    /** 根据关联字段ID获取级联字段的函数（用于多级关联） */
    fetchFieldsByLinkFieldId?: (linkFieldId: string) => Promise<FieldOption[]>
  }>(),
  {
    modelValue: null,
    cardTypeId: undefined,
    cardTypeIds: () => [],
    availableFields: () => [],
    linkTypes: () => [],
    rootCardTypeName: '',
    fetchFieldsByLinkFieldId: undefined,
  }
)

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: Condition]
}>()

/**
 * 本地条件状态（深拷贝）
 */
const localCondition = ref<Condition | null>(null)

/**
 * 是否高亮显示不完整的条件项
 */
const highlightIncomplete = ref(false)

/**
 * 条件显示信息 Composable
 * 用于加载和管理条件中所有需要显示名称的 ID -> Name 映射
 * 注意：必须在使用它的 watch 之前初始化
 */
const conditionDisplayInfo = useConditionDisplayInfo(localCondition)

/**
 * 监听props变化，同步到本地状态
 */
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal) {
      localCondition.value = JSON.parse(JSON.stringify(newVal))
    } else {
      localCondition.value = null
    }
    // 只有当条件变为完整时才关闭高亮，保持未完整条件的高亮提示
    if (highlightIncomplete.value && isConditionComplete(localCondition.value)) {
      highlightIncomplete.value = false
    }
  },
  { immediate: true, deep: true }
)

/**
 * 初始化条件（创建空的AND条件组）
 */
function initCondition() {
  const newCondition: Condition = {
    root: {
      nodeType: 'GROUP',
      operator: 'AND',
      children: [],
    },
  }
  localCondition.value = newCondition
  emitChange()
}

/**
 * 清除所有条件
 */
function clearAllConditions() {
  localCondition.value = null
  emit('update:modelValue', null as any)
}

/**
 * 处理根节点更新
 */
function handleRootUpdate(newRoot: ConditionGroup) {
  if (localCondition.value) {
    localCondition.value = {
      ...localCondition.value,
      root: newRoot,
    }
    emitChange()
  }
}

/**
 * 包装单个条件项为条件组
 */
function wrapAsGroup(item: ConditionNode): ConditionGroup {
  return {
    nodeType: 'GROUP',
    operator: 'AND',
    children: [item],
  }
}

/**
 * 发送变更事件
 */
function emitChange() {
  if (localCondition.value) {
    emit('update:modelValue', JSON.parse(JSON.stringify(localCondition.value)))
  }
}

/**
 * 验证条件完整性，如果不完整则高亮显示
 * @returns 是否完整
 */
function validate(): boolean {
  const complete = isConditionComplete(localCondition.value)
  highlightIncomplete.value = !complete
  return complete
}

/**
 * 提供上下文给子组件
 */
provide('cardTypeId', computed(() => props.cardTypeId))
provide('availableFields', computed(() => props.availableFields))
provide('highlightIncomplete', highlightIncomplete)

// 提供给子组件使用
provide(CONDITION_DISPLAY_INFO_KEY, conditionDisplayInfo)

/**
 * 组件挂载时加载显示信息
 */
onMounted(() => {
  try {
    conditionDisplayInfo.loadDisplayInfo()
  } catch (e) {
    console.error('[ConditionEditor] Failed to load display info in onMounted:', e)
  }
})

/**
 * 监听条件变化，当条件从空变为非空时，自动加载显示信息
 */
watch(
  () => localCondition.value?.root,
  (newRoot, oldRoot) => {
    // 当条件从空变为非空，且未初始化时，加载显示信息
    if (newRoot && !oldRoot && !conditionDisplayInfo.initialized.value) {
      try {
        conditionDisplayInfo.loadDisplayInfo()
      } catch (e) {
        console.error('[ConditionEditor] Failed to load display info in watch root:', e)
      }
    }
  }
)

/**
 * 条件是否完整（所有条件项都已填写完整）
 */
const isComplete = computed(() => isConditionComplete(localCondition.value))

/**
 * 不完整的条件项数量
 */
const incompleteCount = computed(() => getIncompleteConditionCount(localCondition.value))

/**
 * 暴露给父组件的接口
 */
defineExpose({
  /** 条件是否完整 */
  isComplete,
  /** 不完整的条件项数量 */
  incompleteCount,
  /** 验证并高亮不完整项 */
  validate,
})
</script>

<style scoped lang="scss">
.condition-editor {
  width: 100%;
  min-height: 120px;
}

.empty-condition-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: var(--color-fill-1);
  border-radius: 6px;
  border: 1px dashed var(--color-border-2);

  .empty-hint {
    font-size: 13px;
    color: var(--color-text-3);
    margin-bottom: 12px;
  }

  .add-condition-btn {
    border-style: dashed;
    border-color: var(--color-border-2);
    color: var(--color-text-2);

    &:hover {
      border-color: var(--color-primary);
      color: var(--color-primary);
      background: var(--color-primary-light-1);
    }
  }
}

</style>
