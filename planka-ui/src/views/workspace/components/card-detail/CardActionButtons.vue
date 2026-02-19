<script setup lang="ts">
/**
 * 卡片动作按钮组件
 * 显示在卡片详情页头部，提供动作执行入口
 */
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import { IconMore } from '@arco-design/web-vue/es/icon'
import { cardActionApi, cardActionExecuteApi } from '@/api/card-action'
import type { CardActionConfigDefinition, FieldAssignment, RequiredInput } from '@/types/card-action'
import { ActionCategory, BuiltInActionType, ActionExecutionResultType, AssignmentTypeEnum, ExecutionTypeEnum } from '@/types/card-action'
import type { CardDTO } from '@/types/card'
import ActionInputModal from './ActionInputModal.vue'

const { t } = useI18n()

const props = defineProps<{
  card: CardDTO
}>()

const emit = defineEmits<{
  /** 动作执行成功，需要刷新卡片数据 */
  refresh: []
}>()

const loading = ref(false)
const executingActionId = ref<string | null>(null)
const actions = ref<CardActionConfigDefinition[]>([])

// 用户输入弹窗状态（基于动作配置的用户输入）
const inputModalVisible = ref(false)
const currentActionForInput = ref<CardActionConfigDefinition | null>(null)

// 后端要求输入的弹窗状态（基于执行时后端返回的 REQUIRE_INPUT）
const requiredInputModalVisible = ref(false)
const requiredInputs = ref<RequiredInput[]>([])
const requiredInputValues = ref<Record<string, string>>({})
const pendingActionForRequiredInput = ref<CardActionConfigDefinition | null>(null)

// 获取可用的动作列表
const availableActions = computed(() => {
  return actions.value.filter(action => {
    if (!action.enabled) return false
    // 根据卡片状态过滤内置动作
    if (action.builtIn && action.builtInActionType) {
      return isBuiltInActionVisible(action.builtInActionType)
    }
    return true
  })
})

// 内置动作按钮（显示在主按钮区域）
const primaryActions = computed(() => {
  return availableActions.value.filter(action =>
    action.builtIn && action.actionCategory === ActionCategory.LIFECYCLE
  ).slice(0, 2)
})

// 更多动作（显示在下拉菜单中）
const moreActions = computed(() => {
  const primaryIds = new Set(primaryActions.value.map(a => a.id))
  return availableActions.value.filter(action => !primaryIds.has(action.id))
})

// 判断内置动作是否可见
function isBuiltInActionVisible(builtInType: BuiltInActionType): boolean {
  const cardStyle = props.card.cardStyle || 'ACTIVE'
  switch (builtInType) {
    case BuiltInActionType.DISCARD:
    case BuiltInActionType.ARCHIVE:
      return cardStyle === 'ACTIVE'
    case BuiltInActionType.RESTORE:
      return cardStyle === 'DISCARDED' || cardStyle === 'ARCHIVED'
    case BuiltInActionType.BLOCK_TOGGLE:
    case BuiltInActionType.HIGHLIGHT_TOGGLE:
      return cardStyle === 'ACTIVE'
    default:
      return true
  }
}

// 获取内置动作的显示名称（切换类动作根据状态显示不同文案）
function getActionDisplayName(action: CardActionConfigDefinition): string {
  if (!action.builtIn || !action.builtInActionType) {
    return action.name
  }
  // TODO: 切换类动作需要根据卡片状态显示不同文案，待 CardDTO 添加 blocked/highlighted 字段后实现
  return action.name
}

// 加载动作列表
async function fetchActions() {
  if (!props.card?.typeId) return

  loading.value = true
  try {
    actions.value = await cardActionApi.getByCardTypeId(props.card.typeId)
  } catch (error) {
    console.error('Failed to fetch card actions:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 检查动作是否包含用户输入类型的字段赋值
 */
function hasUserInputAssignments(action: CardActionConfigDefinition): boolean {
  const execType = action.executionType
  if (!execType) return false

  let assignments: FieldAssignment[] = []
  if (execType.type === ExecutionTypeEnum.UPDATE_CARD && execType.fieldAssignments) {
    assignments = execType.fieldAssignments
  } else if (execType.type === ExecutionTypeEnum.CREATE_LINKED_CARD && execType.fieldAssignments) {
    assignments = execType.fieldAssignments
  }

  return assignments.some(a => a.assignmentType === AssignmentTypeEnum.USER_INPUT)
}

// 执行动作
async function handleExecuteAction(action: CardActionConfigDefinition) {
  if (!action.id) return

  // 检查是否需要用户输入
  if (hasUserInputAssignments(action)) {
    currentActionForInput.value = action
    inputModalVisible.value = true
    return
  }

  // 如果有确认提示，显示确认弹窗
  if (action.confirmMessage) {
    Modal.confirm({
      title: t('admin.cardAction.confirmTitle'),
      content: action.confirmMessage,
      okText: t('admin.action.confirm'),
      modalClass: 'arco-modal-simple',
      async onOk() {
        await doExecuteAction(action)
      },
    })
  } else {
    await doExecuteAction(action)
  }
}

/**
 * 处理用户输入完成
 */
async function handleInputConfirm(userInputs: Record<string, unknown>) {
  const action = currentActionForInput.value
  if (!action) return

  inputModalVisible.value = false

  // 如果有确认提示，显示确认弹窗
  if (action.confirmMessage) {
    Modal.confirm({
      title: t('admin.cardAction.confirmTitle'),
      content: action.confirmMessage,
      okText: t('admin.action.confirm'),
      modalClass: 'arco-modal-simple',
      async onOk() {
        await doExecuteAction(action, userInputs)
      },
    })
  } else {
    await doExecuteAction(action, userInputs)
  }

  currentActionForInput.value = null
}

/**
 * 处理用户输入取消
 */
function handleInputCancel() {
  inputModalVisible.value = false
  currentActionForInput.value = null
}

// 实际执行动作
async function doExecuteAction(action: CardActionConfigDefinition, userInputs?: Record<string, unknown>) {
  if (!action.id) return

  executingActionId.value = action.id
  try {
    const result = await cardActionExecuteApi.execute(action.id, props.card.id, userInputs)

    if (result.type === ActionExecutionResultType.SUCCESS) {
      // 显示成功提示 - 优先使用API返回的消息，其次使用动作配置的消息，最后使用通用消息
      const message = result.message || action.successMessage || t('admin.cardAction.message.executeSuccess')
      Message.success(message)
      // 刷新卡片数据
      emit('refresh')
    } else if (result.type === ActionExecutionResultType.NAVIGATE) {
      // 跳转页面
      if (result.navigateUrl) {
        // 确保 URL 有协议前缀
        let url = result.navigateUrl
        if (!url.startsWith('http://') && !url.startsWith('https://') && !url.startsWith('/')) {
          url = 'https://' + url
        }
        if (result.openInNewWindow) {
          window.open(url, '_blank')
        } else {
          window.location.href = url
        }
      }
    } else if (result.type === ActionExecutionResultType.REQUIRE_INPUT) {
      // 后端要求用户输入（如丢弃原因）
      if (result.requiredInputs && result.requiredInputs.length > 0) {
        requiredInputs.value = result.requiredInputs
        requiredInputValues.value = {}
        pendingActionForRequiredInput.value = action
        requiredInputModalVisible.value = true
      }
    } else if (result.type === ActionExecutionResultType.ERROR) {
      Message.error(result.message || t('admin.cardAction.message.executeFailed'))
    }
  } catch (error: any) {
    console.error('Failed to execute action:', error)
    Message.error(error.message || t('admin.cardAction.message.executeFailed'))
  } finally {
    executingActionId.value = null
  }
}

/**
 * 处理后端要求的用户输入确认
 */
async function handleRequiredInputConfirm() {
  const action = pendingActionForRequiredInput.value
  if (!action) return

  // 验证必填字段
  for (const input of requiredInputs.value) {
    if (input.required && !requiredInputValues.value[input.fieldId]?.trim()) {
      Message.warning(`请输入${input.label}`)
      return
    }
  }

  requiredInputModalVisible.value = false

  // 将用户输入转换为 FixedValue 格式
  const userInputs: Record<string, unknown> = {}
  for (const input of requiredInputs.value) {
    const value = requiredInputValues.value[input.fieldId]
    if (value !== undefined && value !== null) {
      userInputs[input.fieldId] = { valueType: 'TEXT', text: value }
    }
  }

  // 重新执行动作
  await doExecuteAction(action, userInputs)
  pendingActionForRequiredInput.value = null
}

/**
 * 处理后端要求的用户输入取消
 */
function handleRequiredInputCancel() {
  requiredInputModalVisible.value = false
  pendingActionForRequiredInput.value = null
  requiredInputs.value = []
  requiredInputValues.value = {}
}

// 监听卡片变化，重新加载动作
watch(
  () => props.card?.typeId,
  (newTypeId) => {
    if (newTypeId) {
      fetchActions()
    }
  },
  { immediate: true }
)
</script>

<template>
  <div v-if="availableActions.length > 0" class="card-action-buttons">
    <!-- 主要动作按钮 -->
    <a-button
      v-for="action in primaryActions"
      :key="action.id"
      type="text"
      size="small"
      class="action-btn"
      :loading="executingActionId === action.id"
      @click="handleExecuteAction(action)"
    >
      {{ getActionDisplayName(action) }}
    </a-button>

    <!-- 更多动作下拉菜单 -->
    <a-dropdown v-if="moreActions.length > 0" trigger="click" position="br">
      <a-button type="text" size="small" class="action-btn more-btn">
        <template #icon><IconMore /></template>
      </a-button>
      <template #content>
        <a-doption
          v-for="action in moreActions"
          :key="action.id"
          :disabled="executingActionId === action.id"
          @click="handleExecuteAction(action)"
        >
          {{ getActionDisplayName(action) }}
        </a-doption>
      </template>
    </a-dropdown>
  </div>

  <!-- 用户输入弹窗 -->
  <ActionInputModal
    v-model:visible="inputModalVisible"
    :action="currentActionForInput"
    :card-type-id="props.card.typeId"
    @confirm="handleInputConfirm"
    @cancel="handleInputCancel"
  />

  <!-- 后端要求输入的弹窗（如丢弃原因） -->
  <a-modal
    v-model:visible="requiredInputModalVisible"
    :title="t('admin.cardAction.requiredInputModal.title')"
    :ok-text="t('admin.action.confirm')"
    :cancel-text="t('admin.action.cancel')"
    :mask-closable="false"
    modal-class="arco-modal-simple"
    @ok="handleRequiredInputConfirm"
    @cancel="handleRequiredInputCancel"
  >
    <a-form :model="requiredInputValues" layout="vertical">
      <a-form-item
        v-for="input in requiredInputs"
        :key="input.fieldId"
        :label="input.label"
        :required="input.required"
      >
        <a-textarea
          v-if="input.inputType === 'textarea'"
          v-model="requiredInputValues[input.fieldId]"
          :placeholder="input.placeholder"
          :auto-size="{ minRows: 3, maxRows: 6 }"
        />
        <a-input
          v-else
          v-model="requiredInputValues[input.fieldId]"
          :placeholder="input.placeholder"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped lang="scss">
.card-action-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn {
  color: var(--color-text-2);
  font-size: 13px;
  padding: 0 8px;

  &:hover {
    color: rgb(var(--primary-6));
    background-color: var(--color-fill-2);
  }
}

.more-btn {
  padding: 0 4px;
}
</style>
