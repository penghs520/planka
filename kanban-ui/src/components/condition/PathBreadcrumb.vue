<template>
  <div v-if="pathItems.length > 0 || showCurrentCard" class="path-breadcrumb">
    <div class="breadcrumb-items">
      <!-- 当前卡片（根节点） -->
      <span
        class="breadcrumb-item clickable"
        :class="{ active: currentPath.length === 0 }"
        @click="handleGoToRoot"
      >
        {{ computedRootLabel }}
      </span>

      <!-- 路径节点 -->
      <template v-for="(item, index) in pathItems" :key="index">
        <span class="breadcrumb-separator">&gt;</span>
        <span
          class="breadcrumb-item clickable"
          :class="{ active: index === pathItems.length - 1 }"
          @click="handleGoToLevel(index)"
        >
          {{ item.name }}
        </span>
      </template>
    </div>

    <!-- 深度指示器和返回按钮 -->
    <div class="breadcrumb-actions">
      <span class="depth-indicator">{{ t('common.condition.depth') }} {{ currentPath.length }}/{{ maxDepth }}</span>
      <a-button
        v-if="currentPath.length > 0"
        type="text"
        size="mini"
        @click="handleGoBack"
      >
        <template #icon><icon-arrow-left /></template>
        {{ t('common.action.back') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconArrowLeft } from '@arco-design/web-vue/es/icon'

const { t } = useI18n()

/**
 * 路径项信息（用于显示）
 */
export interface PathItemInfo {
  /** 关联字段ID，格式为 "{linkTypeId}:{SOURCE|TARGET}" */
  linkFieldId: string
  /** 关联名称（用于显示） */
  name: string
  /** 目标卡片类型ID */
  targetCardTypeId: string
  /** 目标卡片类型名称 */
  targetCardTypeName: string
}

/**
 * Props定义
 */
const props = withDefaults(
  defineProps<{
    /** 当前路径（linkFieldId 字符串数组） */
    currentPath: string[]

    /** 路径项信息（包含名称等显示信息） */
    pathItemInfos?: PathItemInfo[]

    /** 最大深度 */
    maxDepth?: number

    /** 根节点标签 */
    rootLabel?: string

    /** 是否始终显示当前卡片 */
    showCurrentCard?: boolean
  }>(),
  {
    currentPath: () => [],
    pathItemInfos: () => [],
    maxDepth: 3,
    rootLabel: undefined,
    showCurrentCard: true,
  }
)

/**
 * Emits定义
 */
const emit = defineEmits<{
  /** 返回到指定层级（-1表示根节点） */
  'go-to-level': [level: number]
  /** 返回上一级 */
  'go-back': []
}>()

/**
 * 计算根节点标签
 */
const computedRootLabel = computed(() => props.rootLabel || t('common.condition.currentCard'))

/**
 * 转换后的路径项（带显示名称）
 */
const pathItems = computed(() => {
  return props.currentPath.map((linkFieldId, index) => {
    // 尝试从 pathItemInfos 中找到对应的信息
    const info = props.pathItemInfos[index]
    return {
      linkFieldId,
      name: info?.name || t('common.condition.linkIndex', { index: index + 1 }),
      targetCardTypeId: info?.targetCardTypeId || '',
      targetCardTypeName: info?.targetCardTypeName || '',
    }
  })
})

/**
 * 返回根节点
 */
function handleGoToRoot() {
  if (props.currentPath.length > 0) {
    emit('go-to-level', -1)
  }
}

/**
 * 跳转到指定层级
 */
function handleGoToLevel(index: number) {
  if (index < props.currentPath.length - 1) {
    emit('go-to-level', index)
  }
}

/**
 * 返回上一级
 */
function handleGoBack() {
  emit('go-back')
}
</script>

<style scoped lang="scss">
.path-breadcrumb {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--color-fill-2);
  border-radius: 4px;
  font-size: 12px;
  margin-bottom: 8px;
}

.breadcrumb-items {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.breadcrumb-item {
  color: var(--color-text-2);
  padding: 2px 6px;
  border-radius: 3px;
  transition: all 0.2s;

  &.clickable {
    cursor: pointer;

    &:hover:not(.active) {
      background: var(--color-fill-3);
      color: var(--color-text-1);
    }
  }

  &.active {
    color: rgb(var(--primary-6));
    font-weight: 500;
  }
}

.breadcrumb-separator {
  color: var(--color-text-3);
  margin: 0 2px;
}

.breadcrumb-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.depth-indicator {
  color: var(--color-text-3);
  font-size: 11px;
}
</style>
