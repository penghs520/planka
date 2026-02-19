<script setup lang="ts">
import { ref, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import { cardCreatePageTemplateApi } from '@/api/card-create-page-template'
import { cardTypeApi } from '@/api/card-type'
import { createEmptyCreatePageTemplate } from '@/types/card-create-page-template'
import type { CardCreatePageTemplateDefinition } from '@/types/card-create-page-template'
import type { FieldConfig } from '@/types/card-type'
import { isBuiltinField } from '@/types/builtin-field'
import { useOrgStore } from '@/stores/org'
import FieldLibrary from '../../card-detail-template/components/FieldLibrary.vue'
import CreatePageCanvas from './CreatePageCanvas.vue'

const props = defineProps<{
  visible: boolean
  templateId?: string
  cardTypeId: string
  cardTypeName?: string
  orgId?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'success'): void
}>()

const orgStore = useOrgStore()

const loading = ref(false)
const saving = ref(false)
const fieldsLoading = ref(false)
const fields = ref<FieldConfig[]>([])
const templateData = ref<CardCreatePageTemplateDefinition | null>(null)

const title = computed(() => props.templateId ? '编辑新建页模板' : '新建新建页模板')

const usedFieldIds = computed(() => {
  if (!templateData.value) return []
  return templateData.value.fieldItems.map(item => item.fieldId)
})

async function loadFields(): Promise<void> {
  if (!props.cardTypeId) return
  fieldsLoading.value = true
  try {
    const result = await cardTypeApi.getFieldConfigsWithSource(props.cardTypeId)
    // 过滤掉系统字段和内置字段，新建页不需要这些字段
    fields.value = result.fields.filter(
      (field) => !field.systemField && !isBuiltinField(field.fieldId)
    )
  } catch (error: any) {
    console.error('Failed to load fields:', error)
    Message.error(error.message || '加载字段失败')
  } finally {
    fieldsLoading.value = false
  }
}

async function loadTemplate(): Promise<void> {
  loading.value = true
  try {
    if (props.templateId) {
      templateData.value = await cardCreatePageTemplateApi.getById(props.templateId)
    } else {
      // 使用实际的 orgId，优先使用 props 传入的，否则使用 store 中的
      const actualOrgId = props.orgId || orgStore.currentOrgId || ''
      templateData.value = createEmptyCreatePageTemplate(actualOrgId, props.cardTypeId)
      templateData.value.name = `新建${props.cardTypeName || '卡片'}模板`
    }
  } catch (error: any) {
    console.error('Failed to load template:', error)
    Message.error(error.message || '加载模板失败')
    handleCancel()
  } finally {
    loading.value = false
  }
}

function handleOpen(): void {
  loadFields()
  loadTemplate()
}

function handleCancel(): void {
  emit('update:visible', false)
}

async function handleSave(): Promise<void> {
  if (!templateData.value) return

  if (!templateData.value.name.trim()) {
    Message.warning('请输入模板名称')
    return
  }

  saving.value = true
  try {
    if (props.templateId) {
      await cardCreatePageTemplateApi.update(
        props.templateId,
        templateData.value,
        templateData.value.contentVersion
      )
      Message.success('保存成功')
    } else {
      await cardCreatePageTemplateApi.create(templateData.value)
      Message.success('创建成功')
    }
    emit('success')
    handleCancel()
  } catch (error: any) {
    console.error('Save failed:', error)
    Message.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-drawer
    :visible="visible"
    :title="title"
    :width="900"
    :mask-closable="true"
    unmount-on-close
    :ok-loading="saving"
    @ok="handleSave"
    @cancel="handleCancel"
    @open="handleOpen"
  >
    <div v-if="templateData" class="drawer-content">
      <div class="config-bar">
         <a-form :model="templateData" layout="inline" size="small">
            <a-form-item label="模板名称" required>
               <a-input v-model="templateData.name" placeholder="请输入模板名称" style="width: 240px" />
            </a-form-item>
            <a-form-item label="默认模板">
               <a-switch v-model="templateData.isDefault" />
            </a-form-item>
         </a-form>
      </div>

      <div class="editor-body">
        <div class="editor-left">
           <FieldLibrary
             :fields="fields"
             :loading="fieldsLoading"
             :used-field-ids="usedFieldIds"
             @refresh="loadFields"
           />
        </div>
        <div class="editor-right">
           <CreatePageCanvas
             v-model="templateData"
             :fields="fields"
           />
        </div>
      </div>
    </div>
    <div v-else class="loading-state">
       <a-spin />
    </div>
  </a-drawer>
</template>

<style scoped lang="scss">
.drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.config-bar {
  padding: 0 0 16px 0;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  margin-top: 16px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
}

.editor-left {
  width: 240px;
  border-right: 1px solid var(--color-border);
  background: var(--color-bg-2);
}

.editor-right {
  flex: 1;
  overflow: hidden;
  background: var(--color-fill-1);
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}
</style>
