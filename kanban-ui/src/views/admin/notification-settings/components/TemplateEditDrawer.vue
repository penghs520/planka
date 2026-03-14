<template>
  <a-drawer
    :visible="visible"
    :title="isEdit ? t('admin.notificationSettings.template.editTitle') : t('admin.notificationSettings.template.createTitle')"
    :width="600"
    :mask-closable="false"
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
      <!-- 模板名称 -->
      <a-form-item field="name" :label="t('admin.notificationSettings.template.form.name')" required>
        <a-input
          v-model="formData.name"
          :placeholder="t('admin.notificationSettings.template.form.namePlaceholder')"
        />
      </a-form-item>

      <!-- 触发事件 -->
      <a-form-item field="triggerEvent" :label="t('admin.notificationSettings.template.form.triggerEvent')" required>
        <a-select
          v-model="formData.triggerEvent"
          :placeholder="t('admin.notificationSettings.template.form.triggerEventPlaceholder')"
          :options="triggerEventOptions"
        />
      </a-form-item>

      <!-- 模板类型 -->
      <a-form-item field="templateType" :label="t('admin.notificationSettings.template.form.templateType')">
        <a-radio-group v-model="formData.templateType" type="button" @change="handleTemplateTypeChange">
          <a-radio value="CUSTOM">{{ t('admin.notificationSettings.template.templateType.custom') }}</a-radio>
          <a-radio value="BUILTIN">{{ t('admin.notificationSettings.template.templateType.builtin') }}</a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- 定义参数类型 - 自定义模板只能选卡片类型，内置模板可选所有类型 -->
      <a-form-item field="parameterType" :label="t('admin.notificationSettings.template.form.parameterType')" required>
        <a-radio-group v-model="formData.parameterType" type="button" @change="handleParameterTypeChange">
          <a-radio value="CARD_TYPE">{{ t('admin.notificationSettings.template.parameterType.cardType') }}</a-radio>
          <template v-if="formData.templateType === 'BUILTIN'">
            <a-radio value="DATE">{{ t('admin.notificationSettings.template.parameterType.date') }}</a-radio>
            <a-radio value="TEXT">{{ t('admin.notificationSettings.template.parameterType.text') }}</a-radio>
            <a-radio value="MULTILINE_TEXT">{{ t('admin.notificationSettings.template.parameterType.multilineText') }}</a-radio>
            <a-radio value="LINK">{{ t('admin.notificationSettings.template.parameterType.link') }}</a-radio>
            <a-radio value="NUMBER">{{ t('admin.notificationSettings.template.parameterType.number') }}</a-radio>
          </template>
        </a-radio-group>
      </a-form-item>

      <!-- 卡片类型选择（仅当参数类型为 CARD_TYPE 时显示） -->
      <a-form-item
        v-if="formData.parameterType === 'CARD_TYPE'"
        field="cardTypeId"
        :label="t('admin.notificationSettings.template.form.cardTypeSelect')"
        required
      >
        <a-select
          v-model="formData.cardTypeId"
          :placeholder="t('admin.notificationSettings.template.form.cardTypePlaceholder')"
          :options="cardTypeOptions"
          :loading="cardTypeLoading"
          @change="handleCardTypeChange"
        />
      </a-form-item>

      <!-- 参数名称（非卡片类型时显示） -->
      <a-form-item
        v-if="formData.parameterType !== 'CARD_TYPE'"
        field="parameterName"
        :label="t('admin.notificationSettings.template.form.parameterName')"
        required
      >
        <a-input
          v-model="formData.parameterName"
          :placeholder="t('admin.notificationSettings.template.form.parameterNamePlaceholder')"
        />
      </a-form-item>

      <!-- 通知对象类型 -->
      <a-form-item field="recipientType" :label="t('admin.notificationSettings.template.form.recipientType')" required>
        <a-radio-group v-model="formData.recipientType" type="button" @change="handleRecipientTypeChange">
          <a-radio value="MEMBER">{{ t('admin.notificationSettings.template.recipientType.member') }}</a-radio>
          <a-radio value="GROUP">{{ t('admin.notificationSettings.template.recipientType.group') }}</a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- 通知人 - 合并选择器 -->
      <a-form-item
        v-if="formData.recipientType === 'MEMBER'"
        field="recipientSelections"
        :label="t('admin.notificationSettings.template.form.recipients')"
        required
      >
        <a-select
          v-model="formData.recipientSelections"
          :placeholder="t('admin.notificationSettings.template.form.recipientsPlaceholder')"
          multiple
          :max-tag-count="3"
          allow-clear
        >
          <template v-for="option in recipientOptions" :key="option.value">
            <a-optgroup v-if="option.isGroup" :label="option.label" />
            <a-option v-else :value="option.value">{{ option.label }}</a-option>
          </template>
        </a-select>
      </a-form-item>

      <!-- 通知群 - 选择卡片类型的文本属性 -->
      <a-form-item
        v-if="formData.recipientType === 'GROUP'"
        field="groupFieldId"
        :label="t('admin.notificationSettings.template.form.groupField')"
        required
      >
        <a-select
          v-model="formData.groupFieldId"
          :placeholder="t('admin.notificationSettings.template.form.groupFieldPlaceholder')"
          :loading="fieldLoading"
          allow-search
        >
          <a-option v-for="field in textFieldOptions" :key="field.id" :value="field.id">
            {{ field.name }}
          </a-option>
        </a-select>
      </a-form-item>

      <!-- 通知渠道 -->
      <a-form-item field="channels" :label="t('admin.notificationSettings.template.form.channels')" required>
        <a-spin :loading="channelLoading">
          <a-checkbox-group v-model="formData.channels" direction="vertical">
            <a-checkbox v-for="channel in availableChannels" :key="channel.channelId" :value="channel.channelId">
              {{ getChannelTypeLabel(channel.channelId) }}
            </a-checkbox>
          </a-checkbox-group>
          <div v-if="availableChannels.length === 0 && !channelLoading" class="empty-channels">
            {{ t('admin.notificationSettings.template.form.noChannelsAvailable') }}
          </div>
        </a-spin>
      </a-form-item>

      <!-- 标题模板 -->
      <a-form-item field="titleTemplate" :label="t('admin.notificationSettings.template.form.titleTemplate')" required>
        <TextExpressionTemplateEditor
          :model-value="formData.titleTemplate"
          :field-provider="expressionFieldProvider"
          :placeholder="t('admin.notificationSettings.template.form.titleTemplatePlaceholder')"
          @update:model-value="(val: string) => formData.titleTemplate = val"
        />
        <template #help>
          <div class="field-variables">
            <span class="help-text">{{ t('admin.notificationSettings.template.form.titleTemplateHelp') }}</span>
          </div>
        </template>
      </a-form-item>

      <!-- 内容类型选择 - 仅邮件渠道可选长内容 -->
      <a-form-item field="contentType" :label="t('admin.notificationSettings.template.form.contentType')" required>
        <a-radio-group v-model="formData.contentType" type="button">
          <a-radio value="SHORT">{{ t('admin.notificationSettings.template.contentType.short') }}</a-radio>
          <a-radio value="LONG" :disabled="!hasEmailChannel">
            {{ t('admin.notificationSettings.template.contentType.long') }}
            <template v-if="!hasEmailChannel">
              <a-tooltip :content="t('admin.notificationSettings.template.contentType.longDisabledTip')">
                <icon-info-circle class="ml-1" />
              </a-tooltip>
            </template>
          </a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- 短内容模板 -->
      <a-form-item
        v-if="formData.contentType === 'SHORT'"
        field="shortContent"
        :label="t('admin.notificationSettings.template.form.shortContent')"
        required
      >
        <TextExpressionTemplateEditor
          :model-value="formData.shortContent"
          :field-provider="expressionFieldProvider"
          :placeholder="t('admin.notificationSettings.template.form.shortContentPlaceholder')"
          @update:model-value="(val: string) => formData.shortContent = val"
        />
        <template #help>
          <div class="field-variables">
            <span class="help-text">{{ t('admin.notificationSettings.template.form.contentHelp') }}</span>
          </div>
        </template>
      </a-form-item>

      <!-- 长内容模板 -->
      <a-form-item
        v-if="formData.contentType === 'LONG'"
        field="longContent"
        :label="t('admin.notificationSettings.template.form.longContent')"
        required
      >
        <TextExpressionTemplateEditor
          :model-value="formData.longContent"
          :field-provider="expressionFieldProvider"
          :placeholder="t('admin.notificationSettings.template.form.longContentPlaceholder')"
          :multiline="true"
          @update:model-value="(val: string) => formData.longContent = val"
        />
        <template #help>
          <div class="field-variables">
            <span class="help-text">{{ t('admin.notificationSettings.template.form.contentHelp') }}</span>
          </div>
        </template>
      </a-form-item>

      <!-- 启用状态 -->
      <a-form-item v-if="isEdit" field="enabled" :label="t('admin.table.status')">
        <a-switch v-model="formData.enabled" />
      </a-form-item>
    </a-form>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconInfoCircle } from '@arco-design/web-vue/es/icon'
import TextExpressionTemplateEditor from '@/components/common/text-expression-template/TextExpressionTemplateEditor.vue'
import type { FieldProvider } from '@/components/common/text-expression-template/types'
// FormInstance type from arco-design
interface FormInstance {
  validate: () => Promise<undefined | Error>
  resetFields: () => void
}
import {
  notificationTemplateApi,
  TRIGGER_EVENT_OPTIONS,
  type NotificationTemplateDefinition,
  type CreateNotificationTemplateRequest,
  type UpdateNotificationTemplateRequest,
  type SelectorType,
  type SelectorItem,
  type RecipientType,
  type RecipientSelector,
  type NotificationContentType,
  type TriggerEvent,
  type DefinitionParameterType,
  type DefinitionParameter,
  type TemplateType,
} from '@/api/notification-template'
import { notificationChannelApi, type NotificationChannelConfigDefinition } from '@/api/notification-channel'
import { cardTypeApi } from '@/api/card-type'
import { fieldOptionsApi } from '@/api/field-options'
import { useOrgStore } from '@/stores/org'
import { getMemberFieldsCached } from '@/views/schema-definition/card-type/components/permission/memberFieldsCache'
import type { FieldOption } from '@/types/field-option'

const props = defineProps<{
  visible: boolean
  template: NotificationTemplateDefinition | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const { t } = useI18n()
const orgStore = useOrgStore()

// 表单引用
const formRef = ref<FormInstance | null>(null)

// 加载状态
const cardTypeLoading = ref(false)
const fieldLoading = ref(false)
const channelLoading = ref(false)
const submitLoading = ref(false)

// 选项数据
const cardTypeOptions = ref<{ label: string; value: string }[]>([])
const memberFieldOptions = ref<any[]>([])
const availableFields = ref<FieldOption[]>([])
const memberCardTypeFields = ref<FieldOption[]>([])
const systemUserOptions = ref<{ id: string; name: string }[]>([])
const availableChannels = ref<NotificationChannelConfigDefinition[]>([])

// 待处理的 recipientSelector（用于在 loadMemberFields 完成后设置 recipientSelections）
const pendingRecipientSelector = ref<any>(null)

// 接收者字段值
const recipientFieldValue = ref<string[]>([])

// 表达式模板编辑器的字段提供者（带请求缓存）
let cardFieldsCache: Promise<FieldOption[]> | null = null
const linkFieldsCache = new Map<string, Promise<FieldOption[]>>()

const expressionFieldProvider: FieldProvider = {
  getCardFields: () => {
    if (!cardFieldsCache && formData.value.cardTypeId) {
      cardFieldsCache = fieldOptionsApi.getFields(formData.value.cardTypeId)
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

// 文本类型字段选项（用于通知群字段选择）
const textFieldOptions = computed(() => {
  const textTypes = ['SINGLE_LINE_TEXT', 'MULTI_LINE_TEXT', 'WEB_URL']
  return availableFields.value.filter((f) => textTypes.includes(f.fieldType))
})

// 是否是编辑模式
const isEdit = computed(() => !!props.template)

// 触发事件选项
const triggerEventOptions = computed(() =>
  TRIGGER_EVENT_OPTIONS.map((item) => ({
    label: t(item.label),
    value: item.value,
  }))
)

// 通知人选项（合并选择器）
const recipientOptions = computed(() => {
  const memberCardTypeId = orgStore.currentOrg?.memberCardTypeId
  const options: { label: string; value: string; isGroup?: boolean }[] = []

  // 1. 操作人选项组
  options.push({
    label: t('admin.notificationSettings.template.recipientOptions.operatorGroup'),
    value: '__operator_group__',
    isGroup: true,
  })
  options.push({
    label: t('admin.notificationSettings.template.recipientOptions.operator'),
    value: 'operator',
  })

  // 操作人的属性（成员卡片类型中 LINK 类型且关联到成员卡片类型的字段）
  memberCardTypeFields.value.forEach((f) => {
    options.push({
      label: t('admin.notificationSettings.template.recipientOptions.operatorField', { fieldName: f.name }),
      value: `operator:${f.id}`,
    })
  })

  // 2. 卡片字段选项组（仅当参数类型为 CARD_TYPE 时显示）
  if (formData.value.parameterType === 'CARD_TYPE' && availableFields.value.length > 0) {
    // 过滤出 LINK 类型且关联到成员卡片类型的字段
    const cardMemberFields = availableFields.value.filter(
      (f) => f.fieldType === 'LINK' && memberCardTypeId && f.targetCardTypeIds?.includes(memberCardTypeId)
    )

    if (cardMemberFields.length > 0) {
      options.push({
        label: t('admin.notificationSettings.template.recipientOptions.cardFieldGroup'),
        value: '__card_field_group__',
        isGroup: true,
      })
      cardMemberFields.forEach((f) => {
        options.push({
          label: f.name,
          value: `field:${f.id}`,
        })
      })
    }
  }

  return options
})

// 表单数据
const formData = ref({
  name: '',
  templateType: 'CUSTOM' as TemplateType,
  parameterType: 'CARD_TYPE' as DefinitionParameterType,
  cardTypeId: '',
  parameterName: '',
  triggerEvent: '' as string,
  recipientType: 'MEMBER' as RecipientType,
  recipientSelections: [] as string[],
  recipientSelector: {
    selectorType: 'FROM_FIELD' as SelectorType,
    memberIds: [] as string[],
    fieldId: '',
    fieldIds: [] as string[],
    includeSystemUsers: false,
    systemUserIds: [] as string[],
  },
  groupFieldId: '',
  channels: ['builtin'] as string[],
  titleTemplate: '',
  contentType: 'SHORT' as NotificationContentType,
  shortContent: '',
  longContent: '',
  enabled: true,
})

// 是否仅选择了邮件渠道（只有仅选择邮件时才能使用长内容）
const hasEmailChannel = computed(() => {
  const channels = formData.value.channels
  return channels.length === 1 && channels.includes('email')
})

// 监听渠道变化，如果不是仅邮件渠道且当前是长内容，自动切换为短内容
watch(
  () => formData.value.channels,
  (channels) => {
    const isOnlyEmail = channels.length === 1 && channels.includes('email')
    if (!isOnlyEmail && formData.value.contentType === 'LONG') {
      formData.value.contentType = 'SHORT'
    }
  }
)

// 表单校验规则
const formRules = {
  name: [{ required: true, message: t('admin.notificationSettings.template.form.nameRequired') }],
  parameterType: [{ required: true, message: t('admin.notificationSettings.template.form.parameterTypeRequired') }],
  cardTypeId: [{
    required: false,
    validator: (value: string, callback: (error?: string) => void) => {
      if (formData.value.parameterType === 'CARD_TYPE' && !value) {
        callback(t('admin.notificationSettings.template.form.cardTypeRequired'))
      } else {
        callback()
      }
    },
  }],
  parameterName: [{
    required: false,
    validator: (value: string, callback: (error?: string) => void) => {
      if (formData.value.parameterType !== 'CARD_TYPE' && !value) {
        callback(t('admin.notificationSettings.template.form.parameterNameRequired'))
      } else {
        callback()
      }
    },
  }],
  triggerEvent: [{ required: true, message: t('admin.notificationSettings.template.form.triggerEventRequired') }],
  'recipientSelector.selectorType': [{ required: true, message: t('admin.notificationSettings.template.form.selectorTypeRequired') }],
  'recipientSelector.fieldId': [{
    required: false,
    validator: (value: string, callback: (error?: string) => void) => {
      if (formData.value.recipientSelector.selectorType === 'FROM_FIELD' && !value) {
        callback(t('admin.notificationSettings.template.form.fieldIdRequired'))
      } else {
        callback()
      }
    },
  }],
  channels: [{ required: true, message: t('admin.notificationSettings.template.form.channelsRequired'), type: 'array' }],
  titleTemplate: [{ required: true, message: t('admin.notificationSettings.template.form.titleTemplateRequired') }],
}

// 加载卡片类型选项
const loadCardTypes = async () => {
  cardTypeLoading.value = true
  try {
    const data = await cardTypeApi.listOptions()
    cardTypeOptions.value = data.map((item) => ({
      label: item.name,
      value: item.id,
    }))
  } finally {
    cardTypeLoading.value = false
  }
}

// 加载可用渠道
const loadAvailableChannels = async () => {
  channelLoading.value = true
  try {
    availableChannels.value = await notificationChannelApi.list()
  } catch {
    Message.error(t('admin.notificationSettings.channel.loadFailed'))
  } finally {
    channelLoading.value = false
  }
}

// 渠道类型标签映射
const channelTypeLabels: Record<string, string> = {
  builtin: 'admin.notificationSettings.channel.type.builtin',
  email: 'admin.notificationSettings.channel.type.email',
  feishu: 'admin.notificationSettings.channel.type.feishu',
  dingtalk: 'admin.notificationSettings.channel.type.dingtalk',
  wecom: 'admin.notificationSettings.channel.type.wecom',
  zhiwei: 'admin.notificationSettings.channel.type.zhiwei',
  xiaowei: 'admin.notificationSettings.channel.type.xiaowei',
}

function getChannelTypeLabel(channelId: string): string {
  const key = channelTypeLabels[channelId]
  return key ? t(key) : channelId
}

// 加载成员卡片类型的字段（用于操作人属性选项）
const loadMemberCardTypeFields = async () => {
  const memberCardTypeId = orgStore.currentOrg?.memberCardTypeId
  if (!memberCardTypeId) return
  try {
    const fields = await fieldOptionsApi.getFields(memberCardTypeId)
    // 获取成员卡片类型中 LINK 类型且关联到成员卡片类型的字段（如上级、导师等）
    memberCardTypeFields.value = fields.filter(
      (f) => f.fieldType === 'LINK' && f.targetCardTypeIds?.includes(memberCardTypeId)
    )
    // 如果有待处理的 recipientSelector 且 availableFields 已加载，则应用
    if (pendingRecipientSelector.value && availableFields.value.length > 0) {
      applyPendingRecipientSelector()
    }
  } catch {
    // 忽略错误
  }
}

// 加载卡片类型的人员字段
const loadMemberFields = async (cardTypeId: string) => {
  console.log('[loadMemberFields] cardTypeId:', cardTypeId)
  if (!cardTypeId) return
  fieldLoading.value = true
  try {
    // 获取卡片类型的属性配置
    const fields: FieldOption[] = await cardTypeApi.getFieldOptions(cardTypeId)
    console.log('[loadMemberFields] fields loaded:', fields.length)
    availableFields.value = fields

    // 构建级联选项（用于通知群的字段选择）
    memberFieldOptions.value = [
      {
        label: t('admin.notificationSettings.template.form.cardFields'),
        value: 'card',
        children: fields.map((f) => ({ label: f.name || f.id, value: f.id })),
      },
      {
        label: t('admin.notificationSettings.template.form.systemUsers'),
        value: 'system',
        children: systemUserOptions.value.map((u) => ({ label: u.name, value: `system_${u.id}` })),
      },
    ]

    // 处理待处理的 recipientSelector（确保 memberCardTypeFields 也已加载）
    console.log('[loadMemberFields] pendingRecipientSelector:', pendingRecipientSelector.value)
    console.log('[loadMemberFields] memberCardTypeFields:', memberCardTypeFields.value)
    if (pendingRecipientSelector.value) {
      // 如果 memberCardTypeFields 还没加载，等待它加载
      if (memberCardTypeFields.value.length === 0 && orgStore.currentOrg?.memberCardTypeId) {
        await loadMemberCardTypeFields()
      }
      applyPendingRecipientSelector()
    }
  } finally {
    fieldLoading.value = false
  }
}

// 解析 recipientSelector 并设置 recipientSelections
const applyPendingRecipientSelector = () => {
  const selector = pendingRecipientSelector.value
  console.log('[applyPendingRecipientSelector] selector:', selector)
  if (!selector) return

  const selections: string[] = []
  // 优先使用多选择器模式
  if (selector.selectors && selector.selectors.length > 0) {
    selector.selectors.forEach((item: { selectorType: string; fieldId?: string; source?: string }) => {
      if (item.selectorType === 'CURRENT_OPERATOR') {
        selections.push('operator')
      } else if (item.selectorType === 'FROM_FIELD' && item.fieldId) {
        if (item.source === 'OPERATOR') {
          selections.push(`operator:${item.fieldId}`)
        } else {
          selections.push(`field:${item.fieldId}`)
        }
      }
    })
  } else {
    // 向后兼容：单选择器模式
    if (selector.selectorType === 'CURRENT_OPERATOR') {
      selections.push('operator')
    }
    const fieldIds = selector.fieldIds || []
    fieldIds.forEach((fieldId: string) => {
      selections.push(`field:${fieldId}`)
    })
  }

  console.log('[applyPendingRecipientSelector] selections:', selections)
  formData.value.recipientSelections = selections
  pendingRecipientSelector.value = null
}

// 处理卡片类型变化
const handleCardTypeChange = (value: string) => {
  formData.value.recipientSelector.fieldId = ''
  formData.value.recipientSelector.fieldIds = []
  recipientFieldValue.value = []
  // 清空字段缓存
  cardFieldsCache = null
  linkFieldsCache.clear()
  loadMemberFields(value)
}

// 处理参数类型变化
const handleParameterTypeChange = () => {
  // 切换类型时重置相关字段
  formData.value.cardTypeId = ''
  formData.value.parameterName = ''
  formData.value.recipientSelector.fieldIds = []
  formData.value.recipientSelector.fieldId = ''
  recipientFieldValue.value = []
  availableFields.value = []
  // 清空字段缓存
  cardFieldsCache = null
  linkFieldsCache.clear()
}

// 处理模板类型变化
const handleTemplateTypeChange = () => {
  // 切换到自定义模板时，参数类型只能是卡片类型
  if (formData.value.templateType === 'CUSTOM' && formData.value.parameterType !== 'CARD_TYPE') {
    formData.value.parameterType = 'CARD_TYPE'
    handleParameterTypeChange()
  }
}

// 处理通知对象类型变化
const handleRecipientTypeChange = () => {
  // 切换类型时重置选择器
  formData.value.recipientSelector.fieldIds = []
  formData.value.recipientSelector.fieldId = ''
  formData.value.recipientSelector.memberIds = []
  recipientFieldValue.value = []
}

// 重置表单
const resetForm = () => {
  formData.value = {
    name: '',
    templateType: 'CUSTOM',
    parameterType: 'CARD_TYPE',
    cardTypeId: '',
    parameterName: '',
    triggerEvent: '',
    recipientType: 'MEMBER',
    recipientSelections: [],
    recipientSelector: {
      selectorType: 'FROM_FIELD',
      memberIds: [],
      fieldId: '',
      fieldIds: [],
      includeSystemUsers: false,
      systemUserIds: [],
    },
    groupFieldId: '',
    channels: ['builtin'],
    titleTemplate: '',
    contentType: 'SHORT',
    shortContent: '',
    longContent: '',
    enabled: true,
  }
  recipientFieldValue.value = []
}

// 处理取消
const handleCancel = () => {
  emit('update:visible', false)
  resetForm()
}

// 处理提交
const handleSubmit = async () => {
  const valid = await formRef.value?.validate()
  if (valid) return

  submitLoading.value = true
  try {
    // 构建 recipientSelector
    let recipientSelector: RecipientSelector
    if (formData.value.recipientType === 'MEMBER') {
      // 解析 recipientSelections 构建 selectors 列表
      const selections = formData.value.recipientSelections
      const selectors: SelectorItem[] = []

      selections.forEach((s) => {
        if (s === 'operator') {
          selectors.push({ selectorType: 'CURRENT_OPERATOR' as SelectorType })
        } else if (s.startsWith('operator:')) {
          // 操作人的属性
          selectors.push({
            selectorType: 'FROM_FIELD' as SelectorType,
            fieldId: s.replace('operator:', ''),
            source: 'OPERATOR',
          })
        } else if (s.startsWith('field:')) {
          // 卡片字段
          selectors.push({
            selectorType: 'FROM_FIELD' as SelectorType,
            fieldId: s.replace('field:', ''),
          })
        }
      })

      recipientSelector = {
        selectors: selectors.length > 0 ? selectors : undefined,
      }
    } else {
      // GROUP 类型：使用 groupFieldId
      recipientSelector = {
        selectorType: 'FROM_FIELD' as SelectorType,
        fieldId: formData.value.groupFieldId,
      }
    }

    // 构建 content 对象
    const content = formData.value.contentType === 'SHORT'
      ? {
          type: 'SHORT' as NotificationContentType,
          textTemplate: formData.value.shortContent,
        }
      : {
          type: 'LONG' as NotificationContentType,
          richTextTemplate: formData.value.longContent,
        }

    // 构建 definitionParameter
    const definitionParameter: DefinitionParameter = formData.value.parameterType === 'CARD_TYPE'
      ? {
          type: 'CARD_TYPE',
          cardTypeId: formData.value.cardTypeId,
        }
      : {
          type: formData.value.parameterType,
          name: formData.value.parameterName,
        }

    if (isEdit.value && props.template) {
      const updateData: UpdateNotificationTemplateRequest = {
        name: formData.value.name,
        templateType: formData.value.templateType,
        definitionParameter,
        triggerEvent: formData.value.triggerEvent as TriggerEvent,
        recipientType: formData.value.recipientType,
        recipientSelector,
        channels: formData.value.channels,
        titleTemplate: formData.value.titleTemplate,
        content,
        enabled: formData.value.enabled,
        expectedVersion: props.template.contentVersion,
      }
      await notificationTemplateApi.update(props.template.id!, updateData)
      Message.success(t('admin.message.saveSuccess'))
    } else {
      const createData: CreateNotificationTemplateRequest = {
        name: formData.value.name,
        templateType: formData.value.templateType,
        definitionParameter,
        triggerEvent: formData.value.triggerEvent as TriggerEvent,
        recipientType: formData.value.recipientType,
        recipientSelector,
        channels: formData.value.channels,
        titleTemplate: formData.value.titleTemplate,
        content,
      }
      await notificationTemplateApi.create(createData)
      Message.success(t('admin.message.createSuccess'))
    }
    emit('success')
    emit('update:visible', false)
    resetForm()
  } catch {
    Message.error(isEdit.value ? t('admin.notificationSettings.template.saveFailed') : t('admin.notificationSettings.template.createFailed'))
  } finally {
    submitLoading.value = false
  }
}

// 监听编辑数据变化
watch(
  () => props.template,
  (newVal) => {
    if (newVal) {
      // 从 content 字段提取 contentType, shortContent 和 longContent
      let contentType: NotificationContentType = 'SHORT'
      let shortContent = ''
      let longContent = ''
      if (newVal.content) {
        contentType = newVal.content.type
        if (newVal.content.type === 'SHORT') {
          shortContent = newVal.content.textTemplate || ''
        } else if (newVal.content.type === 'LONG') {
          longContent = newVal.content.richTextTemplate || ''
        }
      }

      // 从 definitionParameter 提取参数类型和值
      let parameterType: DefinitionParameterType = 'CARD_TYPE'
      let cardTypeId = ''
      let parameterName = ''
      if (newVal.definitionParameter) {
        parameterType = newVal.definitionParameter.type
        if (newVal.definitionParameter.type === 'CARD_TYPE') {
          cardTypeId = newVal.definitionParameter.cardTypeId || ''
        } else {
          parameterName = newVal.definitionParameter.name || ''
        }
      } else if (newVal.cardTypeId) {
        // 兼容旧数据
        parameterType = 'CARD_TYPE'
        cardTypeId = newVal.cardTypeId
      }

      // 保存 recipientSelector 用于后续处理（等 loadMemberFields 完成后再设置 recipientSelections）
      console.log('[watch template] newVal.recipientSelector:', newVal.recipientSelector)
      console.log('[watch template] newVal.recipientType:', newVal.recipientType)
      // recipientType 为 null 或 'MEMBER' 时都处理
      const recipientType = newVal.recipientType || 'MEMBER'
      if (recipientType === 'MEMBER' && newVal.recipientSelector) {
        pendingRecipientSelector.value = newVal.recipientSelector
        console.log('[watch template] set pendingRecipientSelector:', pendingRecipientSelector.value)
      }

      // GROUP 类型时，从 recipientSelector.fieldId 读取 groupFieldId
      const groupFieldId = recipientType === 'GROUP' && newVal.recipientSelector?.fieldId
        ? newVal.recipientSelector.fieldId
        : ''
      console.log('[watch template] recipientType:', recipientType, 'groupFieldId:', groupFieldId)

      formData.value = {
        name: newVal.name,
        templateType: newVal.templateType || 'CUSTOM',
        parameterType,
        cardTypeId,
        parameterName,
        triggerEvent: newVal.triggerEvent,
        recipientType: newVal.recipientType || 'MEMBER',
        recipientSelections: [],
        recipientSelector: {
          selectorType: newVal.recipientSelector?.selectorType || 'FROM_FIELD',
          memberIds: newVal.recipientSelector?.memberIds || [],
          fieldId: newVal.recipientSelector?.fieldId || '',
          fieldIds: newVal.recipientSelector?.fieldIds || [],
          includeSystemUsers: newVal.recipientSelector?.includeSystemUsers || false,
          systemUserIds: newVal.recipientSelector?.systemUserIds || [],
        },
        groupFieldId,
        channels: newVal.channels || ['builtin'],
        titleTemplate: newVal.titleTemplate || '',
        contentType,
        shortContent,
        longContent,
        enabled: newVal.enabled,
      }
      if (parameterType === 'CARD_TYPE' && cardTypeId) {
        loadMemberFields(cardTypeId)
      } else if (pendingRecipientSelector.value) {
        // 非卡片类型参数，直接应用 recipientSelector
        applyPendingRecipientSelector()
      }
    } else {
      resetForm()
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadCardTypes()
  loadMemberCardTypeFields()
  loadAvailableChannels()
})
</script>

<style scoped lang="scss">
.field-variables {
  margin-top: 8px;

  .help-text {
    display: block;
    margin-bottom: 8px;
    color: var(--color-text-3);
    font-size: 12px;
  }

  .field-tag {
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      color: rgb(var(--primary-6));
      border-color: rgb(var(--primary-6));
    }
  }
}

.empty-channels {
  color: var(--color-text-3);
  font-size: 14px;
  padding: 8px 0;
}
</style>
