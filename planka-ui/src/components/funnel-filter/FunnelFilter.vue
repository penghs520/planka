<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconFilter, IconQuestionCircle } from '@arco-design/web-vue/es/icon'
import type { Condition } from '@/types/condition'
import type { FieldOption } from '@/types/field-option'
import type { LinkTypeVO } from '@/types/link-type'
import { cardTypeApi } from '@/api/card-type'
import { linkTypeApi } from '@/api/link-type'
import { fieldOptionsApi } from '@/api/field-options'
import ConditionEditor from '@/components/condition/ConditionEditor.vue'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'

const props = defineProps<{
  /** 卡片类型 ID */
  cardTypeId?: string
  /** 过滤条件 (v-model) */
  modelValue?: Condition | null
  /** 根卡片类型名称（用于路径面包屑显示） */
  rootCardTypeName?: string
  /** 视图内置过滤条件描述（hover 问号图标时显示） */
  viewConditionDescription?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Condition | null]
  /** 应用过滤条件 */
  apply: [condition: Condition | null]
  /** 清除过滤条件 */
  clear: []
}>()

// 弹窗状态
const popoverVisible = ref(false)

// 本地过滤条件（编辑中的条件）
const localCondition = ref<Condition | null>(null)

// 字段和关联类型数据
const availableFields = ref<FieldOption[]>([])
const linkTypes = ref<LinkTypeVO[]>([])
const loadingFields = ref(false)

// ConditionEditor 组件引用
const conditionEditorRef = ref<InstanceType<typeof ConditionEditor> | null>(null)

// 是否有过滤条件
const hasFilterCondition = computed(() => {
  return props.modelValue?.root != null
})

// 是否有本地编辑中的条件
const hasLocalCondition = computed(() => {
  return localCondition.value?.root != null
})

// 同步外部值到本地
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal) {
      localCondition.value = JSON.parse(JSON.stringify(newVal))
    } else {
      localCondition.value = null
    }
  },
  { immediate: true, deep: true }
)

// 加载字段和关联类型
async function fetchFieldsAndLinkTypes() {
  if (!props.cardTypeId) return

  loadingFields.value = true
  try {
    const [fieldsResult, linkTypesResult] = await Promise.all([
      cardTypeApi.getFieldOptions(props.cardTypeId),
      linkTypeApi.list(),
    ])
    availableFields.value = fieldsResult || []
    linkTypes.value = linkTypesResult || []
  } catch (err) {
    console.error('Failed to fetch fields and link types:', err)
  } finally {
    loadingFields.value = false
  }
}

// 根据关联字段ID获取级联字段（用于多级关联）
async function fetchFieldsByLinkFieldId(linkFieldId: string): Promise<FieldOption[]> {
  return fieldOptionsApi.getFieldsByLinkFieldId(linkFieldId)
}

// 打开漏斗过滤
function openFilter() {
  if (props.cardTypeId && availableFields.value.length === 0) {
    fetchFieldsAndLinkTypes()
  }
  // 如果没有条件，初始化一个空的条件组，直接显示编辑界面
  if (!localCondition.value || !localCondition.value.root) {
    localCondition.value = {
      root: {
        nodeType: 'GROUP',
        operator: 'AND',
        children: [],
      },
    }
  }
  popoverVisible.value = true
}

// 应用过滤条件
function applyFilter() {
  // 验证条件完整性
  if (conditionEditorRef.value && !conditionEditorRef.value.validate()) {
    Message.warning('请完善过滤条件')
    return
  }

  emit('update:modelValue', localCondition.value)
  emit('apply', localCondition.value)
  popoverVisible.value = false
}

// 清除过滤条件
function clearFilter() {
  localCondition.value = null
  emit('update:modelValue', null)
  emit('clear')
  popoverVisible.value = false
}

// 监听 cardTypeId 变化，重新加载字段
watch(
  () => props.cardTypeId,
  (newVal, oldVal) => {
    if (newVal && newVal !== oldVal) {
      availableFields.value = []
      linkTypes.value = []
    }
  }
)
</script>

<template>
  <a-popover
    v-model:popup-visible="popoverVisible"
    trigger="click"
    position="bl"
    :content-style="{ padding: 0, width: '800px' }"
  >
    <div
      :class="['funnel-filter-trigger', { active: hasFilterCondition }]"
      @click="openFilter"
    >
      <IconFilter />
      <span>漏斗过滤</span>
    </div>
    <template #content>
      <div class="funnel-filter-popover">
        <div class="filter-header">
          <div class="filter-title-wrapper">
            <span class="filter-title">查看视图内置过滤</span>
            <a-tooltip :content="viewConditionDescription || '暂无内置过滤条件'">
              <IconQuestionCircle class="help-icon" />
            </a-tooltip>
          </div>
          <a-button
            v-if="hasLocalCondition"
            type="text"
            size="small"
            status="danger"
            @click="clearFilter"
          >
            清除
          </a-button>
        </div>
        <div class="filter-body">
          <a-spin :loading="loadingFields">
            <ConditionEditor
              ref="conditionEditorRef"
              v-model="localCondition"
              :card-type-id="cardTypeId"
              :available-fields="availableFields"
              :link-types="linkTypes"
              :any-trait-card-type-name="rootCardTypeName"
              :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
            />
          </a-spin>
        </div>
        <div class="filter-footer">
          <CancelButton @click="popoverVisible = false" />
          <SaveButton text="应用" @click="applyFilter" />
        </div>
      </div>
    </template>
  </a-popover>
</template>

<style scoped lang="scss">
.funnel-filter-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  font-size: 13px;
  color: var(--color-text-2);
  cursor: pointer;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;

  &:hover {
    color: var(--color-text-1);
    background: var(--color-fill-2);
  }

  &.active {
    color: rgb(var(--primary-6));
    background: rgb(var(--primary-1));
  }
}

.funnel-filter-popover {
  display: flex;
  flex-direction: column;
  max-height: 500px;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2px 6px 2px 10px;
}

.filter-title-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
}

.filter-title {
  font-size: 12px;
  color: var(--color-text-3);
}

.help-icon {
  color: var(--color-text-3);
  cursor: help;
  font-size: 14px;

  &:hover {
    color: var(--color-text-2);
  }
}

.filter-body {
  flex: 1;
  padding: 0 0 8px 0;
  overflow-y: auto;
  min-height: 120px;
  max-height: 350px;

  // 关键：a-spin 默认是 inline-block，需要设置为 block 并占满宽度
  :deep(.arco-spin) {
    width: 100%;
    display: block;
  }

  // 条件编辑器及其子组件占满宽度
  :deep(.condition-editor),
  :deep(.condition-group),
  :deep(.condition-item) {
    width: 100%;
  }
}

.filter-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;

  :deep(.arco-btn) {
    height: 24px;
    padding: 0 8px;
    font-size: 12px;
    min-width: auto;
  }
}
</style>
