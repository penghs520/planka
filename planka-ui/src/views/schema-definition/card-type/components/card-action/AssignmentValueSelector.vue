<template>
  <a-trigger
    trigger="click"
    :popup-visible="dropdownVisible"
    position="bl"
    auto-fit-popup-min-width
    @popup-visible-change="handleDropdownVisibleChange"
  >
    <!-- 触发器：模拟 select 外观 -->
    <div
      class="assignment-value-trigger"
      :class="{ 'is-focus': dropdownVisible, 'has-value': hasValue }"
    >
      <span v-if="hasValue" class="value-text">{{ displayText }}</span>
      <span v-else class="placeholder">{{ t('admin.cardAction.updateCard.selectAssignment') }}</span>
      <IconDown class="trigger-icon" :class="{ 'is-open': dropdownVisible }" />
    </div>

    <template #content>
      <div class="assignment-dropdown">
        <!-- 面板容器 -->
        <div class="panels-container">
          <!-- 主面板：赋值方式列表 -->
          <div class="main-panel">
            <div
              v-for="option in mainPanelOptions"
              :key="option.key"
              class="assignment-item"
              :class="{
                selected: isOptionSelected(option),
                expanded: isOptionExpanded(option),
              }"
              @mouseenter="handleOptionHover(option)"
              @mouseleave="handleOptionLeave"
              @click="handleOptionClick(option)"
            >
              <!-- 来源类型图标 -->
              <IconUser v-if="option.type === 'source' && option.source === ReferenceSource.CURRENT_USER" class="item-icon" />
              <IconFile v-if="option.type === 'source' && option.source === ReferenceSource.CURRENT_CARD" class="item-icon" />
              <!-- 赋值类型图标 -->
              <IconPenFill v-if="option.type === 'assignment' && option.assignmentType === AssignmentTypeEnum.USER_INPUT" class="item-icon" />
              <IconEdit v-if="option.type === 'assignment' && option.assignmentType === AssignmentTypeEnum.FIXED_VALUE" class="item-icon" />
              <IconClockCircle v-if="option.type === 'assignment' && option.assignmentType === AssignmentTypeEnum.CURRENT_TIME" class="item-icon" />
              <IconClose v-if="option.type === 'assignment' && option.assignmentType === AssignmentTypeEnum.CLEAR_VALUE" class="item-icon" />
              <IconPlus v-if="option.type === 'assignment' && option.assignmentType === AssignmentTypeEnum.INCREMENT" class="item-icon" />
              <span class="item-text">{{ option.label }}</span>
              <IconRight v-if="optionNeedsSubPanel(option)" class="expand-icon" />
            </div>
          </div>

          <!-- 子面板：根据赋值方式显示不同内容 -->
          <template v-if="showSubPanel">
            <!-- 固定值子面板 -->
            <div v-if="hoveredType === AssignmentTypeEnum.FIXED_VALUE" class="sub-panel fixed-value-panel">
              <div class="panel-content">
                <!-- 文本类型 -->
                <template v-if="valueType === FixedValueTypeEnum.TEXT">
                  <a-input
                    v-if="!isMultiLineText"
                    v-model="textValue"
                    :placeholder="t('admin.cardType.fieldConfig.enterDefaultValue')"
                    @change="handleFixedValueChange"
                  />
                  <a-textarea
                    v-else
                    v-model="textValue"
                    :placeholder="t('admin.cardType.fieldConfig.enterDefaultValue')"
                    :auto-size="{ minRows: 2, maxRows: 4 }"
                    @change="handleFixedValueChange"
                  />
                </template>

                <!-- 数字类型 -->
                <template v-else-if="valueType === FixedValueTypeEnum.NUMBER">
                  <a-input-number
                    v-model="numberValue"
                    :placeholder="t('admin.cardType.fieldConfig.enterValue')"
                    style="width: 100%"
                    @change="handleFixedValueChange"
                  />
                </template>

                <!-- 日期类型 -->
                <template v-else-if="valueType === FixedValueTypeEnum.DATE">
                  <div class="date-value-editor">
                    <a-radio-group v-model="dateMode" @change="handleFixedValueChange">
                      <a-radio :value="DateMode.ABSOLUTE">{{ t('admin.cardAction.fixedValue.absolute') }}</a-radio>
                      <a-radio :value="DateMode.RELATIVE">{{ t('admin.cardAction.fixedValue.relative') }}</a-radio>
                    </a-radio-group>
                    <a-date-picker
                      v-if="dateMode === DateMode.ABSOLUTE"
                      v-model="absoluteDate"
                      style="width: 100%; margin-top: 8px"
                      @change="handleFixedValueChange"
                    />
                    <div v-else class="offset-input" style="margin-top: 8px">
                      <span>{{ t('admin.cardAction.fixedValue.offsetDays') }}:</span>
                      <a-input-number
                        v-model="offsetDays"
                        style="width: 100px; margin-left: 8px"
                        @change="handleFixedValueChange"
                      />
                    </div>
                  </div>
                </template>

                <!-- 枚举类型 -->
                <template v-else-if="valueType === FixedValueTypeEnum.ENUM">
                  <div class="enum-options">
                    <div
                      v-for="opt in enumOptions"
                      :key="opt.value"
                      class="enum-option"
                      :class="{ selected: enumValueIds.includes(opt.value) }"
                      @click="toggleEnumValue(opt.value)"
                    >
                      <a-checkbox :model-value="enumValueIds.includes(opt.value)" @click.stop />
                      <span class="option-label">{{ opt.label }}</span>
                    </div>
                  </div>
                </template>
              </div>
              <div class="panel-footer">
                <a-button type="primary" size="small" @click="confirmFixedValue">
                  {{ t('admin.action.confirm') }}
                </a-button>
              </div>
            </div>

            <!-- 当前用户/当前卡片 字段列表面板（支持级联） -->
            <template v-else-if="hoveredSource !== null">
              <!-- 第一层级联面板 -->
              <div
                v-if="cascadeLevels.length > 0 && cascadeLevels[0]"
                class="sub-panel field-panel"
              >
                <div v-if="cascadeLevels[0].loading" class="loading-container">
                  <a-spin />
                </div>
                <div v-else class="field-list">
                  <div
                    v-for="field in cascadeLevels[0].fields"
                    :key="field.id"
                    class="field-item"
                    :class="{
                      hovered: isFieldHovered(field.id!, 0),
                      disabled: !field.matched,
                      'can-expand': canExpand(field),
                    }"
                    @mouseenter="handleFieldHover(field, 0)"
                    @mouseleave="handleFieldLeave(0)"
                    @click="handleFieldSelect(field, 0)"
                  >
                    <span class="field-name">{{ field.name }}</span>
                    <IconRight v-if="canExpand(field)" class="expand-icon" />
                  </div>
                  <div v-if="cascadeLevels[0].fields.length === 0" class="empty-text">
                    {{ t('common.condition.noAvailableFields') }}
                  </div>
                </div>
              </div>

              <!-- 后续级联层级（第2层及以后） -->
              <div
                v-for="(level, levelIndex) in cascadeLevels.slice(1)"
                :key="'cascade-' + (levelIndex + 1)"
                class="sub-panel field-panel"
              >
                <div v-if="level.loading" class="loading-container">
                  <a-spin />
                </div>
                <div v-else class="field-list">
                  <div
                    v-for="field in level.fields"
                    :key="field.id"
                    class="field-item"
                    :class="{
                      hovered: isFieldHovered(field.id!, levelIndex + 1),
                      disabled: !field.matched,
                      'can-expand': canExpand(field) && levelIndex + 1 < MAX_CASCADE_DEPTH - 1,
                    }"
                    @mouseenter="handleFieldHover(field, levelIndex + 1)"
                    @mouseleave="handleFieldLeave(levelIndex + 1)"
                    @click="handleFieldSelect(field, levelIndex + 1)"
                  >
                    <span class="field-name">{{ field.name }}</span>
                    <IconRight v-if="canExpand(field) && levelIndex + 1 < MAX_CASCADE_DEPTH - 1" class="expand-icon" />
                  </div>
                  <div v-if="level.fields.length === 0" class="empty-text">
                    {{ t('common.condition.noAvailableFields') }}
                  </div>
                </div>
              </div>
            </template>

            <!-- 当前时间子面板 -->
            <div v-else-if="hoveredType === AssignmentTypeEnum.CURRENT_TIME" class="sub-panel current-time-panel">
              <div class="panel-content">
                <div class="offset-input">
                  <span>{{ t('admin.cardAction.fixedValue.offsetDays') }}:</span>
                  <a-input-number
                    v-model="currentTimeOffsetDays"
                    :default-value="0"
                    style="width: 100px; margin-left: 8px"
                  />
                </div>
              </div>
              <div class="panel-footer">
                <a-button type="primary" size="small" @click="confirmCurrentTime">
                  {{ t('admin.action.confirm') }}
                </a-button>
              </div>
            </div>

            <!-- 数值增量子面板 -->
            <div v-else-if="hoveredType === AssignmentTypeEnum.INCREMENT" class="sub-panel increment-panel">
              <div class="panel-content">
                <div class="increment-input">
                  <span>{{ t('admin.cardAction.updateCard.incrementValue') }}:</span>
                  <a-input-number
                    v-model="incrementValue"
                    :default-value="1"
                    style="width: 100px; margin-left: 8px"
                  />
                </div>
                <div class="allow-negative" style="margin-top: 8px">
                  <a-checkbox v-model="allowNegative">
                    {{ t('admin.cardAction.updateCard.allowNegative') }}
                  </a-checkbox>
                </div>
              </div>
              <div class="panel-footer">
                <a-button type="primary" size="small" @click="confirmIncrement">
                  {{ t('admin.action.confirm') }}
                </a-button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </template>
  </a-trigger>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconUser, IconFile, IconDown, IconRight, IconEdit, IconClockCircle, IconClose, IconPlus, IconPenFill } from '@arco-design/web-vue/es/icon'
import { fieldOptionsApi } from '@/api/field-options'
import type { FieldOption } from '@/types/field-option'
import type { FieldAssignment, FixedValue } from '@/types/card-action'
import {
  AssignmentTypeEnum,
  FixedValueTypeEnum,
  ReferenceSource,
  DateMode,
} from '@/types/card-action'

const { t } = useI18n()

/** 最大级联层数 */
const MAX_CASCADE_DEPTH = 2

/** 级联层级数据 */
interface CascadeLevel {
  fields: (FieldOption & { matched: boolean })[]
  hoveredFieldId: string | null
  loading: boolean
  parentLinkFieldId: string
}

// 主面板选项类型
type MainPanelOption =
  | { type: 'assignment'; key: string; assignmentType: AssignmentTypeEnum; label: string }
  | { type: 'source'; key: string; source: ReferenceSource; label: string }

const props = defineProps<{
  /** 目标字段信息 */
  targetField?: FieldOption
  /** 当前卡片字段（用于"当前卡片"引用） */
  referenceFields?: FieldOption[]
  /** 成员卡片字段（用于"当前用户"引用） */
  memberFields?: FieldOption[]
  /** 当前卡片类型ID（用于判断引用来源本身是否可选） */
  currentCardTypeId?: string
  /** 成员卡片类型ID（用于判断引用来源本身是否可选） */
  memberCardTypeId?: string
  /** 是否隐藏用户输入选项（业务规则等自动执行场景使用） */
  hideUserInput?: boolean
}>()

const modelValue = defineModel<FieldAssignment>({ required: true })

const dropdownVisible = ref(false)
const hoveredType = ref<AssignmentTypeEnum | null>(null)
const hoveredSource = ref<ReferenceSource | null>(null)
let hoverTimer: ReturnType<typeof setTimeout> | null = null

// 级联层级数据
const cascadeLevels = ref<CascadeLevel[]>([])

// 已选路径字段名称（用于回显）
const selectedPathFieldNames = ref<string[]>([])
// 用于防止异步加载的竞态条件
let loadPathNamesVersion = 0

// 临时值状态
const textValue = ref('')
const numberValue = ref<number>(0)
const dateMode = ref<DateMode>(DateMode.ABSOLUTE)
const absoluteDate = ref<string>('')
const offsetDays = ref<number>(0)
const enumValueIds = ref<string[]>([])
const currentTimeOffsetDays = ref<number>(0)
const incrementValue = ref<number>(1)
const allowNegative = ref<boolean>(false)

// 主面板选项（扁平化，当前用户和当前卡片直接作为顶级选项）
const mainPanelOptions = computed<MainPanelOption[]>(() => {
  const options: MainPanelOption[] = []

  // 用户输入作为默认首选（业务规则等自动执行场景不显示）
  if (!props.hideUserInput) {
    options.push({ type: 'assignment', key: 'USER_INPUT', assignmentType: AssignmentTypeEnum.USER_INPUT, label: t('admin.cardAction.assignment.USER_INPUT') })
  }

  options.push(
    { type: 'source', key: 'CURRENT_USER', source: ReferenceSource.CURRENT_USER, label: t('admin.cardAction.referenceSource.CURRENT_USER') },
    { type: 'source', key: 'CURRENT_CARD', source: ReferenceSource.CURRENT_CARD, label: t('admin.cardAction.referenceSource.CURRENT_CARD') },
    { type: 'assignment', key: 'FIXED_VALUE', assignmentType: AssignmentTypeEnum.FIXED_VALUE, label: t('admin.cardAction.assignment.FIXED_VALUE') },
  )

  // 日期类型才显示"当前时间"
  if (props.targetField?.fieldType === 'DATE' || props.targetField?.fieldType === 'DATETIME') {
    options.push({ type: 'assignment', key: 'CURRENT_TIME', assignmentType: AssignmentTypeEnum.CURRENT_TIME, label: t('admin.cardAction.assignment.CURRENT_TIME') })
  }

  // 清空值
  options.push({ type: 'assignment', key: 'CLEAR_VALUE', assignmentType: AssignmentTypeEnum.CLEAR_VALUE, label: t('admin.cardAction.assignment.CLEAR_VALUE') })

  // 数字类型才显示"数值增量"
  if (props.targetField?.fieldType === 'NUMBER') {
    options.push({ type: 'assignment', key: 'INCREMENT', assignmentType: AssignmentTypeEnum.INCREMENT, label: t('admin.cardAction.assignment.INCREMENT') })
  }

  return options
})

// 当前选中的赋值类型
const selectedAssignmentType = computed(() => modelValue.value?.assignmentType)

// 当前选中的来源（如果是字段引用类型）
const selectedSource = computed(() => {
  if (modelValue.value?.assignmentType === AssignmentTypeEnum.REFERENCE_FIELD && 'source' in modelValue.value) {
    return modelValue.value.source
  }
  return null
})

// 根据目标字段类型确定固定值类型
const valueType = computed(() => {
  if (!props.targetField) return FixedValueTypeEnum.TEXT
  switch (props.targetField.fieldType) {
    case 'SINGLE_LINE_TEXT':
    case 'MULTI_LINE_TEXT':
    case 'MARKDOWN':
    case 'WEB_URL':
      return FixedValueTypeEnum.TEXT
    case 'NUMBER':
      return FixedValueTypeEnum.NUMBER
    case 'DATE':
    case 'DATETIME':
      return FixedValueTypeEnum.DATE
    case 'SINGLE_ENUM':
    case 'MULTI_ENUM':
    case 'ENUM':
      return FixedValueTypeEnum.ENUM
    default:
      return FixedValueTypeEnum.TEXT
  }
})

const isMultiLineText = computed(() =>
  props.targetField?.fieldType === 'MULTI_LINE_TEXT' ||
  props.targetField?.fieldType === 'MARKDOWN'
)

const enumOptions = computed(() => props.targetField?.enumOptions || [])

const hasValue = computed(() => !!modelValue.value?.assignmentType)

const showSubPanel = computed(() =>
  (hoveredType.value !== null && hoveredType.value !== AssignmentTypeEnum.CLEAR_VALUE) ||
  hoveredSource.value !== null
)

/**
 * 判断引用字段是否与目标字段兼容（严格匹配）
 */
function isFieldCompatible(sourceField: FieldOption, targetField?: FieldOption): boolean {
  if (!targetField) return true

  // 类型必须完全匹配
  if (sourceField.fieldType !== targetField.fieldType) {
    return false
  }

  // ENUM：必须是同一个字段定义（通过 id 判断）
  if (targetField.fieldType === 'ENUM') {
    return sourceField.id === targetField.id
  }

  // STRUCTURE：必须是同一个架构线
  if (targetField.fieldType === 'STRUCTURE') {
    return sourceField.structureId === targetField.structureId
  }

  // LINK：必须是同一个关联类型（通过 linkTypeId 判断）
  if (targetField.fieldType === 'LINK') {
    const sourceLinkTypeId = sourceField.id?.split(':')[0]
    const targetLinkTypeId = targetField.id?.split(':')[0]
    return sourceLinkTypeId === targetLinkTypeId
  }

  // 其他类型：类型匹配即可
  return true
}

/**
 * 判断引用来源本身是否可以直接选择（不选具体字段）
 */
function isSourceSelfSelectableCheck(source: ReferenceSource): boolean {
  if (!props.targetField || props.targetField.fieldType !== 'LINK') {
    return false
  }

  const targetCardTypeIds = props.targetField.targetCardTypeIds || []
  if (targetCardTypeIds.length === 0) return true // 无限制时都可以

  // 获取引用来源的卡片类型ID
  const sourceCardTypeId = source === ReferenceSource.CURRENT_USER
    ? props.memberCardTypeId
    : props.currentCardTypeId

  return sourceCardTypeId ? targetCardTypeIds.includes(sourceCardTypeId) : false
}

/**
 * 判断字段是否可以展开下一级
 */
function canExpand(field: FieldOption & { matched: boolean }): boolean {
  // 只有 LINK 类型且为单选才能展开
  return field.fieldType === 'LINK' && field.multiple !== true
}

/**
 * 加载第一层字段（带类型匹配）
 */
function loadFirstLevelFields(source: ReferenceSource) {
  const sourceFields = source === ReferenceSource.CURRENT_USER
    ? props.memberFields
    : props.referenceFields

  if (!sourceFields) {
    cascadeLevels.value = []
    return
  }

  // 为每个字段标记是否匹配
  const fieldsWithMatch = sourceFields.map(field => ({
    ...field,
    matched: isFieldCompatible(field, props.targetField)
  }))

  // 过滤规则：
  // - 匹配的字段：显示
  // - 不匹配的 LINK 类型且为单选（multiple !== true）：显示但置灰（可展开下一级）
  // - 不匹配的 LINK 类型但为多选（multiple === true）：隐藏
  // - 不匹配的非 LINK 类型：隐藏
  const filteredFields = fieldsWithMatch.filter(f =>
    f.matched || (f.fieldType === 'LINK' && f.multiple !== true)
  )

  cascadeLevels.value = [{
    fields: filteredFields,
    hoveredFieldId: null,
    loading: false,
    parentLinkFieldId: '',
  }]
}

/**
 * 加载下一层级联字段
 */
async function loadNextLevelFields(levelIndex: number, parentLinkFieldId: string) {
  if (levelIndex >= MAX_CASCADE_DEPTH - 1) return

  const nextLevelIndex = levelIndex + 1

  // 设置当前层 hover
  if (cascadeLevels.value[levelIndex]) {
    cascadeLevels.value[levelIndex].hoveredFieldId = parentLinkFieldId
  }

  // 移除后续层级
  cascadeLevels.value = cascadeLevels.value.slice(0, nextLevelIndex)

  // 添加加载中层级
  cascadeLevels.value.push({
    fields: [],
    hoveredFieldId: null,
    loading: true,
    parentLinkFieldId,
  })

  try {
    const fields = await fieldOptionsApi.getFieldsByLinkFieldId(parentLinkFieldId)

    const fieldsWithMatch = fields.map(field => ({
      ...field,
      matched: isFieldCompatible(field, props.targetField)
    }))

    // 最后一层：只显示匹配的字段
    // 非最后一层：匹配的显示，单选LINK类型置灰可展开
    const isLastLevel = nextLevelIndex >= MAX_CASCADE_DEPTH - 1
    const filteredFields = isLastLevel
      ? fieldsWithMatch.filter(f => f.matched)
      : fieldsWithMatch.filter(f => f.matched || (f.fieldType === 'LINK' && f.multiple !== true))

    if (cascadeLevels.value[nextLevelIndex]) {
      cascadeLevels.value[nextLevelIndex].fields = filteredFields
      cascadeLevels.value[nextLevelIndex].loading = false
    }
  } catch (error) {
    console.error('加载级联字段失败:', error)
    if (cascadeLevels.value[nextLevelIndex]) {
      cascadeLevels.value[nextLevelIndex].loading = false
    }
  }
}

/**
 * 加载已选路径的字段名称（用于回显）
 */
async function loadSelectedPathFieldNames() {
  const currentVersion = ++loadPathNamesVersion

  const mv = modelValue.value
  if (mv?.assignmentType !== AssignmentTypeEnum.REFERENCE_FIELD) {
    selectedPathFieldNames.value = []
    return
  }
  if (!('source' in mv) || !('sourceFieldId' in mv)) {
    selectedPathFieldNames.value = []
    return
  }

  const source = (mv as any).source as ReferenceSource
  const sourceFieldId = (mv as any).sourceFieldId as string | undefined
  const path = (mv as any).path as { linkNodes?: string[] } | undefined

  if (!sourceFieldId) {
    selectedPathFieldNames.value = []
    return
  }

  const names: string[] = []

  try {
    // 获取第一层字段列表
    const sourceFields = source === ReferenceSource.CURRENT_USER
      ? props.memberFields
      : props.referenceFields

    if (path?.linkNodes && path.linkNodes.length > 0) {
      // 有级联路径
      const linkNodes = path.linkNodes

      // 第一个 linkNode 在第一层字段列表中查找
      const firstLinkField = sourceFields?.find(f => f.id === linkNodes[0])
      if (firstLinkField) {
        names.push(firstLinkField.name)
      }

      // 检查版本，防止竞态条件
      if (currentVersion !== loadPathNamesVersion) return

      // 后续 linkNodes（如果有）：根据前一个 linkFieldId 加载
      for (let i = 1; i < linkNodes.length; i++) {
        const prevLinkNode = linkNodes[i - 1]
        const currentLinkNode = linkNodes[i]
        if (!prevLinkNode || !currentLinkNode) continue

        const fields = await fieldOptionsApi.getFieldsByLinkFieldId(prevLinkNode)
        // 再次检查版本
        if (currentVersion !== loadPathNamesVersion) return
        const field = fields.find(f => f.id === currentLinkNode)
        if (field) {
          names.push(field.name)
        }
      }

      // 最后的 sourceFieldId：从最后一个 linkNode 加载目标字段列表
      const lastLinkNode = linkNodes[linkNodes.length - 1]
      if (lastLinkNode) {
        const targetFields = await fieldOptionsApi.getFieldsByLinkFieldId(lastLinkNode)
        // 再次检查版本
        if (currentVersion !== loadPathNamesVersion) return
        const targetField = targetFields.find(f => f.id === sourceFieldId)
        if (targetField) {
          names.push(targetField.name)
        }
      }
    } else {
      // 无级联路径，直接在第一层查找
      const field = sourceFields?.find(f => f.id === sourceFieldId)
      if (field) {
        names.push(field.name)
      }
    }
  } catch (error) {
    console.error('加载已选路径字段名称失败:', error)
  }

  // 最终检查版本
  if (currentVersion !== loadPathNamesVersion) return
  selectedPathFieldNames.value = names
}

// 显示文本
const displayText = computed(() => {
  if (!modelValue.value) return ''

  switch (modelValue.value.assignmentType) {
    case AssignmentTypeEnum.USER_INPUT:
      return t('admin.cardAction.assignment.USER_INPUT')
    case AssignmentTypeEnum.FIXED_VALUE: {
      const typeLabel = t('admin.cardAction.assignment.FIXED_VALUE')
      if ('value' in modelValue.value && modelValue.value.value) {
        const val = modelValue.value.value
        if (val.valueType === FixedValueTypeEnum.TEXT) return `${typeLabel}: ${val.text}`
        if (val.valueType === FixedValueTypeEnum.NUMBER) return `${typeLabel}: ${val.number}`
        if (val.valueType === FixedValueTypeEnum.DATE) {
          if (val.mode === DateMode.ABSOLUTE) return `${typeLabel}: ${val.absoluteDate}`
          return `${typeLabel}: ${t('admin.cardAction.fixedValue.offsetDays')} ${val.offsetDays}`
        }
        if (val.valueType === FixedValueTypeEnum.ENUM) {
          const labels = val.enumValueIds.map(id => enumOptions.value.find(o => o.value === id)?.label || id)
          return `${typeLabel}: ${labels.join(', ')}`
        }
      }
      return typeLabel
    }
    case AssignmentTypeEnum.REFERENCE_FIELD: {
      if ('source' in modelValue.value) {
        const sourceLabel = modelValue.value.source === ReferenceSource.CURRENT_USER
          ? t('admin.cardAction.referenceSource.CURRENT_USER')
          : t('admin.cardAction.referenceSource.CURRENT_CARD')
        if ('sourceFieldId' in modelValue.value && (modelValue.value as any).sourceFieldId) {
          // 使用已加载的路径字段名称
          if (selectedPathFieldNames.value.length > 0) {
            return `${sourceLabel}.${selectedPathFieldNames.value.join('.')}`
          }
          // 如果还没加载完成，尝试从第一层字段列表查找（兜底）
          const sourceFields = modelValue.value.source === ReferenceSource.CURRENT_USER
            ? props.memberFields
            : props.referenceFields
          const fieldLabel = sourceFields?.find(f => f.id === (modelValue.value as any).sourceFieldId)?.name || ''
          return `${sourceLabel}.${fieldLabel}`
        }
        return sourceLabel
      }
      return t('admin.cardAction.assignment.REFERENCE_FIELD')
    }
    case AssignmentTypeEnum.CURRENT_TIME: {
      const typeLabel = t('admin.cardAction.assignment.CURRENT_TIME')
      if ('offsetDays' in modelValue.value && modelValue.value.offsetDays) {
        return `${typeLabel} (${t('admin.cardAction.fixedValue.offsetDays')}: ${modelValue.value.offsetDays})`
      }
      return typeLabel
    }
    case AssignmentTypeEnum.CLEAR_VALUE:
      return t('admin.cardAction.assignment.CLEAR_VALUE')
    case AssignmentTypeEnum.INCREMENT: {
      const typeLabel = t('admin.cardAction.assignment.INCREMENT')
      if ('incrementValue' in modelValue.value) {
        return `${typeLabel}: ${modelValue.value.incrementValue > 0 ? '+' : ''}${modelValue.value.incrementValue}`
      }
      return typeLabel
    }
    default:
      return ''
  }
})

// 判断选项是否选中
function isOptionSelected(option: MainPanelOption): boolean {
  if (option.type === 'assignment') {
    return selectedAssignmentType.value === option.assignmentType && !optionNeedsSubPanel(option)
  } else {
    return selectedSource.value === option.source
  }
}

// 判断选项是否展开
function isOptionExpanded(option: MainPanelOption): boolean {
  if (option.type === 'assignment') {
    return hoveredType.value === option.assignmentType && optionNeedsSubPanel(option)
  } else {
    return hoveredSource.value === option.source
  }
}

// 是否需要子面板（用于显示展开箭头）
function optionNeedsSubPanel(option: MainPanelOption): boolean {
  if (option.type === 'source') {
    // 如果引用来源本身可以直接选择，则不需要强制展开子面板（但仍可展开选择其他字段）
    // 这里返回 true 是因为即使可以直接选择，用户也可以选择展开选择其他字段
    return true
  }
  // 用户输入和清空值不需要子面板
  return option.assignmentType !== AssignmentTypeEnum.CLEAR_VALUE &&
         option.assignmentType !== AssignmentTypeEnum.USER_INPUT
}

function handleDropdownVisibleChange(visible: boolean) {
  dropdownVisible.value = visible
  if (!visible) {
    hoveredType.value = null
    hoveredSource.value = null
    cascadeLevels.value = []
  }
}

function handleOptionHover(option: MainPanelOption) {
  if (hoverTimer) clearTimeout(hoverTimer)
  hoverTimer = setTimeout(() => {
    if (option.type === 'assignment') {
      hoveredType.value = option.assignmentType
      hoveredSource.value = null
      cascadeLevels.value = []

      // 初始化临时值
      if (option.assignmentType === AssignmentTypeEnum.FIXED_VALUE && 'value' in modelValue.value && modelValue.value.value) {
        const val = modelValue.value.value
        if (val.valueType === FixedValueTypeEnum.TEXT) textValue.value = val.text
        if (val.valueType === FixedValueTypeEnum.NUMBER) numberValue.value = val.number
        if (val.valueType === FixedValueTypeEnum.DATE) {
          dateMode.value = val.mode
          absoluteDate.value = val.absoluteDate || ''
          offsetDays.value = val.offsetDays || 0
        }
        if (val.valueType === FixedValueTypeEnum.ENUM) enumValueIds.value = [...val.enumValueIds]
      }
      if (option.assignmentType === AssignmentTypeEnum.CURRENT_TIME && 'offsetDays' in modelValue.value) {
        currentTimeOffsetDays.value = modelValue.value.offsetDays || 0
      }
      if (option.assignmentType === AssignmentTypeEnum.INCREMENT && 'incrementValue' in modelValue.value) {
        incrementValue.value = modelValue.value.incrementValue
        allowNegative.value = modelValue.value.allowNegative || false
      }
    } else {
      hoveredType.value = null
      hoveredSource.value = option.source
      // 加载第一层字段
      loadFirstLevelFields(option.source)
    }
  }, 150)
}

function handleOptionLeave() {
  if (hoverTimer) clearTimeout(hoverTimer)
}

function handleOptionClick(option: MainPanelOption) {
  if (option.type === 'assignment' && option.assignmentType === AssignmentTypeEnum.CLEAR_VALUE) {
    modelValue.value = {
      fieldId: modelValue.value.fieldId,
      assignmentType: AssignmentTypeEnum.CLEAR_VALUE,
    }
    dropdownVisible.value = false
  } else if (option.type === 'assignment' && option.assignmentType === AssignmentTypeEnum.USER_INPUT) {
    // 用户输入类型直接选择，不需要子面板
    modelValue.value = {
      fieldId: modelValue.value.fieldId,
      assignmentType: AssignmentTypeEnum.USER_INPUT,
      required: true,
    } as any
    dropdownVisible.value = false
  } else if (option.type === 'source' && isSourceSelfSelectableCheck(option.source)) {
    // 当引用来源本身可以直接选择时，点击主面板选项直接完成选择
    modelValue.value = {
      fieldId: modelValue.value.fieldId,
      assignmentType: AssignmentTypeEnum.REFERENCE_FIELD,
      source: option.source,
      sourceFieldId: undefined,
      path: undefined,
    } as any
    dropdownVisible.value = false
  }
}

/**
 * 判断字段是否处于 hover 状态
 */
function isFieldHovered(fieldId: string, levelIndex: number): boolean {
  const level = cascadeLevels.value[levelIndex]
  return level?.hoveredFieldId === fieldId
}

/**
 * 处理字段 hover
 */
function handleFieldHover(field: FieldOption & { matched: boolean }, levelIndex: number) {
  // 如果可以展开，则加载下一层
  if (canExpand(field) && levelIndex < MAX_CASCADE_DEPTH - 1) {
    if (hoverTimer) clearTimeout(hoverTimer)
    hoverTimer = setTimeout(() => {
      loadNextLevelFields(levelIndex, field.id!)
    }, 200)
  } else {
    // 不能展开时，只设置 hover 状态，不加载下一层
    if (cascadeLevels.value[levelIndex]) {
      cascadeLevels.value[levelIndex].hoveredFieldId = field.id!
    }
    // 移除后续层级
    cascadeLevels.value = cascadeLevels.value.slice(0, levelIndex + 1)
  }
}

/**
 * 处理字段离开
 */
function handleFieldLeave(_levelIndex: number) {
  if (hoverTimer) clearTimeout(hoverTimer)
}

/**
 * 处理字段选择
 */
function handleFieldSelect(field: FieldOption & { matched: boolean }, levelIndex: number) {
  if (!field.matched) return // 不匹配的不能选中

  // 构建路径
  const path: string[] = []
  for (let i = 0; i <= levelIndex; i++) {
    const level = cascadeLevels.value[i]
    if (i < levelIndex && level?.hoveredFieldId) {
      path.push(level.hoveredFieldId)
    } else if (i === levelIndex) {
      path.push(field.id!)
    }
  }

  modelValue.value = {
    fieldId: modelValue.value.fieldId,
    assignmentType: AssignmentTypeEnum.REFERENCE_FIELD,
    source: hoveredSource.value!,
    sourceFieldId: path[path.length - 1],
    path: path.length > 1 ? { linkNodes: path.slice(0, -1) } : undefined,
  } as any

  dropdownVisible.value = false
}

function toggleEnumValue(value: string) {
  const index = enumValueIds.value.indexOf(value)
  if (index >= 0) {
    enumValueIds.value.splice(index, 1)
  } else {
    enumValueIds.value.push(value)
  }
}

function handleFixedValueChange() {
  // 临时保存，等点击确认后才提交
}

function confirmFixedValue() {
  let value: FixedValue
  switch (valueType.value) {
    case FixedValueTypeEnum.TEXT:
      value = { valueType: FixedValueTypeEnum.TEXT, text: textValue.value }
      break
    case FixedValueTypeEnum.NUMBER:
      value = { valueType: FixedValueTypeEnum.NUMBER, number: numberValue.value }
      break
    case FixedValueTypeEnum.DATE:
      value = {
        valueType: FixedValueTypeEnum.DATE,
        mode: dateMode.value,
        absoluteDate: dateMode.value === DateMode.ABSOLUTE ? absoluteDate.value : undefined,
        offsetDays: dateMode.value === DateMode.RELATIVE ? offsetDays.value : undefined,
      }
      break
    case FixedValueTypeEnum.ENUM:
      value = { valueType: FixedValueTypeEnum.ENUM, enumValueIds: [...enumValueIds.value] }
      break
    default:
      value = { valueType: FixedValueTypeEnum.TEXT, text: '' }
  }

  modelValue.value = {
    fieldId: modelValue.value.fieldId,
    assignmentType: AssignmentTypeEnum.FIXED_VALUE,
    value,
  }
  dropdownVisible.value = false
}

function confirmCurrentTime() {
  modelValue.value = {
    fieldId: modelValue.value.fieldId,
    assignmentType: AssignmentTypeEnum.CURRENT_TIME,
    offsetDays: currentTimeOffsetDays.value,
  }
  dropdownVisible.value = false
}

function confirmIncrement() {
  modelValue.value = {
    fieldId: modelValue.value.fieldId,
    assignmentType: AssignmentTypeEnum.INCREMENT,
    incrementValue: incrementValue.value,
    allowNegative: allowNegative.value,
  }
  dropdownVisible.value = false
}

// 监听目标字段变化，重置级联面板
watch(() => props.targetField, () => {
  cascadeLevels.value = []
})

// 监听 modelValue 和字段列表变化，加载路径字段名称
// 合并为一个 watcher，确保任一依赖变化时都能正确加载
watch(
  [
    () => modelValue.value,
    () => props.memberFields?.length ?? 0,
    () => props.referenceFields?.length ?? 0,
  ],
  () => {
    loadSelectedPathFieldNames()
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.assignment-value-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 200px;
  height: 32px;
  padding: 0 12px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--color-primary-light-3);
  }

  &.is-focus {
    border-color: var(--color-primary);
  }

  .value-text {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--color-text-1);
  }

  .placeholder {
    color: var(--color-text-3);
  }

  .trigger-icon {
    margin-left: 8px;
    color: var(--color-text-3);
    transition: transform 0.2s;

    &.is-open {
      transform: rotate(180deg);
    }
  }
}

.assignment-dropdown {
  background: var(--color-bg-popup);
  border-radius: 4px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.panels-container {
  display: flex;
}

.main-panel {
  min-width: 160px;
  padding: 4px 0;
  border-right: 1px solid var(--color-border);
}

.assignment-item {
  display: flex;
  align-items: center;
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

  .item-icon {
    margin-right: 8px;
    color: var(--color-text-3);
  }

  .item-text {
    flex: 1;
  }

  .expand-icon {
    margin-left: 8px;
    color: var(--color-text-3);
  }
}

.sub-panel {
  min-width: 200px;
  max-width: 280px;
  border-right: 1px solid var(--color-border);

  &:last-child {
    border-right: none;
  }
}

.panel-content {
  padding: 12px;
}

.panel-footer {
  padding: 8px 12px;
  border-top: 1px solid var(--color-border);
  text-align: right;
}

.field-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover:not(.disabled) {
    background: var(--color-fill-2);
  }

  &.hovered:not(.disabled) {
    background: var(--color-fill-2);
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.5;

    &:hover {
      background: transparent;
    }
  }

  &.can-expand .expand-icon {
    visibility: visible;
  }

  .field-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .expand-icon {
    margin-left: 8px;
    color: var(--color-text-3);
    visibility: hidden;
  }
}

.field-panel {
  padding: 4px 0;
}

.field-list {
  max-height: 300px;
  overflow-y: auto;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.empty-text {
  padding: 12px;
  text-align: center;
  color: var(--color-text-3);
}

.date-value-editor {
  display: flex;
  flex-direction: column;
}

.enum-options {
  max-height: 200px;
  overflow-y: auto;
}

.enum-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  cursor: pointer;

  &:hover {
    background: var(--color-fill-1);
  }

  &.selected {
    color: var(--color-primary);
  }
}

.offset-input,
.increment-input {
  display: flex;
  align-items: center;
}
</style>
