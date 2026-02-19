<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  IconRefresh,
  IconLineHeight,
  IconSort,
  IconStar,
  IconExport,
  IconSearch,
  IconMore,
} from '@arco-design/web-vue/es/icon'
import type { Condition } from '@/types/condition'
import type { FieldConfig } from '@/types/card-type'
import FunnelFilter from '@/components/funnel-filter/FunnelFilter.vue'

const { t } = useI18n()

/** 行高类型 */
export type RowHeightType = 'standard' | 'double' | 'triple' | 'quadruple'

const ROW_HEIGHT_OPTIONS = computed(() => [
  { value: 'standard', label: t('common.toolbar.rowHeightStandard') },
  { value: 'double', label: t('common.toolbar.rowHeightDouble') },
  { value: 'triple', label: t('common.toolbar.rowHeightTriple') },
  { value: 'quadruple', label: t('common.toolbar.rowHeightQuadruple') },
])

const props = withDefaults(
  defineProps<{
    /** 卡片类型 ID（用于漏斗过滤） */
    cardTypeId?: string
    /** 根卡片类型名称（用于漏斗过滤路径显示） */
    rootCardTypeName?: string
    /** 视图内置过滤条件描述 */
    viewConditionDescription?: string
    /** 过滤条件 (v-model:filter-condition) */
    filterCondition?: Condition | null
    /** 搜索关键词 (v-model:search-keyword) */
    searchKeyword?: string
    /** 快捷过滤字段列表 */
    quickFilterFields?: FieldConfig[]
    /** 快捷过滤选中的字段 */
    quickFilterField?: string
    /** 行高 (v-model:row-height) */
    rowHeight?: RowHeightType

    // 显示/隐藏控制
    /** 是否显示漏斗过滤 */
    showFunnelFilter?: boolean
    /** 是否显示关键字搜索 */
    showKeywordSearch?: boolean
    /** 是否显示快捷过滤 */
    showQuickFilter?: boolean
    /** 是否显示行高按钮 */
    showRowHeight?: boolean
    /** 是否显示排序按钮 */
    showSort?: boolean
    /** 是否显示我的关注按钮 */
    showMyFocus?: boolean
    /** 是否显示视图导出按钮 */
    showExport?: boolean
    /** 是否显示更多按钮 */
    showMore?: boolean
  }>(),
  {
    cardTypeId: undefined,
    rootCardTypeName: '',
    viewConditionDescription: undefined,
    filterCondition: null,
    searchKeyword: '',
    quickFilterFields: () => [],
    quickFilterField: undefined,
    rowHeight: 'standard',
    showFunnelFilter: true,
    showKeywordSearch: true,
    showQuickFilter: true,
    showRowHeight: true,
    showSort: true,
    showMyFocus: true,
    showExport: true,
    showMore: true,
  }
)

const emit = defineEmits<{
  'update:filterCondition': [value: Condition | null]
  'update:searchKeyword': [value: string]
  'update:quickFilterField': [value: string | undefined]
  'update:rowHeight': [value: RowHeightType]
  /** 应用漏斗过滤 */
  filterApply: [condition: Condition | null]
  /** 清除漏斗过滤 */
  filterClear: []
  /** 搜索 */
  search: [keyword: string]
  /** 快捷过滤变化 */
  quickFilterChange: [fieldId: string | undefined]
  /** 排序点击 */
  sortClick: []
  /** 我的关注点击 */
  myFocusClick: []
  /** 导出点击 */
  exportClick: []
  /** 刷新点击 */
  refreshClick: []
}>()

// 本地搜索关键词
const localSearchKeyword = ref(props.searchKeyword)

// 是否显示右侧工具栏
const showRightToolbar = computed(() => {
  return props.showRowHeight || props.showSort || props.showMyFocus || props.showExport || props.showMore
})

// 处理漏斗过滤应用
function handleFilterApply(condition: Condition | null) {
  emit('update:filterCondition', condition)
  emit('filterApply', condition)
}

// 处理漏斗过滤清除
function handleFilterClear() {
  emit('update:filterCondition', null)
  emit('filterClear')
}

// 处理搜索
function handleSearch() {
  emit('update:searchKeyword', localSearchKeyword.value)
  emit('search', localSearchKeyword.value)
}

// 处理快捷过滤变化
function handleQuickFilterChange(fieldId: string | undefined) {
  emit('update:quickFilterField', fieldId)
  emit('quickFilterChange', fieldId)
}

// 监听外部 searchKeyword 变化
import { watch, computed } from 'vue'
watch(
  () => props.searchKeyword,
  (newVal) => {
    localSearchKeyword.value = newVal
  }
)
</script>

<template>
  <div class="view-toolbar">
    <!-- 左侧工具栏 -->
    <div class="toolbar-left">
      <!-- 漏斗过滤 -->
      <FunnelFilter
        v-if="showFunnelFilter"
        :model-value="filterCondition"
        :card-type-id="cardTypeId"
        :any-trait-card-type-name="rootCardTypeName"
        :view-condition-description="viewConditionDescription"
        @update:model-value="emit('update:filterCondition', $event)"
        @apply="handleFilterApply"
        @clear="handleFilterClear"
      />

      <!-- 关键字搜索 -->
      <a-input
        v-if="showKeywordSearch"
        v-model="localSearchKeyword"
        :placeholder="t('common.toolbar.searchPlaceholder')"
        class="search-input"
        allow-clear
        :style="{ height: '24px' }"
        @press-enter="handleSearch"
        @clear="handleSearch"
      >
        <template #suffix>
          <IconSearch class="search-icon" @click="handleSearch" />
        </template>
      </a-input>

      <!-- 快捷过滤 -->
      <template v-if="showQuickFilter">
        <div class="quick-filter-label">
          <span>{{ t('common.toolbar.quickFilter') }}</span>
        </div>
        <a-select
          :model-value="quickFilterField"
          :placeholder="t('common.toolbar.selectField')"
          class="quick-filter-select"
          allow-clear
          :style="{ height: '24px' }"
          @change="handleQuickFilterChange"
        >
          <a-option
            v-for="field in quickFilterFields"
            :key="field.fieldId"
            :value="field.fieldId"
          >
            {{ field.name }}
          </a-option>
        </a-select>
      </template>

      <!-- 左侧插槽 -->
      <slot name="left" />
    </div>

    <!-- 右侧工具栏 -->
    <div v-if="showRightToolbar" class="toolbar-right">
      <a-dropdown v-if="showRowHeight" trigger="click">
        <a-button size="small" :class="['toolbar-btn', { 'is-active': rowHeight !== 'standard' }]">
          <template #icon><IconLineHeight /></template>
          {{ t('common.toolbar.rowHeight') }}
        </a-button>
        <template #content>
          <a-doption
            v-for="option in ROW_HEIGHT_OPTIONS"
            :key="option.value"
            :class="{ 'is-selected': rowHeight === option.value }"
            @click="emit('update:rowHeight', option.value as RowHeightType)"
          >
            {{ option.label }}
          </a-doption>
        </template>
      </a-dropdown>
      <a-button v-if="showSort" size="small" class="toolbar-btn" @click="emit('sortClick')">
        <template #icon><IconSort /></template>
        {{ t('common.toolbar.sort') }}
      </a-button>
      <a-button v-if="showMyFocus" size="small" class="toolbar-btn" @click="emit('myFocusClick')">
        <template #icon><IconStar /></template>
        {{ t('common.toolbar.myFocus') }}
      </a-button>
      <a-button v-if="showExport" size="small" class="toolbar-btn" @click="emit('exportClick')">
        <template #icon><IconExport /></template>
        {{ t('common.toolbar.export') }}
      </a-button>
      <a-dropdown v-if="showMore" trigger="click">
        <a-button size="small" class="toolbar-btn">
          <template #icon><IconMore /></template>
          {{ t('common.toolbar.more') }}
        </a-button>
        <template #content>
          <a-doption @click="emit('refreshClick')">
            <IconRefresh /> {{ t('common.action.refresh') }}
          </a-doption>
          <!-- 更多菜单插槽 -->
          <slot name="more-menu" />
        </template>
      </a-dropdown>

      <!-- 右侧插槽 -->
      <slot name="right" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.view-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 4px;
  background: var(--color-bg-1);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 200px;
  flex-shrink: 0;

  &:deep(.arco-input-wrapper) {
    padding-left: 6px;
  }

  &:deep(.arco-input) {
    font-size: 12px;
    color: var(--color-text-2) !important;
    padding-left: 0 !important;
  }
}

.search-icon {
  cursor: pointer;
  color: var(--color-text-3);

  &:hover {
    color: var(--color-text-1);
  }
}

.quick-filter-label {
  font-size: 13px;
  color: var(--color-text-2);
  white-space: nowrap;
  flex-shrink: 0;
}

.quick-filter-select {
  width: 160px;
  flex-shrink: 0;

  &:deep(.arco-select-view-single) {
    height: 28px;
  }

  &:deep(.arco-select-view-value) {
    font-size: 12px;
  }
}

.toolbar-right {
  display: flex;
  align-items: center;
  background: var(--color-fill-1);
  border-radius: 4px;

  > .toolbar-btn:first-child,
  > :first-child .toolbar-btn {
    border-radius: 4px 0 0 4px;
  }

  > :last-child .toolbar-btn,
  > .toolbar-btn:last-child {
    border-radius: 0 4px 4px 0;
  }

  .toolbar-btn {
    border-radius: 0;
    background: transparent;
    color: var(--color-text-2);

    &.is-active {
      color: #5AC8FA;;
    }
  }
}

</style>

<style lang="scss">
// 下拉选项选中状态（全局样式，因为下拉菜单渲染在 body）
.arco-dropdown .is-selected {
  color: rgb(var(--primary-6));
  background-color: var(--color-fill-2);
}
</style>
