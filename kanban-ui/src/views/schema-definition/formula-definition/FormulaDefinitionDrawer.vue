<template>
  <a-drawer
    :visible="drawerVisible"
    :title="drawerTitle"
    :width="800"
    :footer="false"
    @cancel="handleCancel"
  >
    <a-form v-if="formData" :model="formData" layout="vertical">
      <!-- 公式类型选择 -->
      <a-form-item
        :label="t('admin.formulaDefinition.formulaType')"
        :rules="[{ required: true, message: t('admin.formulaDefinition.formulaTypeRequired') }]"
      >
        <a-radio-group v-model="selectedFormulaType" :disabled="mode === 'edit'">
          <a-radio
            v-for="type in formulaTypeOptions"
            :key="type.value"
            :value="type.value"
          >
            {{ type.label }}
          </a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- 基本信息 -->
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('admin.formulaDefinition.name')"
            :rules="[{ required: true, message: t('admin.formulaDefinition.nameRequired') }]"
          >
            <a-input v-model="formData.name" :placeholder="t('admin.formulaDefinition.namePlaceholder')" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="t('admin.formulaDefinition.code')">
            <a-input v-model="formData.code" :placeholder="t('admin.formulaDefinition.codePlaceholder')" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 公式配置（根据类型动态显示） -->
      <!-- @vue-ignore -->
      <component
        :is="getConfigComponent()"
        v-if="selectedFormulaType && formData"
        :model-value="formData"
        :card-type-ids="formData.cardTypeIds"
        @update:model-value="(val: any) => formData = val"
      />

      <!-- 描述 -->
      <a-form-item :label="t('admin.formulaDefinition.description')">
        <a-textarea
          v-model="formData.description"
          :placeholder="t('admin.formulaDefinition.descriptionPlaceholder')"
          :rows="3"
        />
      </a-form-item>

      <!-- 操作按钮 -->
      <div class="drawer-footer">
        <a-space>
          <SaveButton :loading="submitting" @click="handleSave" />
          <CancelButton @click="handleCancel" />
        </a-space>
      </div>
    </a-form>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { formulaDefinitionApi } from '@/api'
import { useOrgStore } from '@/stores/org'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import type { FormulaDefinition } from '@/types/formula'
import { SchemaSubType } from '@/types/schema'
import { EntityState } from '@/types/common'

// 配置子组件（暂时使用占位符，后续实现）
import TimePointFormulaConfig from './config/TimePointFormulaConfig.vue'
import TimeRangeFormulaConfig from './config/TimeRangeFormulaConfig.vue'
import DateCollectionFormulaConfig from './config/DateCollectionFormulaConfig.vue'
import CardCollectionFormulaConfig from './config/CardCollectionFormulaConfig.vue'
import NumberCalculationFormulaConfig from './config/NumberCalculationFormulaConfig.vue'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  formulaId?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const orgStore = useOrgStore()

// 表单状态
const selectedFormulaType = ref<SchemaSubType | undefined>(undefined)
const formData = ref<FormulaDefinition | null>(null)
const submitting = ref(false)
const loading = ref(false)

// 抽屉可见性
const drawerVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

// 抽屉标题
const drawerTitle = computed(() => {
  return props.mode === 'create'
    ? t('admin.formulaDefinition.createTitle')
    : t('admin.formulaDefinition.editTitle')
})

// 公式类型选项
const formulaTypeOptions = computed(() => [
  {
    value: SchemaSubType.TIME_POINT_FORMULA_DEFINITION,
    label: t('admin.formulaDefinition.type.timePoint'),
  },
  {
    value: SchemaSubType.TIME_RANGE_FORMULA_DEFINITION,
    label: t('admin.formulaDefinition.type.timeRange'),
  },
  {
    value: SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION,
    label: t('admin.formulaDefinition.type.dateCollection'),
  },
  {
    value: SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION,
    label: t('admin.formulaDefinition.type.cardCollection'),
  },
  {
    value: SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION,
    label: t('admin.formulaDefinition.type.numberCalculation'),
  },
])

// 获取配置组件
function getConfigComponent() {
  if (!selectedFormulaType.value) return null
  switch (selectedFormulaType.value) {
    case SchemaSubType.TIME_POINT_FORMULA_DEFINITION:
      return TimePointFormulaConfig
    case SchemaSubType.TIME_RANGE_FORMULA_DEFINITION:
      return TimeRangeFormulaConfig
    case SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION:
      return DateCollectionFormulaConfig
    case SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION:
      return CardCollectionFormulaConfig
    case SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION:
      return NumberCalculationFormulaConfig
    default:
      return null
  }
}

// 创建空公式定义
function createEmptyFormulaDefinition(type: SchemaSubType): FormulaDefinition | null {
  if (!orgStore.currentOrgId) return null

  const base = {
    id: '',
    orgId: orgStore.currentOrgId,
    name: '',
    enabled: true,
    cardTypeIds: [],
    state: EntityState.ACTIVE,
    contentVersion: 0,
  }

  switch (type) {
    case SchemaSubType.TIME_POINT_FORMULA_DEFINITION:
      return {
        ...base,
        schemaSubType: SchemaSubType.TIME_POINT_FORMULA_DEFINITION,
        sourceType: 'CARD_CREATED_TIME' as any,
      } as FormulaDefinition
    case SchemaSubType.TIME_RANGE_FORMULA_DEFINITION:
      return {
        ...base,
        schemaSubType: SchemaSubType.TIME_RANGE_FORMULA_DEFINITION,
        startSourceType: 'CARD_CREATED_TIME' as any,
        endSourceType: 'CARD_UPDATED_TIME' as any,
        precision: 'DAY' as any,
      } as FormulaDefinition
    case SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION:
      return {
        ...base,
        schemaSubType: SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION,
        linkFieldId: '',
        sourceFieldId: '',
        aggregationType: 'EARLIEST' as any,
      } as FormulaDefinition
    case SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION:
      return {
        ...base,
        schemaSubType: SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION,
        linkFieldId: '',
        aggregationType: 'COUNT' as any,
      } as FormulaDefinition
    case SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION:
      return {
        ...base,
        schemaSubType: SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION,
        expression: '',
      } as FormulaDefinition
    default:
      return null
  }
}

// 监听公式类型变化
watch(selectedFormulaType, (newType) => {
  if (props.mode === 'create' && newType) {
    formData.value = createEmptyFormulaDefinition(newType)
  }
})

// 监听抽屉可见性变化
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      initForm()
    }
  }
)

// 初始化表单
async function initForm() {
  if (props.mode === 'create') {
    const defaultType = SchemaSubType.TIME_POINT_FORMULA_DEFINITION
    selectedFormulaType.value = defaultType
    formData.value = createEmptyFormulaDefinition(defaultType)
  } else if (props.formulaId) {
    loading.value = true
    try {
      const data = await formulaDefinitionApi.getById(props.formulaId)
      formData.value = data
      selectedFormulaType.value = data.schemaSubType as SchemaSubType
    } catch (error) {
      console.error('Failed to fetch formula:', error)
      Message.error(t('admin.formulaDefinition.fetchFailed'))
    } finally {
      loading.value = false
    }
  }
}

// 保存处理
async function handleSave() {
  if (!formData.value) return

  if (!formData.value.name?.trim()) {
    Message.warning(t('admin.formulaDefinition.nameRequired'))
    return
  }

  submitting.value = true
  try {
    if (props.mode === 'create') {
      await formulaDefinitionApi.create(formData.value)
      Message.success(t('admin.formulaDefinition.createSuccess'))
    } else if (props.formulaId) {
      await formulaDefinitionApi.update(props.formulaId, formData.value, formData.value.contentVersion)
      Message.success(t('admin.formulaDefinition.updateSuccess'))
    }
    emit('success')
  } catch (error) {
    console.error('Failed to save formula:', error)
    Message.error(t('common.saveFailed'))
  } finally {
    submitting.value = false
  }
}

// 取消处理
function handleCancel() {
  drawerVisible.value = false
}
</script>

<style scoped lang="scss">
.drawer-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-2);
  text-align: right;
}

.form-item-help {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
