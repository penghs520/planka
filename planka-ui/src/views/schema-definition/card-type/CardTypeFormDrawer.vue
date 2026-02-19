<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { cardTypeApi } from '@/api'
import { useOrgStore } from '@/stores/org'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import BasicInfoForm from './components/BasicInfoForm.vue'
import FieldConfigTab from './components/FieldConfigTab.vue'
import DetailTemplateTab from './components/DetailTemplateTab.vue'
import CreatePageTemplateTab from './components/CreatePageTemplateTab.vue'
import ValueStreamTab from './components/ValueStreamTab.vue'
import PermissionTab from './components/PermissionTab.vue'
import CardActionsTab from './components/CardActionsTab.vue'
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

const drawerTitle = computed(() => {
  if (props.mode === 'create') {
    return t('admin.cardType.createTitle')
  }
  return formData.value ? t('admin.cardType.editTitleWithName', { name: formData.value.name }) : t('admin.cardType.editTitle')
})

// 监听 selectedSubType 变化（仅新建模式）
watch(selectedSubType, (newSubType) => {
  if (props.mode === 'create' && orgStore.currentOrgId) {
    formData.value = createEmptyCardType(newSubType, orgStore.currentOrgId)
  }
})

// 监听 tab 切换，懒加载属性配置数据
watch(activeTab, async (newTab) => {
  if (newTab === 'fields' && props.mode === 'edit' && formData.value?.id && !fieldList.value) {
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
    if (props.mode === 'create') {
      await cardTypeApi.create(formData.value)
      Message.success(t('admin.message.createSuccess'))
    } else {
      await cardTypeApi.update(
        formData.value.id!,
        formData.value,
        formData.value.contentVersion,
      )
      Message.success(t('admin.message.saveSuccess'))
    }
    drawerVisible.value = false
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
    :title="drawerTitle"
    :width="mode === 'create' ? 800 : 1100"
    :mask-closable="true"
    :esc-to-close="true"
    unmount-on-close
    @before-open="initDrawer"
  >
    <a-spin :loading="loading && mode === 'edit'" class="drawer-spin">
      <div v-if="formData" class="drawer-content">
        <!-- 新建模式：基础表单 -->
        <template v-if="mode === 'create'">
          <BasicInfoForm
            v-model:form-ref="formRef"
            v-model:selected-sub-type="selectedSubType"
            :form-data="formData"
            :mode="mode"
          />
        </template>

        <!-- 编辑模式：多 Tab 结构 -->
        <template v-else>
          <a-tabs v-model:active-key="activeTab" direction="vertical" class="edit-tabs">
            <a-tab-pane key="basic" :title="t('admin.cardType.tabs.basic')">
              <BasicInfoForm
                v-model:form-ref="formRef"
                :form-data="formData"
                :mode="mode"
              />
            </a-tab-pane>

            <a-tab-pane key="fields" :title="t('admin.cardType.tabs.fields')">
              <FieldConfigTab
                :field-list="fieldList"
                :card-type-id="formData.id || ''"
                @open-detail="handleOpenFieldConfigDetail"
                @create-new="handleCreateNewField"
                @refresh="handleFieldConfigRefresh"
              />
            </a-tab-pane>

            <a-tab-pane key="valueStream" :title="t('admin.cardType.tabs.valueStream')">
              <ValueStreamTab
                v-if="activeTab === 'valueStream' && formData?.id"
                ref="valueStreamTabRef"
                :card-type-id="formData.id"
              />
            </a-tab-pane>

            <a-tab-pane v-if="isEntityType" key="detailTemplate" :title="t('admin.cardType.tabs.detailTemplate')">
              <DetailTemplateTab
                v-if="activeTab === 'detailTemplate' && formData?.id"
                :card-type-id="formData.id"
                :card-type-name="formData.name"
              />
            </a-tab-pane>

            <a-tab-pane v-if="isEntityType" key="createPageTemplate" :title="t('admin.cardType.tabs.createPageTemplate')">
              <CreatePageTemplateTab
                v-if="activeTab === 'createPageTemplate' && formData?.id"
                :card-type-id="formData.id"
                :card-type-name="formData.name"
              />
            </a-tab-pane>

            <a-tab-pane v-if="isEntityType" key="cardFace" :title="t('admin.cardType.tabs.cardFace')">
              <a-empty :description="t('admin.cardType.cardFaceInDev')" />
            </a-tab-pane>

            <a-tab-pane v-if="isEntityType" key="permission" :title="t('admin.cardType.tabs.permission')">
              <PermissionTab
                v-if="activeTab === 'permission' && formData?.id"
                :card-type-id="formData.id"
                :org-id="orgStore.currentOrgId!"
              />
            </a-tab-pane>

            <a-tab-pane v-if="isEntityType" key="cardButtons" :title="t('admin.cardType.tabs.cardButtons')">
              <CardActionsTab
                v-if="activeTab === 'cardButtons' && formData?.id"
                :card-type-id="formData.id"
                :card-type-name="formData.name"
              />
            </a-tab-pane>

            <a-tab-pane v-if="isEntityType" key="businessRules" :title="t('admin.cardType.tabs.businessRules')">
              <BizRulesTab
                v-if="activeTab === 'businessRules' && formData?.id"
                :card-type-id="formData.id"
                :card-type-name="formData.name"
              />
            </a-tab-pane>
          </a-tabs>
        </template>
      </div>
    </a-spin>

    <template #footer>
      <a-space>
        <CancelButton @click="drawerVisible = false" />
        <SaveButton
          :loading="saving"
          :text="mode === 'create' ? t('admin.action.create') : t('admin.action.save')"
          @click="handleSubmit"
        />
      </a-space>
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

.edit-tabs {
  height: 100%;
  display: flex;
  flex-direction: row;
}

.edit-tabs :deep(.arco-tabs-nav) {
  flex-shrink: 0;
  width: auto;
  padding: 0;
}

.edit-tabs :deep(.arco-tabs-nav-vertical .arco-tabs-tab) {
  padding: 6px 8px;
  font-size: 13px;
  margin-bottom: 0;
}

/* 隐藏活动指示器竖线 */
.edit-tabs :deep(.arco-tabs-nav-ink) {
  display: none;
}

.edit-tabs :deep(.arco-tabs-content) {
  flex: 1;
  overflow: hidden;
  padding: 0 12px 0 12px;
  width: 100%;
}

.edit-tabs :deep(.arco-tabs-content-list) {
  height: 100%;
  width: 100%;
}

.edit-tabs :deep(.arco-tabs-pane) {
  height: 100%;
  width: 100%;
}
</style>
