<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconPlus, IconDelete } from '@arco-design/web-vue/es/icon'
import { structureApi, cardTypeApi, fieldOptionsApi } from '@/api'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import type {
  StructureDefinition,
  StructureLevel,
  StructureDefinitionRequest,
} from '@/types/structure'
import type { MatchingLinkFieldDTO } from '@/types/card-type'
import type { FieldOption } from '@/types/field-option'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  structure: StructureDefinition | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

// 表单数据
const formData = ref<StructureDefinitionRequest>({
  name: '',
  description: '',
  levels: [],
})

// 编辑状态
const submitting = ref(false)
const editingVersion = ref(1)

// 选中的层级索引
const selectedLevelIndex = ref(0)

// 选项数据
const parentLinkOptions = ref<MatchingLinkFieldDTO[]>([])
const sortFieldOptions = ref<FieldOption[]>([])
const ownerLinkOptions = ref<FieldOption[]>([])
const cardTypeOptions = ref<{ id: string; name: string; icon?: string; schemaSubType: string }[]>(
  []
)

// 当前选中的层级
const selectedLevel = computed(() => {
  if (formData.value.levels.length === 0) return null
  return formData.value.levels[selectedLevelIndex.value] || null
})

// 排序方向选项
const sortDirectionOptions = computed(() => [
  { value: 'ASC', label: t('common.sort.asc') },
  { value: 'DESC', label: t('common.sort.desc') },
])

// 父级关联选项（从关联属性中获取）
const parentLinkSelectOptions = computed(() => {
  return parentLinkOptions.value.map((field) => ({
    value: `${field.linkTypeId}:${field.linkPosition}`,
    label: field.name,
  }))
})

// 负责人关联选项（从关联属性中获取）
const ownerLinkSelectOptions = computed(() => {
  return ownerLinkOptions.value.map((field) => ({
    // FieldOption 的 id 对于 LINK 类型已经是 "{linkTypeId}:{SOURCE|TARGET}" 格式
    value: field.id,
    label: field.name,
  }))
})

// 排序字段选项
const fieldSelectOptions = computed(() => {
  return sortFieldOptions.value.map((f) => ({
    value: f.id,
    label: f.name,
  }))
})

// 抽屉标题
const drawerTitle = computed(() => {
  return props.mode === 'create' ? t('admin.structure.createTitle') : t('admin.structure.editTitle')
})

// 加载选项数据
async function loadOptions() {
  try {
    const cardTypes = await cardTypeApi.listOptions()
    cardTypeOptions.value = cardTypes
  } catch (error) {
    console.error('Failed to load options:', error)
  }
}

// 加载当前层级的所有字段选项（一次性加载，避免重复请求）
async function loadLevelFieldOptions() {
  const currentIndex = selectedLevelIndex.value
  const currentLevel = formData.value.levels[currentIndex]

  if (!currentLevel?.cardTypeIds?.length) {
    parentLinkOptions.value = []
    ownerLinkOptions.value = []
    sortFieldOptions.value = []
    return
  }

  try {
    // 获取当前层级的所有共同字段
    const response = await fieldOptionsApi.getCommonFields(currentLevel.cardTypeIds)
    const allFields = response.fields

    // 过滤出关联字段（用于负责人）
    ownerLinkOptions.value = allFields.filter((f) => f.fieldType === 'LINK')

    // 所有字段都可用于排序
    sortFieldOptions.value = allFields

    // 加载父级关联选项
    await loadParentLinkOptions()
  } catch (error) {
    console.error('Failed to load field options:', error)
    ownerLinkOptions.value = []
    sortFieldOptions.value = []
  }
}

// 加载父级关联选项（当前层级中能够连接到父层级的关联属性）
async function loadParentLinkOptions() {
  const currentIndex = selectedLevelIndex.value
  if (currentIndex === 0) {
    // 根层级没有父级关联
    parentLinkOptions.value = []
    return
  }

  const currentLevel = formData.value.levels[currentIndex]
  const parentLevel = formData.value.levels[currentIndex - 1]

  if (
    !currentLevel?.cardTypeIds?.length ||
    !parentLevel?.cardTypeIds?.length
  ) {
    parentLinkOptions.value = []
    return
  }

  try {
    // 获取当前层级中能够连接到父层级的关联属性
    // 源侧 = 当前层级，目标侧 = 父层级
    const response = await fieldOptionsApi.getMatchingLinkFields(
      currentLevel.cardTypeIds,
      parentLevel.cardTypeIds,
    )

    // 架构线场景虽然只使用了单选关联，但关联属性本身可能是多选的（比如 N:N），这里不应过滤
    parentLinkOptions.value = response.fields
  } catch (error) {
    console.error('Failed to load parent link options:', error)
    parentLinkOptions.value = []
  }
}

// 监听当前层级的卡片类型变化
watch(
  () => selectedLevel.value?.cardTypeIds,
  () => {
    // 加载所有字段选项（包括父级关联、负责人、排序字段）
    loadLevelFieldOptions()
  },
  { immediate: true, deep: true }
)

// 初始化表单
function initForm() {
  if (props.mode === 'create') {
    formData.value = {
      name: '',
      description: '',
      levels: [createNewLevel(0)],
    }
    selectedLevelIndex.value = 0
  } else if (props.structure) {
    formData.value = {
      name: props.structure.name,
      description: props.structure.description || '',
      levels: props.structure.levels?.map((l) => ({
        ...l,
        cardTypeIds: [...(l.cardTypeIds || [])], // 深拷贝数组
        parentLinkFieldId: l.parentLinkFieldId ?? null,
        ownerLinkFieldId: l.ownerLinkFieldId ?? undefined,
      })) || [createNewLevel(0)],
    }
    editingVersion.value = props.structure.contentVersion
    selectedLevelIndex.value = 0
  }
}

// 创建新层级
function createNewLevel(index: number): StructureLevel {
  return {
    index,
    name: '',
    cardTypeIds: [],
    parentLinkFieldId: index === 0 ? null : '',
    ownerLinkFieldId: undefined,
    sortFieldId: undefined,
    sortDirection: undefined,
  }
}

// 检查层级是否填写完整
function isLevelComplete(level: StructureLevel): boolean {
  if (!level.name.trim()) return false
  if (!level.cardTypeIds || level.cardTypeIds.length === 0) return false
  // 非根层级需要检查父级关联
  if (level.index > 0 && !level.parentLinkFieldId) return false
  return true
}

// 添加层级
function addLevel() {
  // 检查最后一个层级是否填写完整
  const lastLevel = formData.value.levels[formData.value.levels.length - 1]
  if (lastLevel && !isLevelComplete(lastLevel)) {
    Message.warning(t('admin.structure.completeLevelFirst'))
    return
  }

  const newIndex = formData.value.levels.length
  formData.value.levels.push(createNewLevel(newIndex))
  selectedLevelIndex.value = newIndex
}

// 删除层级
function removeLevel(index: number) {
  if (formData.value.levels.length <= 1) {
    Message.warning(t('admin.structure.minLevelRequired'))
    return
  }

  formData.value.levels.splice(index, 1)
  // 重新计算索引
  formData.value.levels.forEach((level, i) => {
    level.index = i
    if (i === 0) {
      level.parentLinkFieldId = null
    } else if (!level.parentLinkFieldId) {
      level.parentLinkFieldId = ''
    }
  })

  // 调整选中索引
  if (selectedLevelIndex.value >= formData.value.levels.length) {
    selectedLevelIndex.value = formData.value.levels.length - 1
  }
}

// 选择层级
function selectLevel(index: number) {
  selectedLevelIndex.value = index
}

// 更新层级数据
function updateLevel(field: keyof StructureLevel, value: unknown) {
  if (!selectedLevel.value) return

  // 特殊处理卡片类型选择
  if (field === 'cardTypeIds' && Array.isArray(value)) {
    const cardTypeIds = value as string[]

    // 检查是否有变化
    const oldIds = selectedLevel.value.cardTypeIds || []
    const hasChanged =
      JSON.stringify(cardTypeIds.slice().sort()) !==
      JSON.stringify(oldIds.slice().sort())

    if (hasChanged) {
      // 清除负责人
      selectedLevel.value.ownerLinkFieldId = undefined
      // 清除排序
      selectedLevel.value.sortFieldId = undefined
      selectedLevel.value.sortDirection = undefined
      // 清除父级关联（非根层级）
      if (selectedLevelIndex.value > 0) {
        selectedLevel.value.parentLinkFieldId = ''
      }
    }

    ;(selectedLevel.value as Record<string, unknown>)[field] = cardTypeIds
    return
  }

  // 特殊处理排序字段：清空时同时清空排序方向
  if (field === 'sortFieldId' && !value) {
    selectedLevel.value.sortFieldId = undefined
    selectedLevel.value.sortDirection = undefined
    return
  }

  // 如果选择排序字段，默认设置为升序
  if (field === 'sortFieldId' && value) {
    selectedLevel.value.sortDirection = (selectedLevel.value.sortDirection || 'ASC') as any
  }

  ;(selectedLevel.value as Record<string, unknown>)[field] = value
}

// 更新父级关联（直接使用 linkFieldId 字符串）
function updateParentLink(combinedValue: string | null) {
  if (!selectedLevel.value) return
  selectedLevel.value.parentLinkFieldId = combinedValue || ''
}

// 获取父级关联的值
function getParentLinkValue(): string | undefined {
  if (!selectedLevel.value?.parentLinkFieldId) return undefined
  return selectedLevel.value.parentLinkFieldId
}

// 更新负责人关联（直接使用 linkFieldId 字符串）
function updateOwnerLink(combinedValue: string | null) {
  if (!selectedLevel.value) return
  selectedLevel.value.ownerLinkFieldId = combinedValue || undefined
}

// 清除负责人关联
function clearOwnerLink() {
  if (selectedLevel.value) {
    selectedLevel.value.ownerLinkFieldId = undefined
  }
}

// 获取负责人关联的值
function getOwnerLinkValue(): string | undefined {
  if (!selectedLevel.value?.ownerLinkFieldId) return undefined
  return selectedLevel.value.ownerLinkFieldId
}

// 验证表单
function validateForm(): boolean {
  if (!formData.value.name.trim()) {
    Message.error(t('admin.structure.structureNameRequired'))
    return false
  }

  for (let i = 0; i < formData.value.levels.length; i++) {
    const level = formData.value.levels[i]
    if (!level) continue
    if (!level.name.trim()) {
      Message.error(t('admin.structure.levelNameRequired', { index: i + 1 }))
      return false
    }
    if (!level.cardTypeIds || level.cardTypeIds.length === 0) {
      Message.error(t('admin.structure.cardTypeRequired', { index: i + 1 }))
      return false
    }
    if (i > 0 && !level.parentLinkFieldId) {
      Message.error(t('admin.structure.parentLinkRequired', { index: i + 1 }))
      return false
    }
  }

  return true
}

// 提交表单
async function handleSubmit() {
  if (!validateForm()) return

  submitting.value = true
  try {
    // 清理空的 ownerLinkFieldId、sortFieldId、sortDirection
    const levels = formData.value.levels.map((level) => ({
      ...level,
      ownerLinkFieldId: level.ownerLinkFieldId || undefined,
      sortFieldId: level.sortFieldId || undefined,
      sortDirection: level.sortFieldId ? level.sortDirection : undefined,
    }))

    const request = { ...formData.value, levels }

    if (props.mode === 'create') {
      await structureApi.create(request)
      Message.success(t('common.message.createSuccess'))
    } else if (props.structure?.id) {
      await structureApi.update(props.structure.id, request, editingVersion.value)
      Message.success(t('common.message.saveSuccess'))
    }
    emit('success')
  } catch (error) {
    console.error('Failed to save:', error)
    Message.error(t('common.message.saveFailed'))
  } finally {
    submitting.value = false
  }
}

// 关闭抽屉
function handleClose() {
  emit('update:visible', false)
}

// 监听 visible 变化
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      initForm()
      loadOptions()
    }
  },
)

onMounted(() => {
  loadOptions()
})
</script>

<template>
  <a-drawer
    :visible="visible"
    :title="drawerTitle"
    :width="720"
    :mask-closable="true"
    :esc-to-close="true"
    unmount-on-close
    @cancel="handleClose"
  >
    <div class="drawer-content">
      <!-- 基本信息 -->
      <div class="form-section">
        <a-form layout="vertical" :model="formData">
          <a-form-item :label="t('admin.structure.structureName')" field="name" required>
            <a-input
              v-model="formData.name"
              :placeholder="t('admin.structure.structureNamePlaceholder')"
              :max-length="50"
            />
          </a-form-item>
        </a-form>
      </div>

      <!-- 层级设置 -->
      <div class="form-section levels-section">
        <div class="section-header">
          <span class="section-title">{{ t('admin.structure.levelSettings') }}</span>
        </div>

        <div class="levels-editor">
          <!-- 左侧层级列表 -->
          <div class="levels-list">
            <div
              v-for="(level, index) in formData.levels"
              :key="index"
              class="level-item"
              :class="{ active: selectedLevelIndex === index }"
              @click="selectLevel(index)"
            >
              <span class="level-name" :title="level.name">
                {{ level.name || t('admin.structure.levelIndex', { index: index + 1 }) }}
              </span>
              <a-button
                v-if="formData.levels.length > 1"
                type="text"
                size="mini"
                status="danger"
                class="delete-btn"
                @click.stop="removeLevel(index)"
              >
                <template #icon><IconDelete /></template>
              </a-button>
            </div>
            <a-button class="add-level-btn" size="small" type="text" @click="addLevel">
              <template #icon><IconPlus /></template>
              {{ t('admin.structure.addLevel') }}
            </a-button>
          </div>

          <!-- 右侧层级配置 -->
          <a-form
            v-if="selectedLevel"
            class="level-config"
            layout="vertical"
          >
            <a-form-item :label="t('admin.structure.levelName')" required>
              <a-input
                :model-value="selectedLevel.name"
                :placeholder="t('admin.structure.levelNamePlaceholder')"
                :max-length="30"
                @update:model-value="updateLevel('name', $event)"
              />
            </a-form-item>

            <a-form-item :label="t('admin.structure.cardType')" required>
              <CardTypeSelect
                :key="`card-type-${selectedLevelIndex}`"
                :model-value="selectedLevel.cardTypeIds"
                :multiple="true"
                :limit-concrete-single="true"
                :options="cardTypeOptions"
                :placeholder="t('admin.structure.cardTypePlaceholder')"
                @update:model-value="updateLevel('cardTypeIds', $event)"
              />
              <template #extra>
                <span class="form-extra">{{ t('admin.structure.abstractMultiSelect') }}</span>
              </template>
            </a-form-item>

            <a-form-item :label="t('admin.structure.owner')">
              <a-select
                :model-value="getOwnerLinkValue()"
                :placeholder="t('admin.structure.ownerPlaceholder')"
                :options="ownerLinkSelectOptions"
                allow-clear
                allow-search
                @update:model-value="updateOwnerLink($event)"
                @clear="clearOwnerLink"
              />
              <template #extra>
                <span class="form-extra">{{ t('admin.structure.ownerHelp') }}</span>
              </template>
            </a-form-item>

            <!-- 非根层级显示与上级的关联关系 -->
            <template v-if="selectedLevelIndex > 0">
              <a-form-item :label="t('admin.structure.parentLink')" required>
                <a-select
                  class="parent-link-select"
                  :model-value="getParentLinkValue()"
                  :placeholder="t('admin.structure.parentLinkPlaceholder')"
                  :options="parentLinkSelectOptions"
                  allow-clear
                  allow-search
                  @update:model-value="updateParentLink($event)"
                />
                <template #extra>
                  <span class="form-extra">{{ t('admin.structure.parentLinkHelp') }}</span>
                </template>
              </a-form-item>
            </template>

            <a-form-item :label="t('admin.structure.sortField')">
              <div class="sort-row">
                <a-select
                  class="sort-field-select"
                  :model-value="selectedLevel.sortFieldId"
                  :placeholder="t('admin.structure.sortFieldPlaceholder')"
                  :options="fieldSelectOptions"
                  allow-clear
                  allow-search
                  @update:model-value="updateLevel('sortFieldId', $event)"
                />
                <a-select
                  class="sort-direction-select"
                  :model-value="selectedLevel.sortDirection"
                  :placeholder="t('admin.structure.sortDirection')"
                  :options="sortDirectionOptions"
                  :disabled="!selectedLevel.sortFieldId"
                  @update:model-value="updateLevel('sortDirection', $event)"
                />
              </div>
            </a-form-item>
          </a-form>
        </div>
      </div>

      <!-- 描述 -->
      <div class="form-section">
        <a-form layout="vertical" :model="formData">
          <a-form-item :label="t('admin.structure.description')" field="description">
            <a-textarea
              v-model="formData.description"
              :placeholder="t('admin.structure.descriptionPlaceholder')"
              :max-length="200"
              :auto-size="{ minRows: 2, maxRows: 4 }"
            />
          </a-form-item>
        </a-form>
      </div>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <a-space>
          <CancelButton @click="handleClose" />
          <SaveButton
            :loading="submitting"
            :text="mode === 'create' ? t('common.action.create') : t('common.action.save')"
            @click="handleSubmit"
          />
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<style scoped>
.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 12px; /* Reduce gap */
}

/* Remove default margin from single-item sections to control spacing via gap */
.form-section :deep(.arco-form-item) {
  margin-bottom: 0;
}

.form-section {
  padding: 0;
  background: transparent;
}

.levels-section {
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  overflow: hidden;
  /* margin-top removed */
}

.levels-section .section-header {
  background: var(--color-fill-2);
  padding: 8px 16px;
  margin-bottom: 0;
  border-bottom: 1px solid var(--color-border-2);
}

.levels-section .levels-editor {
  padding: 0;
  background: #fff;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
}

.levels-editor {
  display: flex;
  min-height: 300px;
}

.levels-list {
  width: 120px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 8px;
  border-right: 1px solid var(--color-border-2);
}

.level-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.level-item:hover {
  background: var(--color-fill-2);
}

.level-item.active {
  background: rgb(var(--primary-1));
}

.level-name {
  flex: 1;
  font-size: 13px;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 4px;
}

.level-item.active .level-name {
  color: rgb(var(--primary-6));
  font-weight: 500;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.level-item:hover .delete-btn {
  opacity: 1;
}

.add-level-btn {
  margin-top: 4px;
  width: 100%;
  justify-content: flex-start;
  padding: 8px;
  color: rgb(var(--primary-6));
}

.add-level-btn:hover {
  background: var(--color-fill-2);
}

.level-config {
  flex: 1;
  padding: 16px; /* Decrease padding */
}

/* 输入框弧度 */
.level-config :deep(.arco-input-wrapper),
.level-config :deep(.arco-select-view-single),
.level-config :deep(.arco-select-view-multiple) {
  border-radius: 6px;
}

.form-extra {
  font-size: 11px;
  color: var(--color-text-3);
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
}

.sort-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.level-config :deep(.arco-form-item) {
  margin-bottom: 16px; /* Restore margin for internal form items */
}
.sort-field-select {
  width: 60%;
  flex-shrink: 0;
}

.sort-direction-select {
  width: calc(40% - 12px);
  flex-shrink: 0;
}

.parent-link-select {
  width: 100%;
}
/* 全局输入框圆角 */
.drawer-content :deep(.arco-input-wrapper),
.drawer-content :deep(.arco-textarea-wrapper),
.drawer-content :deep(.arco-select-view-single),
.drawer-content :deep(.arco-select-view-multiple) {
  border-radius: 6px;
}
</style>
