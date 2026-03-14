<script setup lang="ts">
/**
 * 操作历史时间线组件
 * 以时间线风格展示卡片操作历史，支持筛选和倒序排列
 */
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconFilter, IconRefresh, IconUser, IconClockCircle, IconSortAscending, IconSortDescending } from '@arco-design/web-vue/es/icon'
import { historyApi } from '@/api/history'
import type {
  CardHistoryRecord,
  HistorySearchRequest,
  HistoryFilters,
  OperationType,
  OperationSourceType,
  DiffHunk,
} from '@/types/history'
import { formatDistanceToNow, format, parseISO } from 'date-fns'
import { zhCN, enUS } from 'date-fns/locale'
import { getLocale } from '@/i18n'

const { t } = useI18n()

const props = defineProps<{
  /** 卡片ID */
  cardId: string
  /** 卡片类型ID */
  cardTypeId: string
}>()

// 状态
const loading = ref(false)
const records = ref<CardHistoryRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const hasMore = computed(() => records.value.length < total.value)

// 筛选状态
const showFilter = ref(false)
const filters = ref<HistoryFilters | null>(null)
const selectedOperationTypes = ref<OperationType[]>([])
const selectedSourceTypes = ref<OperationSourceType[]>([])

// 排序状态（false=倒序/最新在前，true=正序/最早在前）
const sortAsc = ref(false)

// 时间范围状态
const dateRange = ref<[Date, Date] | null>(null)

// 加载历史记录
async function loadHistory(reset = false) {
  if (!props.cardId || !props.cardTypeId) return
  if (loading.value) return

  if (reset) {
    page.value = 1
    records.value = []
  }

  loading.value = true
  try {
    // 构建搜索请求
    const searchRequest: HistorySearchRequest = {
      page: page.value,
      size: size.value,
      sortAsc: sortAsc.value,
    }

    // 添加操作类型筛选
    if (selectedOperationTypes.value.length > 0) {
      searchRequest.operationTypes = selectedOperationTypes.value
    }

    // 添加来源类型筛选
    if (selectedSourceTypes.value.length > 0) {
      searchRequest.sourceTypes = selectedSourceTypes.value
    }

    // 添加时间范围筛选
    if (dateRange.value && dateRange.value.length === 2) {
      const [start, end] = dateRange.value
      searchRequest.startTime = formatDateTimeForApi(start)
      searchRequest.endTime = formatDateTimeForApi(end, true)
    }

    const result = await historyApi.searchCardHistory(props.cardTypeId, props.cardId, searchRequest)

    if (reset) {
      records.value = result.content
    } else {
      records.value.push(...result.content)
    }
    total.value = result.total
  } catch (error) {
    console.error('加载操作历史失败:', error)
  } finally {
    loading.value = false
  }
}

// 格式化日期时间为 API 格式（ISO 8601）
function formatDateTimeForApi(date: Date, isEndOfDay = false): string {
  const d = new Date(date)
  if (isEndOfDay) {
    d.setHours(23, 59, 59, 999)
  } else {
    d.setHours(0, 0, 0, 0)
  }
  return d.toISOString().slice(0, 19)
}

// 加载更多
function loadMore() {
  if (!hasMore.value || loading.value) return
  page.value++
  loadHistory()
}

// 刷新
function refresh() {
  loadHistory(true)
}

// 加载筛选选项
async function loadFilters() {
  if (!props.cardId || !props.cardTypeId) return
  try {
    filters.value = await historyApi.getFilters(props.cardTypeId, props.cardId)
  } catch (error) {
    console.error('加载筛选选项失败:', error)
  }
}

// 切换筛选面板
function toggleFilter() {
  showFilter.value = !showFilter.value
  if (showFilter.value && !filters.value) {
    loadFilters()
  }
}

// 应用筛选
function applyFilter() {
  showFilter.value = false
  loadHistory(true)
}

// 清除筛选
function clearFilter() {
  selectedOperationTypes.value = []
  selectedSourceTypes.value = []
  dateRange.value = null
  showFilter.value = false
  loadHistory(true)
}

// 切换排序
function toggleSort() {
  sortAsc.value = !sortAsc.value
  loadHistory(true)
}

// 时间范围变更
function onDateRangeChange() {
  loadHistory(true)
}

// 获取当前日期格式化的 locale
function getDateLocale() {
  return getLocale() === 'zh-CN' ? zhCN : enUS
}

// 格式化时间（相对时间）
function formatRelativeTime(dateStr: string): string {
  try {
    const date = parseISO(dateStr)
    return formatDistanceToNow(date, { addSuffix: true, locale: getDateLocale() })
  } catch {
    return dateStr
  }
}

// 格式化时间（完整时间）
function formatFullTime(dateStr: string): string {
  try {
    const date = parseISO(dateStr)
    return format(date, 'yyyy-MM-dd HH:mm:ss', { locale: getDateLocale() })
  } catch {
    return dateStr
  }
}

// 获取操作类型标签（用于筛选面板，使用 i18n）
function getOperationTypeLabelByType(type: OperationType): string {
  return t(`history.operation.${type}`)
}

// 获取操作类型标签（使用 i18n）
function getOperationTypeLabel(record: CardHistoryRecord): string {
  return t(`history.operation.${record.operationType}`)
}

// 获取操作来源标签（使用 i18n）
function getSourceLabel(record: CardHistoryRecord): string {
  const source = record.operationSource

  // 如果是业务规则且有规则名称，显示规则名称
  if (source.type === 'BIZ_RULE' && source.ruleName) {
    return t('history.source.BIZ_RULE_WITH_NAME', { ruleName: source.ruleName })
  }

  return t(`history.source.${source.type}`)
}

// 渲染消息内容（返回 HTML 字符串，用于 v-html 渲染）
function renderMessage(record: CardHistoryRecord): string {
  const { message } = record
  if (!message || !message.messageKey) {
    return escapeHtml(getOperationTypeLabel(record))
  }

  const { messageKey, args } = message
  const operationLabel = getOperationTypeLabel(record)

  // 根据消息类型渲染
  switch (messageKey) {
    case 'history.stream.moved':
    case 'history.stream.rollback':
      // 状态变更：从 [原状态] 到 [新状态]
      if (args && args.length >= 2) {
        const fromStatus = args[0]?.statusName || args[0]?.displayValue || ''
        const toStatus = args[1]?.statusName || args[1]?.displayValue || ''
        return `${escapeHtml(operationLabel)}：${escapeHtml(fromStatus)} → ${escapeHtml(toStatus)}`
      }
      return escapeHtml(operationLabel)

    case 'history.field.updated':
      // 属性变更：[属性名]：[旧值] → [新值]
      if (args && args.length >= 3) {
        const fieldNameArg = args[0]
        const fieldName = fieldNameArg?.fieldName || fieldNameArg?.displayValue || ''
        const isDeleted = fieldNameArg?.deleted === true
        const fieldNameDisplay = isDeleted
          ? `<del class="deleted-field">${escapeHtml(fieldName)}</del>`
          : escapeHtml(fieldName)
        const oldValue = formatFieldValue(args[1])
        const newValue = formatFieldValue(args[2])
        return `${escapeHtml(operationLabel)}「${fieldNameDisplay}」：${escapeHtml(oldValue) || t('history.ui.emptyValue')} → ${escapeHtml(newValue) || t('history.ui.emptyValue')}`
      }
      return escapeHtml(operationLabel)

    case 'history.field.title.updated':
      // 标题变更：[旧值] → [新值]
      if (args && args.length >= 2) {
        const oldValue = args[0]?.value || ''
        const newValue = args[1]?.value || ''
        return `${escapeHtml(operationLabel)}：${escapeHtml(oldValue) || t('history.ui.emptyValue')} → ${escapeHtml(newValue) || t('history.ui.emptyValue')}`
      }
      return escapeHtml(operationLabel)

    case 'history.field.desc.updated':
      // 描述变更：渲染 diff
      if (args && args.length > 0) {
        const diffArg = args[0]
        if (diffArg?.type === 'TEXT_DIFF' && diffArg.hunks) {
          return `${escapeHtml(operationLabel)}：${renderDiffHunks(diffArg.hunks)}`
        }
        // 兼容旧数据格式（TEXT 类型）
        if (args.length >= 2) {
          const oldValue = args[0]?.value || ''
          const newValue = args[1]?.value || ''
          return `${escapeHtml(operationLabel)}：${escapeHtml(oldValue) || t('history.ui.emptyValue')} → ${escapeHtml(newValue) || t('history.ui.emptyValue')}`
        }
      }
      return escapeHtml(operationLabel)

    case 'history.card.created':
      // 创建卡片：[卡片标题]
      if (args && args.length > 0) {
        const title = args[0]?.value || args[0]?.displayValue || ''
        return title ? `${escapeHtml(operationLabel)}：${escapeHtml(title)}` : escapeHtml(operationLabel)
      }
      return escapeHtml(operationLabel)

    case 'history.card.archived':
      return escapeHtml(t('history.message.cardArchived'))

    case 'history.card.restored':
      return escapeHtml(t('history.message.cardRestored'))

    case 'history.card.abandoned':
      return escapeHtml(t('history.message.cardAbandoned'))

    case 'history.card.abandoned.with_reason':
      // 丢弃卡片（有原因）：使用消息模板翻译，传入原因参数
      if (args && args.length > 0) {
        const reason = args[0]?.value || args[0]?.displayValue || ''
        return escapeHtml(t('history.message.cardAbandonedWithReason', [reason]))
      }
      return escapeHtml(t('history.message.cardAbandoned'))

    case 'history.link.added':
    case 'history.link.removed':
      // 主动方关联变更：[关联属性名]：[关联的卡片]
      if (args && args.length >= 2) {
        const fieldNameArg = args[0]
        const fieldName = fieldNameArg?.fieldName || fieldNameArg?.displayValue || ''
        const isDeleted = fieldNameArg?.deleted === true
        const fieldNameDisplay = isDeleted
          ? `<del class="deleted-field">${escapeHtml(fieldName)}</del>`
          : escapeHtml(fieldName)
        const linkedCardsArg = args[1]
        const linkedCards = formatLinkFieldValue(linkedCardsArg)
        return `${escapeHtml(operationLabel)}「${fieldNameDisplay}」：${linkedCards}`
      }
      return escapeHtml(operationLabel)

    case 'history.link.added.passive':
    case 'history.link.removed.passive':
      // 被动方关联变更：被添加/移除关联「属性名」：由 [发起卡片]
      if (args && args.length >= 2) {
        const fieldNameArg = args[0]
        const fieldName = fieldNameArg?.fieldName || fieldNameArg?.displayValue || ''
        const isDeleted = fieldNameArg?.deleted === true
        const fieldNameDisplay = isDeleted
          ? `<del class="deleted-field">${escapeHtml(fieldName)}</del>`
          : escapeHtml(fieldName)
        const linkedCardsArg = args[1]
        const linkedCards = formatLinkFieldValue(linkedCardsArg)
        const passiveLabel = messageKey === 'history.link.added.passive'
          ? t('history.operation.LINK_ADDED_PASSIVE')
          : t('history.operation.LINK_REMOVED_PASSIVE')
        return `${escapeHtml(passiveLabel)}「${fieldNameDisplay}」：${linkedCards}`
      }
      return escapeHtml(operationLabel)

    default:
      // 默认处理
      if (args && args.length > 0) {
        const firstArg = args[0]
        if (firstArg?.displayValue || firstArg?.value) {
          return `${escapeHtml(operationLabel)}：${escapeHtml(firstArg.displayValue || firstArg.value || '')}`
        }
      }
      return escapeHtml(operationLabel)
  }
}

// HTML 转义，防止 XSS
function escapeHtml(str: string): string {
  if (!str) return ''
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

// 格式化属性值显示
function formatFieldValue(arg: any): string {
  if (!arg) return ''

  // 优先使用 displayValue
  if (arg.displayValue && arg.displayValue !== 'null') {
    // 检查 displayValue 是否是时间戳（兼容旧数据）
    const formattedTimestamp = tryFormatTimestamp(arg.displayValue)
    if (formattedTimestamp) {
      return formattedTimestamp
    }
    return arg.displayValue
  }

  // 处理 rawValue
  const rawValue = arg.rawValue
  if (rawValue === null || rawValue === undefined) {
    return ''
  }

  // 数组类型（如枚举选项、关联卡片）
  if (Array.isArray(rawValue)) {
    if (rawValue.length === 0) return ''
    return rawValue.join(', ')
  }

  return String(rawValue)
}

/**
 * 尝试将时间戳格式化为日期时间字符串
 * 用于兼容旧数据中存储的时间戳值
 */
function tryFormatTimestamp(value: string): string | null {
  // 检查是否是纯数字（时间戳）
  if (!/^\d{13}$/.test(value)) {
    return null
  }

  const timestamp = parseInt(value, 10)
  // 合理的时间戳范围：2000-01-01 到 2100-01-01
  const minTimestamp = 946684800000
  const maxTimestamp = 4102444800000
  if (timestamp < minTimestamp || timestamp > maxTimestamp) {
    return null
  }

  try {
    const date = new Date(timestamp)
    return format(date, 'yyyy-MM-dd HH:mm', { locale: getDateLocale() })
  } catch {
    return null
  }
}

// 格式化关联属性值显示
function formatLinkFieldValue(arg: any): string {
  if (!arg) return ''

  // 优先使用 displayValue
  if (arg.displayValue && arg.displayValue !== 'null') {
    return escapeHtml(arg.displayValue)
  }

  // 处理 cards 数组（FIELD_VALUE_LINK 类型）
  const cards = arg.cards
  if (cards && Array.isArray(cards) && cards.length > 0) {
    return cards.map((card: any) => escapeHtml(card.cardTitle || '')).join('、')
  }

  return ''
}

// 渲染 diff hunks 为 HTML
function renderDiffHunks(hunks: DiffHunk[]): string {
  if (!hunks || hunks.length === 0) {
    return `<span class="diff-no-changes">${escapeHtml(t('history.ui.noChanges'))}</span>`
  }

  let html = '<div class="diff-container">'

  for (const hunk of hunks) {
    html += '<div class="diff-hunk">'
    html += `<div class="diff-hunk-header">@@ -${hunk.oldStart},${hunk.oldCount} +${hunk.newStart},${hunk.newCount} @@</div>`

    for (const line of hunk.lines) {
      const lineClass = `diff-line diff-line-${line.type.toLowerCase()}`
      const prefix = line.type === 'ADD' ? '+' : line.type === 'DELETE' ? '-' : ' '
      html += `<div class="${lineClass}"><span class="diff-line-prefix">${prefix}</span><span class="diff-line-content">${escapeHtml(line.content)}</span></div>`
    }

    html += '</div>'
  }

  html += '</div>'
  return html
}

// 获取操作图标颜色
function getOperationColor(type: OperationType): string {
  switch (type) {
    case 'CARD_CREATED':
      return 'rgb(var(--green-6))'
    case 'CARD_ARCHIVED':
      return 'rgb(var(--orange-6))'
    case 'CARD_ABANDONED':
      return 'rgb(var(--red-6))'
    case 'CARD_RESTORED':
      return 'rgb(var(--blue-6))'
    default:
      return 'rgb(var(--primary-6))'
  }
}

// 是否有激活的筛选
const hasActiveFilter = computed(() => {
  return selectedOperationTypes.value.length > 0 ||
    selectedSourceTypes.value.length > 0 ||
    dateRange.value !== null
})

// 监听卡片变化
watch(
  () => [props.cardId, props.cardTypeId],
  () => {
    if (props.cardId && props.cardTypeId) {
      loadHistory(true)
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="activity-log-panel">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="record-count">{{ t('history.ui.recordCount', { count: total }) }}</span>
      </div>
      <div class="toolbar-right">
        <a-tooltip :content="sortAsc ? t('history.ui.sortAsc') : t('history.ui.sortDesc')">
          <a-button type="text" size="small" @click="toggleSort">
            <template #icon>
              <IconSortAscending v-if="sortAsc" />
              <IconSortDescending v-else />
            </template>
          </a-button>
        </a-tooltip>
        <a-button type="text" size="small" :loading="loading" @click="refresh">
          <template #icon><IconRefresh /></template>
        </a-button>
        <a-button
          type="text"
          size="small"
          :class="{ 'filter-active': hasActiveFilter }"
          @click="toggleFilter"
        >
          <template #icon><IconFilter /></template>
          {{ t('history.ui.filter') }}
        </a-button>
      </div>
    </div>

    <!-- 筛选面板 -->
    <transition name="slide-down">
      <div v-if="showFilter" class="filter-panel">
        <div class="filter-section">
          <div class="filter-label">{{ t('history.ui.operationType') }}</div>
          <a-checkbox-group v-model="selectedOperationTypes" class="filter-options">
            <a-checkbox
              v-for="type in (filters?.operationTypes || [])"
              :key="type"
              :value="type"
            >
              {{ getOperationTypeLabelByType(type) }}
            </a-checkbox>
          </a-checkbox-group>
        </div>
        <div class="filter-section">
          <div class="filter-label">{{ t('history.ui.operationSource') }}</div>
          <a-checkbox-group v-model="selectedSourceTypes" class="filter-options">
            <a-checkbox
              v-for="type in (filters?.sourceTypes || [])"
              :key="type"
              :value="type"
            >
              {{ t(`history.source.${type}`) }}
            </a-checkbox>
          </a-checkbox-group>
        </div>
        <div class="filter-section">
          <div class="filter-label">{{ t('history.ui.timeRange') }}</div>
          <a-range-picker
            v-model="dateRange"
            style="width: 260px"
            :placeholder="[t('history.ui.startDate'), t('history.ui.endDate')]"
            allow-clear
            @change="onDateRangeChange"
          />
        </div>
        <div class="filter-actions">
          <a-button size="small" @click="clearFilter">{{ t('history.ui.clear') }}</a-button>
          <a-button type="primary" size="small" @click="applyFilter">{{ t('history.ui.apply') }}</a-button>
        </div>
      </div>
    </transition>

    <!-- 时间线 -->
    <div class="timeline-container">
      <a-spin :loading="loading && records.length === 0" class="timeline-spin">
        <div v-if="records.length === 0 && !loading" class="empty-state">
          <a-empty :description="t('history.ui.noRecords')" />
        </div>

        <div v-else class="timeline">
          <div
            v-for="record in records"
            :key="record.id"
            class="timeline-item"
          >
            <!-- 时间线节点 -->
            <div class="timeline-node">
              <div
                class="timeline-dot"
                :style="{ backgroundColor: getOperationColor(record.operationType) }"
              />
              <div class="timeline-line" />
            </div>

            <!-- 内容 -->
            <div class="timeline-content">
              <div class="content-header">
                <span class="operation-text" v-html="renderMessage(record)"></span>
              </div>
              <div class="content-meta">
                <span class="meta-item">
                  <IconUser class="meta-icon" />
                  <span>{{ record.operatorName || t('history.ui.systemOperator') }}</span>
                </span>
                <span
                  v-if="record.operationSource.type !== 'USER'"
                  class="source-tag"
                >
                  {{ getSourceLabel(record) }}
                </span>
                <a-tooltip :content="formatFullTime(record.createdAt)">
                  <span class="meta-item time">
                    <IconClockCircle class="meta-icon" />
                    <span>{{ formatRelativeTime(record.createdAt) }}</span>
                  </span>
                </a-tooltip>
              </div>
            </div>
          </div>

          <!-- 加载更多 -->
          <div v-if="hasMore" class="load-more">
            <a-button
              type="text"
              size="small"
              :loading="loading"
              @click="loadMore"
            >
              {{ t('history.ui.loadMore') }}
            </a-button>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<style scoped lang="scss">
.activity-log-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

// 工具栏
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-2);
  flex-shrink: 0;
}

.toolbar-left {
  .record-count {
    font-size: 13px;
    color: var(--color-text-3);
  }
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;

  .filter-active {
    color: rgb(var(--primary-6));
    background-color: rgb(var(--primary-1));
  }
}

// 筛选面板
.filter-panel {
  border-radius: 4px;
  padding: 12px;
  margin: 8px 0;
  flex-shrink: 0;
}

.filter-section {
  margin-bottom: 12px;

  &:last-of-type {
    margin-bottom: 8px;
  }

  :deep(.arco-picker) {
    background-color: transparent;
    border: 1px solid var(--color-border-1);
  }
}

.filter-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-2);
  margin-bottom: 8px;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  :deep(.arco-checkbox) {
    margin-right: 0;
  }
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border-2);
}

// 时间线容器
.timeline-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
}

.timeline-spin {
  min-height: 200px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

// 时间线
.timeline {
  position: relative;
}

.timeline-item {
  display: flex;
  padding-bottom: 20px;

  &:last-child {
    padding-bottom: 0;

    .timeline-line {
      display: none;
    }
  }
}

.timeline-node {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 24px;
  flex-shrink: 0;
  margin-right: 12px;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: rgb(var(--primary-6));
  flex-shrink: 0;
  z-index: 1;
}

.timeline-line {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: calc(100% - 4px);
  background-color: var(--color-border-2);
}

.timeline-content {
  flex: 1;
  min-width: 0;
  padding-top: -2px;
}

.content-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.operation-text {
  font-size: 14px;
  color: var(--color-text-1);

  // 已删除的字段使用删除线样式
  :deep(.deleted-field) {
    text-decoration: line-through;
    color: var(--color-text-3);
  }
}

.source-tag {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-2);
  padding: 2px 8px;
  background: var(--color-fill-2);
  border-radius: 4px;
}

.content-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-3);

  &.time {
    cursor: help;
  }
}

.meta-icon {
  font-size: 12px;
}

// 加载更多
.load-more {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

// 过渡动画
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.2s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

// Diff 样式
:deep(.diff-container) {
  margin-top: 8px;
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
  width: 800px;
}

:deep(.diff-hunk) {
  border-bottom: 1px solid var(--color-border-2);

  &:last-child {
    border-bottom: none;
  }
}

:deep(.diff-hunk-header) {
  background-color: var(--color-fill-2);
  color: var(--color-text-3);
  padding: 4px 8px;
  font-size: 11px;
}

:deep(.diff-line) {
  display: flex;
  padding: 0 8px;
  white-space: pre-wrap;
  word-break: break-all;
}

:deep(.diff-line-prefix) {
  flex-shrink: 0;
  width: 16px;
  user-select: none;
  color: var(--color-text-3);
}

:deep(.diff-line-content) {
  flex: 1;
  min-width: 0;
}

:deep(.diff-line-add) {
  background-color: rgba(var(--green-6), 0.15);

  .diff-line-prefix {
    color: rgb(var(--green-6));
  }
}

:deep(.diff-line-delete) {
  background-color: rgba(var(--red-6), 0.15);

  .diff-line-prefix {
    color: rgb(var(--red-6));
  }
}

:deep(.diff-line-context) {
  background-color: transparent;
}

:deep(.diff-no-changes) {
  color: var(--color-text-3);
  font-style: italic;
}
</style>
