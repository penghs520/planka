<script setup lang="ts">
/**
 * 关联字段显示组件
 * 支持最多显示3行，超出部分可展开查看
 * 点击卡片可在标签页打开详情
 */
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CardDTO } from '@/types/card'
import { getCardTitle } from '@/types/card'
import { useCardTabsStore } from '@/stores/cardTabs'

const { t } = useI18n()

const props = defineProps<{
  /** 关联的卡片列表 */
  linkedCards?: CardDTO[]
  /** 空值占位符 */
  placeholder?: string
  /** 是否在列表中使用（列表中不限制行数） */
  inList?: boolean
  /** 最大显示行数 */
  maxLines?: number
}>()

const cardTabsStore = useCardTabsStore()

// 内容容器引用
const contentRef = ref<HTMLElement | null>(null)

// 是否展开显示全部
const isExpanded = ref(false)

// 是否有溢出内容
const hasOverflow = ref(false)

// 最大行高度
const maxHeight = computed(() => (props.maxLines || 3) * 26)

// 检查是否有溢出
function checkOverflow() {
  if (props.inList) {
    hasOverflow.value = false
    return
  }
  nextTick(() => {
    if (contentRef.value) {
      hasOverflow.value = contentRef.value.scrollHeight > maxHeight.value
    }
  })
}

// 监听卡片列表变化
watch(() => props.linkedCards, () => {
  isExpanded.value = false
  checkOverflow()
}, { deep: true })

onMounted(() => {
  checkOverflow()
})

// 点击卡片，在标签页打开
function handleCardClick(card: CardDTO) {
  cardTabsStore.openTab(card.id, getCardTitle(card))
}

// 计算隐藏的数量
const hiddenCount = computed(() => {
  if (!hasOverflow.value || !props.linkedCards || isExpanded.value) return 0
  // 估算：基于容器能容纳的数量
  const totalCount = props.linkedCards.length
  // 假设每行约3个，3行约9个
  const visibleCount = Math.min(9, totalCount)
  return Math.max(1, totalCount - visibleCount)
})
</script>

<template>
  <div class="field-link-wrapper">
    <div 
      ref="contentRef"
      class="field-link-display" 
      :class="{ 'is-collapsed': hasOverflow && !isExpanded && !inList }"
      :style="{ '--max-height': maxHeight + 'px' }"
    >
      <template v-if="linkedCards && linkedCards.length > 0">
        <span
          v-for="linkedCard in linkedCards"
          :key="linkedCard.id"
          class="linked-card-tag"
          @click.stop="handleCardClick(linkedCard)"
        >
          {{ getCardTitle(linkedCard) }}
        </span>
      </template>
      <span v-else class="field-link-empty">{{ placeholder || '-' }}</span>
    </div>
    
    <!-- 展开/折叠按钮放在容器外部 -->
    <div v-if="hasOverflow && !inList" class="expand-controls">
      <a-popover 
        v-if="!isExpanded" 
        position="rt" 
        trigger="hover"
      >
        <span class="more-tag" @click.stop="isExpanded = true">
          +{{ hiddenCount }} {{ t('common.more') }}
        </span>
        <template #content>
          <div class="hidden-cards-popover">
            <span
              v-for="linkedCard in linkedCards"
              :key="linkedCard.id"
              class="linked-card-tag-popover"
              @click.stop="handleCardClick(linkedCard)"
            >
              {{ getCardTitle(linkedCard) }}
            </span>
          </div>
        </template>
      </a-popover>
      <span v-else class="collapse-tag" @click.stop="isExpanded = false">
        {{ t('common.collapse') }}
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.field-link-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-link-display {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  
  &.is-collapsed {
    max-height: var(--max-height, 78px);
    overflow: hidden;
  }
}

.linked-card-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 18px;
  background-color: var(--color-fill-2);
  color: var(--color-text-2);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background-color: rgb(var(--primary-1));
    color: rgb(var(--primary-6));
  }
}

.expand-controls {
  display: flex;
  align-items: center;
}

.more-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 18px;
  background-color: var(--color-fill-3);
  color: var(--color-text-2);
  cursor: pointer;
  
  &:hover {
    background-color: var(--color-fill-4);
    color: var(--color-text-1);
  }
}

.collapse-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 18px;
  color: var(--color-text-3);
  cursor: pointer;
  
  &:hover {
    color: rgb(var(--primary-6));
  }
}

.field-link-empty {
  color: var(--color-text-4);
}

.hidden-cards-popover {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-width: 400px;
  max-height: 300px;
  overflow-y: auto;
  padding: 4px;
}

.linked-card-tag-popover {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 18px;
  background-color: var(--color-fill-2);
  color: var(--color-text-2);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background-color: rgb(var(--primary-1));
    color: rgb(var(--primary-6));
  }
}
</style>
