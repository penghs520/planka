<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { linkTypeApi } from '@/api/link-type'
import { cardTypeApi } from '@/api/card-type'
import { LinkPosition, type LinkTypeOptionVO } from '@/types/link-type'
import type { FieldOption } from '@/types/field-option'
import type { CreateLinkedCardExecution, FieldAssignment } from '@/types/card-action'
import type { FieldProvider } from '@/components/common/text-expression-template/types'
import FieldAssignmentList from './FieldAssignmentList.vue'
import TextExpressionTemplateEditor from '@/components/common/text-expression-template/TextExpressionTemplateEditor.vue'
import { fieldOptionsApi } from '@/api/field-options'
import { useOrgStore } from '@/stores/org'
import { getMemberFieldsCached } from '@/views/schema-definition/card-type/components/permission/memberFieldsCache'

const { t } = useI18n()
const orgStore = useOrgStore()

const props = defineProps<{
  /** 当前卡片类型 ID */
  cardTypeId: string
}>()

const modelValue = defineModel<CreateLinkedCardExecution>({ required: true })

// 标题模板计算属性（处理 undefined -> 空字符串）
const titleTemplateValue = computed({
  get: () => modelValue.value.titleTemplate ?? '',
  set: (value: string) => {
    modelValue.value = {
      ...modelValue.value,
      titleTemplate: value,
    }
  },
})

// 当前卡片类型的字段列表（用于标题模板引用）
const sourceFieldOptions = ref<FieldOption[]>([])
const loadingSourceFields = ref(false)

// 关联类型列表
const linkTypeOptions = ref<LinkTypeOptionVO[]>([])
const loadingLinkTypes = ref(false)

// 卡片类型列表
const cardTypeOptions = ref<{ id: string; name: string }[]>([])
const loadingCardTypes = ref(false)

// 目标卡片类型的字段列表
const targetFieldOptions = ref<FieldOption[]>([])
const loadingTargetFields = ref(false)

// 过滤掉系统字段，只保留可赋值的字段
const assignableTargetFieldOptions = computed(() =>
  targetFieldOptions.value.filter(f => !f.systemField)
)

// 表达式模板编辑器的字段提供者（带请求缓存）
let cardFieldsCache: Promise<FieldOption[]> | null = null
const linkFieldsCache = new Map<string, Promise<FieldOption[]>>()

const expressionFieldProvider: FieldProvider = {
  getCardFields: () => {
    if (!cardFieldsCache && props.cardTypeId) {
      cardFieldsCache = fieldOptionsApi.getFields(props.cardTypeId)
    }
    return cardFieldsCache ?? Promise.resolve([])
  },
  getMemberFields: () => getMemberFieldsCached(orgStore.currentOrg?.memberCardTypeId),
  getFieldsByLinkFieldId: (id: string) => {
    let cached = linkFieldsCache.get(id)
    if (!cached) {
      cached = fieldOptionsApi.getFieldsByLinkFieldId(id)
      linkFieldsCache.set(id, cached)
    }
    return cached
  },
}

// 加载当前卡片类型的字段
async function loadSourceFields() {
  if (!props.cardTypeId) return

  loadingSourceFields.value = true
  try {
    sourceFieldOptions.value = await cardTypeApi.getFieldOptions(props.cardTypeId)
  } catch (e) {
    console.error('Failed to load source fields:', e)
  } finally {
    loadingSourceFields.value = false
  }
}

// 加载关联类型
async function loadLinkTypes() {
  loadingLinkTypes.value = true
  try {
    linkTypeOptions.value = await linkTypeApi.getAvailableForCardType(props.cardTypeId, LinkPosition.SOURCE)
  } catch (e) {
    console.error('Failed to load link types:', e)
  } finally {
    loadingLinkTypes.value = false
  }
}

// 加载卡片类型
async function loadCardTypes() {
  loadingCardTypes.value = true
  try {
    const types = await cardTypeApi.listOptions()
    cardTypeOptions.value = types.map(t => ({ id: t.id, name: t.name }))
  } catch (e) {
    console.error('Failed to load card types:', e)
  } finally {
    loadingCardTypes.value = false
  }
}

// 加载目标卡片类型的字段
async function loadTargetFields(cardTypeId: string) {
  if (!cardTypeId) {
    targetFieldOptions.value = []
    return
  }

  loadingTargetFields.value = true
  try {
    targetFieldOptions.value = await cardTypeApi.getFieldOptions(cardTypeId)
  } catch (e) {
    console.error('Failed to load target fields:', e)
  } finally {
    loadingTargetFields.value = false
  }
}

// 关联类型变化时
function handleLinkTypeChange(linkTypeId: string) {
  modelValue.value = {
    ...modelValue.value,
    linkTypeId,
  }
}

// 目标卡片类型变化时加载字段
function handleTargetCardTypeChange(cardTypeId: string) {
  modelValue.value = {
    ...modelValue.value,
    targetCardTypeId: cardTypeId,
    fieldAssignments: [],
  }
  loadTargetFields(cardTypeId)
}

// 更新字段赋值列表
function handleFieldAssignmentsChange(assignments: FieldAssignment[]) {
  modelValue.value = {
    ...modelValue.value,
    fieldAssignments: assignments,
  }
}

// 初始化加载
onMounted(() => {
  loadSourceFields()
  loadLinkTypes()
  loadCardTypes()
})

// 监听 cardTypeId 变化
watch(
  () => props.cardTypeId,
  () => {
    loadSourceFields()
    loadLinkTypes()
  }
)

// 监听目标卡片类型变化
watch(
  () => modelValue.value?.targetCardTypeId,
  (cardTypeId) => {
    if (cardTypeId) {
      loadTargetFields(cardTypeId)
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="create-linked-card-execution-form">
    <!-- 关联类型 -->
    <a-form-item :label="t('admin.cardAction.createLinkedCard.linkType')">
      <a-select
        :model-value="modelValue.linkTypeId"
        :placeholder="t('admin.cardAction.createLinkedCard.linkTypePlaceholder')"
        :loading="loadingLinkTypes"
        style="width: 280px"
        @change="handleLinkTypeChange"
      >
        <a-option
          v-for="opt in linkTypeOptions"
          :key="opt.id"
          :value="opt.id"
          :label="opt.sourceName || opt.name"
        />
      </a-select>
    </a-form-item>

    <!-- 目标卡片类型 -->
    <a-form-item :label="t('admin.cardAction.createLinkedCard.targetCardType')">
      <a-select
        :model-value="modelValue.targetCardTypeId"
        :placeholder="t('admin.cardAction.createLinkedCard.targetCardTypePlaceholder')"
        :loading="loadingCardTypes"
        style="width: 280px"
        @change="handleTargetCardTypeChange"
      >
        <a-option
          v-for="opt in cardTypeOptions"
          :key="opt.id"
          :value="opt.id"
          :label="opt.name"
        />
      </a-select>
    </a-form-item>

    <!-- 显示创建弹窗 -->
    <a-form-item>
      <a-checkbox v-model="modelValue.showCreateDialog">
        {{ t('admin.cardAction.createLinkedCard.showCreateDialog') }}
      </a-checkbox>
    </a-form-item>

    <!-- 标题模板 -->
    <a-form-item :label="t('admin.cardAction.titleTemplate.label')">
      <TextExpressionTemplateEditor
        v-model="titleTemplateValue"
        :field-provider="expressionFieldProvider"
        :placeholder="t('admin.cardAction.titleTemplate.placeholder')"
      />
    </a-form-item>

    <!-- 新卡片字段赋值 -->
    <div v-if="modelValue.targetCardTypeId" class="linked-card-fields">
      <FieldAssignmentList
        :model-value="modelValue.fieldAssignments || []"
        :field-options="assignableTargetFieldOptions"
        :reference-field-options="sourceFieldOptions"
        @update:model-value="handleFieldAssignmentsChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.create-linked-card-execution-form {
  display: flex;
  flex-direction: column;
  gap: 4px;

  :deep(.arco-form-item) {
    margin-bottom: 12px;
  }
}

.linked-card-fields {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}
</style>
