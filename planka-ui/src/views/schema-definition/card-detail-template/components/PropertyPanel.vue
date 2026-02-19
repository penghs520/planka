<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  CardDetailTemplateDefinition,
  TabConfig,
  SectionConfig,
  FieldItemConfig,
  SelectedItem,
} from '@/types/card-detail-template'
import type { FieldConfig } from '@/types/card-type'
import { getFieldTypeLabelKey } from '@/types/field'
import { SystemTabTypeConfig, FieldRowSpacingConfig } from '@/types/card-detail-template'

const { t } = useI18n()

const props = defineProps<{
  templateData: CardDetailTemplateDefinition
  selectedItem: SelectedItem | null
  fields: FieldConfig[]
}>()

const emit = defineEmits<{
  (e: 'change'): void
}>()

// 获取选中的 Tab
const selectedTab = computed<TabConfig | null>(() => {
  if (!props.selectedItem) return null
  const tabId = props.selectedItem.tabId || props.selectedItem.id
  return props.templateData.tabs.find((t) => t.tabId === tabId) || null
})

// 获取选中的区域
const selectedSection = computed<SectionConfig | null>(() => {
  if (props.selectedItem?.type !== 'section' && props.selectedItem?.type !== 'field') return null
  const sectionId = props.selectedItem.sectionId || props.selectedItem.id
  return selectedTab.value?.sections?.find((s) => s.sectionId === sectionId) || null
})

// 获取选中的字段项
const selectedFieldItem = computed<FieldItemConfig | null>(() => {
  if (props.selectedItem?.type !== 'field') return null
  return selectedSection.value?.fieldItems.find((f) => f.fieldConfigId === props.selectedItem!.id) || null
})

// 获取字段信息
const selectedFieldInfo = computed<FieldConfig | null>(() => {
  if (!selectedFieldItem.value) return null
  return props.fields.find((f) => f.fieldId === selectedFieldItem.value!.fieldConfigId) || null
})

// 获取字段类型名称
const selectedFieldTypeName = computed(() => {
  if (!selectedFieldInfo.value) return '-'
  const labelKey = getFieldTypeLabelKey(selectedFieldInfo.value.schemaSubType)
  return t(labelKey)
})

// 判断是否为描述字段
const isDescriptionField = computed(() => {
  return selectedFieldItem.value?.fieldConfigId === '$description'
})

// 字段行间距选项
const fieldRowSpacingOptions = Object.entries(FieldRowSpacingConfig).map(([value, config]) => ({
  value,
  label: config.label,
}))

// 触发变更
function handleChange() {
  emit('change')
}
</script>

<template>
  <div class="property-panel">
    <div class="panel-header">
      <h3 class="panel-title">属性配置</h3>
    </div>

    <div class="panel-content">
      <!-- 无选中项 -->
      <div v-if="!selectedItem" class="empty-hint">
        <a-empty description="选择 Tab、区域或字段进行配置" />
      </div>

      <!-- 标签属性 -->
      <div v-else-if="selectedItem.type === 'tab' && selectedTab" class="property-section">
        <div class="section-title">标签属性</div>
        <a-form :model="selectedTab" layout="vertical" size="small">
          <a-form-item label="名称">
            <a-input
              v-model="selectedTab.name"
              placeholder="输入 Tab 名称"
              @change="handleChange"
            />
          </a-form-item>
          <a-form-item label="类型">
            <a-tag v-if="selectedTab.tabType === 'SYSTEM'" color="blue">
              系统预置 - {{ SystemTabTypeConfig[selectedTab.systemTabType!]?.label }}
            </a-tag>
            <a-tag v-else color="green">自定义</a-tag>
          </a-form-item>
          <a-form-item label="字段行间距">
            <a-select v-model="selectedTab.fieldRowSpacing" @change="handleChange">
              <a-option
                v-for="option in fieldRowSpacingOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-option>
            </a-select>
          </a-form-item>
        </a-form>

        <!-- 可见条件配置 -->
        <a-divider />
        <div class="section-title">可见条件</div>
        <div class="condition-hint">
          <a-tag color="gray">暂未配置</a-tag>
          <a-button type="text" size="small" disabled>配置条件</a-button>
        </div>
      </div>

      <!-- 区域属性 -->
      <div v-else-if="selectedItem.type === 'section' && selectedSection" class="property-section">
        <div class="section-title">区域属性</div>
        <a-form :model="selectedSection" layout="vertical" size="small">
          <a-form-item label="名称">
            <a-input
              v-model="selectedSection.name"
              placeholder="输入区域名称"
              @change="handleChange"
            />
          </a-form-item>
          <a-form-item>
            <a-checkbox v-model="selectedSection.collapsible" @change="handleChange">
              可折叠
            </a-checkbox>
          </a-form-item>
          <a-form-item v-if="selectedSection.collapsible">
            <a-checkbox v-model="selectedSection.collapsed" @change="handleChange">
              默认折叠
            </a-checkbox>
          </a-form-item>
        </a-form>
      </div>

      <!-- 字段属性 -->
      <div v-else-if="selectedItem.type === 'field' && selectedFieldItem" class="property-section">
        <div class="section-title">字段属性</div>

        <!-- 字段基本信息 -->
        <div class="field-info">
          <div class="info-label">字段名称</div>
          <div class="info-value">{{ selectedFieldInfo?.name || selectedFieldItem.fieldConfigId }}</div>
          <div class="info-label">字段类型</div>
          <div class="info-value">{{ selectedFieldTypeName }}</div>
        </div>

        <a-divider />

        <a-form :model="selectedFieldItem" layout="vertical" size="small">
          <a-form-item label="宽度">
            <div class="width-presets">
              <a-button
                v-for="preset in [25, 33, 50, 66, 75, 100]"
                :key="preset"
                size="small"
                :type="selectedFieldItem.widthPercent === preset ? 'primary' : 'secondary'"
                @click="selectedFieldItem.widthPercent = preset; handleChange()"
              >
                {{ preset }}%
              </a-button>
            </div>
          </a-form-item>
          <a-form-item label="自定义标签">
            <a-input
              v-model="selectedFieldItem.customLabel"
              placeholder="留空使用默认名称"
              @change="handleChange"
            />
          </a-form-item>
          <a-form-item label="占位提示">
            <a-input
              v-model="selectedFieldItem.placeholder"
              placeholder="输入占位提示文本"
              @change="handleChange"
            />
          </a-form-item>
          <a-form-item v-if="isDescriptionField" label="最小高度(px)">
            <a-input-number
              v-model="selectedFieldItem.height"
              :min="120"
              :max="800"
              :step="20"
              placeholder="默认 120"
              @change="handleChange"
            />
          </a-form-item>
        </a-form>

        <!-- 新建页配置 -->
        <a-divider />
        <div class="section-title">新建页配置</div>
        <a-form :model="selectedFieldItem" layout="vertical" size="small">
          <a-form-item>
            <a-checkbox v-model="selectedFieldItem.visibleOnCreate" @change="handleChange">
              新建页可见
            </a-checkbox>
          </a-form-item>
          <a-form-item v-if="selectedFieldItem.visibleOnCreate" label="是否必填">
            <a-select v-model="selectedFieldItem.requiredOnCreate" @change="handleChange">
              <a-option :value="false">非必填</a-option>
              <a-option :value="true">必填</a-option>
            </a-select>
          </a-form-item>
        </a-form>

        <!-- 可见条件配置（预留） -->
        <a-divider />
        <div class="section-title">可见条件</div>
        <div class="condition-hint">
          <a-tag color="gray">暂未配置</a-tag>
          <a-button type="text" size="small" disabled>配置条件</a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.property-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  padding: 12px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.panel-title {
  font-size: 13px;
  font-weight: 500;
  margin: 0;
  color: var(--color-text-1);
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.empty-hint {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.property-section {
  .section-title {
    font-size: 12px;
    font-weight: 500;
    color: var(--color-text-2);
    margin-bottom: 12px;
  }
}

.field-info {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px;
  font-size: 12px;

  .info-label {
    color: var(--color-text-3);
  }

  .info-value {
    color: var(--color-text-1);
  }
}

.condition-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.width-presets {
  display: flex;
  flex-wrap: wrap; 
  gap: 8px;

  .arco-btn {
    flex: 1;
    min-width: 48px; 
    padding: 0;
  }
}

:deep(.arco-form-item) {
  margin-bottom: 12px;
}

:deep(.arco-form-item-label) {
  font-size: 12px;
}
</style>
