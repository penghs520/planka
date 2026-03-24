<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import { cardTypeApi, cascadeRelationApi, linkTypeApi } from '@/api'
import { fieldOptionsApi } from '@/api/field-options'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import type { FieldConfig, FieldConfigListWithSource } from '@/types/card-type'
import type { LevelBinding } from './composables/useFieldConfigForm'
import type { FieldOption } from '@/types/field-option'
import type { LinkTypeVO } from '@/types/link-type'
import {
  getFieldTypeLabelI18n,
  supportsValueSource,
  getSourceType,
} from './formatters'
import { isSystemField } from '@/types/field-config'
import {
  ValueSourceConfig,
  CommonFieldConfig,
  ValidationRuleConfig,
  FieldHintConfig,
  TextFieldConfig,
  NumberFieldConfig,
  DateFieldConfig,
  EnumFieldConfig as EnumFieldConfigComponent,
  AttachmentFieldConfig,
  WebUrlFieldConfig,
  CascadeFieldConfig,
} from './components/field-config'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  fieldConfig: FieldConfig | null
  cardTypeId: string
  fieldList: FieldConfigListWithSource | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  save: []
}>()

const saving = ref(false)
const currentFieldConfig = ref<FieldConfig | null>(null)
const formRef = ref<FormInstance>()

// 级联属性类型只读展示
const cascadeRelationDisplayName = ref<string>('')
const linkTypeNames = ref<Record<string, { sourceName: string; targetName: string }>>({})

// 校验规则配置所需数据
const availableFields = ref<FieldOption[]>([])
const linkTypes = ref<LinkTypeVO[]>([])

const drawerVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

/** 初始化数据 */
async function initData(): Promise<void> {
  if (props.fieldConfig) {
    currentFieldConfig.value = { ...props.fieldConfig }

    // 默认选中手动输入
    if ('valueSource' in currentFieldConfig.value && !currentFieldConfig.value.valueSource) {
      (currentFieldConfig.value as Record<string, unknown>).valueSource = 'MANUAL'
    }

    // 数字类型兼容处理
    if (currentFieldConfig.value.schemaSubType === 'NUMBER_FIELD') {
      const config = currentFieldConfig.value as Record<string, unknown>
      if (!config.displayFormat) {
        config.displayFormat = config.showThousandSeparator ? 'THOUSAND_SEPARATOR' : 'NORMAL'
      }
      if (!config.percentStyle) {
        config.percentStyle = 'NUMBER'
      }
    }

    // 如果是级联属性类型，加载级联关系定义与关联类型详情
    if (props.fieldConfig.schemaSubType === 'CASCADE_FIELD') {
      await loadCascadeRelationDetails()
    }

    // 加载校验规则配置所需数据
    if (props.cardTypeId) {
      await loadValidationConfigData()
    }
  }
}

/** 加载级联关系定义与层级详情 */
async function loadCascadeRelationDetails(): Promise<void> {
  const cascadeRelationId = (currentFieldConfig.value as Record<string, unknown>)?.cascadeRelationId as
    | string
    | undefined
  if (!cascadeRelationId) return

  try {
    const def = await cascadeRelationApi.getById(cascadeRelationId)
    cascadeRelationDisplayName.value = def.name

    // 加载层级绑定的关联类型名称
    const levelBindings = (currentFieldConfig.value as Record<string, unknown>)?.levelBindings as LevelBinding[] || []
    for (const binding of levelBindings) {
      if (binding.linkFieldId) {
        const linkTypeId = binding.linkFieldId.split(':')[0]
        if (linkTypeId && !linkTypeNames.value[linkTypeId]) {
          try {
            const linkType = await linkTypeApi.getById(linkTypeId)
            linkTypeNames.value[linkTypeId] = {
              sourceName: linkType.sourceName,
              targetName: linkType.targetName,
            }
          } catch {
            console.error('Failed to load link type:', linkTypeId)
          }
        }
      }
    }
  } catch (error) {
    console.error('Failed to load cascade relation definition:', error)
  }
}

/** 加载校验规则配置所需数据 */
async function loadValidationConfigData(): Promise<void> {
  if (!props.cardTypeId) return

  try {
    // 并行加载属性选项和关联类型
    const [fields, links] = await Promise.all([
      fieldOptionsApi.getFields(props.cardTypeId),
      linkTypeApi.list(),
    ])
    availableFields.value = fields
    linkTypes.value = links
  } catch (error) {
    console.error('Failed to load validation config data:', error)
  }
}

/** 抽屉标题 */
const drawerTitle = computed(() => {
  if (!currentFieldConfig.value) return t('admin.cardType.fieldConfig.drawerTitle')
  const typeLabel = getFieldTypeLabelI18n(currentFieldConfig.value.schemaSubType, t)
  return `${typeLabel}${t('admin.cardType.fieldConfig.drawerTitleWithType', { name: currentFieldConfig.value.name })}`
})

// 引用属性选项
const referenceFieldOptions = computed(() => {
  return [] as Array<{ id: string; name: string }>
})

/** 当前属性配置是否为自有配置（可编辑） */
const isOwnConfig = computed(() => {
  if (!currentFieldConfig.value || !props.fieldList) return false
  const sourceType = getSourceType(currentFieldConfig.value, props.fieldList)
  // 自有配置或自有定义
  if (sourceType === 'own-config' || sourceType === 'own-definition') return true
  // 对于关联类型定义，需要检查是否是继承的
  if (sourceType === 'link-type-definition') {
    const key = currentFieldConfig.value.fieldId
    const source = props.fieldList.fieldSources[key]
    return source && !source.definitionInherited
  }
  return false
})

/** 是否为系统内置属性 */
const isSystemFieldConfig = computed(() => {
  if (!currentFieldConfig.value) return false
  return isSystemField(currentFieldConfig.value)
})

// 表单验证规则
const formRules = computed(() => ({
  name: isOwnConfig.value ? [{ required: true, message: t('admin.cardType.fieldConfig.nameRequired') }] : [],
}))

/** 是否为原始配置 */
const isOriginal = computed(() => {
  return (currentFieldConfig.value as Record<string, unknown>)?.original === true
})

/** 值来源是否为手动输入 */
const valueSourceIsManual = computed(() => {
  if (!currentFieldConfig.value) return true
  const valueSource = (currentFieldConfig.value as Record<string, unknown>).valueSource
  return valueSource === undefined || valueSource === 'MANUAL'
})

/** 判断是否为文本类型 */
const isTextType = computed(() => {
  return currentFieldConfig.value && ['TEXT_FIELD', 'MULTI_LINE_TEXT_FIELD', 'MARKDOWN_FIELD'].includes(currentFieldConfig.value.schemaSubType)
})

/** 处理值来源变更 */
function handleValueSourceChange(value: string): void {
  if (!currentFieldConfig.value) return
  (currentFieldConfig.value as Record<string, unknown>).valueSource = value
  if (value !== 'MANUAL') {
    currentFieldConfig.value.required = false
    currentFieldConfig.value.readOnly = true
  }
}

/** 更新层级绑定 */
function handleLevelBindingsUpdate(bindings: LevelBinding[]): void {
  if (!currentFieldConfig.value) return
  (currentFieldConfig.value as Record<string, unknown>).levelBindings = bindings
}

/** 判断属性类型是否支持校验规则 */
function supportsValidation(_schemaSubType?: string): boolean {
  // 系统内置属性不支持校验规则
  if (isSystemFieldConfig.value) {
    return false
  }
  // 所有非系统内置属性类型都支持校验规则
  return true
}

async function handleSave(): Promise<void> {
  if (!currentFieldConfig.value || !props.cardTypeId) return

  const errors = await formRef.value?.validate()
  if (errors) return

  saving.value = true
  try {
    await cardTypeApi.saveFieldConfig(props.cardTypeId, currentFieldConfig.value)
    Message.success(t('admin.cardType.fieldConfig.saveSuccess'))
    emit('update:visible', false)
    emit('save')
  } catch (error) {
    console.error('Failed to save field config:', error)
    // 错误提示已由 request 拦截器显示，这里不再重复显示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-drawer
    v-model:visible="drawerVisible"
    :title="drawerTitle"
    :width="880"
    :mask-closable="true"
    :esc-to-close="true"
    unmount-on-close
    @before-open="initData"
  >
    <a-form
      v-if="currentFieldConfig"
      ref="formRef"
      :model="currentFieldConfig"
      layout="vertical"
      size="small"
      :rules="formRules"
      class="field-config-form"
    >
      <!-- 基础信息 -->
      <div class="form-section">
        <div class="basic-info-grid">
          <div class="config-field">
            <label class="field-label">
              <span class="required-mark">*</span>
              {{ t('admin.cardType.fieldConfig.fieldName') }}
            </label>
            <a-input
              v-model="currentFieldConfig.name"
              :placeholder="t('admin.cardType.fieldConfig.enterFieldName')"
              :disabled="!isOwnConfig"
              style="width: 100%;"
            />
          </div>
          <div class="config-field">
            <label class="field-label">{{ t('admin.cardType.fieldConfig.fieldCode') }}</label>
            <a-input
              v-model="currentFieldConfig.code"
              :disabled="!isOwnConfig"
              style="width: 100%;"
            />
            <span class="field-hint">{{ t('admin.cardType.fieldConfig.codeHint') }}</span>
          </div>
        </div>
      </div>

      <!-- 值来源配置 -->
      <ValueSourceConfig
        v-if="supportsValueSource(currentFieldConfig.schemaSubType)"
        :model-value="(currentFieldConfig as any).valueSource || 'MANUAL'"
        :schema-sub-type="currentFieldConfig.schemaSubType"
        :disabled="!isOwnConfig"
        :reference-field-id="(currentFieldConfig as any).referenceFieldId"
        :reference-field-options="referenceFieldOptions"
        @update:model-value="handleValueSourceChange"
        @update:reference-field-id="(val) => (currentFieldConfig as any).referenceFieldId = val"
      />

      <!-- 通用配置 -->
      <CommonFieldConfig
        v-model:required="(currentFieldConfig as any).required"
        v-model:read-only="(currentFieldConfig as any).readOnly"
        :disabled="!isOwnConfig"
        :value-source-is-manual="valueSourceIsManual"
      />

      <!-- 校验规则配置 -->
      <ValidationRuleConfig
        v-if="supportsValidation(currentFieldConfig.schemaSubType)"
        v-model:validation-rules="(currentFieldConfig as any).validationRules"
        :field-id="currentFieldConfig.fieldId"
        :card-type-id="cardTypeId"
        :available-fields="availableFields"
        :link-types="linkTypes"
        :disabled="!isOwnConfig"
      />

      <!-- 文本类型配置 -->
      <TextFieldConfig
        v-if="isTextType"
        v-model:max-length="(currentFieldConfig as any).maxLength"
        v-model:default-value="(currentFieldConfig as any).defaultValue"
        :disabled="!isOwnConfig"
      />

      <!-- 数字类型配置 -->
      <NumberFieldConfig
        v-if="currentFieldConfig.schemaSubType === 'NUMBER_FIELD'"
        v-model:default-value="(currentFieldConfig as any).defaultValue"
        v-model:min-value="(currentFieldConfig as any).minValue"
        v-model:max-value="(currentFieldConfig as any).maxValue"
        v-model:precision="(currentFieldConfig as any).precision"
        v-model:unit="(currentFieldConfig as any).unit"
        v-model:display-format="(currentFieldConfig as any).displayFormat"
        v-model:percent-style="(currentFieldConfig as any).percentStyle"
        :disabled="!isOwnConfig"
      />

      <!-- 日期类型配置 -->
      <DateFieldConfig
        v-if="currentFieldConfig.schemaSubType === 'DATE_FIELD'"
        v-model:date-format="(currentFieldConfig as any).dateFormat"
        v-model:use-now-as-default="(currentFieldConfig as any).useNowAsDefault"
        :disabled="!isOwnConfig"
      />

      <!-- 枚举类型配置 -->
      <EnumFieldConfigComponent
        v-if="currentFieldConfig.schemaSubType === 'ENUM_FIELD'"
        v-model:multi-select="(currentFieldConfig as any).multiSelect"
        v-model:options="(currentFieldConfig as any).options"
        :disabled="!isOwnConfig"
      />

      <!-- 附件类型配置 -->
      <AttachmentFieldConfig
        v-if="currentFieldConfig.schemaSubType === 'ATTACHMENT_FIELD'"
        v-model:max-file-size="(currentFieldConfig as any).maxFileSize"
        v-model:max-file-count="(currentFieldConfig as any).maxFileCount"
        :disabled="!isOwnConfig"
      />

      <!-- 网页链接类型配置 -->
      <WebUrlFieldConfig
        v-if="currentFieldConfig.schemaSubType === 'WEB_URL_FIELD'"
        v-model:validate-url="(currentFieldConfig as any).validateUrl"
        v-model:show-preview="(currentFieldConfig as any).showPreview"
        v-model:default-url="(currentFieldConfig as any).defaultUrl"
        v-model:default-link-text="(currentFieldConfig as any).defaultLinkText"
        :disabled="!isOwnConfig"
      />

      <!-- 级联属性类型配置 -->
      <CascadeFieldConfig
        v-if="currentFieldConfig.schemaSubType === 'CASCADE_FIELD'"
        :cascade-relation-id="(currentFieldConfig as any).cascadeRelationId"
        :level-bindings="(currentFieldConfig as any).levelBindings || []"
        :card-type-id="props.cardTypeId"
        :field-name="currentFieldConfig.name"
        :disabled="!isOriginal"
        :readonly="!isOriginal"
        :cascade-relation-name="cascadeRelationDisplayName"
        @update:cascade-relation-id="(val) => (currentFieldConfig as any).cascadeRelationId = val"
        @update:level-bindings="handleLevelBindingsUpdate"
      />

      <!-- 提示信息配置 - 放在所有类型特定配置之后，内置属性不显示 -->
      <FieldHintConfig
        v-if="!isSystemFieldConfig"
        v-model:hint-text="(currentFieldConfig as any).hintText"
        v-model:show-hint-below-label="(currentFieldConfig as any).showHintBelowLabel"
        v-model:show-hint-as-tooltip="(currentFieldConfig as any).showHintAsTooltip"
        :disabled="!isOwnConfig"
        :card-type-id="cardTypeId"
      />
    </a-form>

    <template #footer>
      <a-space>
        <CancelButton @click="drawerVisible = false" />
        <SaveButton
          v-if="isOwnConfig || (currentFieldConfig?.schemaSubType === 'CASCADE_FIELD' && isOriginal)"
          :loading="saving"
          :text="t('admin.action.save')"
          @click="handleSave"
        />
      </a-space>
    </template>
  </a-drawer>
</template>

<style scoped>
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

.basic-info-grid .config-field {
  max-width: 320px;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
}

.required-mark {
  color: var(--color-danger);
  margin-right: 2px;
}

.field-value-text {
  color: var(--color-text-2);
  font-size: 14px;
}

.field-hint {
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
