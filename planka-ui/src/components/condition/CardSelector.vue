<template>
  <a-select
    :model-value="modelValue"
    mode="multiple"
    :allow-search="true"
    :allow-clear="true"
    :filter-option="false"
    :loading="loading"
    :placeholder="actualPlaceholder"
    size="small"
    @update:model-value="handleChange"
    @search="handleSearch"
    @popup-visible-change="handlePopupVisibleChange"
  >
    <a-option
      v-for="card in displayCards"
      :key="card.id"
      :value="card.id"
    >
      <div class="card-option">
        <span class="card-code">{{ card.code }}</span>
        <span class="card-title">{{ card.title }}</span>
      </div>
    </a-option>

    <template #empty>
      <div class="empty-hint">
        <template v-if="searchKeyword">
          {{ t('common.condition.noSearchResult') }}
        </template>
        <template v-else>
          {{ t('common.toolbar.searchPlaceholder') }}
        </template>
      </div>
    </template>
  </a-select>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { linkCardApi } from '@/api/link-card'
import type { LinkedCard } from '@/types/card'

const { t } = useI18n()

/**
 * 卡片信息（用于显示）
 */
export interface CardInfo {
  id: string
  code: string
  title: string
}

/**
 * Props定义
 */
const props = withDefaults(
  defineProps<{
    /** 选中的卡片ID列表 */
    modelValue?: string[]

    /** 关联属性ID，格式 "{linkTypeId}:{SOURCE|TARGET}" */
    linkFieldId?: string

    /** 目标卡片类型ID（已废弃，请使用 linkFieldId） */
    cardTypeId?: string

    /** 占位符 */
    placeholder?: string

    /** 已选中卡片的详细信息（用于回显） */
    selectedCards?: CardInfo[]
  }>(),
  {
    modelValue: () => [],
    linkFieldId: '',
    cardTypeId: '',
    placeholder: '',
    selectedCards: () => [],
  }
)

/**
 * 计算实际使用的占位符
 */
const actualPlaceholder = computed(() => {
  return props.placeholder || t('common.linkValue.searchAndSelectCard')
})

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: string[]]
  'change': [value: string[], cards: CardInfo[]]
}>()

/**
 * 加载状态
 */
const loading = ref(false)

/**
 * 搜索关键词
 */
const searchKeyword = ref('')

/**
 * 搜索结果
 */
const searchResults = ref<CardInfo[]>([])

/**
 * 防抖定时器
 */
let debounceTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 是否已加载过首页数据
 */
const hasLoadedInitial = ref(false)

/**
 * 显示的卡片列表（搜索结果 + 已选中的卡片）
 */
const displayCards = computed(() => {
  const selectedMap = new Map<string, CardInfo>()

  // 添加已选中的卡片
  props.selectedCards.forEach((card) => {
    selectedMap.set(card.id, card)
  })

  // 添加搜索结果
  searchResults.value.forEach((card) => {
    if (!selectedMap.has(card.id)) {
      selectedMap.set(card.id, card)
    }
  })

  return Array.from(selectedMap.values())
})

/**
 * 将 LinkedCard 转换为 CardInfo
 */
function toCardInfo(linkedCard: LinkedCard): CardInfo {
  return {
    id: linkedCard.cardId,
    code: linkedCard.title?.displayValue?.split(' ')[0] || '', // 从标题中提取编号
    title: linkedCard.title?.displayValue || '',
  }
}

/**
 * 搜索卡片
 */
async function searchCards(keyword?: string): Promise<CardInfo[]> {
  if (!props.linkFieldId) {
    return []
  }

  const response = await linkCardApi.queryLinkableCards({
    linkFieldId: props.linkFieldId,
    keyword: keyword || undefined,
    page: 0,
    size: 20,
  })

  return response.content.map(toCardInfo)
}

/**
 * 处理搜索（带防抖）
 */
async function handleSearch(keyword: string) {
  searchKeyword.value = keyword

  // 清除之前的定时器
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }

  // 防抖处理
  debounceTimer = setTimeout(async () => {
    loading.value = true
    try {
      searchResults.value = await searchCards(keyword)
    } catch (error) {
      console.error('搜索卡片失败:', error)
      searchResults.value = []
    } finally {
      loading.value = false
    }
  }, 300)
}

/**
 * 处理下拉框显示状态变化
 */
async function handlePopupVisibleChange(visible: boolean) {
  // 首次打开时加载第一页数据
  if (visible && !hasLoadedInitial.value && props.linkFieldId) {
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
}

/**
 * 监听 linkFieldId 变化，重置状态
 */
watch(
  () => props.linkFieldId,
  () => {
    hasLoadedInitial.value = false
    searchResults.value = []
    searchKeyword.value = ''
  }
)

/**
 * 处理选择变化
 */
function handleChange(cardIds: string[]) {
  emit('update:modelValue', cardIds)

  // 获取选中卡片的详细信息
  const selectedCards = cardIds
    .map((id) => displayCards.value.find((card) => card.id === id))
    .filter((card): card is CardInfo => !!card)

  emit('change', cardIds, selectedCards)
}
</script>

<style scoped lang="scss">
.card-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-code {
  color: var(--color-text-3);
  font-size: 11px;
  flex-shrink: 0;
}

.card-title {
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-hint {
  padding: 16px;
  text-align: center;
  color: var(--color-text-3);
  font-size: 12px;
}
</style>
