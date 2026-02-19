<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import { IconRight } from '@arco-design/web-vue/es/icon'
import type { FieldOption } from '@/types/field-option'
import type { FieldProvider, VariableSource } from './types'
import { SYSTEM_VARIABLES, CARD_BUILTIN_PROPERTIES } from './types'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  /** 光标位置上边缘 (用于向上弹出) */
  anchorTop: number
  /** 光标位置下边缘 (用于向下弹出) */
  anchorBottom: number
  /** 光标位置左边缘 */
  anchorLeft: number
  fieldProvider: FieldProvider
}>()

const emit = defineEmits<{
  select: [expression: string, label: string]
  close: []
}>()

// 面板层级数据
interface PanelLevel {
  items: PanelItem[]
  loading: boolean
  selectedIndex: number
}

interface PanelItem {
  id: string
  name: string
  isLink: boolean
  fieldType?: string
}

// 当前选择路径
const selectedSource = ref<VariableSource | null>(null)
const panels = ref<PanelLevel[]>([])
const pathSegments = ref<{ id: string; name: string }[]>([])

// 第一级：变量源列表
const sourceItems = computed(() => [
  { id: 'card', name: t('common.textExpressionTemplate.sources.card') },
  { id: 'member', name: t('common.textExpressionTemplate.sources.member') },
  { id: 'system', name: t('common.textExpressionTemplate.sources.system') },
])

// 面板容器引用
const panelRef = ref<HTMLElement>()

// 计算后的实际位置
const computedTop = ref(0)
const computedLeft = ref(0)

// 键盘导航
const activePanel = ref(0)

// 错误状态
const errorMessage = ref<string | null>(null)

// 防抖的位置更新函数（约一帧的时间）
const debouncedUpdatePosition = useDebounceFn(updatePosition, 16)

/** 面板与光标/锚点之间的间距（像素） */
const PANEL_ANCHOR_GAP_PX = 4

/**
 * 最大关联字段级联深度
 * 从第一级字段面板开始计算，0 = 第一级
 * 限制为2以防止过深的嵌套和过多的API请求
 */
const MAX_LINK_FIELD_DEPTH = 2

/** 根据面板实际尺寸和视口大小，计算最佳位置 */
function updatePosition() {
  if (!panelRef.value) return
  const el = panelRef.value
  const rect = el.getBoundingClientRect()
  const viewportH = window.innerHeight
  const viewportW = window.innerWidth

  // 垂直方向：优先向下，空间不足则向上
  const spaceBelow = viewportH - props.anchorBottom - PANEL_ANCHOR_GAP_PX
  const spaceAbove = props.anchorTop - PANEL_ANCHOR_GAP_PX

  if (rect.height <= spaceBelow) {
    computedTop.value = props.anchorBottom + PANEL_ANCHOR_GAP_PX
  } else if (rect.height <= spaceAbove) {
    computedTop.value = props.anchorTop - PANEL_ANCHOR_GAP_PX - rect.height
  } else {
    // 两边都放不下，贴底部显示
    computedTop.value = Math.max(PANEL_ANCHOR_GAP_PX, viewportH - rect.height - PANEL_ANCHOR_GAP_PX)
  }

  // 水平方向：优先左对齐光标，超出右边界则左移
  if (props.anchorLeft + rect.width <= viewportW - PANEL_ANCHOR_GAP_PX) {
    computedLeft.value = props.anchorLeft
  } else {
    computedLeft.value = Math.max(PANEL_ANCHOR_GAP_PX, viewportW - rect.width - PANEL_ANCHOR_GAP_PX)
  }
}

// 重置状态
function resetState() {
  selectedSource.value = null
  panels.value = []
  pathSegments.value = []
  activePanel.value = 0
}

watch(() => props.visible, (val) => {
  if (val) {
    resetState()
    nextTick(updatePosition)
  }
})

// 面板内容变化时重新计算位置（使用防抖避免频繁更新）
watch(panels, () => nextTick(debouncedUpdatePosition), { deep: true })


// 将 FieldOption[] 转换为 PanelItem[]
// linkDepth: 当前面板的关联深度，达到 MAX_LINK_DEPTH 时过滤掉关联字段
function fieldsToPanelItems(fields: FieldOption[], linkDepth: number): PanelItem[] {
  const systemFields: PanelItem[] = []
  const customFields: PanelItem[] = []
  const linkFields: PanelItem[] = []

  for (const f of fields) {
    const isLink = f.fieldType === 'LINK'
    // 达到最大深度时过滤掉关联字段
    if (isLink && linkDepth >= MAX_LINK_FIELD_DEPTH) continue
    const item: PanelItem = {
      id: f.id,
      name: f.name,
      isLink,
      fieldType: f.fieldType,
    }
    if (f.systemField) {
      systemFields.push(item)
    } else if (f.fieldType === 'LINK') {
      linkFields.push(item)
    } else {
      customFields.push(item)
    }
  }

  return [...systemFields, ...customFields, ...linkFields]
}

// 选择变量源
async function handleSourceSelect(source: VariableSource) {
  selectedSource.value = source
  pathSegments.value = []

  if (source === 'system') {
    const items: PanelItem[] = SYSTEM_VARIABLES.map((sv) => ({
      id: sv.id,
      name: t(sv.nameKey),
      isLink: false,
    }))
    panels.value = [{ items, loading: false, selectedIndex: -1 }]
    activePanel.value = 0
    return
  }

  panels.value = [{ items: [], loading: true, selectedIndex: -1 }]
  activePanel.value = 0

  try {
    errorMessage.value = null
    const fields = source === 'card'
      ? await props.fieldProvider.getCardFields()
      : await props.fieldProvider.getMemberFields()

    // 获取字段转换后的面板项
    const panelItems = fieldsToPanelItems(fields, 0)

    // 如果是卡片源，在列表顶部添加内置属性
    if (source === 'card') {
      const builtinItems: PanelItem[] = CARD_BUILTIN_PROPERTIES.map((prop) => ({
        id: prop.id,
        name: t(prop.nameKey),
        isLink: false,
      }))
      panels.value = [{ items: [...builtinItems, ...panelItems], loading: false, selectedIndex: -1 }]
    } else {
      panels.value = [{ items: panelItems, loading: false, selectedIndex: -1 }]
    }
  } catch (error) {
    console.error('[TextExpression] Failed to load fields:', error)
    errorMessage.value = t('common.textExpressionTemplate.loadError')
    panels.value = [{ items: [], loading: false, selectedIndex: -1 }]
  }
}

// 选择字段
async function handleFieldSelect(panelIndex: number, item: PanelItem) {
  if (!selectedSource.value) return

  if (item.isLink) {
    // 关联字段 — 展开下一级
    const panel = panels.value[panelIndex]
    if (!panel) return
    panel.selectedIndex = panel.items.indexOf(item)
    // 截断后续面板
    panels.value = panels.value.slice(0, panelIndex + 1)
    pathSegments.value = pathSegments.value.slice(0, panelIndex)
    pathSegments.value.push({ id: item.id, name: item.name })

    const nextDepth = panelIndex + 1
    const newPanel: PanelLevel = { items: [], loading: true, selectedIndex: -1 }
    panels.value.push(newPanel)
    activePanel.value = nextDepth

    try {
      errorMessage.value = null
      const fields = await props.fieldProvider.getFieldsByLinkFieldId(item.id)
      panels.value[nextDepth] = { items: fieldsToPanelItems(fields, nextDepth), loading: false, selectedIndex: -1 }
    } catch (error) {
      console.error('[TextExpression] Failed to load linked fields:', error)
      errorMessage.value = t('common.textExpressionTemplate.loadError')
      panels.value[nextDepth] = { items: [], loading: false, selectedIndex: -1 }
    }
    return
  }

  // 普通字段 — 完成选择
  const source = selectedSource.value
  const pathParts = pathSegments.value.slice(0, panelIndex).map((s) => s.id)
  const expression = [source, ...pathParts, item.id].join('.')

  const sourceName = t(`common.textExpressionTemplate.sources.${source}`)
  const pathNames = pathSegments.value.slice(0, panelIndex).map((s) => s.name)
  const label = [sourceName, ...pathNames, item.name].join('.')

  emit('select', expression, label)
}

// 键盘事件处理
function handleKeyDown(event: KeyboardEvent) {
  if (!props.visible) return

  // 如果还没选择 source
  if (!selectedSource.value) {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      // 不做处理，让用户点击选择
    }
    return
  }

  const panel = panels.value[activePanel.value]
  if (!panel) return

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    panel.selectedIndex = panel.selectedIndex < 0
      ? 0
      : Math.min(panel.selectedIndex + 1, panel.items.length - 1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    panel.selectedIndex = panel.selectedIndex <= 0
      ? panel.items.length - 1  // 循环到最后一项
      : panel.selectedIndex - 1
  } else if (event.key === 'Enter') {
    event.preventDefault()
    const item = panel.items[panel.selectedIndex]
    if (item) handleFieldSelect(activePanel.value, item)
  }
}

// 点击外部关闭
function handleClickOutside(event: MouseEvent) {
  if (panelRef.value && !panelRef.value.contains(event.target as Node)) {
    emit('close')
  }
}

// 保存事件处理函数引用，确保正确移除
const boundHandleKeyDown = handleKeyDown
const boundHandleClickOutside = handleClickOutside

onMounted(() => {
  document.addEventListener('keydown', boundHandleKeyDown)
  document.addEventListener('mousedown', boundHandleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('keydown', boundHandleKeyDown)
  document.removeEventListener('mousedown', boundHandleClickOutside)
})

defineExpose({ handleKeyDown })
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="panelRef"
      class="suggestion-panel"
      :style="{ top: `${computedTop}px`, left: `${computedLeft}px` }"
      @mousedown.prevent
    >
      <!-- 第一级：变量源选择 -->
      <div v-if="!selectedSource" class="panel-column">
        <div
          v-for="source in sourceItems"
          :key="source.id"
          class="panel-item"
          @click="handleSourceSelect(source.id as VariableSource)"
        >
          <span class="item-name">{{ source.name }}</span>
          <IconRight class="item-arrow" />
        </div>
      </div>

      <!-- 后续级别：字段选择 -->
      <template v-else>
        <!-- 面包屑导航 -->
        <div class="panel-breadcrumb">
          <span class="breadcrumb-item clickable" @click="resetState">
            {{ t(`common.textExpressionTemplate.sources.${selectedSource}`) }}
          </span>
          <template v-for="(seg, idx) in pathSegments" :key="`${seg.id}-${idx}`">
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-item">{{ seg.name }}</span>
          </template>
        </div>

        <div class="panel-columns">
          <div
            v-for="(panel, panelIndex) in panels"
            :key="panelIndex"
            class="panel-column"
          >
            <div v-if="panel.loading" class="panel-loading">
              <a-spin size="small" />
            </div>
            <template v-else>
              <!-- 系统字段组 -->
              <template v-if="panel.items.some(i => !i.isLink && !i.fieldType?.includes('LINK'))">
                <div
                  v-for="item in panel.items.filter(i => !i.isLink)"
                  :key="item.id"
                  class="panel-item"
                  :class="{
                    selected: panel.selectedIndex === panel.items.indexOf(item),
                  }"
                  @click="handleFieldSelect(panelIndex, item)"
                >
                  <span class="item-name">{{ item.name }}</span>
                </div>
              </template>

              <!-- 关联字段组 -->
              <template v-if="panel.items.some(i => i.isLink)">
                <div class="group-divider" />
                <div class="group-label">{{ t('common.textExpressionTemplate.linkFields') }}</div>
                <div
                  v-for="item in panel.items.filter(i => i.isLink)"
                  :key="item.id"
                  class="panel-item link-item"
                  :class="{
                    expanded: panel.selectedIndex === panel.items.indexOf(item),
                  }"
                  @click="handleFieldSelect(panelIndex, item)"
                >
                  <span class="item-name">{{ item.name }}</span>
                  <IconRight class="item-arrow" />
                </div>
              </template>
            </template>
          </div>
        </div>
      </template>
    </div>
  </Teleport>
</template>

<style scoped lang="scss">
.suggestion-panel {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border: 1px solid var(--color-border-2, #e5e6eb);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.panel-breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--color-text-3);
  border-bottom: 1px solid var(--color-border-1, #e5e6eb);
  background: var(--color-fill-1, #f7f8fa);
}

.breadcrumb-item {
  &.clickable {
    cursor: pointer;
    color: rgb(var(--primary-6));
    &:hover { text-decoration: underline; }
  }
}

.breadcrumb-sep {
  color: var(--color-text-4);
}

.panel-columns {
  display: flex;
  max-height: 280px;
}

.panel-column {
  min-width: 180px;
  max-width: 220px;
  max-height: 280px;
  overflow-y: auto;
  padding: 4px 0;
  border-right: 1px solid var(--color-border-1, #e5e6eb);

  &:last-child {
    border-right: none;
  }
}

.panel-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.group-divider {
  height: 1px;
  background: var(--color-border-1, #e5e6eb);
  margin: 4px 8px;
}

.group-label {
  padding: 4px 12px;
  font-size: 11px;
  color: var(--color-text-3);
  font-weight: 500;
}

.panel-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 13px;
  color: var(--color-text-1);
  transition: background 0.15s;

  &:hover {
    background: var(--color-fill-2, #f2f3f5);
  }

  &.selected {
    background: rgb(var(--primary-1));
    color: rgb(var(--primary-6));
  }

  &.expanded {
    background: var(--color-fill-2, #f2f3f5);
  }
}

.item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-arrow {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-3);
  margin-left: 8px;
}
</style>
