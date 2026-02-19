<script setup lang="ts">
import { ref, computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { Handle, Position } from '@vue-flow/core'
import { IconDown, IconUp } from '@arco-design/web-vue/es/icon'
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
const linkFieldsExpanded = ref(false)
const concretesExpanded = ref(true)

// Get field type label
function getFieldTypeLabel(fieldType: string): string {
  const typeMap: Record<string, string> = {
    SINGLE_LINE_TEXT: 'Text',
    MULTI_LINE_TEXT: 'TextArea',
    MARKDOWN: 'Markdown',
    NUMBER: 'Integer',
    DATE: 'Date',
    ENUM: 'Enum',
    ATTACHMENT: 'Attachment',
    WEB_URL: 'URL',
    STRUCTURE: 'Structure',
    LINK: 'Link',
  }
  return typeMap[fieldType] || 'Text'
}

// Non-link fields for "All Fields" section
const regularFields = computed(() => {
  return (props.data.fields || []).filter(f => f.fieldType !== 'LINK')
})

const linkFields = computed(() => props.data.linkFields || [])
const abstractTypes = computed(() => props.data.abstractTypes || [])
const concreteTypes = computed(() => props.data.concreteTypes || [])

// Handle section toggle with lazy loading
function toggleFieldsSection() {
  if (!fieldsExpanded.value && !props.data.fieldsLoaded && !props.data.fieldsLoading) {
    loadFieldConfigs?.(props.data.entityId)
  }
  fieldsExpanded.value = !fieldsExpanded.value
}

function toggleLinkFieldsSection() {
  if (!linkFieldsExpanded.value && !props.data.fieldsLoaded && !props.data.fieldsLoading) {
    loadFieldConfigs?.(props.data.entityId)
  }
  linkFieldsExpanded.value = !linkFieldsExpanded.value
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

    <!-- Single Mode: All Fields Section -->
    <template v-if="data.mode === 'single'">
      <div class="section-toggle" @click.stop="toggleFieldsSection">
        <component :is="fieldsExpanded ? IconUp : IconDown" class="toggle-icon" />
        <span class="toggle-label">All Fields</span>
        <span v-if="data.fieldsLoading" class="loading-indicator">...</span>
      </div>
      <div v-if="fieldsExpanded" class="fields-list" @wheel.stop>
        <div v-if="data.fieldsLoading" class="field-row empty">
          <span class="field-name">Loading...</span>
        </div>
        <template v-else>
          <div v-for="field in regularFields" :key="field.id" class="field-row">
            <span class="field-name">{{ field.name }}</span>
            <span class="field-type">{{ getFieldTypeLabel(field.fieldType) }}</span>
          </div>
          <div v-if="regularFields.length === 0" class="field-row empty">
            <span class="field-name">No fields defined</span>
          </div>
        </template>
      </div>

      <!-- Single Mode: Association Fields Section -->
      <div class="section-toggle association" @click.stop="toggleLinkFieldsSection">
        <component :is="linkFieldsExpanded ? IconUp : IconDown" class="toggle-icon" />
        <span class="toggle-label">Association Fields</span>
        <span v-if="data.fieldsLoading" class="loading-indicator">...</span>
      </div>
      <div v-if="linkFieldsExpanded" class="fields-list association-fields" @wheel.stop>
        <div v-if="data.fieldsLoading" class="field-row empty">
          <span class="field-name">Loading...</span>
        </div>
        <template v-else>
          <div v-for="field in linkFields" :key="field.id" class="field-row link-field">
            <span class="field-indicator link"></span>
            <span class="field-name">{{ field.name }}</span>
            <span class="field-type">{{ getFieldTypeLabel(field.fieldType) }}</span>
          </div>
          <div v-if="linkFields.length === 0" class="field-row empty">
            <span class="field-name">No associations</span>
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
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.08),
    0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1.5px solid #dcfce7;
  transition: box-shadow 0.2s, transform 0.1s;
}

.card-type-node:hover {
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-type-node.is-selected {
  box-shadow:
    0 0 0 2px rgb(var(--primary-6)),
    0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-type-node.is-highlighted {
  box-shadow:
    0 0 0 2px rgb(var(--success-6)),
    0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-type-node.is-combo {
  border-color: rgba(var(--primary-1), 0.8);
  background: linear-gradient(135deg, rgba(var(--primary-1), 0.5), rgba(255, 255, 255, 0.95));
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
  padding: 10px 12px;
  border-bottom: 1px solid #bbf7d0;
  background: linear-gradient(135deg, #dcfce7 0%, #f0fdf4 100%);
  border-radius: 6px 6px 0 0;
}

.combo-header {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  background: linear-gradient(135deg, rgba(var(--primary-1), 0.8), rgba(var(--primary-2), 0.5));
}

.abstract-types {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.node-name {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.node-actions {
  display: flex;
  gap: 2px;
}

.node-actions .arco-btn {
  color: var(--color-text-3);
}

.node-actions .arco-btn:hover {
  color: var(--color-text-1);
}

.field-row {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  font-size: 13px;
  border-bottom: 1px solid var(--color-border-1);
}

.field-row:last-child {
  border-bottom: none;
}

.field-row.link-field {
  background: rgba(var(--warning-1), 0.3);
}

.field-row.empty {
  color: var(--color-text-3);
  font-style: italic;
}

.field-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgb(var(--danger-6));
  margin-right: 8px;
}

.field-indicator.link {
  background: rgb(var(--warning-6));
}

.field-name {
  flex: 1;
  color: #4e5969;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-type {
  color: #86909c;
  font-size: 12px;
  margin-left: 8px;
}

.section-toggle {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  color: rgb(var(--primary-6));
  font-size: 13px;
  font-weight: 500;
  border-bottom: 1px solid var(--color-border-1);
  transition: background 0.15s;
}

.section-toggle:hover {
  background: rgba(var(--primary-1), 0.5);
}

.section-toggle.association {
  color: rgb(var(--warning-6));
}

.section-toggle.association:hover {
  background: rgba(var(--warning-1), 0.5);
}

.section-toggle.concrete {
  color: rgb(var(--success-6));
}

.section-toggle.concrete:hover {
  background: rgba(var(--success-1), 0.5);
}

.toggle-icon {
  font-size: 12px;
  margin-right: 6px;
}

.toggle-label {
  font-size: 13px;
}

.loading-indicator {
  margin-left: auto;
  color: var(--color-text-3);
  font-size: 11px;
}

.fields-list {
  max-height: 200px;
  overflow-y: auto;
}

.association-fields {
  border-radius: 0 0 8px 8px;
}

.concretes-list {
  max-height: 200px;
  overflow-y: auto;
  border-radius: 0 0 8px 8px;
}

.concrete-item {
  padding: 8px 12px;
  border-bottom: 1px solid var(--color-border-1);
  cursor: pointer;
  transition: background 0.15s;
}

.concrete-item:last-child {
  border-bottom: none;
  border-radius: 0 0 8px 8px;
}

.concrete-item:hover {
  background: rgba(var(--success-1), 0.5);
}

.concrete-name {
  font-size: 13px;
  color: #4e5969;
  font-weight: 500;
}
</style>
