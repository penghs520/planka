<script setup lang="ts">
import { ref, computed } from 'vue'
import { IconEdit, IconDelete, IconDown, IconUp } from '@arco-design/web-vue/es/icon'
import type { FieldConfig } from '@/types/card-type'
import type { CardTypeInfo } from '@/types/link-type'

interface Props {
  mode?: 'single' | 'combo'
  entityId: string
  entityName?: string
  fields?: FieldConfig[]
  linkFields?: FieldConfig[]
  position: { x: number; y: number }
  selected?: boolean
  // Combo mode props
  abstractTypes?: CardTypeInfo[]
  concreteTypes?: CardTypeInfo[]
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'single',
  entityName: '',
  selected: false,
  fields: () => [],
  linkFields: () => [],
  abstractTypes: () => [],
  concreteTypes: () => [],
})

const emit = defineEmits<{
  (e: 'edit', id: string): void
  (e: 'delete', id: string): void
  (e: 'dragStart', event: MouseEvent, id: string): void
  (e: 'updatePosition', id: string, pos: { x: number; y: number }): void
}>()

// Collapsible sections state
const fieldsExpanded = ref(false)
const linkFieldsExpanded = ref(false)
const concretesExpanded = ref(true)

// Get field type label
function getFieldTypeLabel(schemaSubType: string): string {
  const typeMap: Record<string, string> = {
    TEXT_FIELD: 'Text',
    MULTI_LINE_TEXT_FIELD: 'TextArea',
    MARKDOWN_FIELD: 'Markdown',
    NUMBER_FIELD: 'Integer',
    DATE_FIELD: 'Date',
    ENUM_FIELD: 'Enum',
    ATTACHMENT_FIELD: 'Attachment',
    WEB_URL_FIELD: 'URL',
    STRUCTURE_FIELD: 'Structure',
    LINK_FIELD: 'Link',
  }
  return typeMap[schemaSubType] || 'Text'
}

// Non-link fields for "All Fields" section
const regularFields = computed(() => {
  return props.fields.filter(f => f.schemaSubType !== 'LINK_FIELD')
})

// Handle mouse down for dragging
function handleMouseDown(event: MouseEvent) {
  emit('dragStart', event, props.entityId)
}
</script>

<template>
  <div
    class="entity-card"
    :class="{ 'is-selected': selected, 'is-combo': mode === 'combo' }"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
    @mousedown="handleMouseDown"
  >
    <!-- Single Mode Header -->
    <div v-if="mode === 'single'" class="entity-header">
      <span class="entity-name">{{ entityName }}</span>
      <div class="entity-actions">
        <a-button type="text" size="mini" @click.stop="emit('edit', entityId)">
          <template #icon><IconEdit /></template>
        </a-button>
        <a-button type="text" size="mini" status="danger" @click.stop="emit('delete', entityId)">
          <template #icon><IconDelete /></template>
        </a-button>
      </div>
    </div>

    <!-- Combo Mode Header -->
    <div v-else class="entity-header combo-header">
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

    <!-- Single Mode: Always visible ID field -->
    <div v-if="mode === 'single'" class="field-row primary-field">
      <span class="field-indicator"></span>
      <span class="field-name">ID</span>
      <span class="field-type">Integer</span>
    </div>

    <!-- Single Mode: All Fields Section -->
    <template v-if="mode === 'single'">
      <div class="section-toggle" @click.stop="fieldsExpanded = !fieldsExpanded">
        <component :is="fieldsExpanded ? IconUp : IconDown" class="toggle-icon" />
        <span class="toggle-label">All Fields</span>
      </div>
      <div v-if="fieldsExpanded" class="fields-list">
        <div v-for="field in regularFields" :key="field.fieldId" class="field-row">
          <span class="field-name">{{ field.name }}</span>
          <span class="field-type">{{ getFieldTypeLabel(field.schemaSubType) }}</span>
        </div>
        <div v-if="regularFields.length === 0" class="field-row empty">
          <span class="field-name">No fields defined</span>
        </div>
      </div>

      <!-- Single Mode: Association Fields Section -->
      <div class="section-toggle association" @click.stop="linkFieldsExpanded = !linkFieldsExpanded">
        <component :is="linkFieldsExpanded ? IconUp : IconDown" class="toggle-icon" />
        <span class="toggle-label">Association Fields</span>
      </div>
      <div v-if="linkFieldsExpanded" class="fields-list association-fields">
        <div v-for="field in linkFields" :key="field.fieldId" class="field-row link-field">
          <span class="field-indicator link"></span>
          <span class="field-name">{{ field.name }}</span>
          <span class="field-type">{{ getFieldTypeLabel(field.schemaSubType) }}</span>
        </div>
        <div v-if="linkFields.length === 0" class="field-row empty">
          <span class="field-name">No associations</span>
        </div>
      </div>
    </template>

    <!-- Combo Mode: Concrete Implementations Section -->
    <template v-if="mode === 'combo'">
      <div class="section-toggle concrete" @click.stop="concretesExpanded = !concretesExpanded">
        <component :is="concretesExpanded ? IconUp : IconDown" class="toggle-icon" />
        <span class="toggle-label">实现类型 ({{ concreteTypes.length }})</span>
      </div>
      <div v-if="concretesExpanded" class="concretes-list">
        <div 
          v-for="ct in concreteTypes" 
          :key="ct.id" 
          class="concrete-item"
          @click.stop="emit('edit', ct.id)"
        >
          <span class="concrete-name">{{ ct.name }}</span>
        </div>
        <div v-if="concreteTypes.length === 0" class="field-row empty">
          <span class="field-name">暂无实现类型</span>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.entity-card {
  position: absolute;
  min-width: 240px;
  max-width: 300px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.08),
    0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.8);
  cursor: move;
  user-select: none;
  transition: box-shadow 0.2s, transform 0.1s;
  z-index: 10;
}

.entity-card:hover {
  box-shadow: 
    0 8px 24px rgba(0, 0, 0, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.06);
}

.entity-card.is-selected {
  box-shadow: 
    0 0 0 2px rgb(var(--primary-6)),
    0 8px 24px rgba(0, 0, 0, 0.12);
}

.entity-card.is-combo {
  border-color: rgb(var(--primary-4));
  background: linear-gradient(135deg, rgba(var(--primary-1), 0.5), rgba(255, 255, 255, 0.95));
}

.entity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border-1);
  background: rgba(var(--gray-1), 0.5);
  border-radius: 8px 8px 0 0;
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

.entity-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
}

.entity-actions {
  display: flex;
  gap: 2px;
}

.entity-actions .arco-btn {
  color: var(--color-text-3);
}

.entity-actions .arco-btn:hover {
  color: var(--color-text-1);
}

.field-row {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  font-size: 12px;
  border-bottom: 1px solid var(--color-border-1);
}

.field-row:last-child {
  border-bottom: none;
}

.field-row.primary-field {
  background: rgba(var(--primary-1), 0.5);
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
  color: var(--color-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-type {
  color: var(--color-text-3);
  font-size: 11px;
  margin-left: 8px;
}

.section-toggle {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  color: rgb(var(--primary-6));
  font-size: 12px;
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
  font-size: 12px;
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
  font-size: 12px;
  color: var(--color-text-2);
  font-weight: 500;
}
</style>
