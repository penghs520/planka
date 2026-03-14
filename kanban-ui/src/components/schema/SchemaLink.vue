<script setup lang="ts">
/**
 * Schema 链接组件
 * 
 * 用于显示可点击的 Schema 名称链接，点击后在新标签页打开对应的定义页面。
 * 复用 schema-navigation 工具函数，统一跳转逻辑。
 */
import { useRouter } from 'vue-router'
import { SchemaType } from '@/types/schema'
import { navigateToSchemaInNewTab, isNavigableSchemaType, getDefaultTab } from '@/utils/schema-navigation'

interface Props {
  /** Schema ID */
  schemaId: string
  /** Schema 类型 */
  schemaType: SchemaType | string
  /** 显示名称 */
  name: string
  /** 可选的 Tab 参数 */
  tab?: string
}

const props = defineProps<Props>()
const router = useRouter()

// 是否可跳转
const isNavigable = isNavigableSchemaType(props.schemaType)

// 处理点击跳转
function handleClick() {
  if (!isNavigable) return
  
  const defaultTab = props.tab || getDefaultTab(props.schemaType)
  navigateToSchemaInNewTab(props.schemaType, props.schemaId, router, defaultTab)
}
</script>

<template>
  <!-- 可跳转：显示为蓝色链接 -->
  <a-link v-if="isNavigable" @click="handleClick">
    <slot>{{ name }}</slot>
  </a-link>
  <!-- 不可跳转：显示为普通文本 -->
  <span v-else class="schema-link-disabled">
    <slot>{{ name }}</slot>
  </span>
</template>

<style scoped>
.schema-link-disabled {
  color: var(--color-text-2);
}
</style>
