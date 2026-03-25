<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, provide, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconUndo, IconRedo } from '@arco-design/web-vue/es/icon'
import { cardDetailTemplateApi } from '@/api/card-detail-template'
import { cardTypeApi } from '@/api/card-type'
import SaveButton from '@/components/common/SaveButton.vue'
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

const DETAIL_BUILTIN_FIELDS: (FieldConfig & { builtin: true })[] = [
  { fieldId: '$createdAt', name: '创建时间', schemaSubType: 'DATE_FIELD', builtin: true },
  { fieldId: '$updatedAt', name: '更新时间', schemaSubType: 'DATE_FIELD', builtin: true },
  { fieldId: '$archivedAt', name: '存档时间', schemaSubType: 'DATE_FIELD', builtin: true },
  { fieldId: '$discardedAt', name: '回收时间', schemaSubType: 'DATE_FIELD', builtin: true },
  { fieldId: '$description', name: '详情描述', schemaSubType: 'MARKDOWN_FIELD', builtin: true },
]

const props = withDefaults(
  defineProps<{
    /** route：独立路由页；embedded：实体类型抽屉内嵌 */
    mode?: 'route' | 'embedded'
    embeddedCardTypeId?: string
    embeddedCardTypeName?: string
    /** 由父级 getEffective 加载后传入 */
    embeddedDefinition?: CardDetailTemplateDefinition | null
    embeddedPersisted?: boolean
    /** 内嵌时若提供，将右侧工具栏 Teleport 到该节点（与 PageLayoutTab 顶栏同一行） */
    embeddedToolbarMount?: HTMLElement | null
  }>(),
  {
    mode: 'route',
    embeddedDefinition: null,
    embeddedPersisted: false,
    embeddedToolbarMount: null,
  },
)

const emit = defineEmits<{
  (e: 'update:persisted', value: boolean): void
  (e: 'saved', definition: CardDetailTemplateDefinition): void
}>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const savedTemplateId = ref<string | null>(null)
/** 内嵌模式：与 props 同步，保存成功后立即置 true，避免父组件重绘前误判为新建 */
const persistedLocal = ref(props.embeddedPersisted ?? false)
watch(
  () => props.embeddedPersisted,
  (v) => {
    persistedLocal.value = v ?? false
  },
)

const isEditMode = computed(() => {
  if (props.mode === 'embedded') {
    return (
      persistedLocal.value
      && !!templateData.value?.id
      && templateData.value.id !== 'default_template'
    )
  }
  return !!route.params.id || !!savedTemplateId.value
})

const templateId = computed(() => {
  if (props.mode === 'embedded') {
    const id = templateData.value?.id
    if (id && id !== 'default_template') {
      return id
    }
    return savedTemplateId.value || ''
  }
  return (route.params.id as string) || savedTemplateId.value || ''
})

const isCardTypeLocked = computed(() => {
  if (props.mode === 'embedded') {
    return true
  }
  return isEditMode.value || !!route.query.cardTypeId
})

const cardTypeName = ref('')
const loading = ref(false)
const saving = ref(false)
const templateData = ref<CardDetailTemplateDefinition | null>(null)
const orgId = ref('default_org')

const selectedItem = ref<SelectedItem | null>(null)
const fields = ref<FieldConfig[]>([])
const fieldsLoading = ref(false)

const history = ref<string[]>([])
const historyIndex = ref(-1)
const savedHistoryIndex = ref(-1)
const maxHistoryLength = 50

const hasChanges = computed(() => {
  if (!templateData.value) return false
  return historyIndex.value !== savedHistoryIndex.value
})

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

provide('templateData', templateData)
provide('selectedItem', selectedItem)
provide('fields', fields)
provide('usedFieldIds', usedFieldIds)

async function loadTemplateRoute() {
  if (!isEditMode.value) {
    const cardTypeId = (route.query.cardTypeId as string) || ''
    cardTypeName.value = (route.query.cardTypeName as string) || ''
    templateData.value = createEmptyTemplate(orgId.value, cardTypeId)
    pushHistory()
    savedHistoryIndex.value = historyIndex.value
    if (cardTypeId) {
      await loadFields(cardTypeId)
    }
    return
  }

  loading.value = true
  try {
    templateData.value = await cardDetailTemplateApi.getById(templateId.value)
    pushHistory()
    savedHistoryIndex.value = historyIndex.value
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

function applyEmbeddedDefinition(def: CardDetailTemplateDefinition) {
  templateData.value = JSON.parse(JSON.stringify(def))
  if (!templateData.value) return
  if (!templateData.value.orgId) {
    templateData.value.orgId = orgId.value
  }
  cardTypeName.value = props.embeddedCardTypeName || ''
  history.value = []
  historyIndex.value = -1
  pushHistory()
  savedHistoryIndex.value = historyIndex.value
  persistedLocal.value = props.embeddedPersisted ?? false
  savedTemplateId.value =
    persistedLocal.value && def.id && def.id !== 'default_template' ? def.id : null
}

async function loadTemplateEmbedded() {
  if (!props.embeddedDefinition || !props.embeddedCardTypeId) {
    templateData.value = null
    return
  }
  loading.value = true
  try {
    applyEmbeddedDefinition(props.embeddedDefinition)
    await loadFields(props.embeddedCardTypeId)
  } finally {
    loading.value = false
  }
}

// 内嵌：父级异步拉取 effective 后传入。
// 注意：不要监听 embeddedPersisted —— 首次保存后父级会 emit persisted=true，
// 若因此重跑 loadTemplateEmbedded，会用尚未 refetch 的旧 definition（仍含 default_template）
// 覆盖子组件已写入的真实 id，导致 isEditMode 误判，再次保存时重复走 create。
watch(
  () => [props.mode, props.embeddedDefinition, props.embeddedCardTypeId] as const,
  () => {
    if (props.mode !== 'embedded') return
    if (props.embeddedDefinition && props.embeddedCardTypeId) {
      void loadTemplateEmbedded()
    }
  },
  { immediate: true },
)

async function loadFields(cardTypeId: string) {
  if (!cardTypeId) {
    fields.value = []
    return
  }
  fieldsLoading.value = true
  try {
    const result = await cardTypeApi.getFieldConfigsWithSource(cardTypeId)
    fields.value = [...result.fields, ...DETAIL_BUILTIN_FIELDS]
  } catch (error: any) {
    console.error('Failed to load field configs:', error)
    Message.error(error.message || '加载字段配置列表失败')
  } finally {
    fieldsLoading.value = false
  }
}

function handleCardTypeChange(cardTypeId: string) {
  if (templateData.value) {
    templateData.value.cardTypeId = cardTypeId
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

function handleRefreshFields() {
  if (templateData.value?.cardTypeId) {
    loadFields(templateData.value.cardTypeId)
  }
}

function pushHistory() {
  if (!templateData.value) return

  history.value = history.value.slice(0, historyIndex.value + 1)
  history.value.push(JSON.stringify(templateData.value))
  if (history.value.length > maxHistoryLength) {
    history.value.shift()
  } else {
    historyIndex.value++
  }
}

function handleUndo() {
  if (historyIndex.value > 0) {
    const currentVersion = templateData.value?.contentVersion
    historyIndex.value--
    const historyItem = history.value[historyIndex.value]
    if (historyItem) {
      templateData.value = JSON.parse(historyItem)
      if (currentVersion !== undefined && templateData.value) {
        templateData.value.contentVersion = currentVersion
      }
    }
  }
}

function handleRedo() {
  if (historyIndex.value < history.value.length - 1) {
    const currentVersion = templateData.value?.contentVersion
    historyIndex.value++
    const historyItem = history.value[historyIndex.value]
    if (historyItem) {
      templateData.value = JSON.parse(historyItem)
      if (currentVersion !== undefined && templateData.value) {
        templateData.value.contentVersion = currentVersion
      }
    }
  }
}

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

function buildCreatePayload(): CardDetailTemplateDefinition {
  if (!templateData.value) {
    throw new Error('no template')
  }
  const payload = JSON.parse(JSON.stringify(templateData.value)) as CardDetailTemplateDefinition
  delete payload.id
  payload.systemTemplate = false
  payload.isDefault = true
  if (!payload.orgId) {
    payload.orgId = orgId.value
  }
  return payload
}

async function handleSave() {
  if (!templateData.value) return

  if (saving.value) return

  if (!hasChanges.value) {
    Message.info('没有需要保存的内容')
    return
  }

  saving.value = true

  if (!templateData.value.name?.trim()) {
    Message.warning('请输入模板名称')
    saving.value = false
    return
  }
  if (!templateData.value.cardTypeId) {
    Message.warning('请选择实体类型')
    saving.value = false
    return
  }

  let loadingMsg: { close: () => void } | null = null
  const loadingTimer = setTimeout(() => {
    loadingMsg = Message.loading({ content: '保存中...', duration: 0 })
  }, 200)

  const closeLoading = () => {
    clearTimeout(loadingTimer)
    loadingMsg?.close()
  }

  try {
    // 以是否已有持久化 id 为准，避免内嵌模式下 persisted 变更触发重载后 isEditMode 短暂错误导致重复 create
    const hasPersistedTemplateId =
      !!templateData.value.id && templateData.value.id !== 'default_template'
    if (hasPersistedTemplateId) {
      const result = await cardDetailTemplateApi.update(
        templateId.value,
        templateData.value,
        templateData.value.contentVersion,
      )
      templateData.value.contentVersion = result.contentVersion
      closeLoading()
      Message.success('保存成功')
      emit('saved', result)
    } else {
      const payload = buildCreatePayload()
      const result = await cardDetailTemplateApi.create(payload)
      savedTemplateId.value = result.id || null
      if (templateData.value) {
        templateData.value.id = result.id
        templateData.value.contentVersion = result.contentVersion
      }
      closeLoading()
      Message.success('创建成功')
      if (props.mode === 'embedded') {
        persistedLocal.value = true
        emit('update:persisted', true)
        emit('saved', result)
      } else {
        router.replace({ name: 'CardDetailTemplateEdit', params: { id: result.id } })
      }
    }
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

function handleSelect(item: SelectedItem | null) {
  selectedItem.value = item
}

function handleTemplateChange() {
  pushHistory()
}

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

function handleBack() {
  const cardTypeId = templateData.value?.cardTypeId || (route.query.cardTypeId as string)
  if (cardTypeId) {
    router.push({
      path: '/admin/card-type',
      query: { edit: cardTypeId, tab: 'pageLayout' },
    })
  } else {
    router.push({ name: 'CardTypeCard' })
  }
}

onMounted(() => {
  if (props.mode === 'route') {
    void loadTemplateRoute()
  }
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<template>
  <div v-if="templateData" class="template-editor" :class="{ 'template-editor--embedded': mode === 'embedded' }">
    <!-- 内嵌 + 挂载点：工具栏 Teleport 到 PageLayoutTab 顶栏右侧，与「详情/新建」切换同一行 -->
    <Teleport
      v-if="mode === 'embedded' && embeddedToolbarMount"
      :to="embeddedToolbarMount"
    >
      <div class="toolbar-right">
        <span class="save-status" :class="{ 'has-changes': hasChanges }">
          <template v-if="hasChanges">● 未保存</template>
          <template v-else>✓ 已保存</template>
        </span>
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
        <SaveButton
          :loading="saving"
          :disabled="!hasChanges"
          :text="t('common.action.save')"
          @click="handleSave"
        />
      </div>
    </Teleport>

    <header v-if="mode === 'route'" class="editor-toolbar">
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
          <span class="card-type-prefix">所属实体类型</span>
          {{ cardTypeName }}
        </span>
        <CardTypeSelect
          v-else
          v-model="templateData.cardTypeId"
          placeholder="选择实体类型"
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
        </span>
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
        <SaveButton
          :loading="saving"
          :disabled="!hasChanges"
          :text="t('common.action.save')"
          @click="handleSave"
        />
      </div>
    </header>

    <header
      v-if="mode === 'embedded' && !embeddedToolbarMount"
      class="editor-toolbar editor-toolbar--embedded-only-actions"
    >
      <div class="toolbar-right">
        <span class="save-status" :class="{ 'has-changes': hasChanges }">
          <template v-if="hasChanges">● 未保存</template>
          <template v-else>✓ 已保存</template>
        </span>
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
        <SaveButton
          :loading="saving"
          :disabled="!hasChanges"
          :text="t('common.action.save')"
          @click="handleSave"
        />
      </div>
    </header>

    <main class="editor-main">
      <aside class="editor-sidebar-left">
        <FieldLibrary
          :fields="fields"
          :loading="fieldsLoading"
          :used-field-ids="usedFieldIds"
          @refresh="handleRefreshFields"
        />
      </aside>

      <section class="editor-canvas">
        <LayoutCanvas
          v-model="templateData"
          :selected-item="selectedItem"
          @select="handleSelect"
          @change="handleTemplateChange"
        />
      </section>

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
@import './TemplateEditorCore.scss';
</style>
