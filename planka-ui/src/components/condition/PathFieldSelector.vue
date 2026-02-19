<template>
  <a-trigger
    trigger="click"
    :popup-visible="dropdownVisible"
    position="bl"
    auto-fit-popup-min-width
    :popup-style="dropdownStyle"
    @popup-visible-change="handleDropdownVisibleChange"
  >
    <slot>
      <!-- 触发器：模拟 a-select 的外观 -->
      <div
        ref="triggerRef"
        class="field-select-trigger"
        :class="{ 'is-focus': dropdownVisible, 'has-value': displayText }"
        :title="displayText"
      >
        <input
          v-model="searchKeyword"
          class="search-input"
          :placeholder="displayText || actualPlaceholder"
        />
        <icon-search v-if="!displayText" class="trigger-icon search" />

        <icon-down v-else class="trigger-icon arrow" :class="{ 'is-open': dropdownVisible }" />
      </div>
    </slot>

    <template #content>
      <div class="cascader-dropdown">
        <!-- 多级面板 -->
        <!-- 多级面板 -->
        <div class="cascader-panels" :class="{ 'single-panel': panels.length === 1 }">
          <div
            v-for="(panel, panelIndex) in panels"
            :key="panelIndex"
            class="cascader-panel"
            @mouseenter="handlePanelMouseEnter(panelIndex)"
            @mouseleave="handlePanelMouseLeave(panelIndex)"
          >
            <!-- 面板搜索（只在关联展开的子面板显示） -->
            <div
              v-if="panelIndex > 0 || customTrigger"
              class="panel-header"
              @mouseenter="handlePanelMouseEnter(panelIndex)"
            >
              <div class="panel-search">
                <input
                  v-model="panel.searchKeyword"
                  class="panel-search-input"
                  :placeholder="t('common.condition.searchField')"
                  @click.stop
                  @focus="handlePanelMouseEnter(panelIndex)"
                />
                <icon-search class="panel-search-icon" />
              </div>
            </div>

            <!-- 加载状态 -->
            <div v-if="panel.loading" class="panel-loading">
              <a-spin />
            </div>

            <!-- 字段列表 -->
            <div v-else class="panel-content">
              <!-- 系统字段（所有级别都显示） -->
              <div v-if="getFilteredSystemFields(panelIndex).length > 0" class="field-group">
                <div class="group-label">{{ t('common.condition.systemFields') }}</div>
                <div
                  v-for="field in getFilteredSystemFields(panelIndex)"
                  :key="field.id"
                  class="field-item"
                  :class="{ selected: isFieldSelected(panelIndex, field.id) }"
                  @click="handleFieldClick(panelIndex, field.id, t(field.nameKey), 'system')"
                >
                  <component :is="field.icon" class="field-icon" />
                  <span class="field-name">{{ t(field.nameKey) }}</span>
                </div>
              </div>

              <!-- 自定义字段（非关联） -->
              <div v-if="getFilteredNonLinkFields(panel.fields, panelIndex).length > 0" class="field-group">
                <div class="group-label">{{ t('common.condition.customFields') }}</div>
                <div
                  v-for="field in getFilteredNonLinkFields(panel.fields, panelIndex)"
                  :key="getFieldId(field)"
                  class="field-item"
                  :class="{ selected: isFieldSelected(panelIndex, getFieldId(field)) }"
                  @click="handleFieldClick(panelIndex, getFieldId(field), field.name, 'custom', field)"
                >
                  <FieldTypeIcon :field-type="getFieldIconType(field)" size="small" />
                  <span class="field-name">{{ field.name }}</span>
                </div>
              </div>

              <!-- 关联字段 -->
              <div v-if="getFilteredLinkFields(panel.fields, panelIndex).length > 0" class="field-group">
                <div class="group-label">{{ t('common.condition.linkFields') }}</div>
                <div
                  v-for="field in getFilteredLinkFields(panel.fields, panelIndex)"
                  :key="getFieldId(field)"
                  class="field-item link-field"
                  :class="{
                    selected: isFieldSelected(panelIndex, getFieldId(field)),
                    expanded: isLinkExpanded(panelIndex, getFieldId(field)),
                  }"
                  @mouseenter="handleLinkFieldHover(panelIndex, field)"
                  @mouseleave="handleLinkFieldLeave(panelIndex, field)"
                  @click="handleLinkFieldClick(panelIndex, field)"
                >
                  <FieldTypeIcon :field-type="getFieldIconType(field)" size="small" />
                  <span class="field-name">{{ field.name }}</span>
                  <icon-right v-if="panelIndex < maxDepth - 1" class="expand-icon" />
                </div>
              </div>

              <!-- 空状态 -->
              <a-empty
                v-if="isEmptyPanel(panel, panelIndex)"
                :description="panelIndex === 0 && searchKeyword ? t('common.condition.noMatchingFields') : t('common.condition.noAvailableFields')"
                :image-style="{ width: '48px', height: '48px' }"
              />
            </div>
          </div>
        </div>
      </div>
    </template>
  </a-trigger>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconDown, IconRight, IconSearch } from '@arco-design/web-vue/es/icon'
import type { FieldOption } from '@/types/field-option'
import type { LinkTypeVO } from '@/types/link-type'
import type { Path } from '@/types/condition'
import type { FieldConfig } from '@/types/field-config'
import { SYSTEM_DATE_FIELD_PREFIX } from '@/types/condition'
import { parseLinkFieldId } from '@/utils/link-field-utils'
import FieldTypeIcon from '@/components/field/FieldTypeIcon.vue'
import { SYSTEM_FIELDS, FIELD_TYPE_TO_NODE_TYPE, FIELD_SUMMARY_TYPE_TO_NODE_TYPE } from './constants'
import { CONDITION_DISPLAY_INFO_KEY, type ConditionDisplayInfoContext } from './useConditionDisplayInfo'

const { t } = useI18n()

/**
 * 字段选择结果
 */
export interface FieldSelectionResult {
  /** 路径（关联链路） */
  path?: Path
  /** 字段ID（系统字段或自定义字段） */
  fieldId?: string
  /** 节点类型 */
  nodeType: string
  /** 是否为关联字段直接过滤 */
  isLinkFilter?: boolean
  /** 关联字段ID（关联字段直接过滤时使用），格式为 "{linkTypeId}:{SOURCE|TARGET}" */
  linkFieldId?: string
}

/**
 * 面板数据
 */
interface PanelData {
  title: string
  fields: FieldOption[]
  loading: boolean
  selectedFieldId?: string
  expandedLinkFieldId?: string
  /** 当前面板对应的 linkFieldId（从上一级展开而来），格式为 "{linkTypeId}:{SOURCE|TARGET}" */
  linkFieldId?: string
  /** 面板搜索关键词 */
  searchKeyword?: string
}

/**
 * 系统字段列表（使用共享常量）
 */
const systemFields = SYSTEM_FIELDS

/**
 * 注入条件显示信息上下文
 */
const displayInfoContext = inject<ConditionDisplayInfoContext | undefined>(
  CONDITION_DISPLAY_INFO_KEY,
  undefined
)

/**
 * Props定义
 */
const props = withDefaults(
  defineProps<{
    /** 可用字段列表 */
    availableFields?: FieldOption[]

    /** 关联类型列表（用于获取目标卡片类型） */
    linkTypes?: LinkTypeVO[]

    /** 根卡片类型名称 */
    rootCardTypeName?: string

    /** 最大路径深度（默认3级） */
    maxDepth?: number

    /** 占位符 */
    placeholder?: string

    /** 根据关联字段ID获取级联字段的函数 */
    fetchFieldsByLinkFieldId?: (linkFieldId: string) => Promise<FieldOption[]>

    /** 悬停展开延迟（毫秒） */
    hoverDelay?: number

    /** 初始字段ID（用于回显） */
    initialFieldId?: string

    /** 初始路径（用于多级关联回显） */
    initialPath?: Path

    /** 是否使用自定义触发器（如果为 true，则启用首层面板搜索） */
    customTrigger?: boolean

    /** 允许显示的系统字段ID列表（如果不传则显示所有） */
    allowedSystemFieldIds?: string[]
  }>(),
  {
    availableFields: () => [],
    linkTypes: () => [],
    rootCardTypeName: '',
    maxDepth: 3,
    placeholder: '',
    fetchFieldsByLinkFieldId: undefined,
    hoverDelay: 300,
    initialFieldId: '',
    initialPath: undefined,
    customTrigger: false,
  }
)

/**
 * 计算实际使用的占位符
 */
const actualPlaceholder = computed(() => {
  return props.placeholder || t('common.condition.selectField')
})

/**
 * Emits定义
 */
const emit = defineEmits<{
  /** 选择了一个字段（包含路径信息） */
  select: [result: FieldSelectionResult]
}>()

/**
 * 下拉框可见状态
 */
const dropdownVisible = ref(false)

/**
 * 搜索关键词
 */
const searchKeyword = ref('')

/**
 * 面板数据列表
 */
const panels = ref<PanelData[]>([])

/**
 * 悬停定时器
 */
let hoverTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 关闭面板定时器
 */
let closeTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 当前鼠标所在的面板索引（-1 表示不在任何面板上）
 */
let currentHoverPanelIndex = -1

/**
 * 正在等待关闭的面板索引（-1 表示没有面板等待关闭）
 */
let closingPanelIndex = -1

/**
 * 组件卸载时清理定时器
 */
onUnmounted(() => {
  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
})

/** 触发器引用 */
const triggerRef = ref<HTMLElement | null>(null)

/** 下拉框样式 */
const dropdownStyle = computed(() => {
  if (!triggerRef.value || !dropdownVisible.value) return {}
  const width = triggerRef.value.offsetWidth
  
  return {
    '--trigger-width': `${width}px`,
  }
})


/**
 * 显示文本
 */
const displayText = ref('')

/**
 * 获取字段的图标类型（FieldOption.fieldType）
 */
function getFieldIconType(field: FieldOption): string {
  return field.fieldType || 'SINGLE_LINE_TEXT'
}

/**
 * 获取字段的ID（FieldOption.id）
 */
function getFieldId(field: FieldOption): string {
  return field.id || ''
}

/**
 * 根据字段ID获取字段名称（用于回显）
 */
function getFieldNameById(fieldId: string, fields?: FieldOption[]): string {
  if (!fieldId) return ''

  // 先查找系统字段
  const systemField = systemFields.find((f) => f.id === fieldId)
  if (systemField) {
    return t(systemField.nameKey)
  }

  // 优先从 displayInfo 中获取
  if (displayInfoContext) {
    const cachedName = displayInfoContext.getFieldName(fieldId)
    if (cachedName && cachedName !== fieldId) {
      return cachedName
    }
  }

  // 使用传入的字段列表或默认使用 props.availableFields
  const searchFields = fields || props.availableFields

  // 使用 getFieldId 兼容查找
  const foundField = searchFields.find((f) => getFieldId(f) === fieldId)
  if (foundField) {
    return foundField.name
  }

  return ''
}

/**
 * 根据 linkFieldId 获取关联字段名称
 * linkFieldId 格式为 "{linkTypeId}:{SOURCE|TARGET}"
 */
function getLinkFieldNameByLinkFieldId(linkFieldId: string): string {
  // 优先从 displayInfo 中获取
  if (displayInfoContext) {
    const cachedName = displayInfoContext.getLinkFieldName(linkFieldId)
    if (cachedName && cachedName !== linkFieldId) {
      return cachedName
    }
  }

  // 解析 linkFieldId 获取 linkTypeId 和 position
  let linkTypeId: string
  let position: string
  try {
    const parsed = parseLinkFieldId(linkFieldId)
    linkTypeId = parsed.linkTypeId
    position = parsed.position
  } catch {
    return ''
  }

  // 从 availableFields 中查找（关联字段的 id 格式为 "{linkTypeId}:{SOURCE|TARGET}"）
  const linkField = props.availableFields.find((f) => {
    const fid = getFieldId(f)
    return fid === linkFieldId
  })
  if (linkField) {
    return linkField.name
  }

  // 从 linkTypes 中查找（根据 position 决定显示源端还是目标端名称）
  const linkType = props.linkTypes.find((lt) => lt.id === linkTypeId)
  if (linkType) {
    if (position === 'SOURCE') {
      return linkType.sourceName || linkType.name
    } else {
      return linkType.targetName || linkType.name
    }
  }

  return ''
}

/**
 * 判断是否为 linkFieldId 格式
 * linkFieldId 格式为 "{linkTypeId}:{SOURCE|TARGET}"
 */
function isLinkFieldId(id: string): boolean {
  return id.includes(':SOURCE') || id.includes(':TARGET')
}

/**
 * 构建完整的显示文本（包含路径）
 */
function buildInitialDisplayText(): string {
  const parts: string[] = []

  // 添加路径部分（使用 linkFieldId 查找名称）
  if (props.initialPath && props.initialPath.linkNodes.length > 0) {
    for (const linkFieldId of props.initialPath.linkNodes) {
      const linkName = getLinkFieldNameByLinkFieldId(linkFieldId)
      if (linkName) {
        parts.push(linkName)
      }
    }
  }

  // 添加最终字段名称
  if (props.initialFieldId) {
    // 判断 initialFieldId 是否为 linkFieldId 格式（用于 LINK 条件类型）
    let fieldName: string
    if (isLinkFieldId(props.initialFieldId)) {
      fieldName = getLinkFieldNameByLinkFieldId(props.initialFieldId)
    } else {
      fieldName = getFieldNameById(props.initialFieldId)
    }
    if (fieldName) {
      parts.push(fieldName)
    }
  }

  return parts.join('.')
}

/**
 * 监听初始字段ID和路径变化，更新显示文本
 */
watch(
  [() => props.initialFieldId, () => props.initialPath],
  () => {
    if ((props.initialFieldId || props.initialPath) && !displayText.value) {
      const text = buildInitialDisplayText()
      if (text) {
        displayText.value = text
      }
    }
  },
  { immediate: true }
)

/**
 * 监听可用字段变化，重新计算显示文本（字段列表加载完成后）
 */
watch(
  () => props.availableFields,
  () => {
    if ((props.initialFieldId || props.initialPath) && !displayText.value) {
      const text = buildInitialDisplayText()
      if (text) {
        displayText.value = text
      }
    }
  }
)

/**
 * 监听 displayInfo 加载完成，重新计算显示文本
 * 因为 displayInfo 是异步加载的，初始 watch 执行时可能还没有数据
 */
watch(
  () => displayInfoContext?.displayInfo.value,
  () => {
    if (!props.initialFieldId && !props.initialPath) return

    const text = buildInitialDisplayText()
    if (!text) return

    // 只有当新计算的文本比当前文本更完整时才更新
    const newPartCount = text.split('.').length
    const currentPartCount = displayText.value?.split('.').length || 0
    if (newPartCount > currentPartCount) {
      displayText.value = text
    }
  },
  { deep: true }
)

/**
 * 初始化面板
 */
function initPanels() {
  panels.value = [
    {
      title: props.rootCardTypeName || t('common.condition.selectField'),
      fields: props.availableFields,
      loading: false,
    },
  ]
}

/**
 * 监听可用字段变化
 */
watch(
  () => props.availableFields,
  () => {
    if (panels.value.length > 0 && panels.value[0]) {
      panels.value[0].fields = props.availableFields
    }
  },
  { immediate: true }
)

/**
 * 监听下拉框打开
 */
watch(
  () => dropdownVisible.value,
  (visible) => {
    if (visible) {
      initPanels()
      searchKeyword.value = ''
    }
  }
)

/**
 * 处理下拉框可见状态变化
 */
function handleDropdownVisibleChange(visible: boolean) {
  dropdownVisible.value = visible
}





/**
 * 获取面板的搜索关键词
 */
function getPanelSearchKeyword(panelIndex: number): string {
  if (panelIndex === 0 && !props.customTrigger) {
    return searchKeyword.value
  }
  return panels.value[panelIndex]?.searchKeyword || ''
}

/**
 * 获取过滤后的系统字段
 */
function getFilteredSystemFields(panelIndex: number) {
  let fields = systemFields
  if (props.allowedSystemFieldIds) {
    fields = fields.filter(f => props.allowedSystemFieldIds!.includes(f.id))
  }

  const keyword = getPanelSearchKeyword(panelIndex)
  if (!keyword) {
    return fields
  }
  const lowerKeyword = keyword.toLowerCase()
  return fields.filter((f) => t(f.nameKey).toLowerCase().includes(lowerKeyword))
}

/**
 * 判断字段是否为关联字段（FieldOption.fieldType === 'LINK'）
 */
function isLinkField(field: FieldOption): boolean {
  return field.fieldType === 'LINK'
}

/**
 * 获取非关联字段
 */
function getNonLinkFields(fields: FieldOption[]): FieldOption[] {
  return fields.filter((f) => !isLinkField(f))
}

/**
 * 获取过滤后的非关联字段
 */
function getFilteredNonLinkFields(fields: FieldOption[], panelIndex: number): FieldOption[] {
  const nonLinkFields = getNonLinkFields(fields)
  const keyword = getPanelSearchKeyword(panelIndex)
  if (!keyword) {
    return nonLinkFields
  }
  const lowerKeyword = keyword.toLowerCase()
  return nonLinkFields.filter((f) => f.name.toLowerCase().includes(lowerKeyword))
}

/**
 * 获取关联字段
 */
function getLinkFields(fields: FieldOption[]): FieldOption[] {
  return fields.filter((f) => isLinkField(f))
}

/**
 * 获取过滤后的关联字段
 */
function getFilteredLinkFields(fields: FieldOption[], panelIndex: number): FieldOption[] {
  const linkFields = getLinkFields(fields)
  const keyword = getPanelSearchKeyword(panelIndex)
  if (!keyword) {
    return linkFields
  }
  const lowerKeyword = keyword.toLowerCase()
  return linkFields.filter((f) => f.name.toLowerCase().includes(lowerKeyword))
}

/**
 * 判断面板是否为空
 */
function isEmptyPanel(panel: PanelData, panelIndex: number): boolean {
  const hasSystemFields = getFilteredSystemFields(panelIndex).length > 0
  const hasNonLinkFields = getFilteredNonLinkFields(panel.fields, panelIndex).length > 0
  const hasLinkFields = getFilteredLinkFields(panel.fields, panelIndex).length > 0
  return !hasSystemFields && !hasNonLinkFields && !hasLinkFields
}

/**
 * 判断字段是否被选中
 */
function isFieldSelected(panelIndex: number, fieldId: string): boolean {
  return panels.value[panelIndex]?.selectedFieldId === fieldId
}

/**
 * 判断关联字段是否已展开
 */
function isLinkExpanded(panelIndex: number, fieldId: string): boolean {
  return panels.value[panelIndex]?.expandedLinkFieldId === fieldId
}

/**
 * 处理普通字段点击
 */
function handleFieldClick(
  panelIndex: number,
  fieldId: string,
  fieldName: string,
  fieldType: 'system' | 'custom',
  field?: FieldOption
) {
  // 更新选中状态
  const panel = panels.value[panelIndex]
  if (panel) {
    panel.selectedFieldId = fieldId
    panel.expandedLinkFieldId = undefined
  }

  // 移除后续面板
  panels.value = panels.value.slice(0, panelIndex + 1)

  // 构建路径
  const path = buildPath(panelIndex)

  // 构建结果
  const result: FieldSelectionResult = {
    fieldId,
    nodeType: getNodeTypeByFieldId(fieldId, field),
    path: path.linkNodes.length > 0 ? path : undefined,
  }

  // 更新显示文本
  displayText.value = buildDisplayText(panelIndex, fieldName)

  // 更新 displayInfo 缓存
  if (displayInfoContext) {
    // 缓存字段名称
    displayInfoContext.addFieldName(fieldId, fieldName)

    // 如果是自定义字段且有枚举选项，缓存枚举选项
    if (fieldType === 'custom' && field && 'enumOptions' in field && (field as any).enumOptions) {
      displayInfoContext.addEnumOptions(fieldId, (field as any).enumOptions)
    }

    // 缓存路径中的关联字段名称
    for (let i = 1; i <= panelIndex; i++) {
      const panel = panels.value[i]
      if (panel?.linkFieldId && panel.title) {
        displayInfoContext.addLinkFieldName(panel.linkFieldId, panel.title)
      }
    }
  }

  // 发送事件
  emit('select', result)

  // 关闭下拉框
  dropdownVisible.value = false
}

/**
 * 处理关联字段悬停
 */
function handleLinkFieldHover(panelIndex: number, field: FieldOption) {
  // 清除之前的定时器
  if (hoverTimer) {
    clearTimeout(hoverTimer)
  }

  // 检查是否已达到最大深度
  if (panelIndex >= props.maxDepth - 1) {
    return
  }

  // 设置延迟展开
  hoverTimer = setTimeout(() => {
    expandLinkField(panelIndex, field)
  }, props.hoverDelay)
}

/**
 * 处理关联字段鼠标离开
 */
function handleLinkFieldLeave(panelIndex: number, _field: FieldOption) {
  // 清除悬停定时器（如果面板还未展开）
  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }

  // 设置延迟关闭子面板（如果鼠标没有进入子面板，则关闭）
  closeTimer = setTimeout(() => {
    // 只有当鼠标不在任何子面板上时才关闭
    if (currentHoverPanelIndex <= panelIndex) {
      // 关闭当前面板之后的所有子面板
      if (panels.value.length > panelIndex + 1) {
        panels.value = panels.value.slice(0, panelIndex + 1)
        // 清除当前面板的展开状态
        if (panels.value[panelIndex]) {
          panels.value[panelIndex].expandedLinkFieldId = undefined
        }
      }
    }
  }, 100)
}

/**
 * 处理面板鼠标进入
 */
function handlePanelMouseEnter(panelIndex: number) {
  // 记录当前鼠标所在面板
  currentHoverPanelIndex = panelIndex

  // 只有当进入的面板索引 >= 正在关闭的面板索引时，才取消关闭
  // 这样当鼠标从子面板移动到父面板时，子面板仍会被关闭
  if (closeTimer && panelIndex >= closingPanelIndex) {
    clearTimeout(closeTimer)
    closeTimer = null
    closingPanelIndex = -1
  }
}

/**
 * 处理面板鼠标离开
 */
function handlePanelMouseLeave(panelIndex: number) {
  // 重置当前鼠标所在面板
  currentHoverPanelIndex = -1

  // 记录正在等待关闭的面板索引
  closingPanelIndex = panelIndex === 0 ? 0 : panelIndex
  closeTimer = setTimeout(() => {
    // 如果鼠标不在任何面板上，关闭整个下拉框
    if (currentHoverPanelIndex < 0) {
      dropdownVisible.value = false
    }
    // 如果鼠标在父面板上但不在当前面板或其子面板上，只关闭当前面板及其后续面板
    else if (panelIndex > 0 && currentHoverPanelIndex < panelIndex) {
      if (panels.value.length > panelIndex) {
        panels.value = panels.value.slice(0, panelIndex)
        // 清除父面板的展开状态
        const parentPanel = panels.value[panelIndex - 1]
        if (parentPanel) {
          parentPanel.expandedLinkFieldId = undefined
        }
      }
    }
    // 重置正在关闭的面板索引
    closingPanelIndex = -1
  }, 100)
}

/**
 * 处理关联字段点击（直接过滤）
 */
function handleLinkFieldClick(panelIndex: number, field: FieldOption) {
  // 清除悬停定时器
  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }

  const fieldId = getFieldId(field)

  // 更新选中状态
  const linkPanel = panels.value[panelIndex]
  if (linkPanel) {
    linkPanel.selectedFieldId = fieldId
    linkPanel.expandedLinkFieldId = undefined
  }

  // 移除后续面板
  panels.value = panels.value.slice(0, panelIndex + 1)

  // 构建路径
  const path = buildPath(panelIndex)

  // fieldId 本身就是 linkFieldId 格式：{linkTypeId}:{SOURCE|TARGET}
  const linkFieldId = fieldId

  // 构建结果
  const result: FieldSelectionResult = {
    nodeType: 'LINK',
    isLinkFilter: true,
    linkFieldId,
    path: path.linkNodes.length > 0 ? path : undefined,
  }

  // 更新显示文本
  displayText.value = buildDisplayText(panelIndex, field.name)

  // 更新 displayInfo 缓存
  if (displayInfoContext) {
    // 缓存当前关联字段名称
    displayInfoContext.addLinkFieldName(linkFieldId, field.name)
    // 缓存路径中的关联字段名称
    for (let i = 1; i <= panelIndex; i++) {
      const panel = panels.value[i]
      if (panel?.linkFieldId && panel.title) {
        displayInfoContext.addLinkFieldName(panel.linkFieldId, panel.title)
      }
    }
  }

  // 发送事件
  emit('select', result)

  // 关闭下拉框
  dropdownVisible.value = false
}

/**
 * 展开关联字段
 */
async function expandLinkField(panelIndex: number, field: FieldOption) {
  const fieldId = getFieldId(field)
  console.log('[PathFieldSelector] expandLinkField called', { panelIndex, field: field.name, fieldId })

  // 如果已经展开，不重复处理
  if (panels.value[panelIndex]?.expandedLinkFieldId === fieldId) {
    console.log('[PathFieldSelector] already expanded, skipping')
    return
  }

  // 检查是否提供了获取级联字段的函数
  if (!props.fetchFieldsByLinkFieldId) {
    console.log('[PathFieldSelector] fetchFieldsByLinkFieldId not provided')
    return
  }

  // 标记展开
  if (panels.value[panelIndex]) {
    panels.value[panelIndex].expandedLinkFieldId = fieldId
    panels.value[panelIndex].selectedFieldId = undefined
  }

  // 移除后续面板
  panels.value = panels.value.slice(0, panelIndex + 1)

  // 创建新的 linkFieldId（fieldId 本身就是 linkFieldId 格式：{linkTypeId}:{SOURCE|TARGET}）
  const newLinkFieldId = fieldId

  // 创建新面板
  const newPanel: PanelData = {
    title: field.name,
    fields: [],
    loading: true,
    linkFieldId: newLinkFieldId,
  }
  panels.value.push(newPanel)

  // 加载关联字段的级联选项
  try {
    console.log('[PathFieldSelector] fetching fields for linkFieldId:', fieldId)
    const fields = await props.fetchFieldsByLinkFieldId(fieldId)
    console.log('[PathFieldSelector] fetched fields:', fields.length)

    // 检查面板是否还存在（可能用户已经切换了）
    const targetPanel = panels.value[panelIndex + 1]
    if (targetPanel) {
      targetPanel.fields = fields
      targetPanel.loading = false
    }
  } catch (error) {
    console.error(t('common.condition.loadFieldsFailed'), error)
    const errorPanel = panels.value[panelIndex + 1]
    if (errorPanel) {
      errorPanel.loading = false
    }
  }
}



/**
 * 构建路径
 */
function buildPath(endPanelIndex: number): Path {
  const linkNodes: string[] = []

  for (let i = 1; i <= endPanelIndex; i++) {
    const panel = panels.value[i]
    if (panel?.linkFieldId) {
      linkNodes.push(panel.linkFieldId)
    }
  }

  return { linkNodes }
}

/**
 * 构建显示文本
 */
function buildDisplayText(panelIndex: number, fieldName: string): string {
  const parts: string[] = []

  for (let i = 1; i <= panelIndex; i++) {
    parts.push(panels.value[i]?.title || '')
  }

  parts.push(fieldName)

  return parts.filter(Boolean).join('.')
}

/**
 * 根据字段ID获取节点类型
 */
function getNodeTypeByFieldId(fieldId: string, field?: FieldOption | FieldConfig): string {
  // 系统字段
  const systemField = systemFields.find((f) => f.id === fieldId)
  if (systemField) {
    if (fieldId.startsWith(SYSTEM_DATE_FIELD_PREFIX)) {
      return 'DATE'
    }
    return fieldId
  }

  // 自定义字段
  if (field) {
    // 检查 fieldType (FieldOption 格式)
    if ('fieldType' in field && (field as any).fieldType) {
      const fieldType = (field as any).fieldType as string
      return FIELD_SUMMARY_TYPE_TO_NODE_TYPE[fieldType] || 'TEXT'
    }
    // 检查 schemaSubType (FieldConfig 格式)
    if ('schemaSubType' in field) {
      return FIELD_TYPE_TO_NODE_TYPE[(field as FieldConfig).schemaSubType] || 'TEXT'
    }
  }

  return 'TEXT'
}

</script>

<style scoped lang="scss">
.field-select-trigger {
  width: 100%;
  height: 28px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  background: var(--color-fill-2);
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  box-sizing: border-box;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgb(var(--primary-6));
  }

  &.is-focus {
    border-color: rgb(var(--primary-6));
    box-shadow: 0 0 0 2px rgb(var(--primary-2));
  }

  &.has-value {
    background: var(--color-bg-2);
  }

  .search-input {
    flex: 1;
    flex: 1;
    min-width: 0; /* 允许收缩 */
    height: 100%;
    border: none;
    outline: none;
    background: transparent;
    font-size: 14px;
    color: var(--color-text-1);

    &::placeholder {
      color: var(--color-text-3);
    }
  }

  &.has-value .search-input::placeholder {
    color: var(--color-text-1);
  }

  .trigger-icon {
    flex-shrink: 0;
    margin-left: 4px;
    color: var(--color-text-3);
    font-size: 12px;
    transition: transform 0.2s;

    &.arrow.is-open {
      transform: rotate(180deg);
    }

    &.clear:hover {
      color: var(--color-text-2);
    }
  }
}

.cascader-dropdown {
  background: var(--color-bg-popup);
  border-radius: 4px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.cascader-panels {
  display: flex;
  height: 400px;
  /* 单面板模式：填满容器（实际上因为固定宽度，这里可以不需要特殊处理，但保留无害） */
}

.cascader-panel {
  /* 第一个面板匹配 trigger 宽度，子面板固定宽度 */
  flex: 0 0 var(--trigger-width, 220px);
  width: var(--trigger-width, 220px);

  border-right: 1px solid var(--color-border-2);
  display: flex;
  flex-direction: column;

  &:not(:first-child) {
    flex: 0 0 240px;
    width: 240px;
  }

  &:last-child {
    border-right: none;
  }
}

.panel-header {
  padding: 4px 0 0 0;
  background: var(--color-fill-1);
  border-bottom: 1px solid var(--color-border-2);

  .panel-search {
    display: flex;
    align-items: center;
    background: var(--color-bg-2);
    border: 1px solid var(--color-border-2);
    border-radius: 4px;
    padding: 0 8px;
    height: 26px;

    .panel-search-input {
      flex: 1;
      min-width: 0;
      height: 100%;
      border: none;
      outline: none;
      background: transparent;
      font-size: 13px;
      color: var(--color-text-1);

      &::placeholder {
        color: var(--color-text-3);
      }
    }

    .panel-search-icon {
      flex-shrink: 0;
      font-size: 14px;
      color: var(--color-text-3);
    }
  }
}

.panel-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
  height: 100%;
}

.field-group {
  &:not(:first-child) {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid var(--color-border-1);
  }
}

.group-label {
  padding: 4px 12px;
  font-size: 11px;
  color: var(--color-text-3);
}

.field-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: var(--color-fill-2);
  }

  &.selected {
    background: rgb(var(--primary-1));
    color: rgb(var(--primary-6));
  }

  &.expanded {
    background: var(--color-fill-2);
  }

  .field-icon {
    flex-shrink: 0;
    font-size: 14px;
    color: var(--color-text-3);
  }

  .field-name {
    flex: 1;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &.link-field {
    .expand-icon {
      flex-shrink: 0;
      font-size: 12px;
      color: var(--color-text-3);
    }
  }
}
</style>
