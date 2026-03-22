<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  IconPlus,
  IconDelete,
  IconUp,
  IconDown,
  IconEye,
  IconEyeInvisible,
  IconLock,
  IconUnlock,
} from '@arco-design/web-vue/es/icon'
import { cardTypeApi } from '@/api'
import { createEmptyColumnConfig, type ColumnConfig } from '@/types/view'
import type { FieldOption } from '@/types/field-option'
import { isBuiltinField } from '@/types/builtin-field'
import FieldTypeIcon from '@/components/field/FieldTypeIcon.vue'

const props = defineProps<{
  cardTypeId: string
  modelValue: ColumnConfig[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: ColumnConfig[]]
}>()

const loading = ref(false)
const availableFields = ref<FieldOption[]>([])
const selectedFieldIds = ref<Set<string>>(new Set())
const fieldsLoaded = ref(false) // 标记字段是否已加载，避免重复加载

// 本地列配置副本
const localColumnConfigs = ref<ColumnConfig[]>([])

// 拖拽相关状态
const draggedField = ref<FieldOption | null>(null)
const dragOverIndex = ref<number>(-1)
const draggedColumnIndex = ref<number>(-1) // 被拖拽的列索引（右侧内部拖拽）

// 计算未选择的字段
const unselectedFields = computed(() => {
  return availableFields.value.filter((field) => !selectedFieldIds.value.has(field.id))
})

// 分离自定义字段和内置字段（未选择的）
const customUnselectedFields = computed(() => {
  return unselectedFields.value.filter((f) => !isBuiltinField(f.id))
})

const builtinUnselectedFields = computed(() => {
  return unselectedFields.value.filter((f) => isBuiltinField(f.id))
})

// 监听 props.modelValue 变化，同步到本地
watch(
  () => props.modelValue,
  (newValue) => {
    localColumnConfigs.value = JSON.parse(JSON.stringify(newValue))
    selectedFieldIds.value = new Set(newValue.map((c) => c.fieldId))
  },
  { immediate: true, deep: true },
)

// 监听 cardTypeId 变化，重新加载字段列表
watch(
  () => props.cardTypeId,
  async (newCardTypeId, oldCardTypeId) => {
    // 卡片类型变化时，重置加载标志
    if (newCardTypeId !== oldCardTypeId) {
      fieldsLoaded.value = false
      availableFields.value = []
    }
    // 只在未加载且有cardTypeId时才加载
    if (newCardTypeId && !fieldsLoaded.value) {
      await fetchFieldsOptions(newCardTypeId)
    }
  },
  { immediate: true },
)

// 加载可用字段列表
async function fetchFieldsOptions(cardTypeId: string) {
  loading.value = true
  try {
    availableFields.value = await cardTypeApi.getFieldOptions(cardTypeId)
    fieldsLoaded.value = true // 标记为已加载
  } catch (error) {
    console.error('Failed to fetch fields:', error)
    Message.error('加载字段列表失败')
    fieldsLoaded.value = false // 加载失败，允许重试
  } finally {
    loading.value = false
  }
}

// 获取字段名称
function getFieldName(fieldId: string): string {
  const field = availableFields.value.find((f) => f.id === fieldId)
  return field?.name || fieldId
}

// 获取字段类型
function getFieldType(fieldId: string): string {
  const field = availableFields.value.find((f) => f.id === fieldId)
  return field?.fieldType || ''
}

// 添加字段
function handleAddField(field: FieldOption) {
  // 对于 LINK 类型字段，field.id 已经是 "{linkTypeId}:{SOURCE|TARGET}" 格式
  const columnConfig = createEmptyColumnConfig(field.id)
  localColumnConfigs.value.push(columnConfig)
  selectedFieldIds.value.add(field.id)
  emitChange()
}

// 移除字段
function handleRemoveField(index: number) {
  const config = localColumnConfigs.value[index]
  if (!config) return
  const fieldId = config.fieldId
  localColumnConfigs.value.splice(index, 1)
  selectedFieldIds.value.delete(fieldId)
  emitChange()
}

// 上移
function handleMoveUp(index: number) {
  if (index === 0) return
  const current = localColumnConfigs.value[index]
  const previous = localColumnConfigs.value[index - 1]
  if (!current || !previous) return
  localColumnConfigs.value[index] = previous
  localColumnConfigs.value[index - 1] = current
  emitChange()
}

// 下移
function handleMoveDown(index: number) {
  if (index === localColumnConfigs.value.length - 1) return
  const current = localColumnConfigs.value[index]
  const next = localColumnConfigs.value[index + 1]
  if (!current || !next) return
  localColumnConfigs.value[index] = next
  localColumnConfigs.value[index + 1] = current
  emitChange()
}

// 切换可见性
function handleToggleVisible(index: number) {
  const config = localColumnConfigs.value[index]
  if (!config) return
  config.visible = !config.visible
  emitChange()
}

// 切换冻结
function handleToggleFrozen(index: number) {
  const config = localColumnConfigs.value[index]
  if (!config) return
  config.frozen = !config.frozen
  emitChange()
}

// 切换可拖拽宽度大小
function handleToggleResizable(index: number) {
  const config = localColumnConfigs.value[index]
  if (!config) return
  config.resizable = !config.resizable
  emitChange()
}

// 修改宽度
function handleWidthChange(index: number, width: number | undefined) {
  const config = localColumnConfigs.value[index]
  if (!config) return
  config.width = width
  emitChange()
}

// 发送变更事件
function emitChange() {
  emit('update:modelValue', localColumnConfigs.value)
}

// 拖拽开始
function handleDragStart(field: FieldOption, event: DragEvent) {
  draggedField.value = field
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', field.id)
  }
}

// 拖拽结束
function handleDragEnd() {
  draggedField.value = null
  dragOverIndex.value = -1
}

// 拖拽经过目标区域
function handleDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

// 放置到列表末尾
function handleDropToEnd(event: DragEvent) {
  event.preventDefault()

  // 从左侧拖拽到末尾
  if (draggedField.value) {
    // 对于 LINK 类型字段，field.id 已经是 "{linkTypeId}:{SOURCE|TARGET}" 格式
    const columnConfig = createEmptyColumnConfig(draggedField.value.id)
    localColumnConfigs.value.push(columnConfig)
    selectedFieldIds.value.add(draggedField.value.id)

    draggedField.value = null
    dragOverIndex.value = -1
    emitChange()
  }
  // 从右侧拖拽到末尾
  else if (draggedColumnIndex.value !== -1) {
    const lastIndex = localColumnConfigs.value.length - 1
    if (draggedColumnIndex.value === lastIndex) return

    const draggedItem = localColumnConfigs.value[draggedColumnIndex.value]
    if (!draggedItem) return
    const newConfigs = [...localColumnConfigs.value]

    // 移除原位置
    newConfigs.splice(draggedColumnIndex.value, 1)
    // 添加到末尾
    newConfigs.push(draggedItem)

    localColumnConfigs.value = newConfigs

    // 清理状态
    draggedColumnIndex.value = -1
    dragOverIndex.value = -1
    emitChange()
  }
}

// 拖拽进入末尾区域
function handleDragEnterEnd(event: DragEvent) {
  event.preventDefault()
  dragOverIndex.value = localColumnConfigs.value.length
}

// 右侧列拖拽开始
function handleColumnDragStart(index: number, event: DragEvent) {
  draggedColumnIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(index))
  }
}

// 右侧列拖拽结束
function handleColumnDragEnd() {
  draggedColumnIndex.value = -1
  dragOverIndex.value = -1
}

// 右侧列拖拽进入（同时处理右侧内部排序和左侧拖入）
function handleColumnDragEnter(index: number, event: DragEvent) {
  event.preventDefault()

  // 右侧内部拖拽
  if (draggedColumnIndex.value !== -1) {
    // 不能拖拽到自己位置
    if (index === draggedColumnIndex.value) return
    dragOverIndex.value = index
  }
  // 左侧拖拽到右侧
  else if (draggedField.value) {
    dragOverIndex.value = index
  }
}

// 右侧列拖拽放置（同时处理右侧内部排序和左侧拖入）
function handleColumnDrop(index: number, event: DragEvent) {
  event.preventDefault()

  // 右侧内部排序
  if (draggedColumnIndex.value !== -1) {
    if (draggedColumnIndex.value === index) return

    const draggedItem = localColumnConfigs.value[draggedColumnIndex.value]
    if (!draggedItem) return
    const newConfigs = [...localColumnConfigs.value]

    // 移除原位置
    newConfigs.splice(draggedColumnIndex.value, 1)

    // 计算新位置（如果原位置在目标位置之前，需要-1）
    const targetIndex = draggedColumnIndex.value < index ? index - 1 : index

    // 插入新位置
    newConfigs.splice(targetIndex, 0, draggedItem)

    localColumnConfigs.value = newConfigs

    // 清理状态
    draggedColumnIndex.value = -1
    dragOverIndex.value = -1
    emitChange()
  }
  // 左侧拖拽到右侧
  else if (draggedField.value) {
    // 对于 LINK 类型字段，field.id 已经是 "{linkTypeId}:{SOURCE|TARGET}" 格式
    const columnConfig = createEmptyColumnConfig(draggedField.value.id)
    localColumnConfigs.value.splice(index, 0, columnConfig)
    selectedFieldIds.value.add(draggedField.value.id)

    // 清理状态
    draggedField.value = null
    dragOverIndex.value = -1
    emitChange()
  }
}
</script>

<template>
  <div class="column-config-editor">
    <a-spin :loading="loading" style="width: 100%">
      <div class="editor-container">
        <!-- 左侧：可用字段列表 -->
        <div class="available-fields">
          <div class="section-header">
            <h4>可用字段 ({{ unselectedFields.length }})</h4>
          </div>
          <div class="field-list">
            <!-- 自定义属性分组 -->
            <template v-if="customUnselectedFields.length > 0">
              <div class="field-group-title">自定义属性</div>
              <div
                v-for="field in customUnselectedFields"
                :key="field.id"
                class="field-item"
                draggable="true"
                @click="handleAddField(field)"
                @dragstart="handleDragStart(field, $event)"
                @dragend="handleDragEnd"
              >
                <div class="field-info">
                  <FieldTypeIcon :field-type="field.fieldType" />
                  <span class="field-name">{{ field.name }}</span>
                </div>
                <a-button type="text" size="mini">
                  <template #icon>
                    <IconPlus />
                  </template>
                </a-button>
              </div>
            </template>

            <!-- 内置属性分组 -->
            <template v-if="builtinUnselectedFields.length > 0">
              <div class="field-group-title">内置属性</div>
              <div
                v-for="field in builtinUnselectedFields"
                :key="field.id"
                class="field-item builtin-field"
                draggable="true"
                @click="handleAddField(field)"
                @dragstart="handleDragStart(field, $event)"
                @dragend="handleDragEnd"
              >
                <div class="field-info">
                  <FieldTypeIcon :field-type="field.fieldType" />
                  <span class="field-name">{{ field.name }}</span>
                </div>
                <a-button type="text" size="mini">
                  <template #icon>
                    <IconPlus />
                  </template>
                </a-button>
              </div>
            </template>

            <a-empty v-if="unselectedFields.length === 0" description="无可用字段" />
          </div>
        </div>

        <!-- 右侧：已选列配置 -->
        <div class="selected-columns">
          <div class="section-header">
            <h4>已选列 ({{ localColumnConfigs.length }})</h4>
          </div>
          <div class="column-list">
            <div
              v-for="(column, index) in localColumnConfigs"
              :key="column.fieldId"
              class="column-item"
              :class="{
                'drag-over': dragOverIndex === index && draggedColumnIndex !== index,
                'dragging-column': draggedColumnIndex === index
              }"
              draggable="true"
              @dragstart="handleColumnDragStart(index, $event)"
              @dragend="handleColumnDragEnd"
              @dragenter="handleColumnDragEnter(index, $event)"
              @dragover="handleDragOver"
              @drop="handleColumnDrop(index, $event)"
            >
              <div class="column-row">
                <div class="column-info">
                  <FieldTypeIcon :field-type="getFieldType(column.fieldId)" size="small" />
                  <span class="column-name">{{ getFieldName(column.fieldId) }}</span>
                </div>
                <div class="column-config">
                  <a-space :size="6">
                    <div class="config-item">
                      <span class="config-label">宽度:</span>
                      <a-input-number
                        :model-value="column.width"
                        :min="80"
                        :max="800"
                        :step="10"
                        size="mini"
                        style="width: 80px"
                        placeholder="150"
                        @change="(v: number | undefined) => handleWidthChange(index, v)"
                      />
                    </div>
                    <a-checkbox
                      :model-value="column.resizable"
                      @change="handleToggleResizable(index)"
                    >
                      <span style="font-size: 11px;">可拖拽宽度</span>
                    </a-checkbox>
                    <a-tooltip content="切换可见性">
                      <a-button
                        type="text"
                        size="small"
                        class="toggle-btn visibility-btn"
                        :class="{ 'visibility-btn-active': column.visible }"
                        @click="handleToggleVisible(index)"
                      >
                        <template #icon>
                          <component :is="column.visible ? IconEye : IconEyeInvisible" />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip content="切换冻结">
                      <a-button
                        type="text"
                        size="small"
                        class="toggle-btn frozen-btn"
                        :class="{ 'frozen-btn-active': column.frozen }"
                        @click="handleToggleFrozen(index)"
                      >
                        <template #icon>
                          <component :is="column.frozen ? IconLock : IconUnlock" />
                        </template>
                      </a-button>
                    </a-tooltip>
                  </a-space>
                </div>
                <div class="column-actions">
                  <a-button
                    type="text"
                    size="mini"
                    :disabled="index === 0"
                    @click="handleMoveUp(index)"
                  >
                    <template #icon>
                      <IconUp />
                    </template>
                  </a-button>
                  <a-button
                    type="text"
                    size="mini"
                    :disabled="index === localColumnConfigs.length - 1"
                    @click="handleMoveDown(index)"
                  >
                    <template #icon>
                      <IconDown />
                    </template>
                  </a-button>
                  <a-button type="text" size="mini" status="danger" @click="handleRemoveField(index)">
                    <template #icon>
                      <IconDelete />
                    </template>
                  </a-button>
                </div>
              </div>
            </div>
            <a-empty v-if="localColumnConfigs.length === 0" description="请从左侧选择字段" />

            <!-- 末尾拖拽区域 -->
            <div
              v-if="localColumnConfigs.length > 0"
              class="drop-end-area"
              :class="{ 'drop-end-active': dragOverIndex === localColumnConfigs.length }"
              @dragenter="handleDragEnterEnd"
              @dragover="handleDragOver"
              @drop="handleDropToEnd"
            ></div>
          </div>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<style scoped lang="scss">
.column-config-editor {
  width: 100%;
  height: 100%;
}

.editor-container {
  display: flex;
  gap: 16px;
  height: 500px;
}

.available-fields {
  flex: 4;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

.selected-columns {
  flex: 6;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

.section-header {
  padding: 8px 12px;
  background-color: var(--color-fill-2);
  border-bottom: 1px solid var(--color-border);

  h4 {
    margin: 0;
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-1);
  }
}

.field-list,
.column-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.field-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  margin-bottom: 3px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  cursor: move;
  transition: all 0.2s;
  height: 32px;
  box-sizing: border-box;

  &:hover {
    border-color: rgb(var(--primary-6));
    background-color: var(--color-fill-1);
  }

  &:active {
    opacity: 0.5;
  }
}

.field-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.field-name {
  font-size: 12px;
  color: var(--color-text-1);
}

.field-group-title {
  padding: 6px 10px 4px;
  font-size: 11px;
  color: var(--color-text-3);
  background-color: var(--color-fill-1);
  margin-top: 4px;
  margin-bottom: 2px;
  border-radius: 2px;

  &:first-child {
    margin-top: 0;
  }
}

.builtin-field {
  background-color: var(--color-fill-1);
  border-style: dashed;
}

.column-item {
  padding: 6px 10px;
  margin-bottom: 3px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background-color: var(--color-fill-1);
  height: 32px;
  box-sizing: border-box;
  cursor: move;
}

.column-row {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
  height: 100%;
}

.column-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  min-width: 100px;
}

.column-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
  white-space: nowrap;
}

.column-config {
  flex: 1;
  display: flex;
  justify-content: flex-start;
}

.column-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.config-label {
  font-size: 11px;
  color: var(--color-text-3);
  white-space: nowrap;
}

.toggle-btn {
  color: var(--color-text-4) !important;

  :deep(.arco-icon) {
    font-size: 16px;
  }
}

.visibility-btn-active {
  color: #f7ba1e !important;

  :deep(.arco-icon) {
    font-size: 16px;
    font-weight: bold;
  }
}

.frozen-btn-active {
  color: #f7ba1e !important;

  :deep(.arco-icon) {
    font-size: 16px;
    font-weight: bold;
  }
}

.drag-over {
  border-top: 2px solid rgb(var(--primary-6)) !important;
  margin-top: -1px;
}

// 右侧拖拽中的项
.column-item.dragging-column {
  opacity: 0.5;
  cursor: grabbing !important;
  transform: scale(0.98);
  background-color: var(--color-fill-2);
  transition: transform 0.15s ease-out,
              opacity 0.15s ease-out,
              background-color 0.15s ease-out;
}

// 末尾拖拽区域
.drop-end-area {
  height: 20px;
  margin-top: -3px;
  border-top: 2px solid transparent;
  transition: border-color 0.2s ease-in-out;
}

.drop-end-area.drop-end-active {
  border-top: 2px solid rgb(var(--primary-6));
}
</style>
