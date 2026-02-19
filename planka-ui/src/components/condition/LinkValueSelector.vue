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
      ref="triggerRef"
      class="link-value-trigger"
      :class="{ 'is-focus': dropdownVisible, 'has-value': hasValue }"
    >
      <!-- 引用值显示 -->
      <template v-if="isReferenceValue">
        <span class="reference-text">{{ displayReferenceText }}</span>
      </template>
      <!-- 静态值显示 -->
      <template v-else-if="hasSelectedCards">
        <div class="selected-cards">
          <a-tag
            v-for="card in displaySelectedCards.slice(0, 3)"
            :key="card.id"
            size="small"
            closable
            @close="removeCard(card.id)"
          >
            {{ card.title }}
          </a-tag>
          <a-tag v-if="displaySelectedCards.length > 3" size="small">
            +{{ displaySelectedCards.length - 3 }}
          </a-tag>
        </div>
      </template>
      <!-- 占位符 -->
      <template v-else>
        <span class="placeholder">{{ placeholder || t('common.linkValue.selectCard') }}</span>
      </template>
      <icon-down class="trigger-icon" :class="{ 'is-open': dropdownVisible }" />
    </div>

    <template #content>
      <div class="link-value-dropdown" :style="dropdownStyle">
        <!-- 面板容器 -->
        <div class="panels-container">
          <!-- 主面板 -->
          <div class="main-panel">
            <!-- 当前用户入口（固定在顶部） -->
            <div class="reference-section">
              <div
                class="reference-item"
                :class="{ expanded: showMemberPanel, selected: isReferenceValue && !hasReferencePath }"
                @mouseenter="handleMemberHover"
                @mouseleave="handleMemberLeave"
                @click="handleMemberClick"
              >
                <icon-user class="item-icon" />
                <span class="item-text">{{ t('common.linkValue.currentUser') }}</span>
                <icon-right class="expand-icon" />
              </div>
            </div>

            <!-- 分隔线 -->
            <div class="divider"></div>

            <!-- 搜索框 -->
            <div class="search-box">
              <input
                v-model="searchKeyword"
                class="search-input"
                :placeholder="t('common.linkValue.searchAndSelectCard')"
                @input="handleSearchInput"
              />
              <icon-search class="search-icon" />
            </div>

            <!-- 卡片列表 -->
            <div class="cards-section">
              <div v-if="loading" class="loading-container">
                <a-spin />
              </div>
              <div v-else-if="displayCards.length === 0" class="empty-container">
                <span class="empty-text">{{ searchKeyword ? t('common.condition.noSearchResult') : t('common.toolbar.searchPlaceholder') }}</span>
              </div>
              <div v-else class="cards-list">
                <div
                  v-for="card in displayCards"
                  :key="card.id"
                  class="card-item"
                  :class="{ selected: isCardSelected(card.id) }"
                  @click="toggleCardSelection(card)"
                >
                  <a-checkbox :model-value="isCardSelected(card.id)" @click.stop />
                  <!-- 只有当 code 存在且与 title 不同时才显示 code -->
                  <span v-if="card.code && card.code !== card.title" class="card-code">{{ card.code }}</span>
                  <span class="card-title">{{ card.title }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 级联面板（支持多层） -->
          <template v-if="showMemberPanel">
            <div
              v-for="(level, levelIndex) in cascadeLevels"
              :key="levelIndex"
              class="member-panel"
              @mouseenter="handleMemberPanelEnter"
              @mouseleave="handleMemberPanelLeave"
            >
              <div v-if="level.loading" class="loading-container">
                <a-spin />
              </div>
              <template v-else>
                <div
                  v-for="field in level.fields"
                  :key="field.id"
                  class="field-item"
                  :class="{
                    selected: isFieldSelected(field.id!, levelIndex),
                    hovered: isFieldHovered(field.id!, levelIndex),
                    disabled: !(field as any).matched,
                  }"
                  @mouseenter="handleFieldHover(field as any, levelIndex)"
                  @mouseleave="handleFieldLeave()"
                  @click="handleFieldSelect(field as any, levelIndex)"
                >
                  <span class="field-name">{{ field.name }}</span>
                  <!-- 只要没到最大层数，都显示展开箭头（包括置灰的） -->
                  <icon-right
                    v-if="levelIndex < MAX_CASCADE_DEPTH - 1"
                    class="expand-icon"
                  />
                </div>
                <div v-if="level.fields.length === 0" class="empty-container">
                  <span class="empty-text">{{ t('common.condition.noAvailableFields') }}</span>
                </div>
              </template>
            </div>
          </template>
        </div>
      </div>
    </template>
  </a-trigger>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconDown, IconRight, IconSearch, IconUser } from '@arco-design/web-vue/es/icon'
import { useOrgStore } from '@/stores/org'
import { linkCardApi } from '@/api/link-card'
import { linkTypeApi } from '@/api/link-type'
import { fieldOptionsApi } from '@/api/field-options'
import { parseLinkFieldId } from '@/utils/link-field-utils'
import type { LinkValue, StaticLinkValue, ReferenceLinkValue } from '@/types/condition'
import type { LinkedCard } from '@/types/card'
import type { FieldOption } from '@/types/field-option'
import { CONDITION_DISPLAY_INFO_KEY, type ConditionDisplayInfoContext } from './useConditionDisplayInfo'

const { t } = useI18n()
const orgStore = useOrgStore()

/** 最大级联层数 */
const MAX_CASCADE_DEPTH = 3

/**
 * 注入条件显示信息上下文
 */
const displayInfoContext = inject<ConditionDisplayInfoContext | undefined>(
  CONDITION_DISPLAY_INFO_KEY,
  undefined
)

/**
 * 卡片信息
 */
export interface CardInfo {
  id: string
  code: string
  title: string
}

const props = withDefaults(
  defineProps<{
    /** 当前值 */
    modelValue?: LinkValue
    /** 关联字段ID */
    linkFieldId: string
    /** 已选卡片信息（回显用） */
    selectedCards?: CardInfo[]
    /** 占位符 */
    placeholder?: string
  }>(),
  {
    modelValue: undefined,
    selectedCards: () => [],
    placeholder: '',
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: LinkValue]
  change: [value: LinkValue, cards: CardInfo[]]
}>()

// ==================== 状态 ====================

const triggerRef = ref<HTMLElement | null>(null)
const dropdownVisible = ref(false)
const searchKeyword = ref('')
const loading = ref(false)
const searchResults = ref<CardInfo[]>([])
const hasLoadedInitial = ref(false)
const hasLoadedTargetCardTypes = ref(false)

// 左侧 linkFieldId 的目标卡片类型 IDs
const targetCardTypeIds = ref<string[]>([])

// 成员属性相关（支持多层级联）
const showMemberPanel = ref(false)
const memberFieldsLoading = ref(false)

/**
 * 级联层级数据
 * 每一层包含：字段列表、当前 hover 的字段 ID、加载状态
 */
interface CascadeLevel {
  fields: FieldOption[]
  hoveredFieldId: string | null
  loading: boolean
  /** 当前层级对应的 linkFieldId（用于加载下一层） */
  parentLinkFieldId: string
}

const cascadeLevels = ref<CascadeLevel[]>([])

// 已选路径的字段名称（用于回显，从 API 加载）
const selectedPathFieldNames = ref<string[]>([])

// 定时器
let debounceTimer: ReturnType<typeof setTimeout> | null = null
let hoverTimer: ReturnType<typeof setTimeout> | null = null
let closeTimer: ReturnType<typeof setTimeout> | null = null

// ==================== 计算属性 ====================

/**
 * 是否为引用值
 */
const isReferenceValue = computed(() => {
  return props.modelValue?.type === 'REFERENCE'
})

/**
 * 是否有引用路径
 */
const hasReferencePath = computed(() => {
  if (!isReferenceValue.value) return false
  const source = (props.modelValue as ReferenceLinkValue)?.source
  return source?.path?.linkNodes && source.path.linkNodes.length > 0
})

/**
 * 引用值显示文本
 */
const displayReferenceText = computed(() => {
  if (!isReferenceValue.value) return ''

  const source = (props.modelValue as ReferenceLinkValue)?.source
  if (!source) return t('common.linkValue.currentUser')

  const linkNodes = source.path?.linkNodes
  if (!linkNodes || linkNodes.length === 0) {
    return t('common.linkValue.currentUser')
  }

  // 优先从 displayInfo 中获取名称
  if (displayInfoContext) {
    const namesFromContext = linkNodes.map(id => {
      const name = displayInfoContext.getLinkFieldName(id)
      return (name && name !== id) ? name : ''
    })

    if (namesFromContext.every(n => n !== '')) {
      return `${t('common.linkValue.currentUser')}.${namesFromContext.join('.')}`
    }
  }

  // 回退到已加载的字段名称
  if (selectedPathFieldNames.value.length > 0) {
    return `${t('common.linkValue.currentUser')}.${selectedPathFieldNames.value.join('.')}`
  }

  return t('common.linkValue.currentUser')
})

/**
 * 是否有选中的卡片
 */
const hasSelectedCards = computed(() => {
  if (props.modelValue?.type !== 'STATIC') return false
  return (props.modelValue as StaticLinkValue).cardIds?.length > 0
})

/**
 * 显示的已选卡片
 */
const displaySelectedCards = computed(() => {
  if (!hasSelectedCards.value) return []
  const cardIds = (props.modelValue as StaticLinkValue).cardIds

  return cardIds.map(id => {
    // 1. 优先从 displayInfoContext 中获取（回显时使用）
    const fromContext = displayInfoContext?.getCardInfo(id)
    if (fromContext) {
      return { id: fromContext.id, code: fromContext.code || '', title: fromContext.title || id }
    }

    // 2. 其次从 props.selectedCards 中获取
    const fromProps = props.selectedCards.find(c => c.id === id)
    if (fromProps) return fromProps

    // 3. 最后从搜索结果中获取
    const fromSearch = searchResults.value.find(c => c.id === id)
    if (fromSearch) return fromSearch

    // 4. 都没找到，返回一个只有 id 的占位对象
    return { id, code: '', title: id }
  })
})

/**
 * 是否有值
 */
const hasValue = computed(() => {
  return isReferenceValue.value || hasSelectedCards.value
})

/**
 * 显示的卡片列表
 */
const displayCards = computed(() => {
  const selectedMap = new Map<string, CardInfo>()

  // 添加已选中的卡片
  props.selectedCards.forEach(card => {
    selectedMap.set(card.id, card)
  })

  // 添加搜索结果
  searchResults.value.forEach(card => {
    if (!selectedMap.has(card.id)) {
      selectedMap.set(card.id, card)
    }
  })

  return Array.from(selectedMap.values())
})

/**
 * 下拉框样式
 */
const dropdownStyle = computed(() => {
  if (!triggerRef.value) return {}
  const width = triggerRef.value.offsetWidth
  return {
    '--trigger-width': `${Math.max(width, 280)}px`,
  }
})

/**
 * 成员卡片类型ID
 */
const memberCardTypeId = computed(() => {
  if (!orgStore.currentOrgId) return ''
  return `${orgStore.currentOrgId}:member`
})

// ==================== 方法 ====================

/**
 * 将 LinkedCard 转换为 CardInfo
 */
function toCardInfo(linkedCard: LinkedCard): CardInfo {
  return {
    id: linkedCard.cardId,
    code: linkedCard.title?.displayValue?.split(' ')[0] || '',
    title: linkedCard.title?.displayValue || '',
  }
}

/**
 * 搜索卡片
 */
async function searchCards(keyword?: string): Promise<CardInfo[]> {
  if (!props.linkFieldId) return []

  const response = await linkCardApi.queryLinkableCards({
    linkFieldId: props.linkFieldId,
    keyword: keyword || undefined,
    page: 0,
    size: 20,
  })

  return response.content.map(toCardInfo)
}

/**
 * 处理搜索输入（防抖）
 */
function handleSearchInput() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }

  debounceTimer = setTimeout(async () => {
    loading.value = true
    try {
      searchResults.value = await searchCards(searchKeyword.value)
    } catch (error) {
      console.error('搜索卡片失败:', error)
      searchResults.value = []
    } finally {
      loading.value = false
    }
  }, 300)
}

/**
 * 处理下拉框显示变化
 */
async function handleDropdownVisibleChange(visible: boolean) {
  dropdownVisible.value = visible

  if (visible) {
    // 首次打开时加载目标卡片类型（用于"当前用户"级联选择的匹配判断）
    if (!hasLoadedTargetCardTypes.value && props.linkFieldId) {
      hasLoadedTargetCardTypes.value = true
      await loadTargetCardTypes()
    }

    // 首次打开加载卡片数据
    if (!hasLoadedInitial.value && props.linkFieldId) {
      hasLoadedInitial.value = true
      loading.value = true
      try {
        searchResults.value = await searchCards()
      } catch (error) {
        console.error('加载卡片失败:', error)
        searchResults.value = []
      } finally {
        loading.value = false
      }
    }
  } else {
    // 关闭时清理状态
    showMemberPanel.value = false
    searchKeyword.value = ''
  }
}

/**
 * 判断卡片是否选中
 */
function isCardSelected(cardId: string): boolean {
  if (props.modelValue?.type !== 'STATIC') return false
  return (props.modelValue as StaticLinkValue).cardIds?.includes(cardId) || false
}

/**
 * 切换卡片选中状态
 */
function toggleCardSelection(card: CardInfo) {
  const currentCardIds = props.modelValue?.type === 'STATIC'
    ? [...(props.modelValue as StaticLinkValue).cardIds]
    : []

  const index = currentCardIds.indexOf(card.id)
  if (index >= 0) {
    currentCardIds.splice(index, 1)
  } else {
    currentCardIds.push(card.id)
  }

  const newValue: StaticLinkValue = {
    type: 'STATIC',
    cardIds: currentCardIds,
  }

  emit('update:modelValue', newValue)

  // 获取选中卡片的详细信息
  const selectedCards = currentCardIds
    .map(id => displayCards.value.find(c => c.id === id))
    .filter((c): c is CardInfo => !!c)

  emit('change', newValue, selectedCards)
}

/**
 * 移除卡片
 */
function removeCard(cardId: string) {
  if (props.modelValue?.type !== 'STATIC') return

  const currentCardIds = [...(props.modelValue as StaticLinkValue).cardIds]
  const index = currentCardIds.indexOf(cardId)
  if (index >= 0) {
    currentCardIds.splice(index, 1)
  }

  const newValue: StaticLinkValue = {
    type: 'STATIC',
    cardIds: currentCardIds,
  }

  emit('update:modelValue', newValue)
}

/**
 * 加载左侧 linkFieldId 的目标卡片类型
 */
async function loadTargetCardTypes() {
  if (!props.linkFieldId) {
    targetCardTypeIds.value = []
    return
  }

  try {
    const { linkTypeId, position } = parseLinkFieldId(props.linkFieldId)
    const linkType = await linkTypeApi.getById(linkTypeId)

    // 根据 position 确定目标卡片类型
    // SOURCE 端关联的是 TARGET 端卡片，反之亦然
    const cardTypes = position === 'SOURCE'
      ? linkType.targetCardTypes
      : linkType.sourceCardTypes

    targetCardTypeIds.value = cardTypes?.map(ct => ct.id) || []
  } catch (error) {
    console.error('加载目标卡片类型失败:', error)
    targetCardTypeIds.value = []
  }
}

/**
 * 加载已选路径的字段名称（用于回显）
 * 根据 modelValue 中的 linkNodes 逐层加载字段信息
 */
async function loadSelectedPathFieldNames() {
  if (!isReferenceValue.value) {
    selectedPathFieldNames.value = []
    return
  }

  const source = (props.modelValue as ReferenceLinkValue)?.source
  const linkNodes = source?.path?.linkNodes
  if (!linkNodes || linkNodes.length === 0) {
    selectedPathFieldNames.value = []
    return
  }

  // 优先从 displayInfo 中获取名称
  if (displayInfoContext) {
    const names: string[] = []
    for (const linkNode of linkNodes) {
      const name = displayInfoContext.getLinkFieldName(linkNode)
      if (name && name !== linkNode) {
        names.push(name)
      } else {
        break
      }
    }

    if (names.length === linkNodes.length) {
      selectedPathFieldNames.value = names
      return
    }
  }

  // 从 API 加载
  await loadPathFieldNamesFromApi(linkNodes)
}

/**
 * 从 API 加载路径字段名称
 */
async function loadPathFieldNamesFromApi(linkNodes: string[]) {
  const names: string[] = []

  try {
    if (!memberCardTypeId.value) return

    // 第一层：从成员卡片类型加载
    const firstLevelFields = await fieldOptionsApi.getFields(memberCardTypeId.value)
    const firstField = firstLevelFields.find(f => f.id === linkNodes[0])
    if (firstField && linkNodes[0]) {
      names.push(firstField.name)
      displayInfoContext?.addLinkFieldName(linkNodes[0], firstField.name)
    }

    // 后续层级：根据前一层的 linkFieldId 加载
    for (let i = 1; i < linkNodes.length; i++) {
      const prevNode = linkNodes[i - 1]
      if (!prevNode) continue

      const fields = await fieldOptionsApi.getFieldsByLinkFieldId(prevNode)
      const field = fields.find(f => f.id === linkNodes[i])
      const currentNode = linkNodes[i]
      if (field && currentNode) {
        names.push(field.name)
        displayInfoContext?.addLinkFieldName(currentNode, field.name)
      }
    }
  } catch (error) {
    console.error('加载已选路径字段名称失败:', error)
  }

  selectedPathFieldNames.value = names
}

/**
 * 判断字段的目标卡片类型是否与左侧 linkFieldId 的目标卡片类型匹配
 * @param field 成员属性的 FieldOption（包含 targetCardTypeIds）
 * @returns 是否匹配（有交集）
 */
function checkFieldMatch(field: FieldOption): boolean {
  if (targetCardTypeIds.value.length === 0) {
    // 如果没有目标卡片类型约束，则全部可选
    return true
  }

  const fieldTargetCardTypeIds = field.targetCardTypeIds || []
  if (fieldTargetCardTypeIds.length === 0) {
    // 如果该字段没有目标卡片类型约束（即可链接任意卡片类型），则视为匹配
    return true
  }

  // 检查是否有交集
  return fieldTargetCardTypeIds.some(id => targetCardTypeIds.value.includes(id))
}

/**
 * 加载第一层成员卡片的关联属性
 */
async function loadFirstLevelFields() {
  if (!memberCardTypeId.value) return

  memberFieldsLoading.value = true
  try {
    const fields = await fieldOptionsApi.getFields(memberCardTypeId.value)
    // 只显示关联类型的字段
    const linkFields = fields.filter(f => f.fieldType === 'LINK')

    // 检查每个字段是否与目标卡片类型匹配（同步判断，使用内联的 targetCardTypeIds）
    const fieldsWithMatch = linkFields.map(field => ({
      ...field,
      matched: checkFieldMatch(field),
    }))

    // 初始化第一层级联
    cascadeLevels.value = [{
      fields: fieldsWithMatch as (FieldOption & { matched?: boolean })[],
      hoveredFieldId: null,
      loading: false,
      parentLinkFieldId: '',
    }]
  } catch (error) {
    console.error('加载成员属性失败:', error)
    cascadeLevels.value = []
  } finally {
    memberFieldsLoading.value = false
  }
}

/**
 * 加载下一层级联字段
 * @param levelIndex 当前层索引（从0开始）
 * @param parentLinkFieldId 父层级的 linkFieldId
 */
async function loadNextLevelFields(levelIndex: number, parentLinkFieldId: string) {
  // 检查是否超过最大层数
  if (levelIndex >= MAX_CASCADE_DEPTH - 1) return

  const nextLevelIndex = levelIndex + 1

  // 设置当前层的 hoveredFieldId
  if (cascadeLevels.value[levelIndex]) {
    cascadeLevels.value[levelIndex].hoveredFieldId = parentLinkFieldId
  }

  // 移除后续层级
  cascadeLevels.value = cascadeLevels.value.slice(0, nextLevelIndex)

  // 添加新层级（加载中状态）
  cascadeLevels.value.push({
    fields: [],
    hoveredFieldId: null,
    loading: true,
    parentLinkFieldId,
  })

  try {
    const fields = await fieldOptionsApi.getFieldsByLinkFieldId(parentLinkFieldId)
    // 只显示关联类型的字段
    const linkFields = fields.filter(f => f.fieldType === 'LINK')

    // 检查每个字段是否与目标卡片类型匹配（同步判断，使用内联的 targetCardTypeIds）
    const fieldsWithMatch = linkFields.map(field => ({
      ...field,
      matched: checkFieldMatch(field),
    }))

    // 更新该层级的字段
    if (cascadeLevels.value[nextLevelIndex]) {
      cascadeLevels.value[nextLevelIndex].fields = fieldsWithMatch as (FieldOption & { matched?: boolean })[]
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
 * 处理当前用户悬停
 */
function handleMemberHover() {
  if (hoverTimer) {
    clearTimeout(hoverTimer)
  }

  hoverTimer = setTimeout(() => {
    showMemberPanel.value = true
    if (cascadeLevels.value.length === 0) {
      loadFirstLevelFields()
    }
  }, 200)
}

/**
 * 处理当前用户点击
 */
function handleMemberClick() {
  if (hoverTimer) {
    clearTimeout(hoverTimer)
  }

  const newValue: ReferenceLinkValue = {
    type: 'REFERENCE',
    source: { type: 'MEMBER' },
  }

  emit('update:modelValue', newValue)
  emit('change', newValue, [])

  dropdownVisible.value = false
}

/**
 * 判断字段是否选中（在指定层级）
 */
function isFieldSelected(fieldId: string, levelIndex: number): boolean {
  if (!isReferenceValue.value) return false
  const source = (props.modelValue as ReferenceLinkValue)?.source
  if (!source?.path?.linkNodes) return false
  return source.path.linkNodes[levelIndex] === fieldId
}

/**
 * 判断字段是否为当前 hover 的字段（在指定层级）
 */
function isFieldHovered(fieldId: string, levelIndex: number): boolean {
  const level = cascadeLevels.value[levelIndex]
  return level?.hoveredFieldId === fieldId
}

/**
 * 处理字段悬停（展开下一层）
 * 注意：即使是置灰的属性也支持展开下级
 */
function handleFieldHover(field: FieldOption & { matched?: boolean }, levelIndex: number) {
  // 如果还没到最大层数，展开下一层（无论是否匹配都可以展开）
  if (levelIndex < MAX_CASCADE_DEPTH - 1) {
    if (hoverTimer) {
      clearTimeout(hoverTimer)
    }

    hoverTimer = setTimeout(() => {
      loadNextLevelFields(levelIndex, field.id!)
    }, 200)
  }
}

/**
 * 处理字段鼠标离开
 */
function handleFieldLeave() {
  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }
}

/**
 * 处理字段选择（点击选中）
 */
function handleFieldSelect(field: FieldOption & { matched?: boolean }, levelIndex: number) {
  // 只有匹配的字段才能选中
  if (!field.matched) return

  // 构建完整的路径（从第0层到当前层）
  const linkNodes: string[] = []
  const fieldNames: string[] = []

  for (let i = 0; i <= levelIndex; i++) {
    const level = cascadeLevels.value[i]
    if (i < levelIndex && level?.hoveredFieldId) {
      linkNodes.push(level.hoveredFieldId)
      // 从 cascadeLevels 中查找字段名称
      const hoveredField = level.fields.find(f => f.id === level.hoveredFieldId)
      if (hoveredField) {
        fieldNames.push(hoveredField.name)
      }
    } else if (i === levelIndex) {
      linkNodes.push(field.id!)
      fieldNames.push(field.name)
    }
  }

  // 立即设置字段名称用于回显
  selectedPathFieldNames.value = fieldNames

  // 更新 displayInfo 缓存
  if (displayInfoContext) {
    for (let i = 0; i < linkNodes.length; i++) {
      const nodeId = linkNodes[i]
      const nodeName = fieldNames[i]
      if (nodeId && nodeName) {
        displayInfoContext.addLinkFieldName(nodeId, nodeName)
      }
    }
  }

  const newValue: ReferenceLinkValue = {
    type: 'REFERENCE',
    source: {
      type: 'MEMBER',
      path: {
        linkNodes,
      },
    },
  }

  emit('update:modelValue', newValue)
  emit('change', newValue, [])

  dropdownVisible.value = false
}

/**
 * 处理当前用户鼠标离开
 */
function handleMemberLeave() {
  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }
  schedulePanelClose()
}

/**
 * 处理成员面板鼠标进入
 */
function handleMemberPanelEnter() {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
}

/**
 * 处理成员面板鼠标离开
 */
function handleMemberPanelLeave() {
  schedulePanelClose()
}

/**
 * 延迟关闭面板
 */
function schedulePanelClose() {
  closeTimer = setTimeout(() => {
    showMemberPanel.value = false
    cascadeLevels.value = []
  }, 100)
}

// ==================== 监听 ====================

/**
 * 监听 linkFieldId 变化，重置状态
 * 注意：不再立即加载目标卡片类型，而是延迟到下拉框打开时加载
 */
watch(
  () => props.linkFieldId,
  () => {
    hasLoadedInitial.value = false
    hasLoadedTargetCardTypes.value = false
    searchResults.value = []
    searchKeyword.value = ''
    cascadeLevels.value = []
    targetCardTypeIds.value = []
  },
  { immediate: true }
)

/**
 * 监听 modelValue 变化，加载已选路径的字段名称（用于回显）
 */
watch(
  () => props.modelValue,
  async () => {
    // 当 modelValue 变化时，重新加载字段名称用于回显
    await loadSelectedPathFieldNames()
  },
  { immediate: true, deep: true }
)

/**
 * 组件卸载时清理定时器
 */
onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (hoverTimer) clearTimeout(hoverTimer)
  if (closeTimer) clearTimeout(closeTimer)
})
</script>

<style scoped lang="scss">
.link-value-trigger {
  width: 100%;
  min-width: 180px;
  min-height: 28px;
  display: flex;
  align-items: center;
  padding: 2px 8px;
  background: var(--color-fill-2);
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  box-sizing: border-box;

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

  .reference-text {
    flex: 1;
    font-size: 13px;
    color: var(--color-text-1);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .selected-cards {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    overflow: hidden;
  }

  .placeholder {
    flex: 1;
    font-size: 13px;
    color: var(--color-text-3);
  }

  .trigger-icon {
    flex-shrink: 0;
    margin-left: 4px;
    font-size: 12px;
    color: var(--color-text-3);
    transition: transform 0.2s;

    &.is-open {
      transform: rotate(180deg);
    }
  }
}

.link-value-dropdown {
  background: var(--color-bg-popup);
  border-radius: 4px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.panels-container {
  display: flex;
  max-height: 400px;
}

.main-panel {
  flex: 0 0 var(--trigger-width, 280px);
  width: var(--trigger-width, 280px);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border-2);

  &:last-child {
    border-right: none;
  }
}

.reference-section {
  padding: 4px;
}

.reference-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover,
  &.expanded {
    background: var(--color-fill-2);
  }

  &.selected {
    background: rgb(var(--primary-1));
    color: rgb(var(--primary-6));
  }

  .item-icon {
    font-size: 14px;
    color: var(--color-text-3);
  }

  .item-text {
    flex: 1;
    font-size: 13px;
  }

  .expand-icon {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.divider {
  height: 1px;
  background: var(--color-border-2);
  margin: 0 8px;
}

.search-box {
  display: flex;
  align-items: center;
  margin: 8px;
  padding: 0 8px;
  height: 28px;
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  background: var(--color-bg-2);
  gap: 8px;
  transition: border-color 0.2s;

  &:focus-within {
    border-color: rgb(var(--primary-6));
  }

  .search-icon {
    flex-shrink: 0;
    font-size: 14px;
    color: var(--color-text-3);
  }

  .search-input {
    flex: 1;
    min-width: 0;
    height: 100%;
    padding: 0;
    border: none;
    background: transparent;
    font-size: 13px;
    color: var(--color-text-1);
    outline: none;

    &::placeholder {
      color: var(--color-text-3);
    }
  }
}

.cards-section {
  flex: 1;
  overflow-y: auto;
  max-height: 280px;
}

.cards-list {
  padding: 4px;
}

.card-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: var(--color-fill-2);
  }

  &.selected {
    background: rgb(var(--primary-1));
  }

  .card-code {
    flex-shrink: 0;
    font-size: 11px;
    color: var(--color-text-3);
  }

  .card-title {
    flex: 1;
    font-size: 13px;
    color: var(--color-text-1);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.loading-container,
.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.empty-text {
  font-size: 12px;
  color: var(--color-text-3);
}

.member-panel {
  flex: 0 0 200px;
  width: 200px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px;
  border-right: 1px solid var(--color-border-2);

  &:last-child {
    border-right: none;
  }
}

.field-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover:not(.disabled) {
    background: var(--color-fill-2);
  }

  &.hovered:not(.disabled) {
    background: var(--color-fill-2);
  }

  &.selected {
    background: rgb(var(--primary-1));
    color: rgb(var(--primary-6));
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.5;

    .field-name {
      color: var(--color-text-4);
    }
  }

  .field-name {
    flex: 1;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .expand-icon {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--color-text-3);
    margin-left: 4px;
  }
}
</style>
