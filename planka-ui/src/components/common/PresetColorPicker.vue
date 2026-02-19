<script setup lang="ts">
/**
 * 预设颜色选择器
 * 提供常用颜色预选，同时支持自定义颜色选择
 */
import { ref, watch } from 'vue'

const props = defineProps<{
  /** 当前选中的颜色值 */
  modelValue?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

/** 预设颜色列表 */
const presetColors = [
  '#F5222D', // 红
  '#FA541C', // 橙红
  '#FA8C16', // 橙
  '#FAAD14', // 金
  '#FADB14', // 黄
  '#A0D911', // 青柠
  '#52C41A', // 绿
  '#13C2C2', // 青
  '#1890FF', // 蓝
  '#2F54EB', // 极客蓝
  '#722ED1', // 紫
  '#EB2F96', // 洋红
]

/** 自定义颜色值 */
const customColor = ref(props.modelValue || '#1890FF')

/** 同步外部值到自定义颜色 */
watch(() => props.modelValue, (val) => {
  if (val) {
    customColor.value = val
  }
})

/** 选择预设颜色 */
function selectPresetColor(color: string) {
  emit('update:modelValue', color)
}

/** 自定义颜色变化 */
function handleCustomColorChange(color: string) {
  emit('update:modelValue', color)
}

/** 清除颜色 */
function clearColor() {
  emit('update:modelValue', '')
}
</script>

<template>
  <a-popover trigger="click" position="bottom">
    <div class="color-trigger" :class="{ 'has-color': modelValue }">
      <div
        v-if="modelValue"
        class="color-preview"
        :style="{ backgroundColor: modelValue }"
      />
      <span v-else class="no-color">无</span>
    </div>
    <template #content>
      <div class="color-picker-panel">
        <!-- 预设颜色 -->
        <div class="preset-colors">
          <div
            v-for="color in presetColors"
            :key="color"
            class="color-item"
            :class="{ selected: modelValue === color }"
            :style="{ backgroundColor: color }"
            @click="selectPresetColor(color)"
          />
        </div>
        <!-- 操作按钮 -->
        <div class="color-actions">
          <a-button size="mini" @click="clearColor">清除</a-button>
          <a-color-picker
            v-model="customColor"
            size="mini"
            show-text
            @change="handleCustomColorChange"
          />
        </div>
      </div>
    </template>
  </a-popover>
</template>

<style scoped lang="scss">
.color-trigger {
  width: 30px;
  height: 18px;
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-2);
  transition: border-color 0.2s;

  &:hover {
    border-color: var(--color-primary-light-4);
  }

  &.has-color {
    border-color: transparent;
  }
}

.color-preview {
  width: 100%;
  height: 100%;
  border-radius: 3px;
}

.no-color {
  font-size: 10px;
  color: var(--color-text-3);
}

.color-picker-panel {
  width: 216px;
  padding: 8px;
}

.preset-colors {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.color-item {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;

  &:hover {
    transform: scale(1.1);
  }

  &.selected {
    box-shadow: 0 0 0 2px var(--color-bg-1), 0 0 0 4px var(--color-primary-6);
  }
}

.color-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--color-border-1);
  padding-top: 8px;
}
</style>
