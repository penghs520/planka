<template>
  <div class="global-config">
    <a-form :model="modelValue" layout="vertical" auto-label-width>
      <!-- 基础配置卡片 -->
      <div class="config-section">
        <div class="section-header">
          <div class="section-title">
            <span>基础配置</span>
          </div>
          <div class="section-description">配置工时单位和小数精度</div>
        </div>
        <div class="section-content">
          <a-row :gutter="20">
            <a-col :span="12">
              <a-form-item
                :label="t('outsourcingConfig.global.durationUnit')"
                field="durationUnit"
              >
                <a-select
                  v-model="modelValue.durationUnit"
                  :placeholder="t('outsourcingConfig.global.durationUnitPlaceholder')"
                >
                  <a-option
                    v-for="option in durationUnitOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </a-option>
                </a-select>
              </a-form-item>
            </a-col>

            <a-col :span="12">
              <a-form-item
                :label="t('outsourcingConfig.global.decimalScale')"
                field="decimalScale"
              >
                <a-input-number
                  v-model="modelValue.decimalScale"
                  :placeholder="t('outsourcingConfig.global.decimalScalePlaceholder')"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </div>
      </div>

      <!-- 功能开关卡片 -->
      <div class="config-section">
        <div class="section-header">
          <div class="section-title">
            <span>功能开关</span>
          </div>
          <div class="section-description">控制考勤相关功能的启用状态</div>
        </div>
        <div class="section-content">
          <a-row :gutter="20">
            <a-col :span="12">
              <a-form-item
                :label="t('outsourcingConfig.global.cardAttendanceRequired')"
                field="cardAttendanceRequired"
              >
                <div class="switch-wrapper">
                  <a-switch v-model="modelValue.cardAttendanceRequired" :disabled="!attendanceEnabled" />
                  <span class="form-item-tip">
                    {{ t('outsourcingConfig.global.cardAttendanceRequiredTip') }}
                  </span>
                </div>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
      </div>

      <!-- 成员配置卡片 -->
      <div class="config-section">
        <div class="section-header">
          <div class="section-title">
            <span>成员配置</span>
          </div>
          <div class="section-description">配置参与考勤的成员类型和筛选条件</div>
        </div>
        <div class="section-content">
          <a-row :gutter="20">
            <a-col :span="24">
              <a-form-item
                :label="t('outsourcingConfig.global.memberCardType')"
                field="memberCardTypeId"
              >
                <a-select
                  v-model="modelValue.memberCardTypeId"
                  :placeholder="t('outsourcingConfig.global.memberCardTypePlaceholder')"
                  :loading="loadingMemberCardTypes"
                  allow-clear
                  allow-search
                >
                  <a-option
                    v-for="option in memberCardTypeOptions"
                    :key="option.id"
                    :value="option.id"
                  >
                    {{ option.name }}
                  </a-option>
                </a-select>
                <template #extra>
                  <span class="form-item-tip">
                    {{ t('outsourcingConfig.global.memberCardTypeTip') }}
                  </span>
                </template>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row v-if="modelValue.memberCardTypeId" :gutter="20">
            <a-col :span="24">
              <a-form-item
                :label="t('outsourcingConfig.global.memberFilter')"
                field="memberFilter"
              >
                <div class="filter-editor-wrapper">
                  <a-spin :loading="loadingFields" style="width: 100%">
                    <ConditionEditor
                      v-model="modelValue.memberFilter"
                      :card-type-id="modelValue.memberCardTypeId"
                      :available-fields="availableFields"
                      :link-types="linkTypes"
                      :any-trait-card-type-name="selectedCardTypeName"
                      :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
                    />
                  </a-spin>
                </div>
                <template #extra>
                  <span class="form-item-tip">
                    {{ t('outsourcingConfig.global.memberFilterTip') }}
                  </span>
                </template>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
      </div>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OutsourcingConfig } from '@/types/outsourcing-config'
import { DurationUnit, getEnumOptions } from '@/types/outsourcing-config'
import { cardTypeApi, linkTypeApi, fieldOptionsApi } from '@/api'
import { SchemaSubType } from '@/types/schema'
import ConditionEditor from '@/components/condition/ConditionEditor.vue'
import type { FieldOption } from '@/types/field-option'
import type { LinkTypeVO } from '@/types/link-type'
import { useOrgStore } from '@/stores/org'

const { t } = useI18n()
const orgStore = useOrgStore()

const modelValue = defineModel<OutsourcingConfig>({ required: true })

// 从组织设置获取考勤功能是否启用
const attendanceEnabled = computed(() => orgStore.currentOrg?.attendanceEnabled || false)

// 时间单位选项
const durationUnitOptions = computed(() =>
  getEnumOptions(DurationUnit, {
    [DurationUnit.HOUR]: t('outsourcingConfig.enums.durationUnit.HOUR'),
    [DurationUnit.MINUTE]: t('outsourcingConfig.enums.durationUnit.MINUTE')
  })
)

// 成员卡片类型选项
const memberCardTypeOptions = ref<{ id: string; name: string; code?: string }[]>([])
const loadingMemberCardTypes = ref(false)

// 字段选项（用于条件过滤器）
const availableFields = ref<FieldOption[]>([])
const loadingFields = ref(false)

// 关联类型列表
const linkTypes = ref<LinkTypeVO[]>([])
const loadingLinkTypes = ref(false)

// 选中的卡片类型名称
const selectedCardTypeName = computed(() => {
  const selected = memberCardTypeOptions.value.find(
    (opt) => opt.id === modelValue.value.memberCardTypeId
  )
  return selected?.name || t('outsourcingConfig.global.memberCardType')
})

// 加载成员卡片类型及其子类型
async function loadMemberCardTypes() {
  loadingMemberCardTypes.value = true
  try {
    // 使用 by-parent 接口查询 member-trait 及其子卡类型
    // 假设 member-trait 的完整 ID 格式为 schemaId:member-trait
    // 先获取 schemaId（通常是租户的默认 schema）
    const allCardTypes = await cardTypeApi.list()
    const memberAbstract = allCardTypes.find(
      (ct) => ct.code === 'member-trait'
    )

    if (!memberAbstract || !memberAbstract.id) {
      console.warn('member-trait card type not found')
      // 如果找不到 member-trait，显示所有实体类型作为备选
      memberCardTypeOptions.value = allCardTypes
        .filter((ct) => ct.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE)
        .map((ct) => ({
          id: ct.id!,
          name: ct.name,
          code: ct.code
        }))
      return
    }

    console.log('Found member-trait:', memberAbstract)

    // 使用 by-parent 接口查询所有继承 member-trait 的卡片类型
    const childCardTypes = await cardTypeApi.getByParent(memberAbstract.id)
    console.log('Child card types of member-trait:', childCardTypes)

    // 转换为选项格式，包含 member-trait 本身和它的所有子类型
    memberCardTypeOptions.value = [
      // 首先添加 member-trait 属性集本身
      {
        id: memberAbstract.id,
        name: memberAbstract.name,
        code: memberAbstract.code
      },
      // 然后添加所有子卡类型
      ...childCardTypes.map((ct) => ({
        id: ct.id!,
        name: ct.name,
        code: ct.code
      }))
    ]

    console.log('Member card type options:', memberCardTypeOptions.value)
  } catch (error) {
    console.error('Failed to load member card types:', error)
  } finally {
    loadingMemberCardTypes.value = false
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
    console.log('Loaded fields for card type:', cardTypeId, availableFields.value)
  } catch (error) {
    console.error('Failed to fetch fields:', error)
    // 即使失败也要清空 loading 状态
    availableFields.value = []
  } finally {
    loadingFields.value = false
  }
}

// 监听卡片类型变化，加载字段列表
watch(
  () => modelValue.value.memberCardTypeId,
  (newCardTypeId) => {
    if (newCardTypeId) {
      fetchFields(newCardTypeId)
    } else {
      availableFields.value = []
      loadingFields.value = false
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadMemberCardTypes()
  fetchLinkTypes()
})
</script>

<style scoped lang="scss">
.global-config {
  width: 100%;
  max-width: none;

  // 配置区块
  .config-section {
    margin-bottom: 24px;
    background: #f7f8fa;
    border-radius: 12px;
    overflow: hidden;
    transition: all 0.3s cubic-bezier(0.34, 0.69, 0.1, 1);

    &:hover {
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    }

    &:last-child {
      margin-bottom: 0;
    }
  }

  // 区块头部
  .section-header {
    padding: 20px 24px;
    background: linear-gradient(135deg, #f7f8fa 0%, #ffffff 100%);
    border-bottom: 1px solid #e5e6eb;
  }

  .section-title {
    display: flex;
    align-items: center;
    font-size: 16px;
    font-weight: 600;
    color: #1d2129;
    margin-bottom: 6px;
  }

  .section-description {
    font-size: 13px;
    color: #86909c;
    line-height: 1.5;
  }

  // 区块内容
  .section-content {
    padding: 24px;
    background: #fff;
  }

  // 表单项优化
  :deep(.arco-form-item) {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(.arco-form-item-label-col) {
    padding-bottom: 8px;

    .arco-form-item-label {
      font-size: 14px;
      font-weight: 500;
      color: #1d2129;
      line-height: 22px;

      &::before {
        color: #f53f3f;
      }
    }
  }

  // 输入框样式
  :deep(.arco-select),
  :deep(.arco-input-number),
  :deep(.arco-input-wrapper) {
    border-radius: 8px;
    border-color: #e5e6eb;
    transition: all 0.2s cubic-bezier(0.34, 0.69, 0.1, 1);

    &:hover {
      border-color: #c9cdd4;
      background: #f7f8fa;
    }

    &:focus,
    &.arco-select-focused {
      border-color: #165dff;
      background: #fff;
      box-shadow: 0 0 0 3px rgba(22, 93, 255, 0.1);
    }
  }

  :deep(.arco-input-number) {
    .arco-input-number-step {
      border-radius: 0 8px 8px 0;
    }
  }

  // Switch 开关样式
  .switch-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  :deep(.arco-switch) {
    &.arco-switch-checked {
      background-color: #165dff;

      &:hover {
        background-color: #4080ff;
      }
    }
  }

  // 提示文字
  .form-item-tip {
    font-size: 13px;
    color: #86909c;
    line-height: 1.5;
  }

  :deep(.arco-form-item-extra) {
    margin-top: 8px;
    padding: 8px 12px;
    background: #f7f8fa;
    border-radius: 6px;
    font-size: 13px;
    color: #4e5969;
    line-height: 1.5;
    border-left: 3px solid #165dff;
  }

  // 筛选器编辑器包装
  .filter-editor-wrapper {
    width: 100%;
    padding: 20px;
    background: #f7f8fa;
    border-radius: 8px;
    border: 1px solid #e5e6eb;
    transition: all 0.2s cubic-bezier(0.34, 0.69, 0.1, 1);
    box-sizing: border-box;

    &:hover {
      border-color: #c9cdd4;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    }

    // 增加条件编辑器内部空间
    :deep(.condition-editor) {
      width: 100%;
    }

    // 调整条件组和条件项的间距
    :deep(.condition-group),
    :deep(.condition-item) {
      margin-bottom: 16px;
    }

    // 确保内部组件占满宽度
    :deep(.arco-spin) {
      width: 100%;
    }
  }

  // 加载状态
  :deep(.arco-spin) {
    .arco-spin-icon {
      color: #165dff;
    }
  }

  // 下拉选项优化
  :deep(.arco-select-dropdown) {
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    border: 1px solid #e5e6eb;

    .arco-select-option {
      border-radius: 6px;
      margin: 4px 8px;
      padding: 8px 12px;
      transition: all 0.2s cubic-bezier(0.34, 0.69, 0.1, 1);

      &:hover {
        background: #f7f8fa;
      }

      &.arco-select-option-selected {
        background: rgba(22, 93, 255, 0.1);
        color: #165dff;
        font-weight: 500;
      }
    }
  }

  // 响应式优化
  @media (max-width: 768px) {
    .section-content {
      padding: 16px;
    }

    .section-header {
      padding: 16px;
    }

    .section-description {
      padding-left: 0;
      margin-top: 8px;
    }
  }
}
</style>
