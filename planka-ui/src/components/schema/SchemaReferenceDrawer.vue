<script setup lang="ts">
import { ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconInfoCircle } from '@arco-design/web-vue/es/icon'
import { schemaApi } from '@/api'
import SchemaLink from '@/components/schema/SchemaLink.vue'
import type { SchemaReferenceSummaryDTO } from '@/types/schema'
import { SchemaTypeConfig, SchemaType } from '@/types/schema'

interface Props {
  visible: boolean
  schemaId?: string
}

interface Emits {
  (e: 'update:visible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const referenceSummary = ref<SchemaReferenceSummaryDTO | null>(null)

// 监听 schemaId 变化，自动加载引用关系
watch(() => props.schemaId, async (newId) => {
  if (newId && props.visible) {
    await fetchReferenceSummary(newId)
  }
}, { immediate: true })

// 监听 visible 变化
watch(() => props.visible, async (newVisible) => {
  if (newVisible && props.schemaId) {
    await fetchReferenceSummary(props.schemaId)
  } else if (!newVisible) {
    // 关闭时清空数据
    referenceSummary.value = null
  }
})

// 获取引用关系摘要
async function fetchReferenceSummary(schemaId: string) {
  loading.value = true
  try {
    referenceSummary.value = await schemaApi.getReferenceSummary(schemaId)
  } catch (error) {
    console.error('Failed to fetch reference summary:', error)
    Message.error('获取引用关系失败')
  } finally {
    loading.value = false
  }
}

// 获取Schema类型标签
function getSchemaTypeLabel(type: string): string {
  return SchemaTypeConfig[type as SchemaType]?.label || type
}

// 获取引用类型标签
function getReferenceTypeLabel(type: string): string {
  return type === 'COMPOSITION' ? '组合' : '聚合'
}

// 关闭抽屉
function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <a-drawer
    :visible="visible"
    title="引用关系"
    :width="800"
    unmount-on-close
    @cancel="handleClose"
  >
    <a-spin :loading="loading">
      <template v-if="referenceSummary">
        <div class="reference-header">
          <a-tag color="blue">
            {{ getSchemaTypeLabel(referenceSummary.schemaType) }} - {{ referenceSummary.schemaName }}
          </a-tag>
        </div>

        <div class="reference-section">
          <h4>被以下定义引用</h4>
          <a-table
            v-if="referenceSummary.incoming.length > 0"
            :data="referenceSummary.incoming"
            :pagination="false"
            :bordered="false"
            :table-layout-fixed="true"
            row-key="schemaId"
            size="small"
          >
            <template #columns>
              <a-table-column title="定义名称">
                <template #cell="{ record }">
                  <SchemaLink
                    :schema-id="record.schemaId"
                    :schema-type="record.schemaType"
                    :name="record.schemaName"
                  />
                </template>
              </a-table-column>
              <a-table-column title="定义类型" :width="180">
                <template #cell="{ record }">
                  <a-tag size="small">{{ getSchemaTypeLabel(record.schemaType) }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :width="120">
                <template #title>
                  <span>引用类型</span>
                  <a-tooltip position="top" :content-style="{ width: '320px' }">
                    <IconInfoCircle class="reference-type-help-icon" />
                    <template #content>
                      <div class="reference-type-help">
                        <div class="help-item">
                          <div class="help-label">组合（COMPOSITION）</div>
                          <div class="help-desc">
                            强依赖关系，部分不能脱离整体独立存在。当整体被删除时，部分也应该被删除。
                          </div>
                          <div class="help-example">例如：卡片详情模板依赖于卡片类型</div>
                        </div>
                        <div class="help-item">
                          <div class="help-label">聚合（AGGREGATION）</div>
                          <div class="help-desc">
                            弱依赖关系，部分可以独立存在。当存在聚合引用时，无法删除被引用的对象，需先解除引用关系。
                          </div>
                          <div class="help-example">例如：视图聚合引用卡片类型，必须先删除视图或解除引用，才能删除卡片类型</div>
                        </div>
                      </div>
                    </template>
                  </a-tooltip>
                </template>
                <template #cell="{ record }">
                  {{ getReferenceTypeLabel(record.referenceType) }}
                </template>
              </a-table-column>
            </template>
          </a-table>
          <a-empty v-else description="暂无引用关系" />
        </div>

        <div class="reference-section">
          <h4>引用了以下定义</h4>
          <a-table
            v-if="referenceSummary.outgoing.length > 0"
            :data="referenceSummary.outgoing"
            :pagination="false"
            :bordered="false"
            :table-layout-fixed="true"
            row-key="schemaId"
            size="small"
          >
            <template #columns>
              <a-table-column title="定义名称">
                <template #cell="{ record }">
                  <SchemaLink
                    :schema-id="record.schemaId"
                    :schema-type="record.schemaType"
                    :name="record.schemaName"
                  />
                </template>
              </a-table-column>
              <a-table-column title="定义类型" :width="180">
                <template #cell="{ record }">
                  <a-tag size="small">{{ getSchemaTypeLabel(record.schemaType) }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :width="120">
                <template #title>
                  <span>引用类型</span>
                  <a-tooltip position="top" :content-style="{ width: '320px' }">
                    <IconInfoCircle class="reference-type-help-icon" />
                    <template #content>
                      <div class="reference-type-help">
                        <div class="help-item">
                          <div class="help-label">组合（COMPOSITION）</div>
                          <div class="help-desc">
                            强依赖关系，部分不能脱离整体独立存在。当整体被删除时，部分也应该被删除。
                          </div>
                          <div class="help-example">例如：卡片详情模板依赖于卡片类型</div>
                        </div>
                        <div class="help-item">
                          <div class="help-label">聚合（AGGREGATION）</div>
                          <div class="help-desc">
                            弱依赖关系，部分可以独立存在。当存在聚合引用时，无法删除被引用的对象，需先解除引用关系。
                          </div>
                          <div class="help-example">例如：视图聚合引用卡片类型，必须先删除视图或解除引用，才能删除卡片类型</div>
                        </div>
                      </div>
                    </template>
                  </a-tooltip>
                </template>
                <template #cell="{ record }">
                  {{ getReferenceTypeLabel(record.referenceType) }}
                </template>
              </a-table-column>
            </template>
          </a-table>
          <a-empty v-else description="暂无引用关系" />
        </div>
      </template>
    </a-spin>
  </a-drawer>
</template>

<style scoped>
.reference-header {
  margin-bottom: 16px;
  text-align: left;
}

.reference-section {
  margin-bottom: 24px;
}

.reference-section:last-child {
  margin-bottom: 0;
}

.reference-section h4 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-2);
}

/* 引用关系表格样式 */
.reference-section :deep(.arco-table) {
  width: 100% !important;
}

.reference-section :deep(.arco-table-container) {
  width: 100% !important;
}

.reference-section :deep(.arco-table-element) {
  width: 100% !important;
}

.reference-section :deep(.arco-table-content) {
  width: 100% !important;
}

.reference-section :deep(.arco-table-body) {
  width: 100% !important;
}

.reference-section :deep(table) {
  width: 100% !important;
  table-layout: fixed !important;
}

.reference-section :deep(.arco-link) {
  font-weight: 500;
}

/* 引用类型帮助图标 */
.reference-type-help-icon {
  margin-left: 4px;
  font-size: 13px;
  color: rgb(var(--primary-6));
  cursor: help;
  transition: color 0.2s;
}

.reference-type-help-icon:hover {
  color: rgb(var(--primary-5));
}

/* 引用类型帮助内容 */
.reference-type-help {
  font-size: 12px;
  line-height: 1.6;
}

.reference-type-help .help-item {
  margin-bottom: 12px;
}

.reference-type-help .help-item:last-child {
  margin-bottom: 0;
}

.reference-type-help .help-label {
  font-weight: 600;
  color: var(--color-text-1);
  margin-bottom: 4px;
}

.reference-type-help .help-desc {
  color: var(--color-text-2);
  margin-bottom: 4px;
}

.reference-type-help .help-example {
  font-size: 11px;
  color: var(--color-text-3);
  padding: 4px 8px;
  background: var(--color-fill-1);
  border-radius: 3px;
  border-left: 2px solid rgb(var(--primary-6));
}
</style>
