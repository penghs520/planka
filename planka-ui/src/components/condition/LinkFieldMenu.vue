<template>
  <a-dropdown trigger="click" :popup-visible="visible" @update:popup-visible="handleVisibleChange">
    <slot>
      <a-button type="text" size="mini">
        {{ linkTypeName }}
        <template #icon><icon-right /></template>
      </a-button>
    </slot>

    <template #content>
      <div class="link-field-menu">
        <div class="menu-header">
          <span class="menu-title">{{ linkTypeName }}</span>
        </div>

        <a-divider :margin="4" />

        <!-- 直接过滤选项 -->
        <div class="menu-option" @click="handleDirectFilter">
          <icon-filter class="option-icon" />
          <div class="option-content">
            <span class="option-title">直接过滤此关联</span>
            <span class="option-desc">有关联/无关联/在列表中</span>
          </div>
        </div>

        <!-- 展开属性选项 -->
        <div
          class="menu-option"
          :class="{ disabled: !canExpand }"
          @click="handleExpand"
        >
          <icon-layers class="option-icon" />
          <div class="option-content">
            <span class="option-title">展开「{{ linkTypeName }}」的属性</span>
            <span class="option-desc">
              <template v-if="canExpand">
                筛选关联卡片的字段
              </template>
              <template v-else>
                已达到最大深度限制
              </template>
            </span>
          </div>
        </div>

        <!-- 目标卡片类型信息 -->
        <div v-if="targetCardTypes.length > 0" class="target-info">
          <a-divider :margin="4" />
          <div class="target-label">目标卡片类型:</div>
          <div class="target-types">
            <a-tag
              v-for="cardType in targetCardTypes"
              :key="cardType.id"
              size="small"
              color="blue"
            >
              {{ cardType.name }}
            </a-tag>
          </div>
        </div>
      </div>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { IconRight, IconFilter, IconLayers } from '@arco-design/web-vue/es/icon'
import { buildLinkFieldId } from '@/utils/link-field-utils'

/**
 * 卡片类型信息
 */
export interface CardTypeInfo {
  id: string
  name: string
}

/**
 * Props定义
 */
const props = withDefaults(
  defineProps<{
    /** 关联类型ID */
    linkTypeId: string

    /** 关联类型名称 */
    linkTypeName: string

    /** 关联位置 */
    position: 'SOURCE' | 'TARGET'

    /** 目标卡片类型列表 */
    targetCardTypes?: CardTypeInfo[]

    /** 当前路径深度 */
    currentDepth?: number

    /** 最大深度 */
    maxDepth?: number
  }>(),
  {
    targetCardTypes: () => [],
    currentDepth: 0,
    maxDepth: 3,
  }
)

/**
 * Emits定义
 */
const emit = defineEmits<{
  /** 选择直接过滤（创建 LinkConditionItem） */
  'direct-filter': [linkFieldId: string]
  /** 选择展开属性（进入下一级） */
  'expand': [linkFieldId: string, targetCardTypes: CardTypeInfo[]]
}>()

/**
 * 菜单可见状态
 */
const visible = ref(false)

/**
 * 是否可以继续展开
 */
const canExpand = computed(() => {
  return props.currentDepth < props.maxDepth
})

/**
 * 处理菜单可见状态变化
 */
function handleVisibleChange(val: boolean) {
  visible.value = val
}

/**
 * 创建关联字段ID
 */
function createLinkFieldId(): string {
  return buildLinkFieldId(props.linkTypeId, props.position)
}

/**
 * 处理直接过滤
 */
function handleDirectFilter() {
  visible.value = false
  emit('direct-filter', createLinkFieldId())
}

/**
 * 处理展开属性
 */
function handleExpand() {
  if (!canExpand.value) return
  visible.value = false
  emit('expand', createLinkFieldId(), props.targetCardTypes)
}
</script>

<style scoped lang="scss">
.link-field-menu {
  min-width: 240px;
  padding: 8px;
}

.menu-header {
  padding: 4px 8px;
}

.menu-title {
  font-weight: 500;
  color: var(--color-text-1);
  font-size: 13px;
}

.menu-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover:not(.disabled) {
    background: var(--color-fill-2);
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }
}

.option-icon {
  font-size: 16px;
  color: var(--color-text-2);
  margin-top: 2px;
  flex-shrink: 0;
}

.option-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.option-title {
  font-size: 13px;
  color: var(--color-text-1);
  font-weight: 500;
}

.option-desc {
  font-size: 11px;
  color: var(--color-text-3);
}

.target-info {
  padding: 4px 8px;
}

.target-label {
  font-size: 11px;
  color: var(--color-text-3);
  margin-bottom: 6px;
}

.target-types {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
