<script setup lang="ts">
/**
 * 列表视图数据面板
 * 展示卡片列表，支持排序、过滤、行内编辑等功能
 * 
 * 性能优化：
 * - 使用 shallowRef 存储大数据集，减少响应式开销
 * - 使用 ListViewCell 组件隔离单元格渲染
 * - 使用 CellEditorOverlay 浮层渲染编辑器，避免全表重渲染
 */
import { ref, shallowRef, computed, watch, inject, onUnmounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
// import { Message, Modal } from '@arco-design/web-vue'
import { IconPlusCircle } from '@arco-design/web-vue/es/icon'
import type {
  ListViewDataResponse,
  ViewDataRequest,
  ColumnMeta,
  // PageInfo,
  StatusOption,
} from '@/types/view-data'
import type { CardDTO } from '@/types/card'
import type { Condition } from '@/types/condition'
import { getCardId } from '@/types/card'
import { createEmptyViewDataRequest } from '@/types/view-data'
import { viewDataApi } from '@/api/view-data'
// import { cardApi } from '@/api/card'
import CardCreateModal from '../shared/CardCreateModal.vue'
import ListViewCell from './ListViewCell.vue'
import CellEditorOverlay from './CellEditorOverlay.vue'
import ViewToolbar, { type RowHeightType } from '@/components/view-toolbar/ViewToolbar.vue'
import { useCardTabsStore } from '@/stores/cardTabs'
import { useCellEditing } from '../../composables/list/useCellEditing'
import type { ColumnMeta as ViewColumnMeta } from '@/types/view-data'

const PAGE_SIZE = 40

const props = defineProps<{
  viewId: string
}>()

const { t } = useI18n()

// Injections
const setViewName = inject<(name: string) => void>('setViewName')
const setViewColumns = inject<(columns: ViewColumnMeta[]) => void>('setViewColumns')
const cardTabsStore = useCardTabsStore()

// ==================== 数据状态 ====================
const loading = ref(false)
const loadingMore = ref(false)
//当 API 返回包含所有卡片数据的大 JSON 对象时，Vue 的 ref会递归遍历这个对象的每一层属性（卡片 -> 字段值 -> 关联卡片 -> ...），并未每一个属性创建 Proxy 代理。 对于一个包含大量字段和关联关系的列表数据，这会产生数千甚至数万个 Proxy 对象。
const viewData = shallowRef<ListViewDataResponse | null>(null)
const request = ref<ViewDataRequest>(createEmptyViewDataRequest())
const error = ref<string | null>(null)
const searchKeyword = ref('')
// 使用 shallowRef 存储大数据集，避免深度响应式的性能开销
const accumulatedCards = shallowRef<CardDTO[]>([])
const hasMore = ref(true)
const tableBodyRef = ref<HTMLElement | null>(null)
const tableContainerRef = ref<HTMLElement | null>(null)
const tableHeight = ref(500) // 表格高度，用于虚拟滚动

// 卡片新建 Modal 状态
const createModalVisible = ref(false)

// 漏斗过滤条件
const filterCondition = ref<Condition | null>(null)
const selectedRowKeys = ref<string[]>([])
const rowHeight = ref<RowHeightType>('standard')

// ==================== 计算属性 ====================
const columns = computed<ColumnMeta[]>(() => viewData.value?.columns || [])
const cards = computed<CardDTO[]>(() => accumulatedCards.value)
// const pageInfo = computed<PageInfo | undefined>(() => viewData.value?.pageInfo)
// const _totalCount = computed(() => pageInfo.value?.total || 0)
const cardTypeId = computed(() => viewData.value?.cardTypeId)
const rootCardTypeName = computed(() => viewData.value?.viewName || '')
const statusOptions = computed<StatusOption[]>(() => viewData.value?.statusOptions || [])

// 计算表格最小宽度，防止列数少时列宽被拉伸
/*
const _tableScrollX = computed(() => {
  const checkboxWidth = 40  // 复选框列
  const addButtonWidth = 36 // 加号列
  const dataColumnsWidth = columns.value.reduce((sum, col) => sum + (col.width || 150), 0)
  return checkboxWidth + addButtonWidth + dataColumnsWidth
})
*/


// ==================== 单元格编辑 ====================
const {
  editingCell,
  editingTextValue,
  editingNumberValue,
  editingDateValue,
  editingEnumValue,
  editingLinkValue,
  editingStructureValue,
  cellSaving,
  // isEditing: _isEditing,
  startEditing,
  cancelEditing,
  saveEditing,
  handleEditKeydown,
} = useCellEditing(
  () => columns.value,
  () => accumulatedCards.value,
  (updater) => {
    accumulatedCards.value = updater(accumulatedCards.value)
  },
  () => statusOptions.value
)

// ==================== 数据加载 ====================
async function fetchData() {
  if (!props.viewId) return

  loading.value = true
  error.value = null
  accumulatedCards.value = []
  hasMore.value = true
  request.value.page = 0
  request.value.size = PAGE_SIZE

  try {
    const response = await viewDataApi.queryByViewId(props.viewId, request.value) as ListViewDataResponse
    viewData.value = response
    accumulatedCards.value = response.cards || []

    const info = response.pageInfo
    if (info) {
      hasMore.value = (info.page + 1) * info.size < info.total
    }

    await nextTick()
    bindScrollEvent()
  } catch (err: any) {
    console.error('Failed to fetch view data:', err)
    error.value = err.message || t('common.message.loadFailed')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!props.viewId || loadingMore.value || !hasMore.value) return

  loadingMore.value = true
  request.value.page = (request.value.page ?? 0) + 1

  try {
    const response = await viewDataApi.queryByViewId(props.viewId, request.value) as ListViewDataResponse
    viewData.value = response
    accumulatedCards.value = [...accumulatedCards.value, ...(response.cards || [])]

    const info = response.pageInfo
    if (info) {
      hasMore.value = (info.page + 1) * info.size < info.total
    }
  } catch (err: any) {
    console.error('Failed to load more data:', err)
    request.value.page = (request.value.page ?? 1) - 1
  } finally {
    loadingMore.value = false
  }
}

// ==================== 滚动加载 ====================
function handleScroll(event: Event) {
  const target = event.target as HTMLElement
  if (!target) return
  const { scrollTop, scrollHeight, clientHeight } = target
  if (scrollHeight - scrollTop - clientHeight < 100) {
    loadMore()
  }
}

function bindScrollEvent() {
  const tableBody = document.querySelector('.arco-table-body')
  if (tableBody) {
    tableBodyRef.value = tableBody as HTMLElement
    tableBody.addEventListener('scroll', handleScroll)
  }
}

function unbindScrollEvent() {
  if (tableBodyRef.value) {
    tableBodyRef.value.removeEventListener('scroll', handleScroll)
    tableBodyRef.value = null
  }
}

// ==================== 事件处理 ====================
function handleSortChange(field: string, order: 'ascend' | 'descend' | '') {
  if (order) {
    request.value.userSorts = [{ field, direction: order === 'ascend' ? 'ASC' : 'DESC' }]
  } else {
    request.value.userSorts = undefined
  }
  fetchData()
}

// 处理标题点击（从 ListViewCell 接收 CardDTO）
function handleTitleClick(record: CardDTO) {
  cardTabsStore.openTab(getCardId(record))
}

// 适配器函数：将 ListViewCell 发出的 (cardId, fieldId) 转换为 startEditing 需要的 (record, fieldId)
function handleStartEdit(cardId: string, fieldId: string) {
  const record = accumulatedCards.value.find(c => getCardId(c) === cardId)
  if (record) {
    startEditing(record, fieldId)
  }
}

function handleCreate() {
  createModalVisible.value = true
}


/*
function _handleArchive(record: CardDTO) {
  Modal.confirm({
    title: t('common.card.confirmArchive'),
    content: t('common.card.archiveConfirmContent', { title: getCardTitle(record) }),
    okText: t('common.card.archive'),
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await cardApi.batchArchive({ cardIds: [getCardId(record)] })
        Message.success(t('common.card.archiveSuccess'))
        fetchData()
      } catch (error: any) {
        console.error('Archive failed:', error)
      }
    },
  })
}

function _handleDiscard(record: CardDTO) {
  Modal.confirm({
    title: t('common.card.confirmDiscard'),
    content: t('common.card.discardConfirmContent', { title: getCardTitle(record) }),
    okText: t('common.card.discard'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await cardApi.discard(getCardId(record))
        Message.success(t('common.card.discardSuccess'))
        fetchData()
      } catch (error: any) {
        console.error('Discard failed:', error)
      }
    },
  })
}
*/

function handleFormSuccess() {
  fetchData()
}

function refresh() {
  fetchData()
}

function handleSearch(keyword: string) {
  searchKeyword.value = keyword
  fetchData()
}

function handleFilterApply(condition: Condition | null) {
  request.value.userCondition = condition as ViewDataRequest['userCondition']
  fetchData()
}

function handleFilterClear() {
  request.value.userCondition = undefined
  fetchData()
}

// ==================== 生命周期 ====================
watch(
  () => props.viewId,
  (newViewId) => {
    if (newViewId) {
      unbindScrollEvent()
      request.value = createEmptyViewDataRequest()
      fetchData()
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  unbindScrollEvent()
  // 清理 ResizeObserver
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
})

// 动态计算表格高度
let resizeObserver: ResizeObserver | null = null

function updateTableHeight() {
  const container = tableContainerRef.value
  if (container) {
    // 预留一些空间给表头（约 40px）和底部加载提示
    const availableHeight = container.clientHeight - 50
    tableHeight.value = Math.max(availableHeight, 300)
  }
}

// 监听容器大小变化
watch(() => viewData.value, async () => {
  await nextTick()
  const container = document.querySelector('.table-container') as HTMLElement
  if (container && !resizeObserver) {
    tableContainerRef.value = container
    updateTableHeight()
    
    resizeObserver = new ResizeObserver(() => {
      updateTableHeight()
    })
    resizeObserver.observe(container)
  }
})

watch(() => viewData.value?.viewName, (viewName) => {
  if (setViewName) setViewName(viewName || '')
})

watch(() => viewData.value?.columns, (cols) => {
  if (setViewColumns && cols) setViewColumns(cols)
})

defineExpose({ refresh })
</script>

<template>
  <div class="view-data-panel">
    <!-- 工具栏 -->
    <ViewToolbar
      v-model:filter-condition="filterCondition"
      v-model:search-keyword="searchKeyword"
      v-model:row-height="rowHeight"
      :card-type-id="cardTypeId"
      :any-trait-card-type-name="rootCardTypeName"
      @filter-apply="handleFilterApply"
      @filter-clear="handleFilterClear"
      @search="handleSearch"
      @refresh-click="refresh"
    />

    <!-- 数据区域 -->
    <div class="panel-content">
      <!-- 左侧遮罩 - 防止水平滚动时内容穿透 -->
      <div class="left-scroll-mask"></div>

      <a-result v-if="error" status="error" :title="error" class="error-result">
        <template #extra>
          <a-button type="primary" @click="fetchData">{{ t('common.action.retry') }}</a-button>
        </template>
      </a-result>

      <a-spin v-else :loading="loading" :class="['table-container', `row-height-${rowHeight}`]">
        <a-table
          v-model:selected-keys="selectedRowKeys"
          :data="cards"
          :pagination="false"
          :bordered="false"
          row-key="id"
          size="small"
          :column-resizable="true"
          :row-selection="{ type: 'checkbox', showCheckedAll: true, width: 40, fixed: true }"
          :scroll="{ x: '100%', y: tableHeight }"
          :virtual-list-props="{ height: tableHeight, buffer: 20 }"
          @sorter-change="handleSortChange"
        >
          <template #columns>
            <!-- 新建按钮列 - 固定在左侧，不可调整宽度 -->
            <a-table-column v-if="cardTypeId" :width="36" fixed="left" :resizable="false">
              <template #title>
                <IconPlusCircle class="create-icon" @click.stop="handleCreate" />
              </template>
              <template #cell />
            </a-table-column>

            <!-- 数据列 -->
            <a-table-column
              v-for="(col, index) in columns"
              :key="col.fieldId"
              :data-index="col.fieldId"
              :width="col.width || 150"
              :ellipsis="true"
              :fixed="index === 0 ? 'left' : undefined"
              :sortable="col.sortable ? { sortDirections: ['ascend', 'descend'] } : undefined"
              :resizable="!col.frozen"
            >
              <template #title>
                <span>{{ col.title }}</span>
              </template>
              <template #cell="{ record }">
                <!-- 使用独立的 ListViewCell 组件，通过 inject 获取编辑状态 -->
                <div :data-cell-id="`${getCardId(record)}-${col.fieldId}`">
                  <ListViewCell
                    :record="record"
                    :column="col"
                    :columns="columns"
                    :status-options="statusOptions"
                    @start-edit="handleStartEdit"
                    @title-click="handleTitleClick"
                  />
                </div>
              </template>
            </a-table-column>

            <!-- 填充列 - 占据剩余空间，不可调整宽度 -->
            <a-table-column class="filler-column" :resizable="false">
              <template #title />
              <template #cell />
            </a-table-column>
          </template>

          <template #empty>
            <a-empty :description="t('common.state.empty')" />
          </template>
        </a-table>

        <div v-if="loadingMore" class="loading-more">
          <a-spin />
          <span>{{ t('common.state.loading') }}</span>
        </div>
      </a-spin>
    </div>

    <!-- 单元格编辑器浮层 - 独立于表格渲染，避免全表重渲染 -->
    <CellEditorOverlay
      v-model:text-value="editingTextValue"
      v-model:number-value="editingNumberValue"
      v-model:date-value="editingDateValue"
      v-model:enum-value="editingEnumValue"
      v-model:link-value="editingLinkValue"
      v-model:structure-value="editingStructureValue"
      :editing-cell="editingCell"
      :saving="cellSaving"
      :display-value="''"
      :table-container="tableBodyRef"
      @keydown="handleEditKeydown"
      @save="saveEditing"
      @cancel="cancelEditing"
    />

    <!-- 卡片新建 Modal -->
    <CardCreateModal
      v-model:visible="createModalVisible"
      :card-type-id="cardTypeId || ''"
      @success="handleFormSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/list/list-view-table.scss';

.view-data-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-bg-1);
  overflow: hidden;
}

.panel-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;  // 为遮罩提供定位上下文
}

.error-result {
  padding: 48px 0;
}

.create-icon {
  font-size: 18px;
  color: var(--color-text-3);
  cursor: pointer;
  stroke-width: 2;
  vertical-align: middle;
  margin-top: -2px;

  &:hover {
    color: rgb(var(--primary-6));
  }
}

</style>
