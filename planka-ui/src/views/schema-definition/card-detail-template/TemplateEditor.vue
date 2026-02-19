<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { IconUndo, IconRedo } from '@arco-design/web-vue/es/icon'
import { cardDetailTemplateApi } from '@/api/card-detail-template'
import { cardTypeApi } from '@/api/card-type'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import FieldLibrary from './components/FieldLibrary.vue'
import LayoutCanvas from './components/LayoutCanvas.vue'
import PropertyPanel from './components/PropertyPanel.vue'
import type {
  CardDetailTemplateDefinition,
  SelectedItem,
} from '@/types/card-detail-template'
import type { FieldConfig } from '@/types/card-type'
import { createEmptyTemplate } from '@/types/card-detail-template'

/**
 * 详情模板使用的内置字段
 * 转换为 FieldConfig 格式，添加 builtin 标记
 */
const DETAIL_BUILTIN_FIELDS: (FieldConfig & { builtin: true })[] = [
  { fieldId: '$createdAt', name: '创建时间', schemaSubType: 'DATE_FIELD', builtin: true },
  { fieldId: '$updatedAt', name: '更新时间', schemaSubType: 'DATE_FIELD', builtin: true },
  { fieldId: '$archivedAt', name: '归档时间', schemaSubType: 'DATE_FIELD', builtin: true },
  { fieldId: '$discardedAt', name: '丢弃时间', schemaSubType: 'DATE_FIELD', builtin: true },
  { fieldId: '$description', name: '详情描述', schemaSubType: 'MARKDOWN_FIELD', builtin: true },
]

const route = useRoute()
const router = useRouter()

// 编辑模式：有路由ID或已保存过的新建模板
const savedTemplateId = ref<string | null>(null)
const isEditMode = computed(() => !!route.params.id || !!savedTemplateId.value)
const templateId = computed(() => (route.params.id as string) || savedTemplateId.value || '')

// 卡片类型是否锁定（编辑模式或从卡片类型页面进入时锁定）
const isCardTypeLocked = computed(() => isEditMode.value || !!route.query.cardTypeId)

// 卡片类型名称（从路由参数或模板数据获取）
const cardTypeName = ref('')

// 加载状态
const loading = ref(false)
const saving = ref(false)

// 模板数据
const templateData = ref<CardDetailTemplateDefinition | null>(null)
const orgId = ref('default_org') // TODO: 从用户上下文获取

// 选中状态
const selectedItem = ref<SelectedItem | null>(null)

// 字段列表（使用完整的属性配置）
const fields = ref<FieldConfig[]>([])
const fieldsLoading = ref(false)

// 历史记录（撤销/重做）
const history = ref<string[]>([])
const historyIndex = ref(-1)
const savedHistoryIndex = ref(-1) // 记录保存时的历史位置
const maxHistoryLength = 50

// 是否有未保存的更改
const hasChanges = computed(() => {
  if (!templateData.value) return false
  return historyIndex.value !== savedHistoryIndex.value
})

// 计算已使用的字段 ID 列表
const usedFieldIds = computed(() => {
  if (!templateData.value) return []
  const ids: string[] = []
  templateData.value.tabs.forEach((tab) => {
    if (tab.sections) {
      tab.sections.forEach((section) => {
        section.fieldItems.forEach((item) => {
          ids.push(item.fieldConfigId)
        })
      })
    }
  })
  return ids
})

// 提供给子组件的上下文
provide('templateData', templateData)
provide('selectedItem', selectedItem)
provide('fields', fields)
provide('usedFieldIds', usedFieldIds)

// 加载模板数据
async function loadTemplate() {
  if (!isEditMode.value) {
    // 新建模式：从 query 参数获取卡片类型
    const cardTypeId = route.query.cardTypeId as string || ''
    cardTypeName.value = route.query.cardTypeName as string || ''
    templateData.value = createEmptyTemplate(orgId.value, cardTypeId)
    pushHistory()
    savedHistoryIndex.value = historyIndex.value // 初始状态为已保存
    // 如果有卡片类型，加载字段列表
    if (cardTypeId) {
      await loadFields(cardTypeId)
    }
    return
  }

  loading.value = true
  try {
    templateData.value = await cardDetailTemplateApi.getById(templateId.value)
    pushHistory()
    savedHistoryIndex.value = historyIndex.value // 初始状态为已保存
    // 加载卡片类型名称和字段列表
    if (templateData.value.cardTypeId) {
      const cardType = await cardTypeApi.getById(templateData.value.cardTypeId)
      cardTypeName.value = cardType.name
      await loadFields(templateData.value.cardTypeId)
    }
  } catch (error: any) {
    console.error('Failed to load template:', error)
    Message.error(error.message || '加载模板失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 加载字段配置列表
async function loadFields(cardTypeId: string) {
  if (!cardTypeId) {
    fields.value = []
    return
  }
  fieldsLoading.value = true
  try {
    const result = await cardTypeApi.getFieldConfigsWithSource(cardTypeId)
    // 自定义字段在前，内置字段在后
    fields.value = [...result.fields, ...DETAIL_BUILTIN_FIELDS]
  } catch (error: any) {
    console.error('Failed to load field configs:', error)
    Message.error(error.message || '加载字段配置列表失败')
  } finally {
    fieldsLoading.value = false
  }
}

// 卡片类型变更
function handleCardTypeChange(cardTypeId: string) {
  if (templateData.value) {
    templateData.value.cardTypeId = cardTypeId
    // 清空已配置的字段
    templateData.value.tabs.forEach((tab) => {
      if (tab.sections) {
        tab.sections.forEach((section) => {
          section.fieldItems = []
        })
      }
    })
    loadFields(cardTypeId)
    pushHistory()
  }
}

// 刷新字段列表
function handleRefreshFields() {
  if (templateData.value?.cardTypeId) {
    loadFields(templateData.value.cardTypeId)
  }
}

// 推送历史记录
function pushHistory() {
  if (!templateData.value) return

  // 截断后面的历史
  history.value = history.value.slice(0, historyIndex.value + 1)
  // 添加新状态
  history.value.push(JSON.stringify(templateData.value))
  // 限制长度
  if (history.value.length > maxHistoryLength) {
    history.value.shift()
  } else {
    historyIndex.value++
  }
}

// 撤销
function handleUndo() {
  if (historyIndex.value > 0) {
    const currentVersion = templateData.value?.contentVersion
    historyIndex.value--
    const historyItem = history.value[historyIndex.value]
    if (historyItem) {
      templateData.value = JSON.parse(historyItem)
      // 保持最新的版本号，避免乐观锁冲突
      if (currentVersion !== undefined && templateData.value) {
        templateData.value.contentVersion = currentVersion
      }
    }
  }
}

// 重做
function handleRedo() {
  if (historyIndex.value < history.value.length - 1) {
    const currentVersion = templateData.value?.contentVersion
    historyIndex.value++
    const historyItem = history.value[historyIndex.value]
    if (historyItem) {
      templateData.value = JSON.parse(historyItem)
      // 保持最新的版本号，避免乐观锁冲突
      if (currentVersion !== undefined && templateData.value) {
        templateData.value.contentVersion = currentVersion
      }
    }
  }
}

// 可撤销/重做
const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

// 保存模板
async function handleSave() {
  if (!templateData.value) return

  // 防止重复提交（立即设置标志，确保原子性）
  if (saving.value) return

  // 检查是否有变化
  if (!hasChanges.value) {
    Message.info('没有需要保存的内容')
    return
  }

  saving.value = true

  // 验证
  if (!templateData.value.name?.trim()) {
    Message.warning('请输入模板名称')
    saving.value = false
    return
  }
  if (!templateData.value.cardTypeId) {
    Message.warning('请选择卡片类型')
    saving.value = false
    return
  }
  // 延迟 200ms 显示 loading，避免快速操作时闪烁
  let loadingMsg: { close: () => void } | null = null
  const loadingTimer = setTimeout(() => {
    loadingMsg = Message.loading({ content: '保存中...', duration: 0 })
  }, 200)

  const closeLoading = () => {
    clearTimeout(loadingTimer)
    loadingMsg?.close()
  }

  try {
    if (isEditMode.value) {
      const result = await cardDetailTemplateApi.update(
        templateId.value,
        templateData.value,
        templateData.value.contentVersion
      )
      // 更新版本号
      templateData.value.contentVersion = result.contentVersion
      closeLoading()
      Message.success('保存成功')
    } else {
      const result = await cardDetailTemplateApi.create(templateData.value)
      // 更新模板ID和版本号，切换为编辑模式
      savedTemplateId.value = result.id || null
      templateData.value.id = result.id
      templateData.value.contentVersion = result.contentVersion
      closeLoading()
      Message.success('创建成功')
      // 更新URL但不触发重新加载
      router.replace({ name: 'CardDetailTemplateEdit', params: { id: result.id } })
    }
    // 记录保存时的历史位置，并截断后面的历史（保存后不允许重做）
    savedHistoryIndex.value = historyIndex.value
    history.value = history.value.slice(0, historyIndex.value + 1)
  } catch (error: any) {
    closeLoading()
    console.error('Failed to save template:', error)
    Message.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 选中项变更
function handleSelect(item: SelectedItem | null) {
  selectedItem.value = item
}

// 模板数据变更（来自子组件）
function handleTemplateChange() {
  pushHistory()
}

// 监听快捷键
function handleKeyDown(e: KeyboardEvent) {
  if (e.ctrlKey || e.metaKey) {
    if (e.key === 'z' && !e.shiftKey) {
      e.preventDefault()
      handleUndo()
    } else if ((e.key === 'z' && e.shiftKey) || e.key === 'y') {
      e.preventDefault()
      handleRedo()
    } else if (e.key === 's') {
      e.preventDefault()
      handleSave()
    }
  }
}

// 返回卡片类型的详情模板 Tab
function handleBack() {
  const cardTypeId = templateData.value?.cardTypeId || (route.query.cardTypeId as string)
  if (cardTypeId) {
    router.push({
      path: '/admin/card-type',
      query: { edit: cardTypeId, tab: 'detailTemplate' },
    })
  } else {
    router.push({ name: 'CardTypeList' })
  }
}

onMounted(() => {
  loadTemplate()
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<template>
  <div v-if="templateData" class="template-editor">
    <!-- 顶部工具栏 -->
    <header class="editor-toolbar">
      <div class="toolbar-left">
        <a-button size="small" @click="handleBack">返回</a-button>
        <a-divider direction="vertical" />
        <span class="field-label">
          <span class="required-mark">*</span>
          模板名称
        </span>
        <a-input
          v-model="templateData.name"
          placeholder="请输入模板名称"
          size="small"
          class="template-name-input"
          @change="handleTemplateChange"
        />
        <span v-if="isCardTypeLocked" class="card-type-label">
          <span class="card-type-prefix">所属卡片类型</span>
          {{ cardTypeName }}
        </span>
        <CardTypeSelect
          v-else
          v-model="templateData.cardTypeId"
          placeholder="选择卡片类型"
          size="small"
          style="width: 160px"
          :multiple="false"
          @change="handleCardTypeChange"
        />
      </div>
      <div class="toolbar-right">
        <span class="save-status" :class="{ 'has-changes': hasChanges }">
          <template v-if="hasChanges">● 未保存</template>
          <template v-else>✓ 已保存</template>
          <span class="shortcut-hint">Ctrl/⌘+S</span>
        </span>
        <a-divider direction="vertical" />
        <a-button-group size="small">
          <a-tooltip content="撤销 (Ctrl+Z)">
            <a-button :disabled="!canUndo" @click="handleUndo">
              <template #icon><IconUndo /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip content="重做 (Ctrl+Shift+Z)">
            <a-button :disabled="!canRedo" @click="handleRedo">
              <template #icon><IconRedo /></template>
            </a-button>
          </a-tooltip>
        </a-button-group>
      </div>
    </header>

    <!-- 主编辑区域 -->
    <main class="editor-main">
      <!-- 左侧：字段资源库 -->
      <aside class="editor-sidebar-left">
        <FieldLibrary
          :fields="fields"
          :loading="fieldsLoading"
          :used-field-ids="usedFieldIds"
          @refresh="handleRefreshFields"
        />
      </aside>

      <!-- 中间：画布区域 -->
      <section class="editor-canvas">
        <LayoutCanvas
          v-model="templateData"
          :selected-item="selectedItem"
          @select="handleSelect"
          @change="handleTemplateChange"
        />
      </section>

      <!-- 右侧：属性面板 -->
      <aside class="editor-sidebar-right">
        <PropertyPanel
          :template-data="templateData"
          :selected-item="selectedItem"
          :fields="fields"
          @change="handleTemplateChange"
        />
      </aside>
    </main>
  </div>
  <div v-else class="editor-loading">
    <a-spin :loading="loading" tip="加载中..." />
  </div>
</template>

<style scoped lang="scss">
.template-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg-1);
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--color-bg-2);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;

  :deep(.arco-btn) {
    &:focus-visible,
    &:focus {
      outline: none;
      box-shadow: none;
    }
  }
}

.save-status {
  font-size: 12px;
  color: rgb(var(--success-6));
  display: flex;
  align-items: center;
  gap: 6px;

  &.has-changes {
    color: rgb(var(--warning-6));
  }

  .shortcut-hint {
    color: var(--color-text-3);
    font-size: 11px;
    padding: 1px 4px;
    background: var(--color-fill-2);
    border-radius: 3px;
  }
}

.field-label {
  font-size: 14px;
  color: var(--color-text-3);
  white-space: nowrap;
}

.required-mark {
  color: rgb(var(--danger-6));
  margin-right: 2px;
}

.template-name-input {
  width: 200px;
  border-radius: 4px;

  :deep(.arco-input-wrapper) {
    border-radius: 4px;
  }
}

.card-type-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-text-1);
}

.card-type-prefix {
  color: var(--color-text-3);
}

.editor-main {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.editor-sidebar-left {
  width: 220px;
  background: var(--color-bg-2);
  border-right: 1px solid var(--color-border);
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.editor-canvas {
  flex: 1;
  overflow: hidden;
  background: var(--color-fill-1);
}

.editor-sidebar-right {
  width: 280px;
  background: var(--color-bg-2);
  border-left: 1px solid var(--color-border);
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.editor-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}
</style>
