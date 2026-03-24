<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import { cascadeRelationApi } from '@/api'
import CreateButton from '@/components/common/CreateButton.vue'
import CascadeRelationBranch from '@/components/cascade-relation/CascadeRelationBranch.vue'
import CascadeRelationEditDrawer from '@/components/cascade-relation/CascadeRelationEditDrawer.vue'
import { handleReferenceConflictError } from '@/utils/error-handler'
import type { CascadeRelationDefinition } from '@/types/cascade-relation'

const { t } = useI18n()

const loading = ref(false)
const cascadeRelations = ref<CascadeRelationDefinition[]>([])

const drawerVisible = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const editingCascadeRelation = ref<CascadeRelationDefinition | null>(null)

const sortedCascadeRelations = computed(() => {
  return [...cascadeRelations.value].sort((a, b) => {
    const timeA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0
    const timeB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0
    return timeB - timeA
  })
})

async function fetchData() {
  loading.value = true
  try {
    const result = await cascadeRelationApi.list()
    cascadeRelations.value = result.content || []
  } catch (error) {
    console.error('Failed to fetch cascade relations:', error)
    Message.error(t('admin.cascadeRelation.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  drawerMode.value = 'create'
  editingCascadeRelation.value = null
  drawerVisible.value = true
}

function handleEdit(def: CascadeRelationDefinition) {
  drawerMode.value = 'edit'
  editingCascadeRelation.value = def
  drawerVisible.value = true
}

async function handleDelete(def: CascadeRelationDefinition) {
  if (!def.id) return

  Modal.confirm({
    title: t('admin.cascadeRelation.confirmDelete'),
    content: t('admin.cascadeRelation.deleteConfirmContent', { name: def.name }),
    okText: t('common.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await cascadeRelationApi.delete(def.id!)
        Message.success(t('admin.cascadeRelation.deleteSuccess'))
        await fetchData()
      } catch (error) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete:', error)
          Message.error(t('admin.cascadeRelation.deleteFailed'))
        }
      }
    },
  })
}

async function handleSaveSuccess() {
  drawerVisible.value = false
  await fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="cascade-relation-graph-page">
    <div v-if="cascadeRelations.length > 0" class="page-toolbar">
      <CreateButton @click="handleCreate">
        {{ t('admin.cascadeRelation.createButton') }}
      </CreateButton>
    </div>

    <div class="graph-container">
      <a-spin :loading="loading" class="graph-spin">
        <div v-if="cascadeRelations.length > 0" class="branch-list">
          <CascadeRelationBranch
            v-for="def in sortedCascadeRelations"
            :key="def.id"
            :cascade-relation="def"
            @click="handleEdit(def)"
            @delete="handleDelete(def)"
          />
        </div>

        <div v-else-if="!loading" class="empty-state">
          <a-empty :description="t('admin.cascadeRelation.emptyDescription')">
            <template #image>
              <div class="empty-icon">
                <svg viewBox="0 0 100 100" width="80" height="80">
                  <circle cx="50" cy="80" r="8" fill="#e5e6eb" />
                  <line x1="50" y1="72" x2="50" y2="40" stroke="#e5e6eb" stroke-width="2" />
                  <circle cx="50" cy="35" r="5" fill="#e5e6eb" />
                  <line x1="50" y1="30" x2="30" y2="15" stroke="#e5e6eb" stroke-width="2" />
                  <line x1="50" y1="30" x2="70" y2="15" stroke="#e5e6eb" stroke-width="2" />
                  <circle cx="30" cy="12" r="4" fill="#e5e6eb" />
                  <circle cx="70" cy="12" r="4" fill="#e5e6eb" />
                </svg>
              </div>
            </template>
            <CreateButton @click="handleCreate">
              {{ t('admin.cascadeRelation.createButton') }}
            </CreateButton>
          </a-empty>
        </div>
      </a-spin>
    </div>

    <CascadeRelationEditDrawer
      v-model:visible="drawerVisible"
      :mode="drawerMode"
      :cascade-relation="editingCascadeRelation"
      @success="handleSaveSuccess"
    />
  </div>
</template>

<style scoped>
.cascade-relation-graph-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-bg-1);
}

.page-toolbar {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 16px 24px 0;
}

.graph-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  min-height: 0;
}

.graph-spin {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.graph-spin :deep(.arco-spin) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.graph-spin :deep(.arco-spin-children) {
  height: 100%;
  overflow: auto;
}

.branch-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 24px 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  padding: 24px;
}

.empty-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
</style>
