<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
import { schemaApi, memberApi } from '@/api'
import type { SchemaChangelogDTO } from '@/types/schema'
import type { MemberOptionDTO } from '@/types/member'
import { SchemaTypeConfig, SchemaType } from '@/types/schema'
import { formatDateTimeWithSeconds } from '@/utils/format'
import SchemaSnapshotDrawer from './SchemaSnapshotDrawer.vue'

interface Props {
  visible: boolean
  schemaId?: string
  schemaName?: string
  /** 是否包含附属Schema的变更日志 */
  includeChildren?: boolean
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'restored'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()

const loading = ref(false)
const loadingMore = ref(false)
const changelogs = ref<SchemaChangelogDTO[]>([])
const currentPage = ref(0)
const pageSize = ref(20)
const hasMore = ref(true)

// 搜索关键字
const searchKeyword = ref('')
const searchInputValue = ref('')

// 操作人筛选
const selectedOperator = ref<string | undefined>(undefined)
const memberOptions = ref<MemberOptionDTO[]>([])
const loadingMembers = ref(false)
const membersLoaded = ref(false)

// 操作人选项列表
const operatorOptions = computed(() => {
  const options: { value: string; label: string }[] = [
    { value: '', label: t('admin.auditLog.allOperators') },
  ]
  for (const member of memberOptions.value) {
    options.push({
      value: member.memberCardId,
      label: member.name,
    })
  }
  return options
})

// 加载成员选项列表
async function loadMemberOptions(keyword?: string) {
  loadingMembers.value = true
  try {
    const result = await memberApi.getOptions(0, 20, keyword)
    memberOptions.value = result.content || []
    if (!keyword) {
      membersLoaded.value = true
    }
  } catch (error) {
    console.error('Failed to load member options:', error)
  } finally {
    loadingMembers.value = false
  }
}

// 操作人下拉框打开时加载
function handleOperatorDropdownVisibleChange(visible: boolean) {
  if (visible && !loadingMembers.value) {
    loadMemberOptions()
  }
}

// 操作人搜索
function handleOperatorSearch(keyword: string) {
  loadMemberOptions(keyword || undefined)
}

// 操作人变更
function handleOperatorChange() {
  changelogs.value = []
  currentPage.value = 0
  hasMore.value = true
  fetchChangelog()
}

// 快照抽屉状态
const snapshotDrawerVisible = ref(false)
const selectedChangelog = ref<SchemaChangelogDTO | null>(null)

// 最新版本号（取第一条记录的版本号）
const latestVersion = computed(() => {
  const first = changelogs.value[0]
  return first?.contentVersion
})

// 是否有数据
const hasData = computed(() => changelogs.value.length > 0)

// 抽屉标题
const drawerTitle = computed(() => {
  if (props.schemaName) {
    return t('admin.changelog.titleWithName', { name: props.schemaName })
  }
  return t('admin.changelog.title')
})

// 监听 visible 和 schemaId 变化
watch([() => props.visible, () => props.schemaId], async ([newVisible, newId]) => {
  if (newVisible && newId) {
    // 重置状态
    changelogs.value = []
    currentPage.value = 0
    hasMore.value = true
    searchKeyword.value = ''
    searchInputValue.value = ''
    selectedOperator.value = undefined
    membersLoaded.value = false
    memberOptions.value = []
    await fetchChangelog()
  } else if (!newVisible) {
    // 关闭时重置状态
    changelogs.value = []
    membersLoaded.value = false
    memberOptions.value = []
  }
})

// 获取变更历史
async function fetchChangelog() {
  if (!props.schemaId) return

  const isFirstPage = currentPage.value === 0
  if (isFirstPage) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const keyword = searchKeyword.value.trim() || undefined
    const changedBy = selectedOperator.value || undefined
    const result = await schemaApi.getChangelog(
      props.schemaId,
      currentPage.value,
      pageSize.value,
      keyword,
      props.includeChildren ?? false,
      changedBy,
    )
    // 兼容两种返回格式：PageResult 或 数组
    const newData = Array.isArray(result) ? result : (result.content || [])
    const total = Array.isArray(result) ? newData.length : (result.total || 0)

    if (isFirstPage) {
      changelogs.value = newData
    } else {
      changelogs.value = [...changelogs.value, ...newData]
    }

    // 判断是否还有更多数据
    if (Array.isArray(result)) {
      // 如果返回的是数组，根据返回数量判断
      hasMore.value = newData.length >= pageSize.value
    } else {
      // 如果返回的是 PageResult
      hasMore.value = changelogs.value.length < total
    }
  } catch (error) {
    console.error('Failed to fetch changelog:', error)
    Message.error(t('admin.changelog.loadFailed'))
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 加载更多
function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  currentPage.value++
  fetchChangelog()
}

// 搜索处理
function handleSearch() {
  searchKeyword.value = searchInputValue.value
  changelogs.value = []
  currentPage.value = 0
  hasMore.value = true
  fetchChangelog()
}

// 清除搜索
function handleClearSearch() {
  searchInputValue.value = ''
  searchKeyword.value = ''
  changelogs.value = []
  currentPage.value = 0
  hasMore.value = true
  fetchChangelog()
}

// 滚动事件处理
function handleScroll(event: Event) {
  const target = event.target as HTMLElement
  const { scrollTop, scrollHeight, clientHeight } = target
  // 距离底部 100px 时触发加载
  if (scrollHeight - scrollTop - clientHeight < 100) {
    loadMore()
  }
}

// 格式化操作类型
function formatAction(action: string): { text: string; color: string } {
  const colorMap: Record<string, string> = {
    CREATE: 'green',
    UPDATE: 'blue',
    DELETE: 'red',
  }
  const text = t(`admin.changelog.action.${action}`, action)
  return { text, color: colorMap[action] || 'gray' }
}


// 格式化语义变更操作类型
function formatSemanticOperation(operation: string): { text: string; color: string } {
  const colorMap: Record<string, string> = {
    ADDED: 'green',
    MODIFIED: 'blue',
    REMOVED: 'red',
    REORDERED: 'orange',
  }
  const text = t(`admin.changelog.operation.${operation}`, operation)
  return { text, color: colorMap[operation] || 'gray' }
}

// 格式化类别
function formatCategory(category: string): string {
  return t(`admin.changelog.category.${category}`, category)
}

// 格式化显示值
function formatDisplayValue(value: unknown): string {
  if (value === null || value === undefined) return t('admin.changelog.format.empty')
  if (typeof value === 'boolean') return value ? t('admin.changelog.format.yes') : t('admin.changelog.format.no')
  if (typeof value === 'string' && value === '') return t('admin.changelog.format.empty')
  if (Array.isArray(value)) return t('admin.changelog.format.items', { count: value.length })
  return String(value)
}

// 检查是否有变更详情
function hasChangeDetail(log: SchemaChangelogDTO): boolean {
  if (!log.changeDetail) return false
  const detail = log.changeDetail
  return (detail.changes?.length > 0) || (detail.semanticChanges?.length > 0)
}

// 获取基础摘要（不含详细变更）
function getBasicSummary(log: SchemaChangelogDTO): string {
  const actionText = formatAction(log.action).text
  // 如果有 schemaName（附属 Schema），使用它；否则使用主 Schema 名称
  const name = log.schemaName || props.schemaName || log.schemaId
  return t('admin.changelog.format.actionSummary', { action: actionText, name })
}

// 判断是否为附属 Schema 的变更（schemaId 与主 Schema 不同）
function isChildSchemaChange(log: SchemaChangelogDTO): boolean {
  return props.includeChildren === true && log.schemaId !== props.schemaId
}

// 获取 Schema 类型的显示标签
function getSchemaTypeLabel(schemaType: string): string {
  const config = SchemaTypeConfig[schemaType as SchemaType]
  return config?.label || schemaType
}

// 关闭抽屉
function handleClose() {
  emit('update:visible', false)
}

// 查看快照
function viewSnapshot(log: SchemaChangelogDTO) {
  selectedChangelog.value = log
  snapshotDrawerVisible.value = true
}

// 检查是否有快照数据
function hasSnapshot(log: SchemaChangelogDTO): boolean {
  return !!(log.beforeSnapshot || log.afterSnapshot)
}

// 还原成功后刷新数据
function handleRestored() {
  // 重新加载变更历史
  changelogs.value = []
  currentPage.value = 0
  hasMore.value = true
  fetchChangelog()
  // 通知父组件
  emit('restored')
}
</script>

<template>
  <a-drawer
    :visible="visible"
    :title="drawerTitle"
    :width="560"
    :footer="false"
    :body-style="{ padding: '16px', height: 'calc(100vh - 60px)', overflow: 'hidden', display: 'flex', flexDirection: 'column' }"
    unmount-on-close
    @cancel="handleClose"
  >
    <!-- 搜索和筛选 -->
    <div class="search-bar">
      <a-select
        v-model="selectedOperator"
        :placeholder="t('admin.auditLog.selectOperator')"
        :loading="loadingMembers"
        style="width: 140px"
        size="small"
        allow-clear
        allow-search
        :filter-option="false"
        @change="handleOperatorChange"
        @popup-visible-change="handleOperatorDropdownVisibleChange"
        @search="handleOperatorSearch"
      >
        <a-option v-for="opt in operatorOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </a-option>
      </a-select>
      <a-input-search
        v-model="searchInputValue"
        :placeholder="t('admin.changelog.searchPlaceholder')"
        style="flex: 1"
        allow-clear
        @search="handleSearch"
        @clear="handleClearSearch"
        @press-enter="handleSearch"
      />
    </div>

    <a-spin :loading="loading" class="changelog-spin">
      <div v-if="hasData" class="changelog-scroll" @scroll="handleScroll">
        <a-timeline>
          <a-timeline-item v-for="log in changelogs" :key="log.id">
            <div class="changelog-item">
              <!-- 头部：操作类型、版本、时间 -->
              <div class="changelog-header">
                <a-tag :color="formatAction(log.action).color" size="small">
                  {{ formatAction(log.action).text }}
                </a-tag>
                <!-- 附属 Schema 标签 -->
                <a-tag v-if="isChildSchemaChange(log)" color="purple" size="small">
                  {{ getSchemaTypeLabel(log.schemaType) }}
                </a-tag>
                <span class="changelog-version">v{{ log.contentVersion }}</span>
                <span class="changelog-time">{{ formatDateTimeWithSeconds(log.changedAt) }}</span>
              </div>

              <!-- 基础摘要 -->
              <div class="changelog-summary">{{ getBasicSummary(log) }}</div>

              <!-- 详细变更 -->
              <template v-if="hasChangeDetail(log)">
                <div class="change-detail">
                  <!-- 字段级变更 -->
                  <template v-if="log.changeDetail?.changes?.length">
                    <div
                      v-for="(change, index) in log.changeDetail.changes"
                      :key="`field-${index}`"
                      class="change-item field-change"
                    >
                      <span class="change-label">{{ change.fieldLabel }}</span>
                      <span class="change-arrow">:</span>
                      <template v-if="change.changeType === 'MODIFIED'">
                        <span class="old-value">{{ formatDisplayValue(change.oldValue) }}</span>
                        <span class="change-arrow">→</span>
                        <span class="new-value">{{ formatDisplayValue(change.newValue) }}</span>
                      </template>
                      <template v-else-if="change.changeType === 'ADDED'">
                        <a-tag color="green" size="small">新增</a-tag>
                        <span class="new-value">{{ formatDisplayValue(change.newValue) }}</span>
                      </template>
                      <template v-else-if="change.changeType === 'REMOVED'">
                        <a-tag color="red" size="small">删除</a-tag>
                        <span class="old-value line-through">{{ formatDisplayValue(change.oldValue) }}</span>
                      </template>
                    </div>
                  </template>

                  <!-- 语义级变更 -->
                  <template v-if="log.changeDetail?.semanticChanges?.length">
                    <div
                      v-for="(change, index) in log.changeDetail.semanticChanges"
                      :key="`semantic-${index}`"
                      class="semantic-change-wrapper"
                    >
                      <div class="change-item semantic-change">
                        <a-tag :color="formatSemanticOperation(change.operation).color" size="small">
                          {{ formatSemanticOperation(change.operation).text }}
                        </a-tag>
                        <span class="change-target">{{ formatCategory(change.category) }}</span>
                        <span v-if="change.targetName" class="change-name">「{{ change.targetName }}」</span>
                      </div>
                      <!-- 语义变更的详细字段变更 -->
                      <template v-if="change.details?.length">
                        <div
                          v-for="(detail, detailIndex) in change.details"
                          :key="`detail-${index}-${detailIndex}`"
                          class="change-item field-change semantic-detail"
                        >
                          <span class="change-label">{{ detail.fieldLabel }}</span>
                          <span class="change-arrow">:</span>
                          <template v-if="detail.changeType === 'MODIFIED'">
                            <span class="old-value">{{ formatDisplayValue(detail.oldValue) }}</span>
                            <span class="change-arrow">→</span>
                            <span class="new-value">{{ formatDisplayValue(detail.newValue) }}</span>
                          </template>
                          <template v-else-if="detail.changeType === 'ADDED'">
                            <span class="new-value">{{ formatDisplayValue(detail.newValue) }}</span>
                          </template>
                          <template v-else-if="detail.changeType === 'REMOVED'">
                            <span class="old-value line-through">{{ formatDisplayValue(detail.oldValue) }}</span>
                          </template>
                        </div>
                      </template>
                    </div>
                  </template>
                </div>
              </template>

              <!-- 操作人 -->
              <div class="changelog-footer">
                <span class="changelog-operator">
                  {{ t('admin.changelog.operator') }}：{{ log.changedByName || log.changedBy || 'system' }}
                </span>
                <a-button
                  v-if="hasSnapshot(log)"
                  type="text"
                  size="mini"
                  @click="viewSnapshot(log)"
                >
                  {{ t('admin.changelog.viewSnapshot') }}
                </a-button>
              </div>
            </div>
          </a-timeline-item>
        </a-timeline>

        <!-- 加载更多提示 -->
        <div v-if="loadingMore" class="loading-more">
          <a-spin :size="16" />
          <span>{{ t('admin.changelog.loadingMore') }}</span>
        </div>
        <div v-else-if="!hasMore" class="no-more">
          {{ t('admin.changelog.loadedAll') }}
        </div>
      </div>
      <a-empty v-else-if="!loading" :description="t('admin.changelog.empty')" />
    </a-spin>

    <!-- 快照查看抽屉 -->
    <SchemaSnapshotDrawer
      v-model:visible="snapshotDrawerVisible"
      :changelog="selectedChangelog"
      :latest-version="latestVersion"
      @restored="handleRestored"
    />
  </a-drawer>
</template>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.changelog-spin {
  height: 100%;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.changelog-spin :deep(.arco-spin-children) {
  height: 100%;
}

.changelog-scroll {
  height: 100%;
  overflow-y: auto;
  padding-right: 8px;
}

.changelog-item {
  padding-bottom: 8px;
}

.changelog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.changelog-version {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-2);
  background: var(--color-fill-2);
  padding: 2px 8px;
  border-radius: 4px;
}

.changelog-time {
  font-size: 12px;
  color: var(--color-text-3);
}

.changelog-summary {
  font-size: 14px;
  color: var(--color-text-1);
  font-weight: 500;
  margin-bottom: 8px;
}

.change-detail {
  background: var(--color-fill-1);
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 8px;
}

.change-item {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--color-text-2);
}

.change-item + .change-item {
  margin-top: 4px;
}

.field-change .change-label {
  color: var(--color-text-2);
  font-weight: 500;
}

.change-arrow {
  color: var(--color-text-3);
  margin: 0 2px;
}

.old-value {
  color: var(--color-text-3);
  background: rgba(var(--red-1), 0.3);
  padding: 0 4px;
  border-radius: 3px;
}

.new-value {
  color: var(--color-text-1);
  background: rgba(var(--green-1), 0.3);
  padding: 0 4px;
  border-radius: 3px;
}

.line-through {
  text-decoration: line-through;
}

.semantic-change .change-target {
  color: var(--color-text-2);
}

.semantic-change .change-name {
  color: var(--color-text-1);
  font-weight: 500;
}

.semantic-change-wrapper {
  margin-bottom: 4px;
}

.semantic-change-wrapper:last-child {
  margin-bottom: 0;
}

.semantic-detail {
  margin-left: 16px;
  padding-left: 8px;
  border-left: 2px solid var(--color-border-2);
}

.changelog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.changelog-operator {
  font-size: 12px;
  color: var(--color-text-3);
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: var(--color-text-3);
  font-size: 12px;
}

.no-more {
  text-align: center;
  padding: 16px;
  color: var(--color-text-3);
  font-size: 12px;
}
</style>
