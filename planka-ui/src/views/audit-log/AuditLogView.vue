<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
import { IconRefresh } from '@arco-design/web-vue/es/icon'
import { schemaApi, memberApi } from '@/api'
import type { SchemaChangelogDTO } from '@/types/schema'
import type { MemberOptionDTO } from '@/types/member'
import { SchemaTypeConfig, SchemaType } from '@/types/schema'
import { formatDateTimeWithSeconds } from '@/utils/format'

const { t } = useI18n()

const loading = ref(false)
const loadingMore = ref(false)
const changelogs = ref<SchemaChangelogDTO[]>([])
const currentPage = ref(0)
const pageSize = ref(20)
const hasMore = ref(true)
const total = ref(0)

// 搜索关键字
const searchKeyword = ref('')
const searchInputValue = ref('')

// Schema 类型筛选
const selectedSchemaType = ref<string | undefined>(undefined)

// 操作人筛选
const selectedOperator = ref<string | undefined>(undefined)
const memberOptions = ref<MemberOptionDTO[]>([])
const loadingMembers = ref(false)
const membersLoaded = ref(false)

// Schema 类型选项列表
const schemaTypeOptions = computed(() => {
  const options: { value: string; label: string }[] = [
    { value: '', label: t('admin.auditLog.allTypes') },
  ]
  for (const [key, config] of Object.entries(SchemaTypeConfig)) {
    options.push({ value: key, label: config.label })
  }
  return options
})

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

// 获取变更历史
async function fetchChangelog(reset = false) {
  if (reset) {
    currentPage.value = 0
    changelogs.value = []
    hasMore.value = true
  }

  const isFirstPage = currentPage.value === 0
  if (isFirstPage) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const keyword = searchKeyword.value.trim() || undefined
    const schemaType = selectedSchemaType.value || undefined
    const changedBy = selectedOperator.value || undefined
    const result = await schemaApi.getGlobalChangelog(currentPage.value, pageSize.value, keyword, schemaType, changedBy)
    const newData = result.content || []
    total.value = result.total || 0

    if (isFirstPage) {
      changelogs.value = newData
    } else {
      changelogs.value = [...changelogs.value, ...newData]
    }

    hasMore.value = changelogs.value.length < total.value
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
  fetchChangelog(true)
}

// 清除搜索
function handleClearSearch() {
  searchInputValue.value = ''
  searchKeyword.value = ''
  fetchChangelog(true)
}

// Schema 类型变更
function handleSchemaTypeChange() {
  fetchChangelog(true)
}

// 操作人变更
function handleOperatorChange() {
  fetchChangelog(true)
}

// 滚动事件处理
function handleScroll(event: Event) {
  const target = event.target as HTMLElement
  const { scrollTop, scrollHeight, clientHeight } = target
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

// 获取 Schema 类型的显示标签
function getSchemaTypeLabel(schemaType: string): string {
  const config = SchemaTypeConfig[schemaType as SchemaType]
  return config?.label || schemaType
}

onMounted(() => {
  fetchChangelog()
})
</script>

<template>
  <div class="audit-log-page">
    <div class="audit-log-content">
      <div class="content-header">
        <div class="header-title">{{ t('admin.auditLog.title') }}</div>
        <div class="header-spacer"></div>
        <a-select
          v-model="selectedSchemaType"
          :placeholder="t('admin.auditLog.selectType')"
          style="width: 160px"
          size="small"
          allow-clear
          @change="handleSchemaTypeChange"
        >
          <a-option v-for="opt in schemaTypeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-option>
        </a-select>
        <a-select
          v-model="selectedOperator"
          :placeholder="t('admin.auditLog.selectOperator')"
          :loading="loadingMembers"
          style="width: 160px"
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
          :placeholder="t('admin.auditLog.searchPlaceholder')"
          style="width: 260px"
          size="small"
          allow-clear
          @search="handleSearch"
          @clear="handleClearSearch"
          @press-enter="handleSearch"
        />
        <a-button size="small" :loading="loading" @click="fetchChangelog(true)">
          <template #icon>
            <IconRefresh />
          </template>
        </a-button>
      </div>

      <a-spin :loading="loading" class="changelog-spin">
        <div v-if="changelogs.length > 0" class="changelog-scroll" @scroll="handleScroll">
          <a-timeline>
            <a-timeline-item v-for="log in changelogs" :key="log.id">
              <div class="changelog-item">
                <!-- 头部：操作类型、Schema类型、Schema名称、时间 -->
                <div class="changelog-header">
                  <a-tag :color="formatAction(log.action).color" size="small">
                    {{ formatAction(log.action).text }}
                  </a-tag>
                  <a-tag color="purple" size="small">
                    {{ getSchemaTypeLabel(log.schemaType) }}
                  </a-tag>
                  <span class="schema-name">{{ log.schemaName || log.schemaId }}</span>
                  <span class="changelog-time">{{ formatDateTimeWithSeconds(log.changedAt) }}</span>
                </div>

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
                          <a-tag color="green" size="small">{{ t('admin.changelog.changeType.ADDED') }}</a-tag>
                          <span class="new-value">{{ formatDisplayValue(change.newValue) }}</span>
                        </template>
                        <template v-else-if="change.changeType === 'REMOVED'">
                          <a-tag color="red" size="small">{{ t('admin.changelog.changeType.REMOVED') }}</a-tag>
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
        <a-empty v-else-if="!loading" :description="t('admin.auditLog.empty')" />
      </a-spin>
    </div>
  </div>
</template>

<style scoped>
.audit-log-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.audit-log-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-2);
  border-radius: 4px;
  overflow: hidden;
}

.content-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
}

.header-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-1);
}

.header-spacer {
  flex: 1;
}

.changelog-spin {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 16px;
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
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.schema-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
}

.changelog-time {
  font-size: 12px;
  color: var(--color-text-3);
  margin-left: auto;
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
