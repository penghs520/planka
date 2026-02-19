<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconPlus, IconDelete } from '@arco-design/web-vue/es/icon'
import type { CardOperationPermission, FieldPermission, AttachmentPermission } from '@/types/permission'
import {
  CardOperation,
  FieldOperation,
  AttachmentOperation,
} from '@/types/permission'
import type { FieldOption } from '@/types/field-option'
import type { Condition } from '@/types/condition'
import ConditionEditor from '@/components/condition/ConditionEditor.vue'
import TextExpressionTemplateEditor from '@/components/common/text-expression-template/TextExpressionTemplateEditor.vue'
import type { FieldProvider } from '@/components/common/text-expression-template/types'
import { fieldOptionsApi } from '@/api/field-options'
import { useOrgStore } from '@/stores/org'
import { getMemberFieldsCached } from './memberFieldsCache'

const { t } = useI18n()
const orgStore = useOrgStore()

type PermissionRule = CardOperationPermission | FieldPermission | AttachmentPermission

const props = defineProps<{
  /** 弹窗可见性 */
  visible: boolean
  /** 权限规则 */
  rule: PermissionRule | null
  /** 卡片类型ID */
  cardTypeId: string
  /** 可用字段列表（用于条件编辑器） */
  availableFields: FieldOption[]
  /** 可选择的属性列表（仅属性权限和附件权限） */
  selectableFields?: FieldOption[]
  /** 权限类型 */
  permissionType: 'card' | 'field' | 'attachment'
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  save: [rule: PermissionRule]
}>()

// 本地编辑状态
const localRule = ref<PermissionRule | null>(null)

// 弹窗可见性
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

// 弹窗标题
const dialogTitle = computed(() => {
  const isEdit = props.rule && (
    (props.permissionType === 'card' && (props.rule as CardOperationPermission).operation) ||
    (props.permissionType === 'field' && (props.rule as FieldPermission).operation) ||
    (props.permissionType === 'attachment' && (props.rule as AttachmentPermission).attachmentOperation)
  )
  return isEdit
    ? t('admin.cardType.permission.editRule')
    : t('admin.cardType.permission.addRule')
})

// 操作类型选项
const operationOptions = computed(() => {
  if (props.permissionType === 'card') {
    return Object.values(CardOperation).map((op) => ({
      value: op,
      label: t(`admin.cardType.permission.operations.${op}`),
    }))
  }
  if (props.permissionType === 'field') {
    return Object.values(FieldOperation).map((op) => ({
      value: op,
      label: t(`admin.cardType.permission.fieldOperations.${op}`),
    }))
  }
  return Object.values(AttachmentOperation).map((op) => ({
    value: op,
    label: t(`admin.cardType.permission.attachmentOperations.${op}`),
  }))
})

// 获取当前操作类型
const currentOperation = computed({
  get: () => {
    if (!localRule.value) return ''
    if (props.permissionType === 'card') {
      return (localRule.value as CardOperationPermission).operation
    }
    if (props.permissionType === 'field') {
      return (localRule.value as FieldPermission).operation
    }
    return (localRule.value as AttachmentPermission).attachmentOperation
  },
  set: (value) => {
    if (!localRule.value) return
    if (props.permissionType === 'card') {
      (localRule.value as CardOperationPermission).operation = value as CardOperation
    } else if (props.permissionType === 'field') {
      (localRule.value as FieldPermission).operation = value as FieldOperation
    } else {
      (localRule.value as AttachmentPermission).attachmentOperation = value as AttachmentOperation
    }
  },
})

// 获取选中的属性ID列表（仅属性权限和附件权限）
const selectedFieldIds = computed({
  get: () => {
    if (!localRule.value || props.permissionType === 'card') return []
    return (localRule.value as FieldPermission | AttachmentPermission).fieldIds || []
  },
  set: (value) => {
    if (!localRule.value || props.permissionType === 'card') return
    (localRule.value as FieldPermission | AttachmentPermission).fieldIds = value
  },
})

// 卡片条件列表
const cardConditions = computed({
  get: () => {
    if (!localRule.value) return []
    return (localRule.value as CardOperationPermission).cardConditions || []
  },
  set: (value) => {
    if (!localRule.value) return
    (localRule.value as CardOperationPermission).cardConditions = value
  },
})

// 操作人条件列表
const operatorConditions = computed({
  get: () => {
    if (!localRule.value) return []
    return (localRule.value as CardOperationPermission).operatorConditions || []
  },
  set: (value) => {
    if (!localRule.value) return
    (localRule.value as CardOperationPermission).operatorConditions = value
  },
})

// 提示信息
const alertMessage = computed({
  get: () => {
    if (!localRule.value) return ''
    return (localRule.value as CardOperationPermission).alertMessage || ''
  },
  set: (value) => {
    if (!localRule.value) return
    (localRule.value as CardOperationPermission).alertMessage = value
  },
})

// 监听规则变化，深拷贝到本地
watch(
  () => props.rule,
  (newRule) => {
    if (newRule) {
      // 深拷贝，确保数组字段有默认值
      const clonedRule = JSON.parse(JSON.stringify(newRule))
      // 确保条件数组存在
      if (!clonedRule.cardConditions) {
        clonedRule.cardConditions = []
      }
      if (!clonedRule.operatorConditions) {
        clonedRule.operatorConditions = []
      }
      localRule.value = clonedRule
    } else {
      localRule.value = null
    }
  },
  { immediate: true }
)

// 添加卡片条件
function addCardCondition() {
  cardConditions.value = [...cardConditions.value, { root: undefined }]
}

// 删除卡片条件
function removeCardCondition(index: number) {
  const newConditions = [...cardConditions.value]
  newConditions.splice(index, 1)
  cardConditions.value = newConditions
}

// 更新卡片条件
function updateCardCondition(index: number, condition: Condition) {
  const newConditions = [...cardConditions.value]
  newConditions[index] = condition
  cardConditions.value = newConditions
}

// 添加操作人条件
function addOperatorCondition() {
  operatorConditions.value = [...operatorConditions.value, { root: undefined }]
}

// 删除操作人条件
function removeOperatorCondition(index: number) {
  const newConditions = [...operatorConditions.value]
  newConditions.splice(index, 1)
  operatorConditions.value = newConditions
}

// 更新操作人条件
function updateOperatorCondition(index: number, condition: Condition) {
  const newConditions = [...operatorConditions.value]
  newConditions[index] = condition
  operatorConditions.value = newConditions
}

// 保存
function handleSave() {
  if (!localRule.value) return
  // 清理空的条件（root 为 undefined 的条件）
  const ruleToSave = JSON.parse(JSON.stringify(localRule.value))
  if (ruleToSave.cardConditions) {
    ruleToSave.cardConditions = ruleToSave.cardConditions.filter(
      (c: { root?: unknown }) => c.root !== undefined && c.root !== null
    )
  }
  if (ruleToSave.operatorConditions) {
    ruleToSave.operatorConditions = ruleToSave.operatorConditions.filter(
      (c: { root?: unknown }) => c.root !== undefined && c.root !== null
    )
  }
  emit('save', ruleToSave)
}

// 取消
function handleCancel() {
  dialogVisible.value = false
}

// 根据关联字段ID获取级联字段
const fetchFieldsByLinkFieldId = (linkFieldId: string) => {
  return fieldOptionsApi.getFieldsByLinkFieldId(linkFieldId)
}

// 表达式模板编辑器的字段提供者（带请求缓存）
let cardFieldsCache: Promise<FieldOption[]> | null = null
const linkFieldsCache = new Map<string, Promise<FieldOption[]>>()

const expressionFieldProvider: FieldProvider = {
  getCardFields: () => {
    if (!cardFieldsCache) {
      cardFieldsCache = fieldOptionsApi.getFields(props.cardTypeId)
    }
    return cardFieldsCache
  },
  getMemberFields: () => getMemberFieldsCached(orgStore.currentOrg?.memberCardTypeId),
  getFieldsByLinkFieldId: (id) => {
    let cached = linkFieldsCache.get(id)
    if (!cached) {
      cached = fieldOptionsApi.getFieldsByLinkFieldId(id)
      linkFieldsCache.set(id, cached)
    }
    return cached
  },
}

</script>

<template>
  <a-modal
    v-model:visible="dialogVisible"
    :title="dialogTitle"
    :width="1100"
    :body-style="{ maxHeight: '70vh', overflowY: 'auto' }"
    :mask-closable="false"
    @cancel="handleCancel"
    @ok="handleSave"
  >
    <a-form v-if="localRule" class="rule-editor" layout="vertical">
      <!-- 操作类型选择 -->
      <a-form-item :label="t('admin.cardType.permission.operation')" required>
        <a-select v-model="currentOperation" :options="operationOptions" />
      </a-form-item>

      <!-- 目标属性选择（仅属性权限和附件权限） -->
      <a-form-item
        v-if="permissionType !== 'card'"
        :label="t('admin.cardType.permission.targetFields')"
      >
        <a-select
          v-model="selectedFieldIds"
          multiple
          allow-clear
          :placeholder="t('admin.cardType.permission.selectFieldsPlaceholder')"
        >
          <a-option
            v-for="field in selectableFields"
            :key="field.id"
            :value="field.id!"
            :label="field.name"
          />
        </a-select>
        <template #extra>
          <span class="field-hint">{{ t('admin.cardType.permission.emptyFieldsHint') }}</span>
        </template>
      </a-form-item>

      <!-- 卡片条件 -->
      <a-form-item :label="t('admin.cardType.permission.cardCondition')">
        <div class="condition-list">
          <div
            v-for="(condition, index) in cardConditions"
            :key="index"
            class="condition-item"
          >
            <div class="condition-header">
              <span class="condition-index">{{ t('admin.cardType.permission.conditionIndex', { index: index + 1 }) }}</span>
              <a-button
                type="text"
                size="small"
                status="danger"
                @click="removeCardCondition(index)"
              >
                <template #icon><IconDelete /></template>
              </a-button>
            </div>
            <ConditionEditor
              :model-value="condition"
              :available-fields="availableFields"
              :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
              @update:model-value="(val) => updateCardCondition(index, val)"
            />
          </div>
          <a-button type="dashed" size="small" @click="addCardCondition">
            <template #icon><IconPlus /></template>
            {{ t('admin.cardType.permission.addCondition') }}
          </a-button>
        </div>
        <template #extra>
          <span class="condition-hint">{{ t('admin.cardType.permission.cardConditionHint') }}</span>
        </template>
      </a-form-item>

      <!-- 操作人条件 -->
      <a-form-item :label="t('admin.cardType.permission.operatorCondition')">
        <div class="condition-list">
          <div
            v-for="(condition, index) in operatorConditions"
            :key="index"
            class="condition-item"
          >
            <div class="condition-header">
              <span class="condition-index">{{ t('admin.cardType.permission.conditionIndex', { index: index + 1 }) }}</span>
              <a-button
                type="text"
                size="small"
                status="danger"
                @click="removeOperatorCondition(index)"
              >
                <template #icon><IconDelete /></template>
              </a-button>
            </div>
            <ConditionEditor
              :model-value="condition"
              :available-fields="availableFields"
              :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
              @update:model-value="(val) => updateOperatorCondition(index, val)"
            />
          </div>
          <a-button type="dashed" size="small" @click="addOperatorCondition">
            <template #icon><IconPlus /></template>
            {{ t('admin.cardType.permission.addCondition') }}
          </a-button>
        </div>
        <template #extra>
          <span class="condition-hint">{{ t('admin.cardType.permission.operatorConditionHint') }}</span>
        </template>
      </a-form-item>

      <!-- 提示信息 -->
      <a-form-item :label="t('admin.cardType.permission.alertMessage')">
        <TextExpressionTemplateEditor
          :model-value="alertMessage"
          :field-provider="expressionFieldProvider"
          :placeholder="t('admin.cardType.permission.alertMessagePlaceholder')"
          @update:model-value="(val) => alertMessage = val"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.rule-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.rule-editor :deep(.arco-form-item-content) {
  width: 100%;
}

.condition-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.condition-item {
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 12px;
  width: 100%;
  box-sizing: border-box;
}

.condition-item :deep(.condition-editor) {
  width: 100%;
}

.condition-item :deep(.condition-group) {
  width: 100%;
}

.condition-item :deep(.group-children) {
  width: 100%;
}

.condition-item :deep(.condition-item) {
  width: 100%;
}

.condition-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.condition-index {
  font-size: 12px;
  color: var(--color-text-3);
}

.field-hint,
.condition-hint {
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
