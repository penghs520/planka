<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { IconFile, IconTag } from '@arco-design/web-vue/es/icon'
import { cardTypeApi } from '@/api'
import { useOrgStore } from '@/stores/org'
import SaveButton from '@/components/common/SaveButton.vue'
import BasicInfoForm from './components/BasicInfoForm.vue'
import FieldConfigTab from './components/FieldConfigTab.vue'
import PageLayoutTab from './components/PageLayoutTab.vue'
import ValueStreamTab from './components/ValueStreamTab.vue'
import PermissionTab from './components/PermissionTab.vue'
import CardActionsTab from './components/CardActionsTab.vue'
import FlowManagementTab from './components/FlowManagementTab.vue'
import BizRulesTab from './components/BizRulesTab.vue'
import FieldConfigDrawer from './FieldConfigDrawer.vue'
import CreateFieldConfigDrawer from './CreateFieldConfigDrawer.vue'
import { SchemaSubType } from '@/types/schema'
import {
  createEmptyCardType,
  type CardTypeDefinition,
  type FieldConfigListWithSource,
  type FieldConfig,
} from '@/types/card-type'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  editingCardType: CardTypeDefinition | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'success': []
}>()

const orgStore = useOrgStore()

// 表单状态
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const formData = ref<CardTypeDefinition | null>(null)
const selectedSubType = ref<SchemaSubType.TRAIT_CARD_TYPE | SchemaSubType.ENTITY_CARD_TYPE>(SchemaSubType.ENTITY_CARD_TYPE)
const activeTab = ref('basic')
const fieldList = ref<FieldConfigListWithSource | null>(null)

/** 与 props.mode 同步；创建成功后置为 edit，便于继续配置其它 Tab */
const effectiveMode = ref<'create' | 'edit'>('create')

const showFooter = computed(
  () =>
    effectiveMode.value === 'create'
    || (effectiveMode.value === 'edit' && activeTab.value === 'basic'),
)

// 属性配置详情抽屉
const fieldConfigDrawerVisible = ref(false)
const currentFieldConfig = ref<FieldConfig | null>(null)

// ValueStreamTab 组件引用
const valueStreamTabRef = ref<InstanceType<typeof ValueStreamTab> | null>(null)

const drawerVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const isEntityType = computed(() => formData.value?.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE)

/** 标题栏图标：加载中时尚无 formData 时回退 editingCardType */
const headerIsEntityType = computed(() => {
  const sub = formData.value?.schemaSubType ?? props.editingCardType?.schemaSubType
  return sub === SchemaSubType.ENTITY_CARD_TYPE
})

const entityDisplayName = computed(() => {
  const name = formData.value?.name ?? props.editingCardType?.name
  return name?.trim() ? name : ''
})

/** 编辑模式标题行内标签（与 a-tab-pane key 一致） */
const cardTypeEditTabItems = computed(() => {
  const base: { key: string; label: string }[] = [
    { key: 'basic', label: t('admin.cardType.tabs.basic') },
    { key: 'fields', label: t('admin.cardType.tabs.fields') },
    { key: 'valueStream', label: t('admin.cardType.tabs.valueStream') },
  ]
  if (!isEntityType.value) {
    return base
  }
  return [
    ...base,
    { key: 'pageLayout', label: t('admin.cardType.tabs.pageLayout') },
    { key: 'permission', label: t('admin.cardType.tabs.permission') },
    { key: 'cardButtons', label: t('admin.cardType.tabs.cardButtons') },
    { key: 'flowManagement', label: t('admin.cardType.tabs.flowManagement') },
    { key: 'businessRules', label: t('admin.cardType.tabs.businessRules') },
  ]
})

const drawerTitle = computed(() => {
  if (props.mode === 'create') {
    return t('admin.cardType.createTitle')
  }
  return formData.value ? t('admin.cardType.editTitleWithName', { name: formData.value.name }) : t('admin.cardType.editTitle')
})

// 监听 selectedSubType 变化（仅新建模式）
watch(selectedSubType, (newSubType) => {
  if (effectiveMode.value === 'create' && orgStore.currentOrgId) {
    formData.value = createEmptyCardType(newSubType, orgStore.currentOrgId)
  }
})

// 监听 tab 切换，懒加载属性配置数据
watch(activeTab, async (newTab) => {
  if (newTab === 'fields' && effectiveMode.value === 'edit' && formData.value?.id && !fieldList.value) {
    loading.value = true
    try {
      fieldList.value = await cardTypeApi.getFieldConfigsWithSource(formData.value.id)
    } catch (error) {
      console.error('Failed to fetch field configs:', error)
      Message.error(t('admin.cardType.fetchFieldConfigFailed'))
    } finally {
      loading.value = false
    }
  }
})

// 初始化抽屉数据
async function initDrawer() {
  effectiveMode.value = props.mode
  activeTab.value = 'basic'
  fieldList.value = null

  if (props.mode === 'create') {
    selectedSubType.value = SchemaSubType.ENTITY_CARD_TYPE
    if (orgStore.currentOrgId) {
      formData.value = createEmptyCardType(selectedSubType.value, orgStore.currentOrgId)
    }
  } else if (props.editingCardType) {
    loading.value = true
    try {
      formData.value = await cardTypeApi.getById(props.editingCardType.id!)
    } catch (error) {
      console.error('Failed to fetch:', error)
      Message.error(t('admin.cardType.fetchCardTypeFailed'))
      drawerVisible.value = false
    } finally {
      loading.value = false
    }
  }
}

async function doSubmit() {
  if (!formData.value) return

  saving.value = true
  try {
    if (effectiveMode.value === 'create') {
      const created = await cardTypeApi.create(formData.value)
      formData.value = created
      effectiveMode.value = 'edit'
      Message.success(t('admin.message.createSuccess'))
    } else {
      const updated = await cardTypeApi.update(
        formData.value.id!,
        formData.value,
        formData.value.contentVersion,
      )
      formData.value = updated
      Message.success(t('admin.message.saveSuccess'))
    }
    emit('success')
  } catch (error) {
    console.error('Failed to save:', error)
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!formData.value) return

  const errors = await formRef.value?.validate()
  if (errors) return

  // 检查价值流 Tab 是否有未保存的更改
  if (valueStreamTabRef.value?.hasChanges) {
    Modal.warning({
      title: t('admin.cardType.valueStreamUnsavedTitle'),
      content: t('admin.cardType.valueStreamUnsavedContent'),
      okText: t('admin.cardType.iKnow'),
    })
    return
  }

  await doSubmit()
}

function handleOpenFieldConfigDetail(config: FieldConfig) {
  currentFieldConfig.value = config
  fieldConfigDrawerVisible.value = true
}

async function handleFieldConfigRefresh() {
  if (formData.value?.id) {
    fieldList.value = await cardTypeApi.getFieldConfigsWithSource(formData.value.id)
  }
}

// 处理新建属性配置
const createFieldConfigDrawerVisible = ref(false)

function handleCreateNewField() {
  createFieldConfigDrawerVisible.value = true
}

async function handleFieldConfigCreated() {
  // 刷新属性配置列表
  await handleFieldConfigRefresh()
}
</script>

<template>
  <a-drawer
    v-model:visible="drawerVisible"
    class="card-type-form-drawer"
    :width="effectiveMode === 'create' ? 800 : 1100"
    :body-class="effectiveMode === 'edit' ? 'card-type-form-drawer-body' : undefined"
    :footer="showFooter"
    :mask-closable="true"
    :esc-to-close="true"
    unmount-on-close
    @before-open="initDrawer"
  >
    <template #title>
      <template v-if="effectiveMode === 'create'">{{ drawerTitle }}</template>
      <div v-else class="feishu-drawer-header-row">
        <div class="feishu-drawer-brand">
          <span
            class="feishu-entity-icon-wrap"
            :class="headerIsEntityType ? 'is-entity' : 'is-trait'"
            aria-hidden="true"
          >
            <IconFile v-if="headerIsEntityType" />
            <IconTag v-else />
          </span>
          <span class="feishu-entity-name">{{
            entityDisplayName || t('admin.cardType.editTitle')
          }}</span>
        </div>
        <nav class="feishu-inline-tabs" role="tablist" aria-label="Card type sections">
          <button
            v-for="item in cardTypeEditTabItems"
            :key="item.key"
            type="button"
            role="tab"
            class="feishu-inline-tab"
            :class="{ 'feishu-inline-tab--active': activeTab === item.key }"
            :aria-selected="activeTab === item.key"
            @click="activeTab = item.key"
          >
            {{ item.label }}
          </button>
        </nav>
      </div>
    </template>
    <a-spin :loading="loading && effectiveMode === 'edit'" class="drawer-spin">
      <div v-if="formData" class="drawer-content">
        <!-- 新建模式：基础表单 -->
        <template v-if="effectiveMode === 'create'">
          <BasicInfoForm
            v-model:form-ref="formRef"
            v-model:selected-sub-type="selectedSubType"
            :form-data="formData"
            :mode="effectiveMode"
          />
        </template>

        <!-- 编辑模式：标签在标题行，内容区仅面板 -->
        <template v-else>
          <div class="edit-mode-shell">
            <div class="edit-mode-panel">
              <BasicInfoForm
                v-show="activeTab === 'basic'"
                v-model:form-ref="formRef"
                :form-data="formData"
                :mode="effectiveMode"
              />

              <!-- 单根包裹：FieldConfigTab 为多根片段，v-show 直接打在子组件上无法隐藏全部根节点 -->
              <div
                v-show="activeTab === 'fields'"
                class="edit-mode-tab-panel"
              >
                <FieldConfigTab
                  :field-list="fieldList"
                  :card-type-id="formData.id || ''"
                  @open-detail="handleOpenFieldConfigDetail"
                  @create-new="handleCreateNewField"
                  @refresh="handleFieldConfigRefresh"
                />
              </div>

              <ValueStreamTab
                v-if="activeTab === 'valueStream' && formData?.id"
                ref="valueStreamTabRef"
                :card-type-id="formData.id"
              />

              <PageLayoutTab
                v-if="isEntityType && activeTab === 'pageLayout' && formData?.id"
                :card-type-id="formData.id"
                :card-type-name="formData.name"
              />

              <PermissionTab
                v-if="isEntityType && activeTab === 'permission' && formData?.id"
                :card-type-id="formData.id"
                :org-id="orgStore.currentOrgId!"
              />

              <CardActionsTab
                v-if="isEntityType && activeTab === 'cardButtons' && formData?.id"
                :card-type-id="formData.id"
                :card-type-name="formData.name"
              />

              <FlowManagementTab
                v-if="isEntityType && activeTab === 'flowManagement' && formData?.id"
                :card-type-id="formData.id"
                :card-type-name="formData.name"
              />

              <BizRulesTab
                v-if="isEntityType && activeTab === 'businessRules' && formData?.id"
                :card-type-id="formData.id"
                :card-type-name="formData.name"
              />
            </div>
          </div>
        </template>
      </div>
    </a-spin>

    <template #footer>
      <SaveButton
        :loading="saving"
        :text="t('admin.cardType.saveConfig')"
        @click="handleSubmit"
      />
    </template>
  </a-drawer>

  <!-- 属性配置详情抽屉 -->
  <FieldConfigDrawer
    v-model:visible="fieldConfigDrawerVisible"
    :field-config="currentFieldConfig"
    :card-type-id="formData?.id || ''"
    :field-list="fieldList"
    @save="handleFieldConfigRefresh"
  />

  <!-- 新建属性配置抽屉 -->
  <CreateFieldConfigDrawer
    v-model:visible="createFieldConfigDrawerVisible"
    :card-type-id="formData?.id || ''"
    @created="handleFieldConfigCreated"
  />
</template>

<style scoped>
.drawer-spin {
  width: 100%;
  height: 100%;
}

.drawer-content {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.edit-mode-shell {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.edit-mode-panel {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-top: 12px;
}

/* 标题行：左侧图标+名称，右侧可换行标签（与关闭按钮同一 header 行） */
.feishu-drawer-header-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

.feishu-drawer-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  min-width: 0;
  max-width: 42%;
}

.feishu-inline-tabs {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 4px 6px;
}

.feishu-inline-tab {
  margin: 0;
  padding: 4px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--color-text-2);
  font: inherit;
  font-size: 13px;
  line-height: 22px;
  cursor: pointer;
  white-space: nowrap;
}

.feishu-inline-tab:hover {
  background-color: var(--color-fill-2);
  color: var(--color-text-1);
}

.feishu-inline-tab--active {
  color: var(--color-text-1);
  font-weight: 600;
}

.feishu-inline-tab:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
}

.feishu-entity-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  flex-shrink: 0;
  font-size: 16px;
  color: #fff;
}

.feishu-entity-icon-wrap.is-entity {
  background: linear-gradient(145deg, var(--color-primary) 0%, var(--color-primary-active) 100%);
}

.feishu-entity-icon-wrap.is-trait {
  background: linear-gradient(145deg, var(--color-primary-hover) 0%, var(--color-primary) 100%);
}

.feishu-entity-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-1);
  line-height: 24px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

<style>
/* class 在 a-drawer 上，随 attrs 落在 container；header/body 为后代 */
.card-type-form-drawer .arco-drawer-header {
  height: auto;
  min-height: 48px;
  align-items: center;
  padding-top: 10px;
  padding-bottom: 10px;
}

.card-type-form-drawer .arco-drawer-title {
  flex: 1 1 0;
  min-width: 0;
  margin-right: 8px;
}

/* body-class 渲染在 Drawer 内部；与主内容区白底一致，避免 fill-1 灰底 */
.card-type-form-drawer-body.arco-drawer-body {
  background-color: var(--color-main-panel);
  padding: 0 16px 16px;
}
</style>
