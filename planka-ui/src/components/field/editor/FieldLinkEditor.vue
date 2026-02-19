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

}>()

const selectedCards = defineModel<LinkedCard[]>('selectedCards', { required: true })

const emit = defineEmits<{
  /** 保存 */
  save: []
  /** 取消 */
  cancel: []
}>()

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
    // 关闭时保存
    emit('save')
  }
}

// 处理搜索
function handleSearch(value: string) {
  keyword.value = value
  searchLinkableCards()
}

// 初始加载
onMounted(() => {
  searchLinkableCards()
})
</script>

<template>
  <a-select
    v-model="selectedValues"
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
  />
</template>

<style scoped lang="scss">
// Select 样式调整
:deep(.arco-select-view-multiple) {
  min-height: 32px;
}
</style>
