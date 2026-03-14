<script setup lang="ts">
/**
 * 关联卡片编辑器组件
 * 使用 a-select 实现多选下拉框样式
 */
import { ref, computed, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import type { LinkedCard, CardTitle } from '@/types/card'
import type { LinkRenderConfig } from '@/types/view-data'
import { linkCardApi } from '@/api/link-card'
import type { PageResult } from '@/api/link-card'

const props = defineProps<{
  /**
   * 关联属性 ID
   * 格式为 "{linkTypeId}:{SOURCE|TARGET}"
   */
  linkFieldId: string
  /** 渲染配置 */
  renderConfig?: LinkRenderConfig
  /** 是否正在保存 */
  saving?: boolean
  /** 是否自动打开下拉框 */
  autoFocus?: boolean
}>()

const selectedCards = defineModel<LinkedCard[]>('selectedCards', { required: true })

const emit = defineEmits<{
  /** 保存 */
  save: []
  /** 取消 */
  cancel: []
}>()

// 标记是否有未保存的更改
const hasUnsavedChanges = ref(false)

// 搜索关键字
const keyword = ref('')
// 搜索结果
const linkableCards = ref<LinkedCard[]>([])
// 加载状态
const loading = ref(false)
// 分页信息
const pageInfo = ref({
  page: 0,
  size: 50,
  total: 0,
  hasNext: false,
})

// 是否多选
const isMultiple = computed(() => props.renderConfig?.multiple ?? true)

// 下拉框可见状态
const popupVisible = ref(false)

// 用于比较的值快照，用于检测值是否真正发生变化
const previousSelectedIds = ref<string[]>([])

// 内部选中的值列表
const selectedValues = computed({
  get() {
    if (isMultiple.value) {
      return selectedCards.value.map(c => c.cardId)
    }
    return selectedCards.value.length > 0 ? selectedCards.value[0]?.cardId : ''
  },
  set(val: string | string[] | undefined) {
    const values = Array.isArray(val) ? val : (val ? [val] : [])

    // 根据选中的值更新 selectedCards
    const allCardsMap = new Map<string, LinkedCard>()
    selectedCards.value.forEach(c => allCardsMap.set(c.cardId, c))
    linkableCards.value.forEach(c => allCardsMap.set(c.cardId, c))

    const newSelectedCards = values
      .map(id => allCardsMap.get(id))
      .filter((c): c is LinkedCard => c !== undefined)

    selectedCards.value = newSelectedCards

    // 检查值是否真正发生变化
    const currentIds = newSelectedCards.map(c => c.cardId).sort()
    const prevIds = previousSelectedIds.value.slice().sort()
    if (JSON.stringify(currentIds) !== JSON.stringify(prevIds)) {
      hasUnsavedChanges.value = true
    }
  }
})

// 转换为 Select 选项格式
const selectOptions = computed(() => {
  return linkableCards.value.map(card => ({
    value: card.cardId,
    label: getCardTitleText(card.title),
  }))
})

// 搜索可关联的卡片
async function searchLinkableCards(resetPage = true) {
  if (resetPage) {
    pageInfo.value.page = 0
  }

  loading.value = true
  try {
    const result: PageResult<LinkedCard> = await linkCardApi.queryLinkableCards({
      linkFieldId: props.linkFieldId,
      keyword: keyword.value || undefined,
      page: pageInfo.value.page,
      size: pageInfo.value.size,
    })

    linkableCards.value = result.content
    pageInfo.value.total = result.total
    pageInfo.value.hasNext = result.hasNext
  } catch (error: any) {
    console.error('Failed to load linkable cards:', error)
    Message.error('加载可关联卡片失败')
  } finally {
    loading.value = false
  }
}

// 获取标题显示文本
function getCardTitleText(title: CardTitle): string {
  if (title.type === 'PURE') {
    return title.value
  } else {
    // JOINT 类型
    const rawValue = title.value || ''
    const parts = title.multiParts?.map((p: any) => p.value || p).join('') || ''
    if (title.area === 'PREFIX') {
      return parts + rawValue
    } else {
      return rawValue + parts
    }
  }
}

// 处理下拉框可见性变化
function handlePopupVisibleChange(visible: boolean) {
  popupVisible.value = visible
  if (!visible) {
    if (hasUnsavedChanges.value) {
      // 有未保存的更改，触发保存（保存成功后父组件会关闭编辑器）
      doSave()
    } else {
      // 无更改，直接关闭编辑器
      emit('cancel')
    }
  }
}

// 执行保存
function doSave() {
  if (hasUnsavedChanges.value) {
    hasUnsavedChanges.value = false
    previousSelectedIds.value = selectedCards.value.map(c => c.cardId)
    emit('save')
  }
}

// 处理清除（点击 x 号）
function handleClear() {
  // 标记有未保存的更改，但不立即保存（支持连续删除多个）
  hasUnsavedChanges.value = true
  // 更新快照，防止重复触发保存
  previousSelectedIds.value = selectedCards.value.map(c => c.cardId)
}

// 处理搜索
function handleSearch(value: string) {
  keyword.value = value
  searchLinkableCards()
}

// 初始加载
onMounted(() => {
  searchLinkableCards()
  // 初始化快照
  previousSelectedIds.value = selectedCards.value.map(c => c.cardId)
  // 如果设置了 autoFocus，自动打开下拉框
  if (props.autoFocus) {
    popupVisible.value = true
  }
})

// 处理组件失去焦点（blur）
function handleBlur() {
  // blur 事件已由 handlePopupVisibleChange 处理，这里不需要额外操作
  // 保留此函数以防未来需要
}
</script>

<template>
  <a-select
    v-model="selectedValues"
    class="arco-select-tag-blue"
    :multiple="isMultiple"
    :loading="loading"
    :popup-visible="popupVisible"
    :options="selectOptions"
    style="width: 100%"
    :trigger-props="{
      contentStyle: { minWidth: '240px' }
    }"
    :max-tag-count="2"
    placeholder="请选择"
    allow-search
    allow-clear
    :filter-option="false"
    @popup-visible-change="handlePopupVisibleChange"
    @search="handleSearch"
    @clear="handleClear"
    @blur="handleBlur"
  />
</template>

