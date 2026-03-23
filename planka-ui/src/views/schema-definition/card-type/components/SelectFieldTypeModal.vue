<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import CancelButton from '@/components/common/CancelButton.vue'
import { SchemaSubType } from '@/types/schema'
import FieldTypeCardPreview from './FieldTypeCardPreview.vue'
import { getFieldTypeLabelI18n } from '../formatters'
import { FIELD_TYPE_CATEGORIES, type FieldTypeCategoryId } from './field-type-picker'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  cancel: []
  confirm: [subType: SchemaSubType]
}>()

const { t } = useI18n()

const highlightedSubType = ref<SchemaSubType | null>(null)
const activeCategoryId = ref<FieldTypeCategoryId>('text')
const scrollEl = ref<HTMLElement | null>(null)

watch(
  () => props.visible,
  (v) => {
    if (v) {
      highlightedSubType.value = null
      activeCategoryId.value = 'text'
    }
  }
)

function sectionDomId(catId: FieldTypeCategoryId): string {
  return `ft-field-type-section-${catId}`
}

function scrollToCategory(catId: FieldTypeCategoryId): void {
  activeCategoryId.value = catId
  const root = scrollEl.value
  if (!root) return
  const el = root.querySelector(`#${sectionDomId(catId)}`)
  el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function handleCardClick(subType: SchemaSubType): void {
  highlightedSubType.value = subType
  const cat = FIELD_TYPE_CATEGORIES.find((c) => c.subTypes.includes(subType))
  if (cat) activeCategoryId.value = cat.id
}

function fieldTypeHelpKey(subType: SchemaSubType): string {
  return `admin.cardType.fieldConfig.fieldTypeHelp.${subType}`
}

function labelFor(subType: SchemaSubType): string {
  return getFieldTypeLabelI18n(subType, t)
}

function handleCancel(): void {
  emit('cancel')
}

function handleNext(): void {
  if (!highlightedSubType.value) {
    Message.warning(t('admin.cardType.fieldConfig.selectTypeFirstHint'))
    return
  }
  emit('confirm', highlightedSubType.value)
}
</script>

<template>
  <a-modal
    :visible="visible"
    :title="t('admin.cardType.fieldConfig.selectFieldType')"
    :width="800"
    :mask-closable="true"
    :esc-to-close="true"
    modal-class="select-field-type-modal-wrap"
    unmount-on-close
    @cancel="handleCancel"
  >
    <div class="picker-shell">
      <nav class="picker-nav" aria-label="field type categories">
        <button
          v-for="cat in FIELD_TYPE_CATEGORIES"
          :key="cat.id"
          type="button"
          class="nav-item"
          :class="{ 'nav-item--active': activeCategoryId === cat.id }"
          @click="scrollToCategory(cat.id)"
        >
          {{ t(`admin.cardType.fieldConfig.fieldTypeCategory.${cat.id}`) }}
        </button>
      </nav>
      <div ref="scrollEl" class="picker-scroll">
        <section
          v-for="cat in FIELD_TYPE_CATEGORIES"
          :id="sectionDomId(cat.id)"
          :key="cat.id"
          class="picker-section"
        >
          <h3 class="section-title">
            {{ t(`admin.cardType.fieldConfig.fieldTypeCategory.${cat.id}`) }}
          </h3>
          <div class="card-grid">
            <button
              v-for="subType in cat.subTypes"
              :key="subType"
              type="button"
              class="type-card"
              :class="{ 'type-card--selected': highlightedSubType === subType }"
              @click="handleCardClick(subType)"
            >
              <div class="type-card-preview">
                <FieldTypeCardPreview :schema-sub-type="subType" />
              </div>
              <div class="type-card-body">
                <div class="type-card-title">{{ labelFor(subType) }}</div>
                <p class="type-card-desc">{{ t(fieldTypeHelpKey(subType)) }}</p>
              </div>
            </button>
          </div>
        </section>
      </div>
    </div>
    <template #footer>
      <div class="picker-footer">
        <CancelButton @click="handleCancel" />
        <a-button
          type="primary"
          class="btn-primary"
          :disabled="!highlightedSubType"
          @click="handleNext"
        >
          {{ t('admin.cardType.fieldConfig.nextStep') }}
        </a-button>
      </div>
    </template>
  </a-modal>
</template>

<style scoped>
.picker-shell {
  display: flex;
  gap: 0;
  min-height: 300px;
  max-height: min(460px, 68vh);
  margin: -8px -4px 0;
}

.picker-nav {
  flex: 0 0 144px;
  padding: 6px 10px 6px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  text-align: left;
  padding: 8px 10px;
  border: none;
  border-radius: var(--radius-md, 6px);
  background: transparent;
  font-size: 13px;
  color: var(--color-text-2);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-family: inherit;
}

.nav-item:hover {
  background: var(--color-fill-2);
  color: var(--color-text-1);
}

.nav-item--active {
  background: rgb(var(--primary-1));
  color: rgb(var(--primary-6));
  font-weight: 500;
}

.picker-scroll {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 6px 0 6px 14px;
}

.picker-section {
  margin-bottom: 20px;
}

.picker-section:last-child {
  margin-bottom: 8px;
}

.section-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 0;
  border: 1px solid var(--color-border-2);
  border-radius: var(--radius-md, 6px);
  background: var(--color-bg-2);
  cursor: pointer;
  text-align: left;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
  font-family: inherit;
}

.type-card:hover {
  border-color: rgb(var(--primary-4));
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.type-card--selected {
  border-color: rgb(var(--primary-6));
  background: rgb(var(--primary-1));
  box-shadow: 0 0 0 1px rgb(var(--primary-6));
}

.type-card-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 76px;
  padding: 4px 0 6px;
  background: rgb(var(--primary-1));
}

.type-card--selected .type-card-preview {
  background: rgb(var(--primary-2));
}

.type-card-body {
  padding: 8px 10px 10px;
}

.type-card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-1);
  margin-bottom: 4px;
}

.type-card-desc {
  margin: 0;
  font-size: 11px;
  line-height: 1.45;
  color: var(--color-text-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.picker-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

<style>
.select-field-type-modal-wrap .arco-modal {
  border-radius: var(--radius-xl, 12px);
}

.select-field-type-modal-wrap .arco-modal-header {
  border-bottom: none;
}

.select-field-type-modal-wrap .arco-modal-footer {
  border-top: none;
}
</style>
