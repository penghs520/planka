<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { useOrgStore } from '@/stores/org'
import {
  createEmptyListView,
  ensureDefaultListViewSorts,
  type ListViewDefinition,
} from '@/types/view'
import type { FieldOption } from '@/types/field-option'
import { cardTypeApi, linkTypeApi } from '@/api'
import type { CardTypeDefinition } from '@/types/card-type'
import type { LinkTypeVO } from '@/types/link-type'
import { getIncompleteConditionCount } from '@/utils/condition-factory'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import ViewCreateTypePicker from './ViewCreateTypePicker.vue'
import ListViewEditForm from './list-view/ListViewEditForm.vue'
import {
  WORKSPACE_CREATE_VIEW_TYPE_OPTIONS,
  type WorkspaceCreateViewKind,
} from '@/constants/workspace-create-view'
import { useListViewFormValidation } from './list-view/composables/useListViewFormValidation'
import {
  syncVisibilityForSave,
  ensureListViewPageDefaults,
} from './list-view/composables/listViewSaveHelpers'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    visible: boolean
    mode: 'create' | 'edit'
    view?: ListViewDefinition | null
    cascadeRelationNodeContextId?: string
    saveLoading?: boolean
  }>(),
  {
    visible: false,
    mode: 'create',
    view: null,
    cascadeRelationNodeContextId: undefined,
    saveLoading: false,
  },
)

const emit = defineEmits<{
  'update:visible': [value: boolean]
  save: [view: ListViewDefinition]
}>()

const orgStore = useOrgStore()
const activeTab = ref('basic')
const createPhase = ref<'pickType' | 'form'>('pickType')
const pickedCreateKind = ref<WorkspaceCreateViewKind | null>(null)
const formData = ref<ListViewDefinition | null>(null)
const cardTypes = ref<CardTypeDefinition[]>([])
const loadingCardTypes = ref(false)
const availableFields = ref<FieldOption[]>([])
const loadingFields = ref(false)
const linkTypes = ref<LinkTypeVO[]>([])
const loadingLinkTypes = ref(false)
const listViewFormRef = ref<InstanceType<typeof ListViewEditForm> | null>(null)

const { hasIncompleteSortRules, saveDisabledTooltip } = useListViewFormValidation(formData)

const drawerTitle = computed(() => {
  if (props.mode === 'create') {
    return createPhase.value === 'pickType'
      ? t('sidebar.createViewPickTypeTitle')
      : t('sidebar.createViewDetailsTitle')
  }
  return formData.value ? `编辑视图 - ${formData.value.name}` : '编辑视图'
})

const cardTypeOptions = computed(() => {
  return cardTypes.value.map((ct) => ({
    value: ct.id!,
    label: ct.name,
  }))
})

const rootCardTypeName = computed(() => {
  if (!formData.value?.cardTypeId) return ''
  const cardType = cardTypes.value.find((ct) => ct.id === formData.value?.cardTypeId)
  return cardType?.name || ''
})

function resetCreateFlow() {
  createPhase.value = 'pickType'
  pickedCreateKind.value = null
  formData.value = null
}

watch(
  () => props.visible,
  (newVisible) => {
    if (!newVisible) {
      return
    }
    activeTab.value = 'basic'
    if (props.mode === 'create') {
      resetCreateFlow()
    } else {
      createPhase.value = 'form'
      if (props.view) {
        const parsed = JSON.parse(JSON.stringify(props.view)) as ListViewDefinition
        ensureDefaultListViewSorts(parsed)
        formData.value = parsed
      }
    }
    if (!cardTypes.value.length) {
      fetchCardTypes()
    }
    if (!linkTypes.value.length) {
      fetchLinkTypes()
    }
  },
  { immediate: true },
)

watch(
  () => props.view,
  (newView) => {
    if (props.visible && props.mode === 'edit' && newView) {
      const parsed = JSON.parse(JSON.stringify(newView)) as ListViewDefinition
      ensureDefaultListViewSorts(parsed)
      formData.value = parsed
    }
  },
)

async function fetchCardTypes() {
  loadingCardTypes.value = true
  try {
    cardTypes.value = await cardTypeApi.listAll()
  } catch (error) {
    console.error('Failed to fetch card types:', error)
  } finally {
    loadingCardTypes.value = false
  }
}

async function fetchLinkTypes() {
  loadingLinkTypes.value = true
  try {
    linkTypes.value = await linkTypeApi.list()
  } catch (error) {
    console.error('Failed to fetch link types:', error)
  } finally {
    loadingLinkTypes.value = false
  }
}

async function fetchFields(cardTypeId: string) {
  loadingFields.value = true
  try {
    availableFields.value = await cardTypeApi.getFieldOptions(cardTypeId)
  } catch (error) {
    console.error('Failed to fetch fields:', error)
  } finally {
    loadingFields.value = false
  }
}

watch(
  () => formData.value?.cardTypeId,
  (newCardTypeId, oldCardTypeId) => {
    if (newCardTypeId) {
      fetchFields(newCardTypeId)
    } else {
      availableFields.value = []
    }
    if (
      formData.value &&
      oldCardTypeId !== undefined &&
      oldCardTypeId !== newCardTypeId
    ) {
      formData.value.groupBy = undefined
    }
  },
  { immediate: true },
)

watch(
  () => props.visible,
  (newVisible) => {
    if (newVisible) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
  },
  { immediate: true },
)

function handleClose() {
  emit('update:visible', false)
}

function onPickViewKind(kind: WorkspaceCreateViewKind) {
  pickedCreateKind.value = kind
}

function handleCreateTypeNext() {
  if (!pickedCreateKind.value) {
    Message.warning(t('sidebar.createViewPickTypeRequired'))
    return
  }
  const opt = WORKSPACE_CREATE_VIEW_TYPE_OPTIONS.find((o) => o.kind === pickedCreateKind.value)
  if (!opt?.enabled || !orgStore.currentOrgId) {
    return
  }
  if (opt.kind === 'LIST') {
    const draft = createEmptyListView(orgStore.currentOrgId)
    if (props.cascadeRelationNodeContextId) {
      draft.viewVisibilityScope = 'CASCADE_RELATION_NODE'
      draft.visibleCascadeRelationNodeIds = [props.cascadeRelationNodeContextId]
    }
    formData.value = draft
    createPhase.value = 'form'
  }
}

function handleBackToPickType() {
  createPhase.value = 'pickType'
  formData.value = null
}

function handleSave() {
  if (!formData.value) {
    return
  }
  const d = formData.value

  if (!d.name?.trim()) {
    Message.warning(t('viewForm.validation.nameRequired'))
    activeTab.value = 'basic'
    return
  }
  if (!d.cardTypeId) {
    Message.warning(t('viewForm.validation.cardTypeRequired'))
    activeTab.value = 'basic'
    return
  }

  const rawSorts = d.sorts ?? []
  if (rawSorts.some((s) => !s.field || String(s.field).trim() === '')) {
    Message.warning(t('viewForm.validation.sortFieldRequired'))
    activeTab.value = 'basic'
    return
  }

  syncVisibilityForSave(d)

  if (d.viewVisibilityScope === 'TEAMS' && !(d.visibleTeamCardIds?.length)) {
    Message.warning(t('viewForm.validation.teamsRequired'))
    activeTab.value = 'visibility'
    return
  }
  if (
    d.viewVisibilityScope === 'CASCADE_RELATION_NODE' &&
    !(d.visibleCascadeRelationNodeIds?.length)
  ) {
    Message.warning(t('viewForm.validation.cascadeRelationNodesRequired'))
    activeTab.value = 'visibility'
    return
  }

  const cols = (d.columnConfigs ?? []).filter(
    (c) => c.fieldId && String(c.fieldId).trim() !== '',
  )
  if (cols.length === 0) {
    Message.warning(t('viewForm.validation.columnsRequired'))
    activeTab.value = 'columns'
    return
  }
  d.columnConfigs = cols

  ensureListViewPageDefaults(d)

  d.sorts = (d.sorts ?? []).filter(
    (s) => s.field && String(s.field).trim() !== '',
  )

  if (listViewFormRef.value && !listViewFormRef.value.validateFilterCondition()) {
    const count = getIncompleteConditionCount(d.condition)
    Message.warning(`存在 ${count} 个未填写完整的过滤条件或空条件组`)
    activeTab.value = 'filter'
    return
  }

  emit('save', d)
}
</script>

<template>
  <a-drawer
    :visible="visible"
    :title="drawerTitle"
    :width="720"
    :mask-closable="true"
    :esc-to-close="true"
    :render-to-body="true"
    unmount-on-close
    @update:visible="(val) => emit('update:visible', val)"
    @cancel="handleClose"
  >
    <ViewCreateTypePicker
      v-if="mode === 'create' && createPhase === 'pickType'"
      :picked-kind="pickedCreateKind"
      @select="onPickViewKind"
    />

    <div
      v-else-if="formData"
      class="drawer-content"
    >
      <!-- 列表视图：其它 schemaSubType 可在此拆分为独立表单组件 -->
      <ListViewEditForm
        ref="listViewFormRef"
        v-model:active-tab="activeTab"
        :list-view="formData"
        :mode="mode"
        :cascade-relation-node-context-id="cascadeRelationNodeContextId"
        :card-type-options="cardTypeOptions"
        :loading-card-types="loadingCardTypes"
        :available-fields="availableFields"
        :loading-fields="loadingFields"
        :link-types="linkTypes"
        :root-card-type-name="rootCardTypeName"
      />
    </div>

    <template #footer>
      <a-space v-if="mode === 'create' && createPhase === 'pickType'">
        <CancelButton @click="handleClose" />
        <a-button
          type="primary"
          @click="handleCreateTypeNext"
        >
          {{ t('sidebar.createViewNext') }}
        </a-button>
      </a-space>
      <a-space v-else>
        <CancelButton @click="handleClose" />
        <a-button
          v-if="mode === 'create'"
          @click="handleBackToPickType"
        >
          {{ t('sidebar.createViewBack') }}
        </a-button>
        <a-tooltip
          :content="saveDisabledTooltip"
          :disabled="!hasIncompleteSortRules"
        >
          <span class="save-btn-tooltip-wrap">
            <SaveButton
              :text="mode === 'create' ? '创建' : '保存'"
              :loading="saveLoading"
              :disabled="hasIncompleteSortRules"
              @click="handleSave"
            />
          </span>
        </a-tooltip>
      </a-space>
    </template>
  </a-drawer>
</template>

<style scoped lang="scss">
.drawer-content {
  padding: 0 16px;
}

.save-btn-tooltip-wrap {
  display: inline-block;
  vertical-align: middle;
}
</style>
