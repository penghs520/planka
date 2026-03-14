<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconPlus, IconDelete, IconDragDotVertical, IconDown, IconRight } from '@arco-design/web-vue/es/icon'
import { Modal } from '@arco-design/web-vue'
import {
  type RuleAction,
  type DiscardCardAction,
  type ArchiveCardAction,
  type RestoreCardAction,
  type MoveCardAction,
  type UpdateCardAction,
  type CreateCardAction,
  type CreateLinkedCardAction,
  type CommentCardAction,
  type SendNotificationAction,
  type CallExternalApiAction,
  type ActionTargetSelector as ActionTargetSelectorType,
  RuleActionType,
  ActionTargetType,
} from '@/types/biz-rule'
import type { FieldOption } from '@/types/field-option'
import type { FieldAssignment } from '@/types/card-action'
import { fieldOptionsApi } from '@/api/field-options'
import { cardTypeApi } from '@/api/card-type'
import { valueStreamBranchApi, type StatusOption } from '@/api/value-stream'
import { useOrgStore } from '@/stores/org'
import FieldAssignmentList from '../card-action/FieldAssignmentList.vue'
import ActionTargetSelector from './ActionTargetSelector.vue'
import TextExpressionTemplateEditor from '@/components/common/text-expression-template/TextExpressionTemplateEditor.vue'
import type { FieldProvider } from '@/components/common/text-expression-template/types'
import { getMemberFieldsCached } from '@/views/schema-definition/card-type/components/permission/memberFieldsCache'
import { notificationTemplateApi, type NotificationTemplateDefinition } from '@/api/notification-template'

const { t } = useI18n()
const orgStore = useOrgStore()

const props = defineProps<{
  modelValue: RuleAction[]
  cardTypeId: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: RuleAction[]]
}>()

const actions = computed({
  get: () => props.modelValue || [],
  set: (value) => emit('update:modelValue', value),
})

// 字段选项列表
const fieldOptions = ref<FieldOption[]>([])
const loadingFields = ref(false)

// 成员卡片类型字段选项
const memberFieldOptions = ref<FieldOption[]>([])

// 成员卡片类型ID
const memberCardTypeId = computed(() => {
  if (!orgStore.currentOrgId) return ''
  return `${orgStore.currentOrgId}:member`
})

// 过滤掉系统内置字段，只保留可编辑的自定义字段
const editableFieldOptions = computed(() =>
  fieldOptions.value.filter(field => !field.systemField)
)

// 加载字段选项
async function loadFieldOptions() {
  if (!props.cardTypeId) return
  loadingFields.value = true
  try {
    fieldOptions.value = await fieldOptionsApi.getFields(props.cardTypeId)
  } catch (e) {
    console.error('Failed to load field options:', e)
  } finally {
    loadingFields.value = false
  }
}

// 加载成员卡片类型的字段选项
async function loadMemberFieldOptions() {
  if (!memberCardTypeId.value) return
  try {
    memberFieldOptions.value = await cardTypeApi.getFieldOptions(memberCardTypeId.value)
  } catch (e) {
    console.error('Failed to load member field options:', e)
  }
}

// 监听 cardTypeId 变化，加载字段选项
watch(
  () => props.cardTypeId,
  (newId) => {
    if (newId) {
      loadFieldOptions()
    }
  },
  { immediate: true }
)

// 监听 memberCardTypeId 变化，加载成员字段选项
watch(memberCardTypeId, () => {
  loadMemberFieldOptions()
}, { immediate: true })

// 每个动作的目标卡片类型字段选项缓存（按动作索引）
const actionTargetFieldOptions = ref<Map<number, FieldOption[]>>(new Map())
const actionTargetFieldsLoading = ref<Map<number, boolean>>(new Map())

// 每个动作的目标卡片类型状态选项缓存（按动作索引）
const actionTargetStatusOptions = ref<Map<number, StatusOption[]>>(new Map())
const actionTargetStatusLoading = ref<Map<number, boolean>>(new Map())

// 表达式模板编辑器的字段提供者（带请求缓存）
let cardFieldsCache: Promise<FieldOption[]> | null = null
const linkFieldsCache = new Map<string, Promise<FieldOption[]>>()

const expressionFieldProvider: FieldProvider = {
  getCardFields: () => {
    if (!cardFieldsCache && props.cardTypeId) {
      cardFieldsCache = fieldOptionsApi.getFields(props.cardTypeId)
    }
    return cardFieldsCache ?? Promise.resolve([])
  },
  getMemberFields: () => getMemberFieldsCached(orgStore.currentOrg?.memberCardTypeId),
  getFieldsByLinkFieldId: (id: string) => {
    let cached = linkFieldsCache.get(id)
    if (!cached) {
      cached = fieldOptionsApi.getFieldsByLinkFieldId(id)
      linkFieldsCache.set(id, cached)
    }
    return cached
  },
}

// 加载动作目标卡片类型的字段选项
async function loadActionTargetFieldOptions(index: number, targetCardTypeId: string) {
  actionTargetFieldsLoading.value.set(index, true)
  try {
    const fields = await fieldOptionsApi.getFields(targetCardTypeId)
    actionTargetFieldOptions.value.set(index, fields.filter(f => !f.systemField))
  } catch (e) {
    console.error('Failed to load action target field options:', e)
    actionTargetFieldOptions.value.set(index, [])
  } finally {
    actionTargetFieldsLoading.value.set(index, false)
  }
}

// 加载动作目标卡片类型的状态选项
async function loadActionTargetStatusOptions(index: number, targetCardTypeId: string) {
  actionTargetStatusLoading.value.set(index, true)
  try {
    const statuses = await valueStreamBranchApi.getStatusOptions(targetCardTypeId)
    actionTargetStatusOptions.value.set(index, statuses)
  } catch (e) {
    console.error('Failed to load action target status options:', e)
    actionTargetStatusOptions.value.set(index, [])
  } finally {
    actionTargetStatusLoading.value.set(index, false)
  }
}

// 获取动作的字段选项（根据操作对象）
function getActionFieldOptions(index: number, action: RuleAction): FieldOption[] {
  const target = getActionTarget(action)
  if (target.targetType === ActionTargetType.CURRENT_CARD) {
    return editableFieldOptions.value
  }
  return actionTargetFieldOptions.value.get(index) || []
}

// 获取动作的状态选项（根据操作对象）
function getActionStatusOptions(index: number, action: RuleAction): StatusOption[] {
  const target = getActionTarget(action)
  if (target.targetType === ActionTargetType.CURRENT_CARD) {
    // 当前卡片使用当前卡片类型的状态选项
    return currentCardTypeStatusOptions.value
  }
  return actionTargetStatusOptions.value.get(index) || []
}

// 通知模板列表
const notificationTemplates = ref<NotificationTemplateDefinition[]>([])
const loadingTemplates = ref(false)

async function loadNotificationTemplates() {
  if (!props.cardTypeId) return
  loadingTemplates.value = true
  try {
    notificationTemplates.value = await notificationTemplateApi.listByCardType(props.cardTypeId)
  } catch (e) {
    console.error('Failed to load notification templates:', e)
  } finally {
    loadingTemplates.value = false
  }
}

watch(() => props.cardTypeId, (newVal) => {
  if (newVal) loadNotificationTemplates()
}, { immediate: true })

// 当前卡片类型的状态选项
const currentCardTypeStatusOptions = ref<StatusOption[]>([])

// 加载当前卡片类型的状态选项
async function loadCurrentCardTypeStatusOptions() {
  if (!props.cardTypeId) return
  try {
    currentCardTypeStatusOptions.value = await valueStreamBranchApi.getStatusOptions(props.cardTypeId)
  } catch (e) {
    console.error('Failed to load current card type status options:', e)
    currentCardTypeStatusOptions.value = []
  }
}

// 监听 cardTypeId 变化，加载状态选项
watch(
  () => props.cardTypeId,
  (newId) => {
    if (newId) {
      loadCurrentCardTypeStatusOptions()
    }
  },
  { immediate: true }
)

// 初始化已有动作的目标字段和状态选项
function initActionTargetOptions() {
  actions.value.forEach((action, index) => {
    if (!needsTargetSelector(action.actionType)) return

    const target = getActionTarget(action)
    if (target.targetType === ActionTargetType.LINKED_CARD && target.linkPath?.linkNodes?.length) {
      const linkFieldId = target.linkPath.linkNodes[0]
      const linkField = fieldOptions.value.find(f => f.id === linkFieldId)
      if (linkField?.targetCardTypeIds?.length) {
        const targetCardTypeId = linkField.targetCardTypeIds[0]
        if (targetCardTypeId) {
          loadActionTargetFieldOptions(index, targetCardTypeId)
          loadActionTargetStatusOptions(index, targetCardTypeId)
        }
      }
    }
  })
}

// 监听 actions 和 fieldOptions 变化，初始化目标选项
watch(
  [() => props.modelValue, () => fieldOptions.value.length],
  () => {
    // 只在字段选项加载完成后初始化
    if (fieldOptions.value.length > 0) {
      initActionTargetOptions()
    }
  },
  { immediate: true }
)

// 动作类型选项
const actionTypeOptions = computed(() => [
  { label: t('admin.bizRule.actionType.UPDATE_CARD'), value: RuleActionType.UPDATE_CARD },
  { label: t('admin.bizRule.actionType.MOVE_CARD'), value: RuleActionType.MOVE_CARD },
  { label: t('admin.bizRule.actionType.DISCARD_CARD'), value: RuleActionType.DISCARD_CARD },
  { label: t('admin.bizRule.actionType.ARCHIVE_CARD'), value: RuleActionType.ARCHIVE_CARD },
  { label: t('admin.bizRule.actionType.RESTORE_CARD'), value: RuleActionType.RESTORE_CARD },
  { label: t('admin.bizRule.actionType.CREATE_CARD'), value: RuleActionType.CREATE_CARD },
  { label: t('admin.bizRule.actionType.CREATE_LINKED_CARD'), value: RuleActionType.CREATE_LINKED_CARD },
  { label: t('admin.bizRule.actionType.COMMENT_CARD'), value: RuleActionType.COMMENT_CARD },
  { label: t('admin.bizRule.actionType.SEND_NOTIFICATION'), value: RuleActionType.SEND_NOTIFICATION },
  { label: t('admin.bizRule.actionType.CALL_EXTERNAL_API'), value: RuleActionType.CALL_EXTERNAL_API },
])

// HTTP 方法选项
const httpMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'PATCH', value: 'PATCH' },
  { label: 'DELETE', value: 'DELETE' },
]

// 创建默认的动作对象
function createDefaultAction(actionType: RuleActionType, sortOrder: number): RuleAction {
  const baseTarget = { targetType: ActionTargetType.CURRENT_CARD }

  switch (actionType) {
    case RuleActionType.DISCARD_CARD:
      return {
        actionType,
        sortOrder,
        target: baseTarget,
        reasonTemplate: '',
      } as DiscardCardAction

    case RuleActionType.ARCHIVE_CARD:
      return {
        actionType,
        sortOrder,
        target: baseTarget,
      } as ArchiveCardAction

    case RuleActionType.RESTORE_CARD:
      return {
        actionType,
        sortOrder,
        target: baseTarget,
      } as RestoreCardAction

    case RuleActionType.MOVE_CARD:
      return {
        actionType,
        sortOrder,
        target: baseTarget,
        toStatusId: '',
      } as MoveCardAction

    case RuleActionType.UPDATE_CARD:
      return {
        actionType,
        sortOrder,
        target: baseTarget,
        fieldAssignments: [],
      } as UpdateCardAction

    case RuleActionType.CREATE_CARD:
      return {
        actionType,
        sortOrder,
        cardTypeId: '',
        titleTemplate: '',
        initialStatusId: '',
        fieldAssignments: [],
      } as CreateCardAction

    case RuleActionType.CREATE_LINKED_CARD:
      return {
        actionType,
        sortOrder,
        linkFieldId: '',
        cardTypeId: '',
        titleTemplate: '',
        initialStatusId: '',
        fieldAssignments: [],
      } as CreateLinkedCardAction

    case RuleActionType.COMMENT_CARD:
      return {
        actionType,
        sortOrder,
        target: baseTarget,
        contentTemplate: '',
      } as CommentCardAction

    case RuleActionType.SEND_NOTIFICATION:
      return {
        actionType,
        sortOrder,
        templateIds: [],
      } as SendNotificationAction

    case RuleActionType.CALL_EXTERNAL_API:
      return {
        actionType,
        sortOrder,
        urlTemplate: '',
        method: 'POST',
        headers: {},
        bodyTemplate: '',
        timeoutMs: 30000,
      } as CallExternalApiAction

    default:
      return {
        actionType: RuleActionType.UPDATE_CARD,
        sortOrder,
        target: baseTarget,
        fieldAssignments: [],
      } as UpdateCardAction
  }
}

// 添加动作
function handleAddAction() {
  const newAction = createDefaultAction(RuleActionType.UPDATE_CARD, actions.value.length)
  actions.value = [...actions.value, newAction]
}

// 删除动作
function handleDeleteAction(index: number) {
  const newActions = [...actions.value]
  newActions.splice(index, 1)
  newActions.forEach((action, i) => {
    action.sortOrder = i
  })
  actions.value = newActions
}


// 更新动作属性
function updateAction(index: number, updates: Partial<RuleAction>) {
  const newActions = [...actions.value]
  newActions[index] = { ...newActions[index], ...updates } as RuleAction
  actions.value = newActions
}

// 获取丢弃原因
function getDiscardReason(action: RuleAction): string {
  const a = action as DiscardCardAction
  return a.reasonTemplate || ''
}

// 设置丢弃原因
function setDiscardReason(index: number, value: string) {
  const action = actions.value[index] as DiscardCardAction
  updateAction(index, {
    ...action,
    reasonTemplate: value,
  })
}

// 获取移动目标状态
function getMoveToStatus(action: RuleAction): string {
  const a = action as MoveCardAction
  return a.toStatusId || ''
}

// 设置移动目标状态
function setMoveToStatus(index: number, value: string) {
  updateAction(index, { toStatusId: value } as any)
}

// 获取评论内容
function getCommentContent(action: RuleAction): string {
  const a = action as CommentCardAction
  return a.contentTemplate || ''
}

// 设置评论内容
function setCommentContent(index: number, value: string) {
  const action = actions.value[index] as CommentCardAction
  updateAction(index, {
    ...action,
    contentTemplate: value,
  })
}

// 获取 API URL
function getApiUrl(action: RuleAction): string {
  const a = action as CallExternalApiAction
  return a.urlTemplate || ''
}

// 设置 API URL
function setApiUrl(index: number, value: string) {
  const action = actions.value[index] as CallExternalApiAction
  updateAction(index, {
    ...action,
    urlTemplate: value,
  })
}

// 获取 HTTP 方法
function getHttpMethod(action: RuleAction): string {
  const a = action as CallExternalApiAction
  return a.method || 'POST'
}

// 设置 HTTP 方法
function setHttpMethod(index: number, value: string) {
  updateAction(index, { method: value } as any)
}

// 获取请求体
function getApiBody(action: RuleAction): string {
  const a = action as CallExternalApiAction
  return a.bodyTemplate || ''
}

// 设置请求体
function setApiBody(index: number, value: string) {
  const action = actions.value[index] as CallExternalApiAction
  updateAction(index, {
    ...action,
    bodyTemplate: value,
  })
}

// 获取超时时间
function getTimeout(action: RuleAction): number {
  const a = action as CallExternalApiAction
  return a.timeoutMs || 30000
}

// 设置超时时间
function setTimeout(index: number, value: number) {
  updateAction(index, { timeoutMs: value } as any)
}

// 获取通知模板ID列表
function getTemplateIds(action: RuleAction): string[] {
  return (action as SendNotificationAction).templateIds || []
}

// 设置通知模板ID列表
function setTemplateIds(index: number, value: string[]) {
  updateAction(index, { templateIds: value } as Partial<SendNotificationAction>)
}

// 获取创建卡片类型
function getCreateCardType(action: RuleAction): string {
  const a = action as CreateCardAction
  return a.cardTypeId || ''
}

// 设置创建卡片类型
function setCreateCardType(index: number, value: string) {
  updateAction(index, { cardTypeId: value } as any)
}

// 获取卡片标题模板
function getCardTitleTemplate(action: RuleAction): string {
  const a = action as CreateCardAction
  return a.titleTemplate || ''
}

// 设置卡片标题模板
function setCardTitleTemplate(index: number, value: string) {
  const action = actions.value[index] as CreateCardAction
  updateAction(index, {
    ...action,
    titleTemplate: value,
  })
}

// 获取关联字段
function getLinkFieldId(action: RuleAction): string {
  const a = action as CreateLinkedCardAction
  return a.linkFieldId || ''
}

// 设置关联字段
function setLinkFieldId(index: number, value: string) {
  updateAction(index, { linkFieldId: value } as any)
}

// 获取字段赋值列表
function getFieldAssignments(action: RuleAction): FieldAssignment[] {
  const a = action as UpdateCardAction | CreateCardAction | CreateLinkedCardAction
  return a.fieldAssignments || []
}

// 设置字段赋值列表
function setFieldAssignments(index: number, value: FieldAssignment[]) {
  updateAction(index, { fieldAssignments: value } as any)
}

// 获取操作对象（target）
function getActionTarget(action: RuleAction): ActionTargetSelectorType {
  const a = action as DiscardCardAction | ArchiveCardAction | RestoreCardAction | MoveCardAction | UpdateCardAction | CommentCardAction
  return a.target || { targetType: ActionTargetType.CURRENT_CARD }
}

// 设置操作对象
function setActionTarget(index: number, value: ActionTargetSelectorType) {
  const action = actions.value[index]
  if (!action) return

  const oldTarget = getActionTarget(action)

  // 如果操作对象类型变化，清空相关配置
  if (oldTarget.targetType !== value.targetType ||
      JSON.stringify(oldTarget.linkPath) !== JSON.stringify(value.linkPath)) {

    // 根据动作类型清空不同的配置
    const updates: Record<string, any> = { target: value }

    if (action.actionType === RuleActionType.UPDATE_CARD) {
      updates.fieldAssignments = []
    } else if (action.actionType === RuleActionType.MOVE_CARD) {
      updates.toStatusId = ''
    }

    updateAction(index, updates as any)

    // 如果切换到关联卡片，加载目标卡片类型的字段和状态选项
    if (value.targetType === ActionTargetType.LINKED_CARD && value.linkPath?.linkNodes?.length) {
      const linkFieldId = value.linkPath.linkNodes[0]
      const linkField = fieldOptions.value.find(f => f.id === linkFieldId)
      if (linkField?.targetCardTypeIds?.length) {
        const targetCardTypeId = linkField.targetCardTypeIds[0]
        if (targetCardTypeId) {
          loadActionTargetFieldOptions(index, targetCardTypeId)
          loadActionTargetStatusOptions(index, targetCardTypeId)
        }
      }
    }
  } else {
    updateAction(index, { target: value } as any)
  }
}

// 判断动作类型是否需要操作对象选择器
function needsTargetSelector(actionType: RuleActionType): boolean {
  return [
    RuleActionType.UPDATE_CARD,
    RuleActionType.MOVE_CARD,
    RuleActionType.DISCARD_CARD,
    RuleActionType.ARCHIVE_CARD,
    RuleActionType.RESTORE_CARD,
    RuleActionType.COMMENT_CARD,
  ].includes(actionType)
}

// 折叠状态
const collapsedIndices = ref<Set<number>>(new Set())

function toggleCollapse(index: number) {
  if (collapsedIndices.value.has(index)) {
    collapsedIndices.value.delete(index)
  } else {
    collapsedIndices.value.add(index)
  }
}

function isCollapsed(index: number): boolean {
  return collapsedIndices.value.has(index)
}

// 判断动作是否有配置
function hasActionConfig(action: RuleAction): boolean {
  // UPDATE_CARD 检查字段赋值
  if (action.actionType === RuleActionType.UPDATE_CARD) {
    const a = action as UpdateCardAction
    return (a.fieldAssignments?.length || 0) > 0
  }
  // CREATE_CARD / CREATE_LINKED_CARD 检查字段赋值或标题或卡片类型
  if (action.actionType === RuleActionType.CREATE_CARD ||
      action.actionType === RuleActionType.CREATE_LINKED_CARD) {
    const a = action as CreateCardAction | CreateLinkedCardAction
    return (a.fieldAssignments?.length || 0) > 0 || !!a.titleTemplate || !!a.cardTypeId
  }
  // MOVE_CARD 检查目标状态
  if (action.actionType === RuleActionType.MOVE_CARD) {
    const a = action as MoveCardAction
    return !!a.toStatusId
  }
  // DISCARD_CARD 检查原因
  if (action.actionType === RuleActionType.DISCARD_CARD) {
    const a = action as DiscardCardAction
    return !!a.reasonTemplate
  }
  // COMMENT_CARD 检查内容
  if (action.actionType === RuleActionType.COMMENT_CARD) {
    const a = action as CommentCardAction
    return !!a.contentTemplate
  }
  // SEND_NOTIFICATION 检查模板
  if (action.actionType === RuleActionType.SEND_NOTIFICATION) {
    const a = action as SendNotificationAction
    return (a.templateIds?.length || 0) > 0
  }
  // CALL_EXTERNAL_API 检查 URL
  if (action.actionType === RuleActionType.CALL_EXTERNAL_API) {
    const a = action as CallExternalApiAction
    return !!a.urlTemplate
  }
  // ARCHIVE_CARD / RESTORE_CARD 无需配置
  return false
}

// 获取动作摘要
function getActionSummary(action: RuleAction): string {
  const actionTypeLabel = actionTypeOptions.value.find(o => o.value === action.actionType)?.label || action.actionType
  const configCount = hasActionConfig(action) ? '已配置' : '未配置'
  return `${actionTypeLabel} · ${configCount}`
}

// 更新动作类型（带确认）
function handleActionTypeChangeWithConfirm(index: number, newActionType: RuleActionType) {
  const oldAction = actions.value[index]
  if (!oldAction || oldAction.actionType === newActionType) return

  // 检查是否有配置，有配置时才需要确认
  const hasConfig = hasActionConfig(oldAction)
  if (!hasConfig) {
    doActionTypeChange(index, newActionType)
    return
  }

  Modal.confirm({
    title: '切换动作类型',
    content: '切换动作类型会清空当前动作的所有配置，确定要继续吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      doActionTypeChange(index, newActionType)
    },
  })
}

// 实际执行动作类型切换
function doActionTypeChange(index: number, actionType: RuleActionType) {
  const newActions = [...actions.value]
  const oldAction = newActions[index]
  const newAction = createDefaultAction(actionType, oldAction?.sortOrder ?? index)
  newActions[index] = newAction
  actions.value = newActions
}
</script>

<template>
  <div class="rule-actions-editor">
    <!-- 动作列表 -->
    <div v-if="actions.length > 0" class="action-list">
      <div
        v-for="(action, index) in actions"
        :key="index"
        class="action-item"
      >
        <div class="action-header" @click="toggleCollapse(index)">
          <div class="action-drag" @click.stop>
            <IconDragDotVertical />
          </div>
          <div class="action-index">{{ index + 1 }}</div>
          <div class="action-collapse-icon">
            <IconDown v-if="!isCollapsed(index)" />
            <IconRight v-else />
          </div>
          <template v-if="isCollapsed(index)">
            <span class="action-summary">{{ getActionSummary(action) }}</span>
          </template>
          <template v-else>
            <a-select
              :model-value="action.actionType"
              :options="actionTypeOptions"
              class="action-type-select"
              size="small"
              @change="(val: RuleActionType) => handleActionTypeChangeWithConfirm(index, val)"
              @click.stop
            />
          </template>
          <a-button
            type="text"
            size="small"
            class="action-delete"
            @click.stop="handleDeleteAction(index)"
          >
            <template #icon><IconDelete /></template>
          </a-button>
        </div>

        <!-- 动作配置区域 -->
        <div v-show="!isCollapsed(index)" class="action-config">
          <!-- 操作对象选择器（适用于操作卡片的动作类型） -->
          <div v-if="needsTargetSelector(action.actionType)" class="action-target-row">
            <span class="action-target-label">{{ t('admin.bizRule.actionConfig.actionTarget') }}：</span>
            <ActionTargetSelector
              :model-value="getActionTarget(action)"
              :field-options="fieldOptions"
              :card-type-id="cardTypeId"
              @update:model-value="(val: ActionTargetSelectorType) => setActionTarget(index, val)"
            />
          </div>

          <!-- UPDATE_CARD 配置 -->
          <template v-if="action.actionType === RuleActionType.UPDATE_CARD">
            <div class="config-section">
              <FieldAssignmentList
                :model-value="getFieldAssignments(action)"
                :field-options="getActionFieldOptions(index, action)"
                :reference-field-options="editableFieldOptions"
                :member-field-options="memberFieldOptions"
                :current-card-type-id="cardTypeId"
                :member-card-type-id="memberCardTypeId"
                :hide-user-input="true"
                @update:model-value="(val: FieldAssignment[]) => setFieldAssignments(index, val)"
              />
            </div>
          </template>

          <!-- MOVE_CARD 配置 -->
          <template v-else-if="action.actionType === RuleActionType.MOVE_CARD">
            <a-form-item :label="t('admin.bizRule.actionConfig.toStatus')">
              <a-select
                :model-value="getMoveToStatus(action)"
                :placeholder="t('admin.bizRule.actionConfig.toStatusPlaceholder')"
                allow-clear
                @change="(val: string) => setMoveToStatus(index, val || '')"
              >
                <a-option
                  v-for="status in getActionStatusOptions(index, action)"
                  :key="status.id"
                  :value="status.id"
                  :label="status.name"
                />
              </a-select>
            </a-form-item>
          </template>

          <!-- DISCARD_CARD 配置 -->
          <template v-else-if="action.actionType === RuleActionType.DISCARD_CARD">
            <a-form-item :label="t('admin.bizRule.actionConfig.reason')">
              <TextExpressionTemplateEditor
                :model-value="getDiscardReason(action)"
                :field-provider="expressionFieldProvider"
                :placeholder="t('admin.bizRule.actionConfig.reasonPlaceholder')"
                @update:model-value="(val: string) => setDiscardReason(index, val)"
              />
            </a-form-item>
          </template>

          <!-- ARCHIVE_CARD / RESTORE_CARD 无需额外配置 -->
          <template v-else-if="action.actionType === RuleActionType.ARCHIVE_CARD || action.actionType === RuleActionType.RESTORE_CARD">
            <div class="no-config-hint">
              {{ t('admin.bizRule.actionConfig.noConfigRequired') }}
            </div>
          </template>

          <!-- CREATE_CARD 配置 -->
          <template v-else-if="action.actionType === RuleActionType.CREATE_CARD">
            <a-form-item :label="t('admin.bizRule.actionConfig.targetCardType')">
              <a-input
                :model-value="getCreateCardType(action)"
                :placeholder="t('admin.bizRule.actionConfig.targetCardTypePlaceholder')"
                @update:model-value="(val: string) => setCreateCardType(index, val)"
              />
            </a-form-item>
            <a-form-item :label="t('admin.bizRule.actionConfig.cardTitle')">
              <TextExpressionTemplateEditor
                :model-value="getCardTitleTemplate(action)"
                :field-provider="expressionFieldProvider"
                :placeholder="t('admin.bizRule.actionConfig.cardTitlePlaceholder')"
                @update:model-value="(val: string) => setCardTitleTemplate(index, val)"
              />
            </a-form-item>
            <div class="config-section">
              <FieldAssignmentList
                :model-value="getFieldAssignments(action)"
                :field-options="editableFieldOptions"
                :reference-field-options="editableFieldOptions"
                :member-field-options="memberFieldOptions"
                :current-card-type-id="cardTypeId"
                :member-card-type-id="memberCardTypeId"
                :hide-user-input="true"
                @update:model-value="(val: FieldAssignment[]) => setFieldAssignments(index, val)"
              />
            </div>
          </template>

          <!-- CREATE_LINKED_CARD 配置 -->
          <template v-else-if="action.actionType === RuleActionType.CREATE_LINKED_CARD">
            <a-form-item :label="t('admin.bizRule.actionConfig.linkField')">
              <a-input
                :model-value="getLinkFieldId(action)"
                :placeholder="t('admin.bizRule.actionConfig.linkFieldPlaceholder')"
                @update:model-value="(val: string) => setLinkFieldId(index, val)"
              />
            </a-form-item>
            <a-form-item :label="t('admin.bizRule.actionConfig.targetCardType')">
              <a-input
                :model-value="getCreateCardType(action)"
                :placeholder="t('admin.bizRule.actionConfig.targetCardTypePlaceholder')"
                @update:model-value="(val: string) => setCreateCardType(index, val)"
              />
            </a-form-item>
            <a-form-item :label="t('admin.bizRule.actionConfig.cardTitle')">
              <TextExpressionTemplateEditor
                :model-value="getCardTitleTemplate(action)"
                :field-provider="expressionFieldProvider"
                :placeholder="t('admin.bizRule.actionConfig.cardTitlePlaceholder')"
                @update:model-value="(val: string) => setCardTitleTemplate(index, val)"
              />
            </a-form-item>
            <div class="config-section">
              <FieldAssignmentList
                :model-value="getFieldAssignments(action)"
                :field-options="editableFieldOptions"
                :reference-field-options="editableFieldOptions"
                :member-field-options="memberFieldOptions"
                :current-card-type-id="cardTypeId"
                :member-card-type-id="memberCardTypeId"
                :hide-user-input="true"
                @update:model-value="(val: FieldAssignment[]) => setFieldAssignments(index, val)"
              />
            </div>
          </template>

          <!-- COMMENT_CARD 配置 -->
          <template v-else-if="action.actionType === RuleActionType.COMMENT_CARD">
            <a-form-item :label="t('admin.bizRule.actionConfig.commentContent')">
              <TextExpressionTemplateEditor
                :model-value="getCommentContent(action)"
                :field-provider="expressionFieldProvider"
                :placeholder="t('admin.bizRule.actionConfig.commentPlaceholder')"
                @update:model-value="(val: string) => setCommentContent(index, val)"
              />
            </a-form-item>
          </template>

          <!-- SEND_NOTIFICATION 配置 - 简化版 -->
          <template v-else-if="action.actionType === RuleActionType.SEND_NOTIFICATION">
            <a-form-item :label="t('admin.bizRule.actionConfig.notificationTemplates')">
              <a-select
                :model-value="getTemplateIds(action)"
                :options="notificationTemplates.map(t => ({ label: t.name, value: t.id }))"
                :loading="loadingTemplates"
                multiple
                allow-clear
                :placeholder="t('admin.bizRule.actionConfig.selectTemplatesPlaceholder')"
                @update:model-value="(val: string[]) => setTemplateIds(index, val)"
              />
            </a-form-item>
          </template>

          <!-- CALL_EXTERNAL_API 配置 -->
          <template v-else-if="action.actionType === RuleActionType.CALL_EXTERNAL_API">
            <a-row :gutter="16">
              <a-col :span="16">
                <a-form-item :label="t('admin.bizRule.actionConfig.apiUrl')">
                  <TextExpressionTemplateEditor
                    :model-value="getApiUrl(action)"
                    :field-provider="expressionFieldProvider"
                    :placeholder="t('admin.bizRule.actionConfig.apiUrlPlaceholder')"
                    @update:model-value="(val: string) => setApiUrl(index, val)"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item :label="t('admin.bizRule.actionConfig.httpMethod')">
                  <a-select
                    :model-value="getHttpMethod(action)"
                    :options="httpMethodOptions"
                    @update:model-value="(val: string) => setHttpMethod(index, val)"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item :label="t('admin.bizRule.actionConfig.requestBody')">
              <TextExpressionTemplateEditor
                :model-value="getApiBody(action)"
                :field-provider="expressionFieldProvider"
                :placeholder="t('admin.bizRule.actionConfig.requestBodyPlaceholder')"
                @update:model-value="(val: string) => setApiBody(index, val)"
              />
            </a-form-item>
            <a-form-item :label="t('admin.bizRule.actionConfig.timeout')">
              <a-input-number
                :model-value="getTimeout(action)"
                :min="1000"
                :max="300000"
                :step="1000"
                @update:model-value="(val: number) => setTimeout(index, val)"
              >
                <template #suffix>ms</template>
              </a-input-number>
            </a-form-item>
          </template>
        </div>
      </div>
    </div>

    <!-- 空状态提示 -->
    <div v-if="actions.length === 0" class="empty-action-state">
      <div class="empty-hint">{{ t('admin.bizRule.actionConfig.emptyHint') }}</div>
      <a-button type="outline" class="add-action-btn" @click="handleAddAction">
        <template #icon><IconPlus /></template>
        {{ t('admin.bizRule.actionConfig.addAction') }}
      </a-button>
    </div>

    <!-- 添加动作按钮（有内容时显示在底部） -->
    <a-button v-else type="outline" long class="add-action-btn-bottom" @click="handleAddAction">
      <template #icon><IconPlus /></template>
      {{ t('admin.bizRule.actionConfig.addAction') }}
    </a-button>
  </div>
</template>

<style scoped lang="scss">
.rule-actions-editor {
  width: 100%;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.action-item {
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  background-color: var(--color-bg-1);
}

.action-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: var(--color-bg-1);
  border-bottom: 1px solid var(--color-border-2);
  border-radius: 6px 6px 0 0;
  cursor: pointer;

  &:hover {
    background-color: var(--color-fill-1);
  }

  :deep(.action-type-select) {
    flex: 0 0 180px;
    width: 180px;
  }
}

.action-drag {
  cursor: grab;
  color: var(--color-text-3);

  &:hover {
    color: var(--color-text-2);
  }
}

.action-index {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary-light-1);
  color: var(--color-primary-6);
  border-radius: 50%;
  font-size: 11px;
  font-weight: 500;
}

.action-collapse-icon {
  color: var(--color-text-3);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-summary {
  flex: 1;
  font-size: 13px;
  color: var(--color-text-2);
}

.action-delete {
  margin-left: auto;
  color: var(--color-text-3);
  opacity: 0;
  transition: opacity 0.2s;

  &:hover {
    color: rgb(var(--danger-6));
    background-color: rgb(var(--danger-1));
  }
}

.action-header:hover .action-delete {
  opacity: 1;
}

.action-config {
  padding: 12px;

  :deep(.arco-form-item) {
    margin-bottom: 12px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(.arco-form-item-label) {
    font-weight: 500;
  }

  :deep(.arco-select),
  :deep(.arco-input-wrapper) {
    max-width: 300px;
  }
}

.action-target-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;

  .action-target-label {
    font-size: 13px;
    color: var(--color-text-2);
    font-weight: 500;
    white-space: nowrap;
  }
}

.config-section {
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
}

.no-config-hint {
  color: var(--color-text-3);
  font-size: 13px;
  text-align: center;
  padding: 8px 0;
}

.empty-action-state {
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

  .add-action-btn {
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

.add-action-btn-bottom {
  margin-top: 8px;
  border-style: dashed;
  border-color: var(--color-border-2);
  color: var(--color-text-2);

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
    background: var(--color-primary-light-1);
  }
}
</style>
