<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconEdit,
  IconDelete,
  IconCheckCircle,
  IconMinusCircle,
  IconLink,
  IconHistory,
  IconTag,
  IconFile,
} from '@arco-design/web-vue/es/icon'
import { cardTypeApi } from '@/api'
import CreateButton from '@/components/common/CreateButton.vue'
import SchemaReferenceDrawer from '@/components/schema/SchemaReferenceDrawer.vue'
import SchemaChangelogDrawer from '@/components/schema/SchemaChangelogDrawer.vue'
import CardTypeFormDrawer from './CardTypeFormDrawer.vue'
import { handleReferenceConflictError } from '@/utils/error-handler'
import type { CardTypeDefinition } from '@/types/card-type'
import { SchemaSubType } from '@/types/schema'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

// 列表状态
const loading = ref(false)
const cardTypes = ref<CardTypeDefinition[]>([])
const searchKeyword = ref('')

// 表单抽屉状态
const drawerVisible = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const editingCardType = ref<CardTypeDefinition | null>(null)

// 引用关系抽屉状态
const referenceDrawerVisible = ref(false)
const currentReferenceSchemaId = ref<string | undefined>(undefined)

// 变更日志抽屉状态
const changelogDrawerVisible = ref(false)
const currentChangelogSchemaId = ref<string | undefined>(undefined)
const currentChangelogSchemaName = ref<string | undefined>(undefined)

// 搜索过滤
const filteredCardTypes = computed(() => {
  if (!searchKeyword.value) return cardTypes.value
  const keyword = searchKeyword.value.toLowerCase()
  return cardTypes.value.filter(
    (ct) =>
      ct.name.toLowerCase().includes(keyword) ||
      ct.code?.toLowerCase().includes(keyword)
  )
})

// 分组数据
const traitTypes = computed(() => {
  return filteredCardTypes.value.filter(
    (ct) => ct.schemaSubType === SchemaSubType.TRAIT_CARD_TYPE
  )
})

const entityTypes = computed(() => {
  return filteredCardTypes.value.filter(
    (ct) => ct.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE
  )
})

/** 与 a-col :xs="24" :sm="12" :md="8" :lg="6" 对齐的列数，用于瀑布流分列 */
const { width: windowWidth } = useWindowSize()
const entityMasonryColumnCount = computed(() => {
  const w = windowWidth.value || 1200
  if (w >= 992) return 4
  if (w >= 768) return 3
  if (w >= 576) return 2
  return 1
})

/** 按列轮询分配，每列独立纵向堆叠，实现不等高卡片的向上对齐（瀑布流） */
const entityTypeColumns = computed(() => {
  const list = entityTypes.value
  const n = entityMasonryColumnCount.value
  const cols: CardTypeDefinition[][] = Array.from({ length: n }, () => [])
  list.forEach((ct, i) => {
    cols[i % n]!.push(ct)
  })
  return cols
})

// 数据加载
async function fetchData() {
  if (loading.value) return
  loading.value = true
  try {
    cardTypes.value = await cardTypeApi.listAll()
  } catch (error) {
    console.error('Failed to fetch card types:', error)
  } finally {
    loading.value = false
  }
}

// 打开新建抽屉
function handleCreate() {
  drawerMode.value = 'create'
  editingCardType.value = null
  drawerVisible.value = true
}

// 打开编辑抽屉
function handleEdit(cardType: CardTypeDefinition) {
  drawerMode.value = 'edit'
  editingCardType.value = cardType
  drawerVisible.value = true
}

// 删除实体类型
async function handleDelete(cardType: CardTypeDefinition) {
  Modal.confirm({
    title: t('admin.message.confirmDelete'),
    content: t('admin.message.deleteConfirmContent', {
      type: t('admin.cardType.title'),
      name: cardType.name,
    }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await cardTypeApi.delete(cardType.id!)
        Message.success(t('admin.message.deleteSuccess'))
        await fetchData()
      } catch (error) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete:', error)
        }
      }
    },
  })
}

// 启用/停用
async function handleToggleEnabled(cardType: CardTypeDefinition) {
  try {
    if (cardType.enabled) {
      await cardTypeApi.disable(cardType.id!)
      Message.success(t('admin.message.disableSuccess'))
    } else {
      await cardTypeApi.activate(cardType.id!)
      Message.success(t('admin.message.enableSuccess'))
    }
    await fetchData()
  } catch (error) {
    console.error('Failed to toggle enabled:', error)
  }
}

// 打开引用关系抽屉
function handleReference(cardType: CardTypeDefinition) {
  if (!cardType.id) return
  currentReferenceSchemaId.value = cardType.id
  referenceDrawerVisible.value = true
}

// 打开变更日志抽屉
function handleChangelog(cardType: CardTypeDefinition) {
  if (!cardType.id) return
  currentChangelogSchemaId.value = cardType.id
  currentChangelogSchemaName.value = cardType.name
  changelogDrawerVisible.value = true
}

// 表单保存成功
async function handleFormSuccess() {
  await fetchData()
}

// 处理路由参数中的编辑请求
async function handleEditFromRoute() {
  const editId = route.query.edit as string | undefined
  if (editId) {
    router.replace({ path: '/admin/card-type/card', query: {} })
    const cardType = cardTypes.value.find((ct) => ct.id === editId)
    if (cardType) {
      handleEdit(cardType)
    } else {
      handleEdit({ id: editId, name: '' } as CardTypeDefinition)
    }
  }
}

onMounted(async () => {
  await fetchData()
  await handleEditFromRoute()
})
</script>

<template>
  <div class="card-view-page">
    <div class="card-view-content">
      <div class="content-header">
        <CreateButton @click="handleCreate">
          {{ t('admin.cardType.createButton') }}
        </CreateButton>
        <div class="header-spacer" />
        <a-input-search
          v-model="searchKeyword"
          :placeholder="t('admin.search.placeholder')"
          style="width: 260px"
          size="small"
          allow-clear
        />
      </div>

      <div class="card-view-container">
        <a-spin :loading="loading" class="loading-wrapper">
          <!-- 特征类型分组 -->
          <div v-if="traitTypes.length > 0" class="card-group">
            <div class="group-header">
              <IconTag class="group-icon trait-color" />
              <span class="group-title">{{ t('admin.cardType.schemaSubType.TRAIT_CARD_TYPE') }}</span>
              <span class="group-count">({{ traitTypes.length }})</span>
            </div>
            <a-row :gutter="[16, 16]" class="card-grid">
              <a-col v-for="ct in traitTypes" :key="ct.id" :xs="24" :sm="12" :md="8" :lg="6">
                <a-card class="type-card trait-card" hoverable @click="handleEdit(ct)">
                  <div class="type-card-header">
                    <div class="type-card-icon trait-bg">
                      <IconTag />
                    </div>
                    <div class="type-card-info">
                      <div class="type-card-name">{{ ct.name }}</div>
                      <div v-if="ct.code" class="type-card-code">{{ ct.code }}</div>
                    </div>
                  </div>
                  <div v-if="ct.description" class="type-card-description">
                    {{ ct.description }}
                  </div>
                  <div class="type-card-tags">
                    <span
                      class="type-meta-status"
                      :class="ct.enabled ? 'type-meta-status--on' : 'type-meta-status--off'"
                    >
                      <span class="type-meta-dot" aria-hidden="true" />
                      {{ ct.enabled ? t('admin.status.enabled') : t('admin.status.disabled') }}
                    </span>
                    <span v-if="ct.systemCardType" class="type-meta-pill type-meta-pill--neutral">
                      {{ t('admin.table.systemBuiltin') }}
                    </span>
                  </div>
                  <div class="type-card-actions" @click.stop>
                    <a-tooltip v-if="!ct.systemCardType" :content="t('admin.action.edit')" mini>
                      <a-button size="mini" type="text" @click="handleEdit(ct)">
                        <template #icon><IconEdit /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="t('admin.action.reference')" mini>
                      <a-button size="mini" type="text" @click="handleReference(ct)">
                        <template #icon><IconLink /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="t('admin.action.changelog')" mini>
                      <a-button size="mini" type="text" @click="handleChangelog(ct)">
                        <template #icon><IconHistory /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="ct.enabled ? t('admin.action.disable') : t('admin.action.enable')" mini>
                      <a-button size="mini" type="text" @click="handleToggleEnabled(ct)">
                        <template #icon>
                          <IconCheckCircle v-if="!ct.enabled" />
                          <IconMinusCircle v-else />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip v-if="!ct.systemCardType" :content="t('admin.action.delete')" mini>
                      <a-button size="mini" type="text" status="danger" @click="handleDelete(ct)">
                        <template #icon><IconDelete /></template>
                      </a-button>
                    </a-tooltip>
                  </div>
                </a-card>
              </a-col>
            </a-row>
          </div>

          <!-- 实体类型分组 -->
          <div v-if="entityTypes.length > 0" class="card-group">
            <div class="group-header">
              <IconFile class="group-icon entity-color" />
              <span class="group-title">{{ t('admin.cardType.schemaSubType.ENTITY_CARD_TYPE') }}</span>
              <span class="group-count">({{ entityTypes.length }})</span>
            </div>
            <div class="entity-masonry">
              <div v-for="(col, ci) in entityTypeColumns" :key="ci" class="entity-masonry-col">
                <a-card
                  v-for="ct in col"
                  :key="ct.id"
                  class="type-card entity-card"
                  hoverable
                  @click="handleEdit(ct)"
                >
                  <div class="type-card-header">
                    <div class="type-card-icon entity-bg">
                      <IconFile />
                    </div>
                    <div class="type-card-info">
                      <div class="type-card-name">{{ ct.name }}</div>
                      <div v-if="ct.code" class="type-card-code">{{ ct.code }}</div>
                    </div>
                  </div>
                  <div v-if="ct.description" class="type-card-description">
                    {{ ct.description }}
                  </div>
                  <div class="type-card-tags">
                    <span
                      class="type-meta-status"
                      :class="ct.enabled ? 'type-meta-status--on' : 'type-meta-status--off'"
                    >
                      <span class="type-meta-dot" aria-hidden="true" />
                      {{ ct.enabled ? t('admin.status.enabled') : t('admin.status.disabled') }}
                    </span>
                    <span v-if="ct.systemCardType" class="type-meta-pill type-meta-pill--neutral">
                      {{ t('admin.table.systemBuiltin') }}
                    </span>
                  </div>
                  <div
                    v-if="'parentTypes' in ct && ct.parentTypes && ct.parentTypes.length > 0"
                    class="type-card-parents"
                  >
                    <span v-for="p in ct.parentTypes" :key="p.id" class="type-meta-pill type-meta-pill--link">
                      {{ p.name }}
                    </span>
                  </div>
                  <div class="type-card-actions" @click.stop>
                    <a-tooltip v-if="!ct.systemCardType" :content="t('admin.action.edit')" mini>
                      <a-button size="mini" type="text" @click="handleEdit(ct)">
                        <template #icon><IconEdit /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="t('admin.action.reference')" mini>
                      <a-button size="mini" type="text" @click="handleReference(ct)">
                        <template #icon><IconLink /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="t('admin.action.changelog')" mini>
                      <a-button size="mini" type="text" @click="handleChangelog(ct)">
                        <template #icon><IconHistory /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="ct.enabled ? t('admin.action.disable') : t('admin.action.enable')" mini>
                      <a-button size="mini" type="text" @click="handleToggleEnabled(ct)">
                        <template #icon>
                          <IconCheckCircle v-if="!ct.enabled" />
                          <IconMinusCircle v-else />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip v-if="!ct.systemCardType" :content="t('admin.action.delete')" mini>
                      <a-button size="mini" type="text" status="danger" @click="handleDelete(ct)">
                        <template #icon><IconDelete /></template>
                      </a-button>
                    </a-tooltip>
                  </div>
                </a-card>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="!loading && traitTypes.length === 0 && entityTypes.length === 0" class="empty-state">
            <a-empty :description="t('admin.cardType.emptyDescription')" />
          </div>
        </a-spin>
      </div>
    </div>

    <!-- 新建/编辑抽屉 -->
    <CardTypeFormDrawer
      v-model:visible="drawerVisible"
      :mode="drawerMode"
      :editing-card-type="editingCardType"
      @success="handleFormSuccess"
    />

    <!-- 引用关系抽屉 -->
    <SchemaReferenceDrawer
      v-model:visible="referenceDrawerVisible"
      :schema-id="currentReferenceSchemaId"
    />

    <!-- 变更日志抽屉 -->
    <SchemaChangelogDrawer
      v-model:visible="changelogDrawerVisible"
      :schema-id="currentChangelogSchemaId"
      :schema-name="currentChangelogSchemaName"
      :include-children="true"
    />
  </div>
</template>

<style scoped>
.card-view-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-view-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-2);
  border-radius: 4px;
  overflow: hidden;
}

.content-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-bottom: 1px solid var(--color-border-1);
}

.header-spacer {
  flex: 1;
}

.card-view-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.loading-wrapper {
  width: 100%;
}

/* 分组 */
.card-group {
  margin-bottom: 24px;
}

.card-group:last-child {
  margin-bottom: 0;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding: 2px 0 10px;
  border-bottom: 1px solid var(--color-border-2);
}

.group-icon {
  font-size: 16px;
}

.trait-color {
  color: #6b4f9e;
}

.entity-color {
  color: var(--color-primary);
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
}

.group-count {
  font-size: 12px;
  color: var(--color-text-3);
}

/* 实体类型：瀑布流（列内向上堆叠，避免整行被最高卡片撑开） */
.entity-masonry {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
}

.entity-masonry-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 卡片：白底 + 左侧类别色条，悬停微抬起 */
.type-card {
  cursor: pointer;
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid var(--color-border-2);
  background: var(--color-bg-1);
  box-shadow: 0 1px 2px rgba(15, 73, 116, 0.06);
  transition:
    border-color 0.2s ease,
    box-shadow 0.22s ease,
    transform 0.22s ease;
}

.trait-card {
  box-shadow:
    inset 2px 0 0 0 rgba(107, 79, 158, 0.28),
    0 1px 2px rgba(15, 73, 116, 0.06);
}

.entity-card {
  box-shadow:
    inset 2px 0 0 0 rgba(0, 131, 224, 0.28),
    0 1px 2px rgba(15, 73, 116, 0.06);
}

.type-card:hover {
  border-color: var(--color-border-3);
  transform: translateY(-2px);
}

.trait-card:hover {
  box-shadow:
    inset 2px 0 0 0 rgba(107, 79, 158, 0.38),
    0 10px 24px rgba(15, 73, 116, 0.09);
}

.entity-card:hover {
  box-shadow:
    inset 2px 0 0 0 rgba(0, 131, 224, 0.38),
    0 10px 24px rgba(15, 73, 116, 0.09);
}

.type-card :deep(.arco-card-body) {
  padding: 12px 14px 10px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.type-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.type-card-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

/* 图标区：与左侧色条同系的浅底 */
.trait-bg {
  background: linear-gradient(165deg, #f5f0ff 0%, #efe8fc 100%);
  color: #574080;
  border: 1px solid rgba(107, 79, 158, 0.1);
}

.entity-bg {
  background: linear-gradient(165deg, #eef6fc 0%, #e5f0fa 100%);
  color: var(--color-primary);
  border: 1px solid rgba(0, 131, 224, 0.1);
}

.type-card-info {
  min-width: 0;
  flex: 1;
}

.type-card-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-card-code {
  font-size: 12px;
  color: var(--color-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.type-card-description {
  font-size: 12px;
  color: var(--color-text-3);
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.type-card-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

/* 启用/停用：圆点 + 文案，避免大块色底 */
.type-meta-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.type-meta-status--on {
  color: var(--color-text-2);
}

.type-meta-status--off {
  color: var(--color-text-3);
}

.type-meta-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--color-text-4);
}

.type-meta-status--on .type-meta-dot {
  background: var(--color-success);
  box-shadow: 0 0 0 2px rgba(52, 199, 89, 0.2);
}

.type-meta-status--off .type-meta-dot {
  background: var(--color-fill-4);
}

/* 系统内置 / 父类型：轻量胶囊 */
.type-meta-pill {
  display: inline-flex;
  align-items: center;
  padding: 0 7px;
  height: 21px;
  font-size: 11px;
  line-height: 1;
  border-radius: 4px;
  font-weight: 500;
  color: var(--color-text-3);
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-2);
}

.type-meta-pill--neutral {
  color: var(--color-text-3);
}

.type-meta-pill--link {
  color: var(--color-primary-active);
  background: rgba(0, 131, 224, 0.05);
  border-color: rgba(0, 131, 224, 0.12);
  font-size: 12px;
  height: 22px;
  padding: 0 8px;
}

.type-card-parents {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.type-card-actions {
  display: flex;
  align-items: center;
  gap: 1px;
  margin-top: 0;
  padding-top: 0;
  border-top: none;
  height: 0;
  padding-bottom: 0;
  overflow: hidden;
  opacity: 0;
  transition:
    height 0.2s,
    padding-top 0.2s,
    margin-top 0.2s,
    opacity 0.2s;
}

.type-card:hover .type-card-actions {
  height: auto;
  margin-top: 4px;
  padding: 2px 0 0;
  border-top: 1px solid var(--color-border-1);
  overflow: visible;
  opacity: 1;
}

/* 悬停条内的图标按钮：尽量贴齐一行高度 */
.type-card-actions :deep(.arco-btn) {
  min-height: 20px;
  height: 20px;
  padding: 0 1px;
  line-height: 1;
}

.type-card-actions :deep(.arco-btn-icon) {
  margin-right: 0;
}

.type-card-actions :deep(.arco-icon) {
  font-size: 13px;
}

/* 空状态 */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}
</style>
