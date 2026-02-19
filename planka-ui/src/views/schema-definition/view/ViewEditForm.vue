<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconQuestionCircle } from '@arco-design/web-vue/es/icon'
import { useOrgStore } from '@/stores/org'
import { createEmptyListView, type ListViewDefinition } from '@/types/view'
import type { FieldOption } from '@/types/field-option'
import { cardTypeApi, linkTypeApi, fieldOptionsApi } from '@/api'
import type { CardTypeDefinition } from '@/types/card-type'
import type { LinkTypeVO } from '@/types/link-type'
import ColumnConfigEditor from './ColumnConfigEditor.vue'
import ConditionEditor from '@/components/condition/ConditionEditor.vue'
import { getIncompleteConditionCount } from '@/utils/condition-factory'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'

const props = withDefaults(
  defineProps<{
    visible: boolean
    mode: 'create' | 'edit'
    view?: ListViewDefinition | null
  }>(),
  {
    visible: false,
    mode: 'create',
    view: null,
  },
)

const emit = defineEmits<{
  'update:visible': [value: boolean]
  save: [view: ListViewDefinition]
}>()

const orgStore = useOrgStore()
const activeTab = ref('basic')
const formData = ref<ListViewDefinition | null>(null)
const cardTypes = ref<CardTypeDefinition[]>([])
const loadingCardTypes = ref(false)
const availableFields = ref<FieldOption[]>([])
const loadingFields = ref(false)
const linkTypes = ref<LinkTypeVO[]>([])
const loadingLinkTypes = ref(false)
const conditionEditorRef = ref<InstanceType<typeof ConditionEditor> | null>(null)

const drawerTitle = computed(() => {
  if (props.mode === 'create') {
    return '新建视图'
  }
  return formData.value ? `编辑视图 - ${formData.value.name}` : '编辑视图'
})

const cardTypeOptions = computed(() => {
  return cardTypes.value.map((ct) => ({
    value: ct.id!,
    label: ct.name,
  }))
})

// 获取当前卡片类型名称（用于路径面包屑显示）
const rootCardTypeName = computed(() => {
  if (!formData.value?.cardTypeId) return ''
  const cardType = cardTypes.value.find((ct) => ct.id === formData.value?.cardTypeId)
  return cardType?.name || ''
})

// 监听 visible 和 view 变化，初始化表单数据
watch(
  [() => props.visible, () => props.view, () => props.mode],
  ([newVisible, newView, newMode]) => {
    if (newVisible) {
      activeTab.value = 'basic'
      if (newMode === 'create') {
        if (orgStore.currentOrgId) {
          formData.value = createEmptyListView(orgStore.currentOrgId)
        }
      } else if (newView) {
        formData.value = JSON.parse(JSON.stringify(newView)) // 深拷贝
      }
      // 加载卡片类型列表（只在打开时加载一次）
      if (!cardTypes.value.length) {
        fetchCardTypes()
      }
      // 加载关联类型列表（只在打开时加载一次）
      if (!linkTypes.value.length) {
        fetchLinkTypes()
      }
    }
  },
  { immediate: true },
)

// 加载卡片类型列表
async function fetchCardTypes() {
  loadingCardTypes.value = true
  try {
    cardTypes.value = await cardTypeApi.listAll()
  } catch (error) {
    console.error('Failed to fetch card types:', error)
    // 错误提示已在 request.ts 统一处理
  } finally {
    loadingCardTypes.value = false
  }
}

// 加载关联类型列表
async function fetchLinkTypes() {
  loadingLinkTypes.value = true
  try {
    linkTypes.value = await linkTypeApi.list()
    console.log('Loaded link types:', linkTypes.value)
  } catch (error) {
    console.error('Failed to fetch link types:', error)
    // 错误提示已在 request.ts 统一处理
  } finally {
    loadingLinkTypes.value = false
  }
}

// 根据关联字段ID获取级联字段（用于多级关联）
async function fetchFieldsByLinkFieldId(linkFieldId: string): Promise<FieldOption[]> {
  return fieldOptionsApi.getFieldsByLinkFieldId(linkFieldId)
}

// 加载字段列表
async function fetchFields(cardTypeId: string) {
  loadingFields.value = true
  try {
    availableFields.value = await cardTypeApi.getFieldOptions(cardTypeId)
    console.log('Loaded fields:', availableFields.value)
  } catch (error) {
    console.error('Failed to fetch fields:', error)
    // 错误提示已在 request.ts 统一处理
  } finally {
    loadingFields.value = false
  }
}

// 监听卡片类型变化，加载字段列表
watch(
  () => formData.value?.cardTypeId,
  (newCardTypeId) => {
    if (newCardTypeId) {
      fetchFields(newCardTypeId)
    } else {
      availableFields.value = []
    }
  },
  { immediate: true }
)

// 监听 visible 变化，控制 body 滚动
watch(
  () => props.visible,
  (newVisible) => {
    if (newVisible) {
      // 打开 Drawer 时锁定 body 滚动
      document.body.style.overflow = 'hidden'
    } else {
      // 关闭 Drawer 时恢复 body 滚动
      document.body.style.overflow = ''
    }
  },
  { immediate: true }
)

function handleClose() {
  emit('update:visible', false)
}

function handleSave() {
  if (!formData.value) return

  // 表单验证
  if (!formData.value.name) {
    Message.warning('请输入视图名称')
    return
  }
  if (!formData.value.cardTypeId) {
    Message.warning('请选择关联的卡片类型')
    return
  }

  // 验证过滤条件是否完整
  if (conditionEditorRef.value && !conditionEditorRef.value.validate()) {
    const count = getIncompleteConditionCount(formData.value.condition)
    Message.warning(`存在 ${count} 个未填写完整的过滤条件或空条件组`)
    activeTab.value = 'filter' // 切换到过滤条件 Tab
    return
  }

  emit('save', formData.value)
}
</script>

<template>
  <a-drawer
    :visible="visible"
    :title="drawerTitle"
    :width="1100"
    :mask-closable="true"
    :esc-to-close="true"
    :render-to-body="true"
    unmount-on-close
    @update:visible="(val) => emit('update:visible', val)"
    @cancel="handleClose"
  >
    <div v-if="formData" class="drawer-content">
      <!-- 创建模式：简化表单 -->
      <template v-if="mode === 'create'">
        <a-form :model="formData" layout="vertical">
          <div class="form-section">
            <h3>基础信息</h3>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="视图名称" required>
                  <a-input
                    v-model="formData.name"
                    placeholder="请输入视图名称"
                    :max-length="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="关联卡片类型" required>
                  <a-select
                    v-model="formData.cardTypeId"
                    placeholder="请选择卡片类型"
                    :loading="loadingCardTypes"
                    allow-search
                  >
                    <a-option
                      v-for="option in cardTypeOptions"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="描述">
              <a-textarea
                v-model="formData.description"
                placeholder="请输入描述"
                :max-length="200"
                :auto-size="{ minRows: 2, maxRows: 4 }"
              />
            </a-form-item>

            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item>
                  <template #label>
                    默认视图
                    <a-tooltip content="是否作为该卡片类型的默认视图">
                      <icon-question-circle
                        style="margin-left: 4px; color: var(--color-text-3); cursor: help"
                      />
                    </a-tooltip>
                  </template>
                  <a-switch v-model="formData.defaultView" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item>
                  <template #label>
                    共享视图
                    <a-tooltip content="其他用户是否可见此视图">
                      <icon-question-circle
                        style="margin-left: 4px; color: var(--color-text-3); cursor: help"
                      />
                    </a-tooltip>
                  </template>
                  <a-switch v-model="formData.shared" />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </a-form>
      </template>

      <!-- 编辑模式：多 Tab 结构 -->
      <template v-else>
        <a-tabs v-model:active-key="activeTab" class="edit-tabs">
          <a-tab-pane key="basic" title="基础信息">
            <a-form :model="formData" layout="vertical">
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="视图名称" required>
                    <a-input
                      v-model="formData.name"
                      placeholder="请输入视图名称"
                      :max-length="50"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="关联卡片类型" required>
                    <a-select
                      v-model="formData.cardTypeId"
                      placeholder="请选择卡片类型"
                      :loading="loadingCardTypes"
                      allow-search
                      disabled
                    >
                      <a-option
                        v-for="option in cardTypeOptions"
                        :key="option.value"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </a-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-form-item label="描述">
                <a-textarea
                  v-model="formData.description"
                  placeholder="请输入描述"
                  :max-length="200"
                  :auto-size="{ minRows: 2, maxRows: 4 }"
                />
              </a-form-item>

              <a-row :gutter="16">
                <a-col :span="8">
                  <a-form-item>
                    <template #label>
                      默认视图
                      <a-tooltip content="是否作为该卡片类型的默认视图">
                        <icon-question-circle
                          style="margin-left: 4px; color: var(--color-text-3); cursor: help"
                        />
                      </a-tooltip>
                    </template>
                    <a-switch v-model="formData.defaultView" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item>
                    <template #label>
                      共享视图
                      <a-tooltip content="其他用户是否可见此视图">
                        <icon-question-circle
                          style="margin-left: 4px; color: var(--color-text-3); cursor: help"
                        />
                      </a-tooltip>
                    </template>
                    <a-switch v-model="formData.shared" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="分组字段">
                    <a-input
                      v-model="formData.groupBy"
                      placeholder="字段 ID（暂不支持选择）"
                      disabled
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </a-form>
          </a-tab-pane>

          <a-tab-pane key="columns" title="列配置">
            <ColumnConfigEditor
              v-if="formData.cardTypeId && activeTab === 'columns'"
              :model-value="formData.columnConfigs || []"
              :card-type-id="formData.cardTypeId"
              @update:model-value="formData.columnConfigs = $event"
            />
            <a-empty v-else-if="!formData.cardTypeId" description="请先选择关联的卡片类型" />
          </a-tab-pane>

          <a-tab-pane key="sortPage" title="排序与分页">
            <a-empty description="排序与分页配置开发中..." />
          </a-tab-pane>

          <a-tab-pane key="filter" title="过滤条件">
            <div v-if="formData.cardTypeId && activeTab === 'filter'" class="filter-tab">
              <a-spin :loading="loadingFields" style="width: 100%">
                <ConditionEditor
                  ref="conditionEditorRef"
                  v-model="formData.condition"
                  :card-type-id="formData.cardTypeId"
                  :available-fields="availableFields"
                  :link-types="linkTypes"
                  :any-trait-card-type-name="rootCardTypeName"
                  :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
                />
              </a-spin>
            </div>
            <a-empty v-else-if="!formData.cardTypeId" description="请先选择关联的卡片类型" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </div>

    <template #footer>
      <a-space>
        <CancelButton @click="handleClose" />
        <SaveButton
          :text="mode === 'create' ? '创建' : '保存'"
          @click="handleSave"
        />
      </a-space>
    </template>
  </a-drawer>
</template>

<style scoped lang="scss">
.drawer-content {
  padding: 0 16px;
}

.form-section {
  margin-bottom: 24px;
  padding: 20px;
  background-color: var(--color-fill-1);
  border-radius: 8px;
}

.form-section h3 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 500;
}

.edit-tabs {
  height: 100%;
}

.edit-tabs :deep(.arco-tabs-content) {
  padding-top: 16px;
}

.filter-tab {
  padding: 0;
}
</style>
