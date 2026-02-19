<script setup lang="ts" generic="T extends { cardConditions?: unknown[]; operatorConditions?: unknown[]; alertMessage?: string }">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconDelete, IconPlus } from '@arco-design/web-vue/es/icon'
import type { FieldOption } from '@/types/field-option'
import type { CardOperationPermission, FieldPermission, AttachmentPermission } from '@/types/permission'
import { formatExpressionTemplate } from '@/components/common/text-expression-template/useTextExpressionTemplate'
import { useOrgStore } from '@/stores/org'
import { getMemberFieldsCached } from './memberFieldsCache'
import { getLinkTargetFieldsCached } from './linkTargetFieldsCache'
import PermissionRuleEditor from './PermissionRuleEditor.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import SaveButton from '@/components/common/SaveButton.vue'

const { t } = useI18n()
const orgStore = useOrgStore()

type PermissionRule = CardOperationPermission | FieldPermission | AttachmentPermission

interface Props {
  /** 权限规则列表 */
  modelValue?: T[]
  /** 卡片类型ID */
  cardTypeId: string
  /** 可用字段列表（用于条件编辑） */
  availableFields: FieldOption[]
  /** 权限类型 */
  permissionType: 'card' | 'field' | 'attachment'
  /** 可选择的属性列表（仅属性权限和附件权限） */
  selectableFields?: FieldOption[]
  /** 获取规则标题 */
  getRuleTitle: (rule: T) => string
  /** 创建默认规则 */
  createDefaultRule: () => T
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: T[]]
}>()

// 本地规则列表
const localRules = computed({
  get: () => props.modelValue ?? [],
  set: (value) => emit('update:modelValue', value),
})

// 编辑弹窗状态
const editorVisible = ref(false)
const editingIndex = ref<number>(-1)
const editingRule = ref<PermissionRule | null>(null)

// 成员字段缓存（按需加载）
const memberFields = ref<FieldOption[]>([])
let memberFieldsLoaded = false

function ensureMemberFields() {
  if (memberFieldsLoaded) return
  memberFieldsLoaded = true
  getMemberFieldsCached(orgStore.currentOrg?.memberCardTypeId)
    .then((fields) => { memberFields.value = fields })
    .catch(() => { /* ignore */ })
}

// Link 字段目标字段缓存（按需加载）
const linkTargetFieldsMap = ref<Map<string, FieldOption[]>>(new Map())
const loadedLinkFieldIds = new Set<string>()

/**
 * 从模板表达式中提取多级路径中的 link 字段 ID
 * 例如: ${card.linkField.targetField} 会提取出 linkField
 */
function extractLinkFieldIdsFromTemplate(template: string): string[] {
  const linkFieldIds: string[] = []
  const regex = /\$\{([^}]+)\}/g
  let match: RegExpExecArray | null

  while ((match = regex.exec(template)) !== null) {
    const expr = match[1]
    if (!expr) continue

    const parts = expr.split('.')
    if (parts.length < 3) continue // 至少需要 3 级: source.field1.field2

    const source = parts[0]
    if (source !== 'card') continue // 只处理 card 源

    // 从第 2 个字段开始，每个中间字段都可能是 link 字段
    for (let i = 1; i < parts.length - 1; i++) {
      const fieldId = parts[i]
      if (fieldId) {
        linkFieldIds.push(fieldId)
      }
    }
  }

  return [...new Set(linkFieldIds)] // 去重
}

/**
 * 确保加载指定 link 字段的目标卡片类型字段
 */
function ensureLinkTargetFields(linkFieldId: string) {
  if (loadedLinkFieldIds.has(linkFieldId)) return
  loadedLinkFieldIds.add(linkFieldId)

  getLinkTargetFieldsCached(linkFieldId)
    .then((fields) => {
      if (fields.length > 0) {
        linkTargetFieldsMap.value.set(linkFieldId, fields)
      }
    })
    .catch(() => { /* ignore */ })
}

// 格式化条件数量
function formatConditionCount(conditions?: unknown[]): string {
  if (!conditions || conditions.length === 0) {
    return t('admin.cardType.permission.noLimit')
  }
  return t('admin.cardType.permission.conditionCount', { count: conditions.length })
}

// 格式化提示信息预览
function formatAlertMessage(message?: string): string {
  if (!message) return ''

  // 检测并加载成员字段
  if (message.includes('${member.')) {
    ensureMemberFields()
  }

  // 检测并加载多级路径中的 link 字段目标字段
  const linkFieldIds = extractLinkFieldIdsFromTemplate(message)
  for (const linkFieldId of linkFieldIds) {
    ensureLinkTargetFields(linkFieldId)
  }

  return formatExpressionTemplate(message, props.availableFields, memberFields.value, t, linkTargetFieldsMap.value)
}

// 添加规则
function handleAdd() {
  editingIndex.value = -1
  editingRule.value = props.createDefaultRule() as unknown as PermissionRule
  editorVisible.value = true
}

// 编辑规则
function handleEdit(index: number) {
  editingIndex.value = index
  editingRule.value = { ...localRules.value[index] } as unknown as PermissionRule
  editorVisible.value = true
}

// 删除规则
function handleDelete(index: number) {
  const newRules = [...localRules.value]
  newRules.splice(index, 1)
  localRules.value = newRules
}

// 保存规则
function handleSave(rule: PermissionRule) {
  try {
    const newRules = [...localRules.value]
    if (editingIndex.value === -1) {
      newRules.push(rule as unknown as T)
    } else {
      newRules[editingIndex.value] = rule as unknown as T
    }
    localRules.value = newRules
    editorVisible.value = false
  } catch (error) {
    console.error('Error saving rule:', error)
  }
}

// 当卡片类型变化时，清除 link 字段缓存
watch(() => props.cardTypeId, () => {
  linkTargetFieldsMap.value.clear()
  loadedLinkFieldIds.clear()
})
</script>

<template>
  <div class="permission-panel">
    <!-- 空状态 -->
    <a-empty
      v-if="localRules.length === 0"
      :image="null"
      :description="t('admin.cardType.permission.noRules')"
    >
      <a-button class="add-rule-btn empty-add-btn" @click="handleAdd">
        <template #icon><IconPlus /></template>
        {{ t('admin.cardType.permission.addRule') }}
      </a-button>
    </a-empty>

    <!-- 规则列表 -->
    <div v-else class="rule-list">
      <div
        v-for="(rule, index) in localRules"
        :key="index"
        class="rule-item"
        @click="handleEdit(index)"
      >
        <div class="rule-header">
          <span class="rule-title">{{ getRuleTitle(rule) }}</span>
          <a-popconfirm
            :content="t('admin.cardType.permission.deleteRuleConfirm')"
            @ok="handleDelete(index)"
          >
            <template #icon>
              <span></span>
            </template>
            <template #ok>
              <SaveButton :text="t('common.action.confirm')" @click="handleDelete(index)" />
            </template>
            <template #cancel>
              <CancelButton />
            </template>
            <a-button type="text" size="small" status="danger" class="delete-btn" @click.stop>
              <template #icon><IconDelete /></template>
            </a-button>
          </a-popconfirm>
        </div>
        <div class="rule-content">
          <div class="rule-row">
            <span class="rule-label">{{ t('admin.cardType.permission.cardCondition') }}:</span>
            <span class="rule-value">{{ formatConditionCount(rule.cardConditions) }}</span>
          </div>
          <div class="rule-row">
            <span class="rule-label">{{ t('admin.cardType.permission.operatorCondition') }}:</span>
            <span class="rule-value">{{ formatConditionCount(rule.operatorConditions) }}</span>
          </div>
          <div v-if="rule.alertMessage" class="rule-row">
            <span class="rule-label">{{ t('admin.cardType.permission.alertMessage') }}:</span>
            <span class="rule-value alert-message">{{ formatAlertMessage(rule.alertMessage) }}</span>
          </div>
        </div>
      </div>

      <div class="add-button-wrapper">
        <a-button long class="add-rule-btn" @click="handleAdd">
          <template #icon><IconPlus /></template>
          {{ t('admin.cardType.permission.addRule') }}
        </a-button>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <PermissionRuleEditor
      v-model:visible="editorVisible"
      :rule="editingRule"
      :card-type-id="cardTypeId"
      :available-fields="availableFields"
      :selectable-fields="selectableFields"
      :permission-type="permissionType"
      @save="handleSave"
    />
  </div>
</template>

<style scoped>
.permission-panel {
  min-height: 100px;
  width: 100%;
  display: block;
}

.permission-panel :deep(.arco-empty) {
  width: 100%;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  padding-right: 4px;
}

.rule-item {
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 6px 20px !important;
  width: 100%;
  box-sizing: border-box;
  cursor: pointer;
}

.rule-item:hover {
  border-color: var(--color-primary);
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.rule-item:hover .delete-btn {
  opacity: 1;
}

.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.rule-title {
  font-weight: 500;
  color: var(--color-text-1);
}

.rule-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rule-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
}

.rule-label {
  color: var(--color-text-3);
  flex-shrink: 0;
}

.rule-value {
  color: var(--color-text-2);
}

.alert-message {
  color: var(--color-text-2);
}

.add-button-wrapper {
  margin-top: 8px;
  margin-bottom: 8px;
}

.add-rule-btn {
  background-color: transparent !important;
  border: 1px dashed var(--color-border) !important;
  color: var(--color-text-2) !important;
}

.add-rule-btn:hover {
  border-color: rgb(var(--primary-6)) !important;
  color: rgb(var(--primary-6)) !important;
}

.empty-add-btn {
  width: 100%;
}

.permission-panel :deep(.arco-empty-image) {
  display: none;
}

.permission-panel :deep(.arco-empty) {
  padding: 16px 0;
}
</style>
