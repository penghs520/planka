<script setup lang="ts">
import { computed, ref, watch, provide } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from '@arco-design/web-vue'
import { IconDelete, IconPlus, IconMenu } from '@arco-design/web-vue/es/icon'
import dayjs from 'dayjs'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import LabelHelpTooltip from '@/components/common/LabelHelpTooltip.vue'
import PathFieldSelector, { type FieldSelectionResult } from '@/components/condition/PathFieldSelector.vue'
import { useConditionDisplayInfo, CONDITION_DISPLAY_INFO_KEY } from '@/components/condition/useConditionDisplayInfo'
import { type Condition, type ConditionItem, NodeType, SYSTEM_DATE_FIELD_PREFIX, SystemDateField } from '@/types/condition'
import { SchemaSubType } from '@/types/schema'
import type { CardTypeDefinition, EntityCardType, TitlePart } from '@/types/card-type'
import { cardTypeApi } from '@/api/card-type'
import type { FieldOption } from '@/types/field-option'
import { VueDraggable } from 'vue-draggable-plus'

const { t } = useI18n()

const props = defineProps<{
  formData: CardTypeDefinition
  mode: 'create' | 'edit'
  selectedSubType?: SchemaSubType.TRAIT_CARD_TYPE | SchemaSubType.ENTITY_CARD_TYPE
}>()

const emit = defineEmits<{
  'update:selectedSubType': [value: SchemaSubType.TRAIT_CARD_TYPE | SchemaSubType.ENTITY_CARD_TYPE]
}>()

defineExpose({
  validate: () => formRef.value?.validate(),
})

const formRef = defineModel<FormInstance | undefined>('formRef')

const formRules = computed(() => ({
  name: [{ required: true, message: t('admin.cardType.form.nameRequired') }],
}))

const subTypeOptions = computed(() => [
  { value: SchemaSubType.ENTITY_CARD_TYPE, label: t('admin.cardType.schemaSubType.ENTITY_CARD_TYPE') },
  { value: SchemaSubType.TRAIT_CARD_TYPE, label: t('admin.cardType.schemaSubType.TRAIT_CARD_TYPE') },
])

// ID Generation Rule Logic
const ruleEnabled = ref(false)

watch(() => {
  if (props.formData.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE) {
    return props.formData.codeGenerationRule
  }
  return undefined
}, (val) => {
  ruleEnabled.value = !!val
}, { immediate: true })

watch(ruleEnabled, (val) => {
  if (props.formData.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE) {
    if (val) {
      if (!props.formData.codeGenerationRule) {
        props.formData.codeGenerationRule = {
          sequenceLength: 6,
          dateFormat: ''
        }
      }
    } else {
      props.formData.codeGenerationRule = undefined
    }
  }
})

const dateFormatOptions = computed(() => [
  { label: t('admin.cardType.form.dateFormatNone'), value: '' },
  { label: 'YYYYMMDD (例: 20240126)', value: 'yyyyMMdd' },
  { label: 'YYYY-MM-DD (例: 2024-01-26)', value: 'yyyy-MM-dd' },
  { label: 'YYYYMM (例: 202401)', value: 'yyyyMM' },
  { label: 'YYMMDD (例: 240126)', value: 'yyMMdd' },
])

const previewCode = computed(() => {
  if (props.formData.schemaSubType !== SchemaSubType.ENTITY_CARD_TYPE) return ''
  if (!ruleEnabled.value || !props.formData.codeGenerationRule) return ''
  const rule = props.formData.codeGenerationRule
  let code = ''
  if (rule.prefix) code += rule.prefix
  if (rule.dateFormat) {
    // Convert Java format to Dayjs format (simple)
    const format = rule.dateFormat.replace(/y/g, 'Y').replace(/d/g, 'D')
    code += dayjs().format(format)
    if (rule.dateSequenceConnector) {
      code += rule.dateSequenceConnector
    }
  }
  const len = rule.sequenceLength || 6
  code += '0'.repeat(Math.max(0, len - 1)) + '1'
  return code
})

// 标题组合规则允许使用的系统字段
const allowedSystemFieldIds = [
  NodeType.CODE,
  NodeType.STATUS,
  `${SYSTEM_DATE_FIELD_PREFIX}${SystemDateField.CREATED_AT}`
]

// Title Composition Rule Logic
const titleRuleEnabled = ref(false)
const availableFields = ref<FieldOption[]>([])

const concreteFormData = computed(() => {
  if (props.formData.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE) {
    return props.formData as EntityCardType
  }
  return null
})

watch(() => concreteFormData.value?.titleCompositionRule, (val) => {
  titleRuleEnabled.value = !!val?.enabled
}, { immediate: true })

watch(titleRuleEnabled, (val) => {
  if (concreteFormData.value) {
    if (val) {
      if (!concreteFormData.value.titleCompositionRule) {
        concreteFormData.value.titleCompositionRule = {
          enabled: true,
          area: 'PREFIX',
          parts: []
        }
      } else {
        concreteFormData.value.titleCompositionRule.enabled = true
      }
      loadAvailableFields()
    } else {
      if (concreteFormData.value.titleCompositionRule) {
        concreteFormData.value.titleCompositionRule.enabled = false
      }
    }
  }
})

async function loadAvailableFields() {
  if (!concreteFormData.value?.id) return
  try {
     const fields = await cardTypeApi.getFieldOptions(concreteFormData.value.id)
     availableFields.value = fields
  } catch (e) {
    console.error('Failed to load fields', e)
  }
}

// 监听 ID 变化以加载字段
watch(() => concreteFormData.value?.id, (newId) => {
    // 只有在编辑模式且启用时加载，且 ID 存在
    if (newId && titleRuleEnabled.value) {
        loadAvailableFields()
    }
}, { immediate: true })

function handleAddTitlePart(result: FieldSelectionResult) {
  if (!concreteFormData.value?.titleCompositionRule) return

  // 构建 TitlePart 对象
  const titlePart: TitlePart = {
    fieldId: result.fieldId || result.linkFieldId || '',
    path: result.path && result.path.linkNodes && result.path.linkNodes.length > 0 
      ? { linkNodes: result.path.linkNodes } 
      : undefined
  }

  if (titlePart.fieldId) {
    concreteFormData.value.titleCompositionRule.parts.push(titlePart)
  }
}

function handleRemoveTitlePart(index: number) {
  concreteFormData.value?.titleCompositionRule?.parts.splice(index, 1)
}

// 构造虚拟 Condition 以加载关联字段名称
const fakeCondition = computed<Condition | undefined>(() => {
  const parts = concreteFormData.value?.titleCompositionRule?.parts
  if (!parts || parts.length === 0) return undefined

  const children: ConditionItem[] = parts.map(part => ({
    nodeType: 'TEXT',
    subject: {
      fieldId: part.fieldId,
      path: part.path || undefined
    },
    operator: { type: 'IS_EMPTY' }
  } as any)) // Cast to any to avoid strict type checking for fake condition

  return {
    root: {
      nodeType: 'GROUP',
      operator: 'AND',
      children
    }
  }
})

const displayInfoContext = useConditionDisplayInfo(fakeCondition)
provide(CONDITION_DISPLAY_INFO_KEY, displayInfoContext)

watch(fakeCondition, () => {
  if (fakeCondition.value) {
    displayInfoContext.loadDisplayInfo()
  }
}, { immediate: true })

function getPartDisplayName(part: TitlePart) {
    const fieldId = part.fieldId

    // 解析关联路径
    let prefix = ''
    if (part.path && part.path.linkNodes && part.path.linkNodes.length > 0) {
        prefix = part.path.linkNodes.map(linkNodeId => {
            return displayInfoContext.getLinkFieldName(linkNodeId)
        }).join(' > ') + ' > '
    }

    // 尝试从 availableFields 中查找名称
    const field = availableFields.value.find(f => f.id === fieldId)
    if (field) {
        return prefix + field.name
    }

    // 尝试从 displayInfoContext 中查找
    const nameFromContext = displayInfoContext.getFieldName(fieldId)
    if (nameFromContext && nameFromContext !== fieldId) {
        return prefix + nameFromContext
    }

    // 处理系统字段的显示名称
    if (fieldId === 'CODE') return prefix + t('admin.cardType.form.code')
    if (fieldId === 'STATUS') return prefix + t('admin.table.status')
    if (fieldId.startsWith('SYSTEM_DATE:')) {
        const dateType = fieldId.split(':')[1]
        if (dateType === 'CREATED_AT') return prefix + t('admin.table.createdAt')
        if (dateType === 'UPDATED_AT') return prefix + t('admin.table.updatedAt')
        if (dateType === 'DISCARDED_AT') return prefix + t('admin.table.discardedAt')
    }

    return prefix + fieldId
}

// 获取级联字段的方法
const fetchFieldsByLinkFieldId = async (_linkFieldId: string) => {
    // 调用 API 获取关联卡片类型的字段
    // 这里需要解析 linkFieldId 获取关联类型，然后获取该类型的字段
    // 暂时返回空或 mock
    return []
}
</script>

<template>
  <div class="basic-info-form-container">
    <a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
      <!-- 新建模式：卡片类型种类选择 -->
    <a-form-item v-if="mode === 'create'">
      <template #label>
        <span>{{ t('admin.cardType.form.subType') }}</span>
        <LabelHelpTooltip :title="t('admin.cardType.help.subTypeTitle')" position="rt" width="420px">
          <div class="type-help-content">
            <div class="type-help-section">
              <div class="type-help-subtitle">
                <span class="type-badge abstract">{{ t('admin.cardType.help.abstractType') }}</span>
              </div>
              <div class="type-help-desc">
                {{ t('admin.cardType.help.abstractTypeDesc') }}
              </div>
              <div class="type-help-example">
                <span class="example-label">{{ t('admin.cardType.help.exampleLabel') }}</span>
                {{ t('admin.cardType.help.abstractTypeExample') }}
              </div>
            </div>

            <div class="type-help-section">
              <div class="type-help-subtitle">
                <span class="type-badge concrete">{{ t('admin.cardType.help.concreteType') }}</span>
              </div>
              <div class="type-help-desc">
                {{ t('admin.cardType.help.concreteTypeDesc') }}
              </div>
              <div class="type-help-example">
                <span class="example-label">{{ t('admin.cardType.help.exampleLabel') }}</span>
                {{ t('admin.cardType.help.concreteTypeExample') }}
              </div>
            </div>

            <div class="type-help-tip">
              <strong>{{ t('admin.cardType.help.suggestion') }}</strong>{{ t('admin.cardType.help.suggestionContent') }}
            </div>
          </div>
        </LabelHelpTooltip>
      </template>
      <a-radio-group
        :model-value="selectedSubType"
        @update:model-value="emit('update:selectedSubType', $event)"
      >
        <a-radio v-for="option in subTypeOptions" :key="option.value" :value="option.value">
          {{ option.label }}
        </a-radio>
      </a-radio-group>
    </a-form-item>

    <!-- 名称和编码 -->
    <a-row :gutter="16">
      <a-col :span="mode === 'create' ? 14 : 10">
        <a-form-item :label="t('admin.cardType.form.name')" field="name" :validate-trigger="['change', 'blur']">
          <a-input
            v-model="formData.name"
            :placeholder="t('admin.cardType.form.namePlaceholder')"
            :max-length="50"
          />
        </a-form-item>
      </a-col>
      <a-col :span="mode === 'create' ? 10 : 5">
        <a-form-item>
          <template #label>
            {{ t('admin.cardType.form.code') }}
            <LabelHelpTooltip :content="t('admin.cardType.form.codeHelp')" simple />
          </template>
          <a-input
            v-model="formData.code"
            :placeholder="t('admin.cardType.form.codePlaceholder')"
            :max-length="50"
          />
        </a-form-item>
      </a-col>
      <!-- 编辑模式：继承配置在同一行 -->
      <a-col v-if="mode === 'edit' && formData.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE" :span="10">
        <a-form-item>
          <template #label>
            {{ t('admin.cardType.form.parentTypes') }}
            <LabelHelpTooltip :content="t('admin.cardType.form.parentTypesHelp')" simple />
          </template>
          <CardTypeSelect
            v-model="formData.parentTypeIds"
            :schema-sub-type="SchemaSubType.TRAIT_CARD_TYPE"
            :placeholder="t('admin.cardType.form.parentTypesPlaceholder')"
          />
        </a-form-item>
      </a-col>
    </a-row>

    <!-- 新建模式：继承配置独占一行 -->
    <a-form-item
      v-if="mode === 'create' && formData.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE"
    >
      <template #label>
        {{ t('admin.cardType.form.parentTypes') }}
        <LabelHelpTooltip :content="t('admin.cardType.form.parentTypesHelp')" simple />
      </template>
      <CardTypeSelect
        v-model="formData.parentTypeIds"
        :schema-sub-type="SchemaSubType.TRAIT_CARD_TYPE"
        :placeholder="t('admin.cardType.form.parentTypesPlaceholder')"
      />
    </a-form-item>

    <!-- 编号生成规则 -->
    <a-form-item
      v-if="formData.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE"
      :class="{ 'collapsed-form-item': !ruleEnabled }"
    >
      <template #label>
        <div class="rule-label-container">
            <span @click.prevent>{{ t('admin.cardType.form.codeGenerationRule') }}</span>
             <LabelHelpTooltip :content="t('admin.cardType.form.codeGenerationWarning')" simple />
            <div @click.stop>
              <a-switch v-model="ruleEnabled" size="small">
                  <template #checked>{{ t('admin.status.enabled') }}</template>
                  <template #unchecked>{{ t('admin.status.disabled') }}</template>
              </a-switch>
            </div>
        </div>
      </template>
      <a-space direction="vertical" style="width: 100%">
        <div v-if="ruleEnabled && formData.codeGenerationRule" class="rule-config-panel">
          <a-row :gutter="16">
            <a-col :span="3">
              <a-form-item
                :label="t('admin.cardType.form.prefix')"
                field="codeGenerationRule.prefix"
                no-style
              >
                <a-input
                  v-model="formData.codeGenerationRule.prefix"
                  :placeholder="t('admin.cardType.form.prefix')"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
               <a-form-item
                :label="t('admin.cardType.form.dateFormat')"
                field="codeGenerationRule.dateFormat"
                no-style
              >
                <a-select
                   v-model="formData.codeGenerationRule.dateFormat"
                   :options="dateFormatOptions"
                   :placeholder="t('admin.cardType.form.dateFormat')"
                   allow-clear
                />
               </a-form-item>
            </a-col>
            <a-col :span="3">
              <a-form-item
                :label="t('admin.cardType.form.dateSequenceConnector')"
                field="codeGenerationRule.dateSequenceConnector"
                no-style
              >
                <a-input
                  v-model="formData.codeGenerationRule.dateSequenceConnector"
                  :placeholder="t('admin.cardType.form.dateSequenceConnector')"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item
                :label="t('admin.cardType.form.sequenceLength')"
                field="codeGenerationRule.sequenceLength"
                no-style
              >
                 <a-input-number
                   v-model="formData.codeGenerationRule.sequenceLength"
                   :min="1"
                   :max="10"
                   :placeholder="t('admin.cardType.form.sequenceLength')"
                 />
              </a-form-item>
            </a-col>
          </a-row>

          <div class="preview-box">
             <span class="label">{{ t('admin.cardType.form.preview') }}: </span>
             <span class="code">{{ previewCode }}</span>
             <div class="help-text">{{ t('admin.cardType.form.previewHelp') }}</div>
          </div>
        </div>
      </a-space>
    </a-form-item>

    <!-- 标题组合规则 -->
    <a-form-item
      v-if="formData.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE"
      :class="{ 'collapsed-form-item': !titleRuleEnabled }"
    >
      <template #label>
          <div class="rule-label-container">
              <span @click.prevent>{{ t('admin.cardType.form.titleCompositionRule') }}</span>
              <div @click.stop>
                <a-switch v-model="titleRuleEnabled" size="small">
                    <template #checked>{{ t('admin.status.enabled') }}</template>
                    <template #unchecked>{{ t('admin.status.disabled') }}</template>
                </a-switch>
              </div>
          </div>
      </template>
      <a-space direction="vertical" style="width: 100%">
        <div v-if="titleRuleEnabled && concreteFormData?.titleCompositionRule" class="rule-config-panel">
           <a-form-item style="margin-bottom: 8px">
               <a-radio-group v-model="concreteFormData.titleCompositionRule.area">
                   <a-radio value="PREFIX">{{ t('admin.cardType.form.prefix') }}</a-radio>
                   <a-radio value="SUFFIX">{{ t('admin.cardType.form.suffix') }}</a-radio>
               </a-radio-group>
           </a-form-item>

           <div class="composition-parts-list">
               <div class="list-header">{{ t('admin.cardType.form.compositionParts') }}</div>
               <VueDraggable
                 v-model="concreteFormData.titleCompositionRule.parts"
                 handle=".drag-handle"
                 :animation="200"
               >
                   <div v-for="(element, index) in concreteFormData.titleCompositionRule.parts" :key="index" class="part-item">
                       <icon-menu class="drag-handle" />
                       <span class="part-name">{{ getPartDisplayName(element) }}</span>
                       <a-button type="text" status="danger" size="mini" @click="handleRemoveTitlePart(index)">
                           <template #icon><icon-delete /></template>
                       </a-button>
                   </div>
               </VueDraggable>
               <div v-if="concreteFormData.titleCompositionRule.parts.length === 0" class="empty-parts">
                   {{ t('admin.cardType.form.noParts') }}
               </div>
           </div>

           <div class="add-part-action">
               <PathFieldSelector
                 :available-fields="availableFields"
                 :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
                 :custom-trigger="true"
                 :allowed-system-field-ids="allowedSystemFieldIds"
                 @select="handleAddTitlePart"
               >
                   <a-button type="outline" size="small">
                       <template #icon><icon-plus /></template>
                       {{ t('admin.cardType.form.addField') }}
                   </a-button>
               </PathFieldSelector>
           </div>
        </div>
      </a-space>
    </a-form-item>

    <!-- 描述 -->
    <a-form-item :label="t('admin.cardType.form.description')">
      <a-textarea
        v-model="formData.description"
        :placeholder="t('admin.cardType.form.descriptionPlaceholder')"
        :max-length="200"
        :auto-size="{ minRows: 2, maxRows: 4 }"
      />
    </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
.basic-info-form-container {
  height: 100%;
  overflow-y: auto;
  padding: 0 12px; /* Prevent horizontal scrollbar caused by a-row gutter negative margins */
}

/* 类型帮助内容 */
.type-help-content {
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-2);
}

.type-help-section {
  margin-bottom: 16px;
}

.type-help-section:last-of-type {
  margin-bottom: 0;
}

.type-help-subtitle {
  margin-bottom: 8px;
}

.type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.type-badge.abstract {
  background: rgb(var(--purple-1));
  color: rgb(var(--purple-6));
}

.type-badge.concrete {
  background: rgb(var(--primary-1));
  color: rgb(var(--primary-6));
}

.type-help-desc {
  color: var(--color-text-2);
  margin-bottom: 6px;
  padding-left: 4px;
}

.type-help-example {
  font-size: 11px;
  color: var(--color-text-3);
  padding: 6px 10px;
  background: var(--color-fill-1);
  border-radius: 4px;
  border-left: 2px solid rgb(var(--primary-6));
}

.type-help-example .example-label {
  font-weight: 600;
  color: var(--color-text-2);
}

.type-help-tip {
  margin-top: 16px;
  padding: 10px 12px;
  background: rgb(var(--warning-1));
  border-left: 3px solid rgb(var(--warning-6));
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-2);
}

.rule-label-container {
  display: flex;
  align-items: center;
  gap: 4px;
}  

.rule-label-container > div:last-child {
  margin-left: 8px;
}  

.rule-config-panel {
  margin-top: 16px;
  padding: 12px 16px;
  border: 1px dashed var(--color-border-2);
  border-radius: 4px;
}

.preview-box {
  margin-top: 16px;
  padding-top: 12px;
}

.preview-box .label {
  color: var(--color-text-2);
  margin-right: 8px;
}

.preview-box .code {
  font-family: monospace;
  font-size: 14px;
  color: var(--color-text-2);
  padding: 0 4px;
}

.preview-box .help-text {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-3);
}

.composition-parts-list {
  margin-bottom: 12px;
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  background: var(--color-bg-2);
  width: 480px;
}

.list-header {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-3);
  border-bottom: 1px solid var(--color-border-2);
  background: var(--color-fill-1);
}

.part-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--color-border-1);
  background: var(--color-bg-2);
}

.part-item:last-child {
  border-bottom: none;
}

.drag-handle {
  cursor: move;
  color: var(--color-text-3);
  margin-right: 8px;
}

.part-name {
  flex: 1;
  font-size: 13px;
  color: var(--color-text-1);
}

.empty-parts {
  padding: 16px;
  text-align: center;
  color: var(--color-text-3);
  font-size: 13px;
}

.add-part-action {
  margin-top: 12px;
}

:deep(.collapsed-form-item.arco-form-item) {
  margin-bottom: 12px;
}

:deep(.collapsed-form-item .arco-form-item-label) {
  margin-bottom: 0;
}
</style>
