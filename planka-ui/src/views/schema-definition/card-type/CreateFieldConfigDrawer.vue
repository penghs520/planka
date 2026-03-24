<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import { cardTypeApi, linkTypeApi } from '@/api'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import SelectFieldTypeModal from './components/SelectFieldTypeModal.vue'
import LinkFieldQuickCreateSection from './components/LinkFieldQuickCreateSection.vue'
import type { FieldTypeModalConfirmPayload } from './components/field-type-modal-payload'
import {
  defaultLinkFieldQuickCreateState,
  type LinkFieldQuickCreateState,
} from './components/link-field-quick-types'
import { SchemaSubType } from '@/types/schema'
import { DateFormat } from '@/types/field'
import { getFieldTypeLabelI18n, supportsValueSource } from './formatters'
import type { ValueSourceType } from '@/types/card-type'
import type { EnumOptionDTO } from '@/types/view-data'
import type { LevelBinding } from './composables/useFieldConfigForm'
import {
  ValueSourceConfig,
  CommonFieldConfig,
  TextFieldConfig,
  NumberFieldConfig,
  DateFieldConfig,
  EnumFieldConfig,
  AttachmentFieldConfig,
  WebUrlFieldConfig,
  CascadeFieldConfig,
} from './components/field-config'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  cardTypeId: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  created: []
}>()

const drawerVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const saving = ref(false)
const formRef = ref<FormInstance>()
const selectedType = ref<SchemaSubType | null>(null)
const step = ref<1 | 2>(1)
const linkQuick = ref<LinkFieldQuickCreateState>(defaultLinkFieldQuickCreateState(false))
const linkQuickSectionRef = ref<InstanceType<typeof LinkFieldQuickCreateSection> | null>(null)

// 表单验证规则
const formRules = {
  name: [{ required: true, message: t('admin.cardType.fieldConfig.nameRequired') }],
}

// 表单数据
const formData = ref<{
  name: string
  code: string
  required: boolean
  readOnly: boolean
  valueSource: ValueSourceType
  referenceFieldId?: string
  // 文本类型
  maxLength?: number
  defaultValue?: string | number
  // 数字类型
  minValue?: number
  maxValue?: number
  precision: number
  unit?: string
  displayFormat: 'NORMAL' | 'PERCENT' | 'THOUSAND_SEPARATOR'
  percentStyle: 'NUMBER' | 'PROGRESS_BAR'
  // 日期类型
  dateFormat: DateFormat
  useNowAsDefault: boolean
  // 枚举类型
  multiSelect: boolean
  enumOptions: EnumOptionDTO[]
  // 附件类型
  maxFileSize?: number
  maxFileCount: number
  // 网页链接类型
  validateUrl: boolean
  showPreview: boolean
  defaultUrl: string
  defaultLinkText: string
  // 级联层级类型
  cascadeRelationId: string
  levelBindings: LevelBinding[]
}>({
  name: '',
  code: '',
  required: false,
  readOnly: false,
  valueSource: 'MANUAL',
  referenceFieldId: undefined,
  maxLength: undefined,
  defaultValue: '',
  minValue: undefined,
  maxValue: undefined,
  precision: 0,
  unit: '',
  displayFormat: 'NORMAL',
  percentStyle: 'NUMBER',
  dateFormat: DateFormat.DATE,
  useNowAsDefault: false,
  multiSelect: false,
  enumOptions: [],
  maxFileSize: undefined,
  maxFileCount: 10,
  validateUrl: true,
  showPreview: false,
  defaultUrl: '',
  defaultLinkText: '',
  cascadeRelationId: '',
  levelBindings: [],
})

function getFieldTypeLabel(subType: SchemaSubType): string {
  return getFieldTypeLabelI18n(subType, t)
}

// 引用属性选项
const referenceFieldOptions = computed(() => {
  return [] as Array<{ id: string; name: string }>
})

/** 处理值来源变更 */
function handleValueSourceChange(value: ValueSourceType): void {
  formData.value.valueSource = value
  if (value !== 'MANUAL') {
    formData.value.required = false
    formData.value.readOnly = true
  }
}

/** 动态生成抽屉标题 */
const drawerTitle = computed(() => {
  if (selectedType.value === SchemaSubType.LINK_FIELD) {
    const typeLabel = linkQuick.value.targetMultiSelect
      ? t('admin.fieldType.LINK_MULTI')
      : t('admin.fieldType.LINK_SINGLE')
    return t('admin.cardType.fieldConfig.createNewWithType', { type: typeLabel })
  }
  if (selectedType.value === SchemaSubType.ENUM_FIELD) {
    const typeLabel = formData.value.multiSelect
      ? t('admin.fieldType.ENUM_MULTI')
      : t('admin.fieldType.ENUM_SINGLE')
    return t('admin.cardType.fieldConfig.createNewWithType', { type: typeLabel })
  }
  if (selectedType.value) {
    return t('admin.cardType.fieldConfig.createNewWithType', { type: getFieldTypeLabel(selectedType.value) })
  }
  return t('admin.cardType.fieldConfig.createNew')
})

/** 判断是否为文本类型 */
const isTextType = computed(() => {
  return selectedType.value && [
    SchemaSubType.TEXT_FIELD,
    SchemaSubType.MULTI_LINE_TEXT_FIELD,
    SchemaSubType.MARKDOWN_FIELD,
  ].includes(selectedType.value)
})

/** 值来源是否为手动输入 */
const valueSourceIsManual = computed(() => formData.value.valueSource === 'MANUAL')

interface ApplyTypeOptions {
  linkTargetMulti?: boolean
  enumMultiSelect?: boolean
}

/** 选中类型后写入表单默认值（不含步骤切换） */
function applyTypeDefaults(type: SchemaSubType, opts?: ApplyTypeOptions): void {
  selectedType.value = type
  switch (type) {
    case SchemaSubType.NUMBER_FIELD:
      formData.value.precision = 0
      formData.value.displayFormat = 'NORMAL'
      formData.value.percentStyle = 'NUMBER'
      break
    case SchemaSubType.DATE_FIELD:
      formData.value.dateFormat = DateFormat.DATE
      formData.value.useNowAsDefault = false
      break
    case SchemaSubType.ENUM_FIELD:
      formData.value.multiSelect = opts?.enumMultiSelect ?? false
      formData.value.enumOptions = []
      break
    case SchemaSubType.ATTACHMENT_FIELD:
      formData.value.maxFileCount = 10
      break
    case SchemaSubType.WEB_URL_FIELD:
      formData.value.validateUrl = true
      formData.value.showPreview = false
      break
    case SchemaSubType.CASCADE_FIELD:
      formData.value.cascadeRelationId = ''
      formData.value.levelBindings = []
      break
    case SchemaSubType.LINK_FIELD:
      linkQuick.value = defaultLinkFieldQuickCreateState(opts?.linkTargetMulti ?? false)
      break
    default:
      break
  }
}

function handleFieldTypePicked(payload: FieldTypeModalConfirmPayload): void {
  applyTypeDefaults(payload.schemaSubType, {
    linkTargetMulti: payload.linkTargetMulti,
    enumMultiSelect: payload.enumMultiSelect,
  })
  step.value = 2
}

function resetForm(): void {
  step.value = 1
  selectedType.value = null
  formData.value = {
    name: '',
    code: '',
    required: false,
    readOnly: false,
    valueSource: 'MANUAL',
    referenceFieldId: undefined,
    maxLength: undefined,
    defaultValue: '',
    minValue: undefined,
    maxValue: undefined,
    precision: 0,
    unit: '',
    displayFormat: 'NORMAL',
    percentStyle: 'NUMBER',
    dateFormat: DateFormat.DATE,
    useNowAsDefault: false,
    multiSelect: false,
    enumOptions: [],
    maxFileSize: undefined,
    maxFileCount: 10,
    validateUrl: true,
    showPreview: false,
    defaultUrl: '',
    defaultLinkText: '',
    cascadeRelationId: '',
    levelBindings: [],
  }
  linkQuick.value = defaultLinkFieldQuickCreateState(false)
}

function handleCancel(): void {
  drawerVisible.value = false
  resetForm()
}

async function handleSave(): Promise<void> {
  const errors = await formRef.value?.validate()
  if (errors) return

  if (selectedType.value === SchemaSubType.LINK_FIELD) {
    if (!linkQuickSectionRef.value?.validate()) return
  }

  saving.value = true
  try {
    if (selectedType.value === SchemaSubType.LINK_FIELD) {
      const q = linkQuick.value
      const sourceName = formData.value.name.trim()
      const sourceCode = formData.value.code?.trim() || undefined
      const relationName = `${sourceName}-${q.targetName.trim()}`

      const peerId = q.peerCardTypeIds[0]
      if (!peerId) {
        Message.error(t('admin.cardType.fieldConfig.createFailed'))
        return
      }

      const linkType = await linkTypeApi.create({
        name: relationName,
        description: q.description || undefined,
        sourceName,
        targetName: q.targetName.trim(),
        sourceCode,
        targetCode: q.targetCode || undefined,
        sourceCardTypeId: props.cardTypeId,
        targetCardTypeId: peerId,
        sourceMultiSelect: false,
        targetMultiSelect: q.targetMultiSelect,
      })

      if (!linkType.id) {
        Message.error(t('admin.cardType.fieldConfig.createFailed'))
        return
      }

      const position = 'SOURCE'
      const fieldConfig: Record<string, unknown> = {
        schemaSubType: SchemaSubType.LINK_FIELD,
        name: formData.value.name,
        code: formData.value.code,
        fieldId: `${linkType.id}:${position}`,
        required: formData.value.required,
        readOnly: formData.value.readOnly,
        cardTypeId: props.cardTypeId,
        systemField: false,
      }

      try {
        await cardTypeApi.saveFieldConfig(
          props.cardTypeId,
          fieldConfig as unknown as Parameters<typeof cardTypeApi.saveFieldConfig>[1]
        )
        Message.success(t('admin.cardType.fieldConfig.createSuccess'))
        emit('created')
        drawerVisible.value = false
        resetForm()
      } catch (saveErr) {
        console.error('Failed to save link field config after link type created:', saveErr)
        Message.warning(t('admin.cardType.fieldConfig.linkFieldQuickCreate.saveFieldPartialFail'))
      }
      return
    }

    const fieldConfig: Record<string, unknown> = {
      schemaSubType: selectedType.value,
      name: formData.value.name,
      code: formData.value.code,
      required: formData.value.required,
      readOnly: formData.value.readOnly,
      cardTypeId: props.cardTypeId,
      systemField: false,
    }

    // 添加值来源配置
    if (selectedType.value && supportsValueSource(selectedType.value)) {
      fieldConfig.valueSource = formData.value.valueSource || 'MANUAL'
      if (formData.value.valueSource === 'REFERENCE' && formData.value.referenceFieldId) {
        fieldConfig.referenceFieldId = formData.value.referenceFieldId
      }
    }

    // 根据类型添加特有配置
    switch (selectedType.value) {
      case SchemaSubType.TEXT_FIELD:
      case SchemaSubType.MULTI_LINE_TEXT_FIELD:
      case SchemaSubType.MARKDOWN_FIELD:
        Object.assign(fieldConfig, {
          maxLength: formData.value.maxLength,
          defaultValue: formData.value.defaultValue,
        })
        break
      case SchemaSubType.NUMBER_FIELD:
        Object.assign(fieldConfig, {
          minValue: formData.value.minValue,
          maxValue: formData.value.maxValue,
          precision: formData.value.precision,
          unit: formData.value.displayFormat === 'PERCENT' ? undefined : formData.value.unit,
          displayFormat: formData.value.displayFormat,
          percentStyle: formData.value.percentStyle,
          showThousandSeparator: formData.value.displayFormat === 'THOUSAND_SEPARATOR',
          defaultValue: formData.value.defaultValue ? parseFloat(String(formData.value.defaultValue)) : undefined,
        })
        break
      case SchemaSubType.DATE_FIELD:
        Object.assign(fieldConfig, {
          dateFormat: formData.value.dateFormat,
          useNowAsDefault: formData.value.useNowAsDefault,
        })
        break
      case SchemaSubType.ENUM_FIELD:
        Object.assign(fieldConfig, {
          multiSelect: formData.value.multiSelect,
          options: formData.value.enumOptions?.map((opt) => ({
            id: opt.id,
            value: opt.value,
            label: opt.label,
            order: opt.order,
            enabled: opt.enabled,
            color: opt.color,
          })) || [],
        })
        break
      case SchemaSubType.ATTACHMENT_FIELD:
        Object.assign(fieldConfig, {
          maxFileSize: formData.value.maxFileSize,
          maxFileCount: formData.value.maxFileCount,
        })
        break
      case SchemaSubType.WEB_URL_FIELD:
        Object.assign(fieldConfig, {
          validateUrl: formData.value.validateUrl,
          showPreview: formData.value.showPreview,
          defaultUrl: formData.value.defaultUrl,
          defaultLinkText: formData.value.defaultLinkText,
        })
        break
      case SchemaSubType.CASCADE_FIELD:
        Object.assign(fieldConfig, {
          cascadeRelationId: formData.value.cascadeRelationId,
          levelBindings: formData.value.levelBindings,
        })
        break
    }

    await cardTypeApi.saveFieldConfig(props.cardTypeId, fieldConfig as unknown as Parameters<typeof cardTypeApi.saveFieldConfig>[1])
    Message.success(t('admin.cardType.fieldConfig.createSuccess'))
    emit('created')
    drawerVisible.value = false
    resetForm()
  } catch (error) {
    console.error('Failed to create field config:', error)
    // 错误提示已由 request 拦截器显示，这里不再重复显示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <SelectFieldTypeModal
    :visible="drawerVisible && step === 1"
    @cancel="handleCancel"
    @confirm="handleFieldTypePicked"
  />

  <!-- 步骤2：配置表单抽屉 -->
  <a-drawer
    :visible="drawerVisible && step === 2"
    :title="drawerTitle"
    :width="880"
    :mask-closable="true"
    :esc-to-close="true"
    unmount-on-close
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formData"
      layout="vertical"
      :rules="formRules"
      class="field-config-form"
    >
      <!-- 基础信息 -->
      <div class="form-section">
        <div class="basic-info-grid">
          <a-form-item
            field="name"
            :label="t('admin.cardType.fieldConfig.fieldName')"
            :validate-trigger="['change', 'blur']"
            class="form-field"
          >
            <a-input
              v-model="formData.name"
              :placeholder="t('admin.cardType.fieldConfig.enterFieldName')"
              :max-length="50"
            />
          </a-form-item>
          <a-form-item :label="t('admin.cardType.fieldConfig.fieldCode')" class="form-field">
            <a-input
              v-model="formData.code"
              :max-length="50"
            />
            <template #extra>
              <span class="field-extra">{{ t('admin.cardType.fieldConfig.codeHint') || '用于API调用，留空自动生成' }}</span>
            </template>
          </a-form-item>
        </div>
      </div>

      <!-- 值来源配置 -->
      <ValueSourceConfig
        v-if="selectedType && supportsValueSource(selectedType)"
        :model-value="formData.valueSource"
        :schema-sub-type="selectedType"
        :reference-field-id="formData.referenceFieldId"
        :reference-field-options="referenceFieldOptions"
        @update:model-value="handleValueSourceChange"
        @update:reference-field-id="(val) => formData.referenceFieldId = val"
      />

      <!-- 通用配置 -->
      <CommonFieldConfig
        v-model:required="formData.required"
        v-model:read-only="formData.readOnly"
        :value-source-is-manual="valueSourceIsManual"
      />

      <!-- 文本类型配置 -->
      <TextFieldConfig
        v-if="isTextType"
        v-model:max-length="formData.maxLength"
        v-model:default-value="formData.defaultValue as string"
      />

      <!-- 数字类型配置 -->
      <NumberFieldConfig
        v-if="selectedType === SchemaSubType.NUMBER_FIELD"
        v-model:default-value="formData.defaultValue as number"
        v-model:min-value="formData.minValue"
        v-model:max-value="formData.maxValue"
        v-model:precision="formData.precision"
        v-model:unit="formData.unit"
        v-model:display-format="formData.displayFormat"
        v-model:percent-style="formData.percentStyle"
      />

      <!-- 日期类型配置 -->
      <DateFieldConfig
        v-if="selectedType === SchemaSubType.DATE_FIELD"
        v-model:date-format="formData.dateFormat"
        v-model:use-now-as-default="formData.useNowAsDefault"
      />

      <!-- 枚举类型配置 -->
      <EnumFieldConfig
        v-if="selectedType === SchemaSubType.ENUM_FIELD"
        v-model:multi-select="formData.multiSelect"
        v-model:options="formData.enumOptions"
        lock-multi-select
      />

      <!-- 附件类型配置 -->
      <AttachmentFieldConfig
        v-if="selectedType === SchemaSubType.ATTACHMENT_FIELD"
        v-model:max-file-size="formData.maxFileSize"
        v-model:max-file-count="formData.maxFileCount"
      />

      <!-- 网页链接类型配置 -->
      <WebUrlFieldConfig
        v-if="selectedType === SchemaSubType.WEB_URL_FIELD"
        v-model:validate-url="formData.validateUrl"
        v-model:show-preview="formData.showPreview"
        v-model:default-url="formData.defaultUrl"
        v-model:default-link-text="formData.defaultLinkText"
      />

      <!-- 级联层级类型配置 -->
      <CascadeFieldConfig
        v-if="selectedType === SchemaSubType.CASCADE_FIELD"
        v-model:cascade-relation-id="formData.cascadeRelationId"
        v-model:level-bindings="formData.levelBindings"
        :card-type-id="props.cardTypeId"
        :field-name="formData.name"
      />

      <LinkFieldQuickCreateSection
        v-if="selectedType === SchemaSubType.LINK_FIELD"
        ref="linkQuickSectionRef"
        v-model="linkQuick"
        :card-type-id="props.cardTypeId"
      />
    </a-form>

    <template #footer>
      <a-space>
        <CancelButton @click="handleCancel" />
        <SaveButton
          :loading="saving"
          :text="t('admin.action.create')"
          @click="handleSave"
        />
      </a-space>
    </template>
  </a-drawer>
</template>

<style scoped>
/* 表单配置样式 */
.field-config-form {
  padding: 4px 0;
}

.form-section {
  margin-bottom: 4px;
}

.basic-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.basic-info-grid :deep(.arco-form-item) {
  max-width: 320px;
}

.form-field {
  margin-bottom: 0;
}

.field-extra {
  font-size: 12px;
  color: var(--color-text-3);
  margin-top: 4px;
}

/* 表单项标签样式 */
:deep(.arco-form-item-label-col > label) {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
}

/* 输入框聚焦样式增强 */
:deep(.arco-input-wrapper:focus-within),
:deep(.arco-select-view:focus-within) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(51, 112, 255, 0.1);
}
</style>
