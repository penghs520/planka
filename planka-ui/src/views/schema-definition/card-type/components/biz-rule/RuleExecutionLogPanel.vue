<script setup lang="ts">
/**
 * 规则执行日志面板
 * 以Drawer形式展示执行日志列表，支持筛选和分页加载
 */
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  IconFilter,
  IconRefresh,
  IconClockCircle,
  IconSortAscending,
  IconSortDescending,
  IconCheckCircleFill,
  IconCloseCircleFill,
  IconMinusCircleFill,
  IconIdcard,
  IconUser,
  IconFile,
  IconDown,
  IconUp,
  IconCopy,
} from '@arco-design/web-vue/es/icon'
import { Message } from '@arco-design/web-vue'
import { ruleExecutionLogApi } from '@/api/rule-execution-log'
import type {
  RuleExecutionLog,
  RuleExecutionLogSearchRequest,
  RuleExecutionLogFilters,
  ExecutionStatus,
} from '@/types/rule-execution-log'
import { formatDistanceToNow, format, parseISO } from 'date-fns'
import { zhCN, enUS } from 'date-fns/locale'
import { getLocale } from '@/i18n'

const { t } = useI18n()

const props = defineProps<{
  cardTypeId: string
}>()

const visible = defineModel<boolean>('visible', { default: false })

// 状态
const loading = ref(false)
const records = ref<RuleExecutionLog[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const hasMore = computed(() => records.value.length < total.value)

// 筛选状态
const showFilter = ref(false)
const filters = ref<RuleExecutionLogFilters | null>(null)
const selectedRuleIds = ref<string[]>([])
const selectedStatuses = ref<ExecutionStatus[]>([])
const dateRange = ref<[Date, Date] | null>(null)

// 排序状态（false=倒序/最新在前）
const sortAsc = ref(false)

// 展开的记录ID集合
const expandedIds = ref<Set<string>>(new Set())

// 切换展开状态
function toggleExpand(id: string) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
  // 触发响应式更新
  expandedIds.value = new Set(expandedIds.value)
}

// 检查是否展开
function isExpanded(id: string): boolean {
  return expandedIds.value.has(id)
}

// 复制到剪贴板
async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    Message.success(t('admin.bizRule.executionLog.copySuccess'))
  } catch {
    Message.error(t('admin.bizRule.executionLog.copyFailed'))
  }
}

// 获取动作类型标签
function getActionTypeLabel(actionType: string): string {
  return t(`admin.bizRule.actionType.${actionType}`, actionType)
}

// 加载日志
async function loadLogs(reset = false) {
  if (!props.cardTypeId) return
  if (loading.value) return

  if (reset) {
    page.value = 1
    records.value = []
  }

  loading.value = true
  try {
    const request: RuleExecutionLogSearchRequest = {
      page: page.value,
      size: size.value,
      sortAsc: sortAsc.value,
    }

    if (selectedRuleIds.value.length > 0) {
      request.ruleIds = selectedRuleIds.value
    }

    if (selectedStatuses.value.length > 0) {
      request.statuses = selectedStatuses.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      const [start, end] = dateRange.value
      request.startTime = formatDateTimeForApi(start)
      request.endTime = formatDateTimeForApi(end, true)
    }

    const result = await ruleExecutionLogApi.search(props.cardTypeId, request)

    if (reset) {
      records.value = result.content
    } else {
      records.value.push(...result.content)
    }
    total.value = result.total
  } catch (error) {
    console.error('加载执行日志失败:', error)
  } finally {
    loading.value = false
  }
}

// 格式化日期时间为 API 格式
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
  loadLogs()
}

// 刷新
function refresh() {
  loadLogs(true)
}

// 加载筛选选项
async function loadFilters() {
  if (!props.cardTypeId) return
  try {
    filters.value = await ruleExecutionLogApi.getFilters(props.cardTypeId)
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
  loadLogs(true)
}

// 清除筛选
function clearFilter() {
  selectedRuleIds.value = []
  selectedStatuses.value = []
  dateRange.value = null
  showFilter.value = false
  loadLogs(true)
}

// 切换排序
function toggleSort() {
  sortAsc.value = !sortAsc.value
  loadLogs(true)
}

// 获取日期格式化的 locale
function getDateLocale() {
  return getLocale() === 'zh-CN' ? zhCN : enUS
}

// 格式化相对时间
function formatRelativeTime(dateStr: string): string {
  try {
    const date = parseISO(dateStr)
    return formatDistanceToNow(date, { addSuffix: true, locale: getDateLocale() })
  } catch {
    return dateStr
  }
}

// 格式化完整时间
function formatFullTime(dateStr: string): string {
  try {
    const date = parseISO(dateStr)
    return format(date, 'yyyy-MM-dd HH:mm:ss', { locale: getDateLocale() })
  } catch {
    return dateStr
  }
}

// 获取状态颜色
function getStatusColor(status: ExecutionStatus): string {
  switch (status) {
    case 'SUCCESS':
      return 'rgb(var(--green-6))'
    case 'FAILED':
      return 'rgb(var(--red-6))'
    case 'SKIPPED':
      return 'rgb(var(--orange-6))'
    default:
      return 'rgb(var(--primary-6))'
  }
}

// 获取状态标签
function getStatusLabel(status: ExecutionStatus): string {
  return t(`admin.bizRule.executionLog.status.${status}`)
}

// 获取触发事件标签
function getTriggerEventLabel(event: string): string {
  return t(`admin.bizRule.triggerEvent.${event}`)
}

// 是否有激活的筛选
const hasActiveFilter = computed(() => {
  return (
    selectedRuleIds.value.length > 0 ||
    selectedStatuses.value.length > 0 ||
    dateRange.value !== null
  )
})

// 监听可见性变化
watch(
  visible,
  (newVisible) => {
    if (newVisible && props.cardTypeId) {
      loadLogs(true)
    }
  },
  { immediate: true }
)
</script>

<template>
  <a-drawer
    v-model:visible="visible"
    :title="t('admin.bizRule.executionLog.title')"
    :width="800"
    :footer="false"
    unmount-on-close
  >
    <div class="execution-log-panel">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="record-count">{{
            t('admin.bizRule.executionLog.recordCount', { count: total })
          }}</span>
        </div>
        <div class="toolbar-right">
          <a-tooltip
            :content="sortAsc ? t('admin.bizRule.executionLog.sortAsc') : t('admin.bizRule.executionLog.sortDesc')"
          >
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
            {{ t('admin.bizRule.executionLog.filter') }}
          </a-button>
        </div>
      </div>

      <!-- 筛选面板 -->
      <transition name="slide-down">
        <div v-if="showFilter" class="filter-panel">
          <div class="filter-section">
            <div class="filter-label">{{ t('admin.bizRule.executionLog.filterByRule') }}</div>
            <a-select
              v-model="selectedRuleIds"
              :placeholder="t('admin.bizRule.executionLog.selectRule')"
              multiple
              allow-clear
              style="width: 100%"
            >
              <a-option
                v-for="rule in filters?.rules || []"
                :key="rule.ruleId"
                :value="rule.ruleId"
              >
                {{ rule.ruleName }}
              </a-option>
            </a-select>
          </div>
          <div class="filter-section">
            <div class="filter-label">{{ t('admin.bizRule.executionLog.filterByStatus') }}</div>
            <a-checkbox-group v-model="selectedStatuses" class="filter-options">
              <a-checkbox
                v-for="status in filters?.statuses || []"
                :key="status"
                :value="status"
              >
                {{ getStatusLabel(status) }}
              </a-checkbox>
            </a-checkbox-group>
          </div>
          <div class="filter-section">
            <div class="filter-label">{{ t('admin.bizRule.executionLog.filterByTime') }}</div>
            <a-range-picker
              v-model="dateRange"
              style="width: 100%"
              :placeholder="[
                t('admin.bizRule.executionLog.startDate'),
                t('admin.bizRule.executionLog.endDate'),
              ]"
              allow-clear
            />
          </div>
          <div class="filter-actions">
            <a-button size="small" @click="clearFilter">{{
              t('admin.bizRule.executionLog.clear')
            }}</a-button>
            <a-button type="primary" size="small" @click="applyFilter">{{
              t('admin.bizRule.executionLog.apply')
            }}</a-button>
          </div>
        </div>
      </transition>

      <!-- 日志列表 -->
      <div class="log-list-container">
        <a-spin :loading="loading && records.length === 0" class="log-list-spin">
          <div v-if="records.length === 0 && !loading" class="empty-state">
            <a-empty :description="t('admin.bizRule.executionLog.noRecords')" />
          </div>

          <div v-else class="log-list">
            <div v-for="record in records" :key="record.id" class="log-item">
              <!-- 状态图标 -->
              <div class="status-icon">
                <IconCheckCircleFill
                  v-if="record.status === 'SUCCESS'"
                  :style="{ color: getStatusColor('SUCCESS') }"
                />
                <IconCloseCircleFill
                  v-else-if="record.status === 'FAILED'"
                  :style="{ color: getStatusColor('FAILED') }"
                />
                <IconMinusCircleFill
                  v-else
                  :style="{ color: getStatusColor('SKIPPED') }"
                />
              </div>

              <!-- 内容 -->
              <div class="log-content">
                <!-- 头部：规则名称和状态 -->
                <div class="log-header">
                  <span class="rule-name">{{ record.ruleName }}</span>
                  <a-tag :color="record.status === 'SUCCESS' ? 'green' : record.status === 'FAILED' ? 'red' : 'orange'" size="small">
                    {{ getStatusLabel(record.status) }}
                  </a-tag>
                </div>

                <!-- 基础信息行 -->
                <div class="log-info">
                  <span class="trigger-event">{{ getTriggerEventLabel(record.triggerEvent) }}</span>
                  <span v-if="record.durationMs > 0" class="duration">
                    {{ record.durationMs }}ms
                  </span>
                </div>

                <!-- 详细信息 -->
                <div class="log-details">
                  <!-- 触发卡片 -->
                  <div v-if="record.cardTitle || record.cardId" class="detail-item">
                    <IconIdcard class="detail-icon" />
                    <span class="detail-label">{{ t('admin.bizRule.executionLog.triggerCard') }}:</span>
                    <span class="detail-value">{{ record.cardTitle || record.cardId }}</span>
                  </div>

                  <!-- 操作人 -->
                  <div v-if="record.operatorName || record.operatorId" class="detail-item">
                    <IconUser class="detail-icon" />
                    <span class="detail-label">{{ t('admin.bizRule.executionLog.operator') }}:</span>
                    <span class="detail-value">{{ record.operatorName || record.operatorId }}</span>
                  </div>
                </div>

                <!-- 受影响卡片数（独立行） -->
                <div v-if="record.affectedCardIds && record.affectedCardIds.length > 0" class="detail-item affected-cards-row">
                  <IconFile class="detail-icon" />
                  <span class="detail-label">{{ t('admin.bizRule.executionLog.affectedCards') }}:</span>
                  <span class="detail-value">{{ record.affectedCardIds.length }}</span>
                </div>

                <!-- 错误信息 -->
                <div v-if="record.errorMessage" class="error-message">
                  {{ record.errorMessage }}
                </div>

                <!-- 时间和操作 -->
                <div class="log-meta">
                  <a-tooltip :content="formatFullTime(record.executionTime)">
                    <span class="meta-item time">
                      <IconClockCircle class="meta-icon" />
                      <span>{{ formatRelativeTime(record.executionTime) }}</span>
                    </span>
                  </a-tooltip>

                  <!-- 展开/收起按钮 -->
                  <a-button
                    v-if="record.actionResults?.length || record.traceId || (record.affectedCardIds && record.affectedCardIds.length > 0)"
                    type="text"
                    size="mini"
                    class="expand-btn"
                    @click="toggleExpand(record.id)"
                  >
                    <template #icon>
                      <IconUp v-if="isExpanded(record.id)" />
                      <IconDown v-else />
                    </template>
                    {{ isExpanded(record.id) ? t('admin.bizRule.executionLog.collapse') : t('admin.bizRule.executionLog.expand') }}
                  </a-button>
                </div>

                <!-- 展开的详细内容 -->
                <transition name="slide-down">
                  <div v-if="isExpanded(record.id)" class="expanded-content">
                    <!-- 追踪ID -->
                    <div v-if="record.traceId" class="expanded-section">
                      <div class="section-label">{{ t('admin.bizRule.executionLog.traceId') }}</div>
                      <div class="trace-id">
                        <code>{{ record.traceId }}</code>
                        <a-button type="text" size="mini" @click="copyToClipboard(record.traceId!)">
                          <template #icon><IconCopy /></template>
                        </a-button>
                      </div>
                    </div>

                    <!-- 受影响的卡片ID列表 -->
                    <div v-if="record.affectedCardIds && record.affectedCardIds.length > 0" class="expanded-section">
                      <div class="section-label">{{ t('admin.bizRule.executionLog.affectedCardIds') }}</div>
                      <div class="card-ids-list">
                        <a-tag v-for="cardId in record.affectedCardIds" :key="cardId" size="small" class="card-id-tag">
                          {{ cardId }}
                        </a-tag>
                      </div>
                    </div>

                    <!-- 动作执行结果 -->
                    <div v-if="record.actionResults?.length" class="expanded-section">
                      <div class="section-label">{{ t('admin.bizRule.executionLog.actionResults') }}</div>
                      <div class="action-results">
                        <div
                          v-for="(action, index) in record.actionResults"
                          :key="index"
                          class="action-result-item"
                        >
                          <div class="action-header">
                            <span class="action-order">#{{ action.sortOrder + 1 }}</span>
                            <span class="action-type">{{ getActionTypeLabel(action.actionType) }}</span>
                            <a-tag :color="action.success ? 'green' : 'red'" size="small">
                              {{ action.success ? t('admin.bizRule.executionLog.status.SUCCESS') : t('admin.bizRule.executionLog.status.FAILED') }}
                            </a-tag>
                            <span v-if="action.durationMs > 0" class="action-duration">{{ action.durationMs }}ms</span>
                          </div>
                          <div v-if="action.errorMessage" class="action-error">
                            {{ action.errorMessage }}
                          </div>
                          <div v-if="action.affectedCardIds?.length" class="action-affected">
                            {{ t('admin.bizRule.executionLog.actionAffected', { count: action.affectedCardIds.length }) }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </transition>
              </div>
            </div>

            <!-- 加载更多 -->
            <div v-if="hasMore" class="load-more">
              <a-button type="text" size="small" :loading="loading" @click="loadMore">
                {{ t('admin.bizRule.executionLog.loadMore') }}
              </a-button>
            </div>
          </div>
        </a-spin>
      </div>
    </div>
  </a-drawer>
</template>

<style scoped lang="scss">
// 覆盖Drawer默认样式，减少内边距以增加内容区宽度
:deep(.arco-drawer-body) {
  padding: 16px 12px;
}

.execution-log-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}

// 工具栏
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
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
  background-color: var(--color-fill-1);
}

.filter-section {
  margin-bottom: 12px;

  &:last-of-type {
    margin-bottom: 8px;
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

// 日志列表容器
.log-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
  width: 100%;
}

.log-list-spin {
  min-height: 200px;
  width: 100%;

  :deep(.arco-spin-children) {
    width: 100%;
  }
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

// 日志列表
.log-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 12px 8px;
  background-color: var(--color-fill-1);
  border-radius: 4px;
  width: 100%;
  box-sizing: border-box;
}

.status-icon {
  flex-shrink: 0;
  font-size: 18px;
  line-height: 1;
  padding-top: 2px;
}

.log-content {
  flex: 1;
  min-width: 0;
}

.log-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.rule-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
}

.log-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--color-text-3);
  margin-bottom: 6px;
}

.trigger-event {
  color: var(--color-text-2);
}

.duration {
  font-family: monospace;
}

// 详细信息
.log-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 6px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-3);

  &.affected-cards-row {
    margin-bottom: 6px;
  }
}

.detail-icon {
  font-size: 12px;
  color: var(--color-text-4);
}

.detail-label {
  color: var(--color-text-3);
}

.detail-value {
  color: var(--color-text-2);

  &.link {
    cursor: pointer;
    color: rgb(var(--primary-6));

    &:hover {
      text-decoration: underline;
    }
  }
}

.error-message {
  font-size: 12px;
  color: rgb(var(--red-6));
  margin-bottom: 6px;
  word-break: break-word;
  padding: 6px 8px;
  background-color: rgb(var(--red-1));
  border-radius: 4px;
}

.log-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.expand-btn {
  font-size: 12px;
  color: var(--color-text-3);
  padding: 0 4px;
}

// 展开的内容
.expanded-content {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--color-border-2);
}

.expanded-section {
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-2);
  margin-bottom: 6px;
}

.trace-id {
  display: flex;
  align-items: center;
  gap: 4px;

  code {
    font-size: 11px;
    padding: 2px 6px;
    background-color: var(--color-fill-2);
    border-radius: 2px;
    font-family: monospace;
    color: var(--color-text-2);
  }
}

.card-ids-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.card-id-tag {
  font-family: monospace;
  font-size: 11px;
}

// 动作结果
.action-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-result-item {
  padding: 8px;
  background-color: var(--color-fill-2);
  border-radius: 4px;
}

.action-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-order {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-3);
}

.action-type {
  font-size: 12px;
  color: var(--color-text-1);
}

.action-duration {
  font-size: 11px;
  font-family: monospace;
  color: var(--color-text-3);
}

.action-error {
  font-size: 11px;
  color: rgb(var(--red-6));
  margin-top: 4px;
}

.action-affected {
  font-size: 11px;
  color: var(--color-text-3);
  margin-top: 4px;
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
</style>
