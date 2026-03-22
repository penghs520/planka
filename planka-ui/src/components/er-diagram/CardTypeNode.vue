<script setup lang="ts">
import { ref, computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { Handle, Position } from '@vue-flow/core'
import { IconDown, IconUp } from '@arco-design/web-vue/es/icon'
import FieldTypeIcon from '@/components/field/FieldTypeIcon.vue'
import type { FieldOption } from '@/types/field-option'
import type { CardTypeInfo } from '@/types/link-type'

const { t } = useI18n()

// Inject loadFieldConfigs from parent
const loadFieldConfigs = inject<(cardTypeId: string) => Promise<void>>('loadFieldConfigs')

interface Props {
  id: string
  data: {
    mode: 'single' | 'combo'
    entityId: string
    entityName?: string
    fields?: FieldOption[]
    linkFields?: FieldOption[]
    abstractTypes?: CardTypeInfo[]
    concreteTypes?: CardTypeInfo[]
    isSelected?: boolean
    isHighlighted?: boolean
    fieldsLoaded?: boolean
    fieldsLoading?: boolean
  }
}

const props = defineProps<Props>()

// Collapsible sections state
const fieldsExpanded = ref(false)
const concretesExpanded = ref(true)

// All fields (regular + link) merged together
const allFields = computed(() => {
  const regular = (props.data.fields || []).filter(f => f.fieldType !== 'LINK')
  const links = props.data.linkFields || []
  return [...regular, ...links]
})
const abstractTypes = computed(() => props.data.abstractTypes || [])
const concreteTypes = computed(() => props.data.concreteTypes || [])

// Handle section toggle with lazy loading
function toggleFieldsSection() {
  if (!fieldsExpanded.value && !props.data.fieldsLoaded && !props.data.fieldsLoading) {
    loadFieldConfigs?.(props.data.entityId)
  }
  fieldsExpanded.value = !fieldsExpanded.value
}
</script>

<template>
  <div
    class="card-type-node"
    :class="{
      'is-combo': data.mode === 'combo',
      'is-selected': data.isSelected,
      'is-highlighted': data.isHighlighted
    }"
  >
    <!-- Connection handles - multiple handles per side for parallel edges -->
    <!-- Left handles -->
    <Handle id="left-top" type="target" :position="Position.Left" class="handle handle-left" :style="{ top: '30%' }" />
    <Handle id="left" type="target" :position="Position.Left" class="handle handle-left" :style="{ top: '50%' }" />
    <Handle id="left-bottom" type="target" :position="Position.Left" class="handle handle-left" :style="{ top: '70%' }" />
    <!-- Right handles -->
    <Handle id="right-top" type="source" :position="Position.Right" class="handle handle-right" :style="{ top: '30%' }" />
    <Handle id="right" type="source" :position="Position.Right" class="handle handle-right" :style="{ top: '50%' }" />
    <Handle id="right-bottom" type="source" :position="Position.Right" class="handle handle-right" :style="{ top: '70%' }" />
    <!-- Top handles -->
    <Handle id="top-left" type="target" :position="Position.Top" class="handle handle-top" :style="{ left: '30%' }" />
    <Handle id="top" type="target" :position="Position.Top" class="handle handle-top" :style="{ left: '50%' }" />
    <Handle id="top-right" type="target" :position="Position.Top" class="handle handle-top" :style="{ left: '70%' }" />
    <!-- Bottom handles -->
    <Handle id="bottom-left" type="source" :position="Position.Bottom" class="handle handle-bottom" :style="{ left: '30%' }" />
    <Handle id="bottom" type="source" :position="Position.Bottom" class="handle handle-bottom" :style="{ left: '50%' }" />
    <Handle id="bottom-right" type="source" :position="Position.Bottom" class="handle handle-bottom" :style="{ left: '70%' }" />

    <!-- Single Mode Header -->
    <div v-if="data.mode === 'single'" class="node-header">
      <span class="node-name">{{ data.entityName }}</span>
    </div>

    <!-- Combo Mode Header -->
    <div v-else class="node-header combo-header">
      <div class="abstract-types">
        <a-tag
          v-for="at in abstractTypes"
          :key="at.id"
          size="small"
          color="arcoblue"
        >
          {{ at.name }}
        </a-tag>
      </div>
    </div>

    <!-- Single Mode: Fields Section -->
    <template v-if="data.mode === 'single'">
      <div class="section-toggle" @click.stop="toggleFieldsSection">
        <component :is="fieldsExpanded ? IconUp : IconDown" class="toggle-icon" />
        <span class="toggle-label">{{ t('common.erDiagram.attributes') }}</span>
        <span v-if="data.fieldsLoading" class="loading-indicator">...</span>
      </div>
      <div v-if="fieldsExpanded" class="fields-list" @wheel.stop>
        <div v-if="data.fieldsLoading" class="field-row empty">
          <span class="field-name">Loading...</span>
        </div>
        <template v-else>
          <div v-for="field in allFields" :key="field.id" class="field-row">
            <FieldTypeIcon :field-type="field.fieldType" size="small" class="field-icon" />
            <span class="field-name">{{ field.name }}</span>
          </div>
          <div v-if="allFields.length === 0" class="field-row empty">
            <span class="field-name">{{ t('common.erDiagram.noFields') }}</span>
          </div>
        </template>
      </div>
    </template>

    <!-- Combo Mode: Concrete Implementations Section -->
    <template v-if="data.mode === 'combo'">
      <div class="section-toggle concrete" @click.stop="concretesExpanded = !concretesExpanded">
        <component :is="concretesExpanded ? IconUp : IconDown" class="toggle-icon" />
        <span class="toggle-label">{{ t('common.erDiagram.implementTypes') }} ({{ concreteTypes.length }})</span>
      </div>
      <div v-if="concretesExpanded" class="concretes-list" @wheel.stop>
        <div
          v-for="ct in concreteTypes"
          :key="ct.id"
          class="concrete-item"
        >
          <span class="concrete-name">{{ ct.name }}</span>
        </div>
        <div v-if="concreteTypes.length === 0" class="field-row empty">
          <span class="field-name">{{ t('common.erDiagram.noImplementTypes') }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.card-type-node {
  min-width: 240px;
  max-width: 300px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #d9d9d9;
  transition: all 0.2s;
  overflow: hidden;
}

.card-type-node:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-color: #b3b3b3;
}

.card-type-node.is-selected {
  border: 2px solid rgb(var(--primary-6));
  box-shadow: 0 4px 16px rgba(var(--primary-6), 0.3);
}

.card-type-node.is-highlighted {
  border: 2px solid rgb(var(--warning-6));
  box-shadow: 0 4px 16px rgba(var(--warning-6), 0.3);
}

.card-type-node.is-combo {
  border-color: rgb(var(--primary-5));
  background: linear-gradient(to bottom, rgba(var(--primary-1), 0.3), #ffffff);
}

/* Vue Flow Handles - hidden but functional */
.handle {
  width: 10px;
  height: 10px;
  background: transparent;
  border: none;
  opacity: 0;
}

.handle-left {
  left: 0;
}

.handle-right {
  right: 0;
}

.handle-top {
  top: 0;
}

.handle-bottom {
  bottom: 0;
}

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 2px solid #e8e8e8;
  background: linear-gradient(to bottom, #fafafa, #f5f5f5);
}

.combo-header {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  background: linear-gradient(to bottom, rgba(var(--primary-1), 0.6), rgba(var(--primary-1), 0.3));
}

.abstract-types {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.node-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
  letter-spacing: 0.3px;
}

.field-row {
  display: flex;
  align-items: center;
  padding: 5px 14px;
  font-size: 12px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
}

.field-row:hover {
  background: #fafafa;
}

.field-row:last-child {
  border-bottom: none;
}

.field-row.empty {
  color: var(--color-text-3);
  font-style: italic;
  justify-content: center;
  padding: 8px 14px;
}

.field-icon {
  font-size: 13px;
  color: #8c8c8c;
  margin-right: 8px;
  flex-shrink: 0;
}

.field-name {
  flex: 1;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-toggle {
  display: flex;
  align-items: center;
  padding: 7px 14px;
  cursor: pointer;
  color: #595959;
  font-size: 12px;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  transition: all 0.15s;
}

.section-toggle:hover {
  background: #f0f0f0;
  color: #262626;
}

.section-toggle.concrete {
  color: rgb(var(--primary-6));
}

.section-toggle.concrete:hover {
  background: rgba(var(--primary-1), 0.3);
}

.toggle-icon {
  font-size: 11px;
  margin-right: 6px;
}

.toggle-label {
  font-size: 11px;
}

.loading-indicator {
  margin-left: auto;
  color: var(--color-text-3);
  font-size: 10px;
}

.fields-list {
  max-height: 200px;
  overflow-y: auto;
}

.concretes-list {
  max-height: 200px;
  overflow-y: auto;
  border-radius: 0 0 4px 4px;
}

.concrete-item {
  padding: 7px 14px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}

.concrete-item:last-child {
  border-bottom: none;
  border-radius: 0 0 4px 4px;
}

.concrete-item:hover {
  background: rgba(var(--primary-1), 0.3);
}

.concrete-name {
  font-size: 12px;
  color: #262626;
  font-weight: 500;
}
</style>
