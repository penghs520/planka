<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconRight } from '@arco-design/web-vue/es/icon'
import type { FieldOption } from '@/types/field-option'
import { type ActionTargetSelector, ActionTargetType } from '@/types/biz-rule'
import { fieldOptionsApi } from '@/api/field-options'

const { t } = useI18n()

const props = defineProps<{
  /** 当前卡片类型的字段选项（用于获取关联属性） */
  fieldOptions: FieldOption[]
  /** 当前卡片类型ID */
  cardTypeId: string
}>()

const modelValue = defineModel<ActionTargetSelector>({ required: true })

// 下拉框显示状态
const dropdownVisible = ref(false)

// 加载关联属性目标卡片类型的字段
const linkedCardFieldOptions = ref<FieldOption[]>([])
const loadingLinkedFields = ref(false)

// 获取所有关联类型字段（只显示单选的关联属性，多选的不支持）
const linkFields = computed(() =>
  props.fieldOptions.filter(f => f.fieldType === 'LINK' && f.multiple !== true)
)

// 当前选中的关联属性
const selectedLinkField = computed(() => {
  if (modelValue.value.targetType !== ActionTargetType.LINKED_CARD) return null
  const linkNodes = modelValue.value.linkPath?.linkNodes
  if (!linkNodes || linkNodes.length === 0) return null
  return props.fieldOptions.find(f => f.id === linkNodes[0])
})

// 目标类型选项
const targetTypeOptions = computed(() => [
  { label: t('admin.bizRule.actionTarget.CURRENT_CARD'), value: ActionTargetType.CURRENT_CARD },
  { label: t('admin.bizRule.actionTarget.LINKED_CARD'), value: ActionTargetType.LINKED_CARD },
])

// 显示文本
const displayText = computed(() => {
  if (modelValue.value.targetType === ActionTargetType.CURRENT_CARD) {
    return t('admin.bizRule.actionTarget.CURRENT_CARD')
  }
  if (selectedLinkField.value) {
    return `${t('admin.bizRule.actionTarget.LINKED_CARD')}: ${selectedLinkField.value.name}`
  }
  return t('admin.bizRule.actionTarget.LINKED_CARD')
})

// hover 状态
const hoveredTargetType = ref<ActionTargetType | null>(null)
const hoveredLinkFieldId = ref<string | null>(null)
let hoverTimer: ReturnType<typeof setTimeout> | null = null

// 处理目标类型 hover
function handleTargetTypeHover(type: ActionTargetType) {
  if (hoverTimer) clearTimeout(hoverTimer)
  hoverTimer = setTimeout(() => {
    hoveredTargetType.value = type
    if (type === ActionTargetType.LINKED_CARD) {
      // 展示关联属性列表
      hoveredLinkFieldId.value = null
    }
  }, 100)
}

function handleTargetTypeLeave() {
  if (hoverTimer) clearTimeout(hoverTimer)
}

// 处理关联属性 hover
function handleLinkFieldHover(fieldId: string) {
  hoveredLinkFieldId.value = fieldId
}

// 处理目标类型点击
function handleTargetTypeClick(type: ActionTargetType) {
  if (type === ActionTargetType.CURRENT_CARD) {
    modelValue.value = {
      targetType: ActionTargetType.CURRENT_CARD,
    }
    dropdownVisible.value = false
  }
  // LINKED_CARD 需要选择关联属性，不直接关闭
}

// 处理关联属性点击
function handleLinkFieldClick(field: FieldOption) {
  modelValue.value = {
    targetType: ActionTargetType.LINKED_CARD,
    linkPath: { linkNodes: [field.id] },
  }
  dropdownVisible.value = false
}

// 处理下拉框显示变化
function handleDropdownVisibleChange(visible: boolean) {
  dropdownVisible.value = visible
  if (!visible) {
    hoveredTargetType.value = null
    hoveredLinkFieldId.value = null
  }
}

// 加载关联属性目标卡片类型的字段
async function loadLinkedCardFields(linkFieldId: string) {
  loadingLinkedFields.value = true
  try {
    linkedCardFieldOptions.value = await fieldOptionsApi.getFieldsByLinkFieldId(linkFieldId)
  } catch (e) {
    console.error('Failed to load linked card fields:', e)
    linkedCardFieldOptions.value = []
  } finally {
    loadingLinkedFields.value = false
  }
}

// 监听选中的关联属性变化，加载目标卡片类型的字段
watch(selectedLinkField, (field) => {
  if (field) {
    loadLinkedCardFields(field.id)
  } else {
    linkedCardFieldOptions.value = []
  }
}, { immediate: true })
</script>

<template>
  <a-trigger
    trigger="click"
    :popup-visible="dropdownVisible"
    position="bl"
    auto-fit-popup-min-width
    @popup-visible-change="handleDropdownVisibleChange"
  >
    <!-- 触发器 -->
    <div
      class="target-selector-trigger"
      :class="{ 'is-focus': dropdownVisible }"
    >
      <span class="value-text">{{ displayText }}</span>
      <IconRight class="trigger-icon" :class="{ 'is-open': dropdownVisible }" />
    </div>

    <template #content>
      <div class="target-dropdown">
        <div class="panels-container">
          <!-- 主面板：目标类型 -->
          <div class="main-panel">
            <div
              v-for="option in targetTypeOptions"
              :key="option.value"
              class="target-item"
              :class="{
                selected: modelValue.targetType === option.value,
                expanded: hoveredTargetType === option.value && option.value === ActionTargetType.LINKED_CARD,
              }"
              @mouseenter="handleTargetTypeHover(option.value)"
              @mouseleave="handleTargetTypeLeave"
              @click="handleTargetTypeClick(option.value)"
            >
              <span class="item-text">{{ option.label }}</span>
              <IconRight
                v-if="option.value === ActionTargetType.LINKED_CARD"
                class="expand-icon"
              />
            </div>
          </div>

          <!-- 子面板：关联属性列表 -->
          <div
            v-if="hoveredTargetType === ActionTargetType.LINKED_CARD"
            class="sub-panel link-field-panel"
          >
            <div v-if="linkFields.length === 0" class="empty-text">
              {{ t('admin.bizRule.actionTarget.noLinkFields') }}
            </div>
            <div v-else class="field-list">
              <div
                v-for="field in linkFields"
                :key="field.id"
                class="field-item"
                :class="{
                  selected: selectedLinkField?.id === field.id,
                  hovered: hoveredLinkFieldId === field.id,
                }"
                @mouseenter="handleLinkFieldHover(field.id)"
                @click="handleLinkFieldClick(field)"
              >
                <span class="field-name">{{ field.name }}</span>
                <span v-if="field.targetCardTypeIds?.length" class="field-hint">
                  → {{ field.targetCardTypeIds.length }} {{ t('admin.bizRule.actionTarget.targetTypes') }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </a-trigger>
</template>

<style scoped lang="scss">
.target-selector-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 4px;
  margin: -2px -4px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  color: var(--color-primary-6);

  &:hover {
    background: var(--color-primary-light-1);
  }

  .value-text {
    color: var(--color-text-1);
  }

  .trigger-icon {
    color: var(--color-text-3);
    font-size: 12px;
    transition: transform 0.2s;

    &.is-open {
      transform: rotate(90deg);
    }
  }
}

.target-dropdown {
  background: var(--color-bg-popup);
  border-radius: 4px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.panels-container {
  display: flex;
}

.main-panel {
  min-width: 140px;
  padding: 4px 0;
  border-right: 1px solid var(--color-border);
}

.target-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover,
  &.expanded {
    background: var(--color-fill-2);
  }

  &.selected {
    background: var(--color-primary-light-1);
    color: var(--color-primary);
  }

  .item-text {
    flex: 1;
  }

  .expand-icon {
    margin-left: 8px;
    color: var(--color-text-3);
    font-size: 12px;
  }
}

.sub-panel {
  min-width: 180px;
  max-width: 260px;
  padding: 4px 0;
}

.field-list {
  max-height: 300px;
  overflow-y: auto;
}

.field-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover,
  &.hovered {
    background: var(--color-fill-2);
  }

  &.selected {
    background: var(--color-primary-light-1);
    color: var(--color-primary);
  }

  .field-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .field-hint {
    margin-left: 8px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.empty-text {
  padding: 12px;
  text-align: center;
  color: var(--color-text-3);
  font-size: 13px;
}
</style>
