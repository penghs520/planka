<script setup lang="ts">
import { computed } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
import { schemaApi } from '@/api'
import type { SchemaChangelogDTO } from '@/types/schema'

interface Props {
  visible: boolean
  changelog?: SchemaChangelogDTO | null
  /** 当前 Schema 的最新版本号 */
  latestVersion?: number
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'restored'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()

// 解析后的 JSON 数据
const beforeData = computed(() => {
  if (!props.changelog?.beforeSnapshot) return null
  try {
    return JSON.parse(props.changelog.beforeSnapshot)
  } catch {
    return null
  }
})

const afterData = computed(() => {
  if (!props.changelog?.afterSnapshot) return null
  try {
    return JSON.parse(props.changelog.afterSnapshot)
  } catch {
    return null
  }
})

// 格式化的 JSON 字符串（用于差异视图）
const beforeJson = computed(() => {
  if (!beforeData.value) return ''
  return JSON.stringify(beforeData.value, null, 2)
})

const afterJson = computed(() => {
  if (!afterData.value) return ''
  return JSON.stringify(afterData.value, null, 2)
})

// 抽屉标题
const drawerTitle = computed(() => {
  if (props.changelog?.contentVersion) {
    return t('admin.changelog.snapshot.titleWithVersion', { version: props.changelog.contentVersion })
  }
  return t('admin.changelog.snapshot.title')
})

// 是否有快照数据
const hasSnapshot = computed(() => {
  return props.changelog?.beforeSnapshot || props.changelog?.afterSnapshot
})

// 计算差异
interface DiffLine {
  type: 'added' | 'removed' | 'unchanged'
  content: string
  lineNumber?: number
}

const diffLines = computed((): { before: DiffLine[]; after: DiffLine[] } => {
  const beforeLines = beforeJson.value.split('\n')
  const afterLines = afterJson.value.split('\n')

  // 简单的逐行对比
  const maxLen = Math.max(beforeLines.length, afterLines.length)
  const before: DiffLine[] = []
  const after: DiffLine[] = []

  for (let i = 0; i < maxLen; i++) {
    const bLine = beforeLines[i] ?? ''
    const aLine = afterLines[i] ?? ''

    if (bLine === aLine) {
      before.push({ type: 'unchanged', content: bLine, lineNumber: i + 1 })
      after.push({ type: 'unchanged', content: aLine, lineNumber: i + 1 })
    } else {
      if (bLine) {
        before.push({ type: 'removed', content: bLine, lineNumber: i + 1 })
      }
      if (aLine) {
        after.push({ type: 'added', content: aLine, lineNumber: i + 1 })
      }
    }
  }

  return { before, after }
})

// 复制 JSON
async function copyJson(type: 'before' | 'after') {
  const json = type === 'before' ? beforeJson.value : afterJson.value
  if (!json) return

  try {
    await navigator.clipboard.writeText(json)
    Message.success(t('admin.changelog.snapshot.copySuccess'))
  } catch {
    Message.error(t('admin.changelog.snapshot.copyFailed'))
  }
}

// 关闭抽屉
function handleClose() {
  emit('update:visible', false)
}

// 是否可以还原（有 afterSnapshot 且不是最新版本才能还原）
const canRestore = computed(() => {
  if (!props.changelog?.afterSnapshot) return false
  // 如果传入了最新版本号，检查当前版本是否是最新版本
  if (props.latestVersion !== undefined) {
    return props.changelog.contentVersion < props.latestVersion
  }
  return true
})

// 还原至此版本
function handleRestore() {
  if (!props.changelog) return

  Modal.confirm({
    title: t('admin.changelog.snapshot.restoreConfirmTitle'),
    content: t('admin.changelog.snapshot.restoreConfirmContent', { version: props.changelog.contentVersion }),
    okText: t('admin.changelog.snapshot.restoreConfirm'),
    cancelText: t('admin.action.cancel'),
    okButtonProps: { status: 'warning' },
    onOk: async () => {
      try {
        await schemaApi.restoreToVersion(props.changelog!.schemaId, props.changelog!.id)
        Message.success(t('admin.changelog.snapshot.restoreSuccess'))
        emit('restored')
        handleClose()
      } catch (error) {
        console.error('Failed to restore:', error)
        Message.error(t('admin.changelog.snapshot.restoreFailed'))
      }
    },
  })
}
</script>

<template>
  <a-drawer
    :visible="visible"
    :title="drawerTitle"
    :width="900"
    :footer="canRestore ? undefined : false"
    :body-style="{ padding: '16px', height: 'calc(100vh - 60px)', overflow: 'hidden' }"
    unmount-on-close
    @cancel="handleClose"
  >
    <!-- 底部操作按钮 -->
    <template v-if="canRestore" #footer>
      <div class="drawer-footer">
        <a-button
          type="primary"
          status="warning"
          @click="handleRestore"
        >
          {{ t('admin.changelog.snapshot.restoreToVersion') }}
        </a-button>
      </div>
    </template>

    <div v-if="hasSnapshot" class="snapshot-container">
      <!-- 差异视图 -->
      <div class="diff-view">
        <div class="diff-panel">
          <div class="diff-header">
            <span class="diff-title">{{ t('admin.changelog.snapshot.before') }}</span>
            <a-button
              v-if="beforeJson"
              type="text"
              size="mini"
              @click="copyJson('before')"
            >
              {{ t('admin.changelog.snapshot.copyJson') }}
            </a-button>
          </div>
          <div class="diff-content">
            <template v-if="beforeJson">
              <div
                v-for="(line, index) in diffLines.before"
                :key="`before-${index}`"
                class="diff-line"
                :class="line.type"
              >
                <span class="line-number">{{ line.lineNumber }}</span>
                <pre class="line-content">{{ line.content }}</pre>
              </div>
            </template>
            <div v-else class="no-data">
              {{ t('admin.changelog.snapshot.noBeforeSnapshot') }}
            </div>
          </div>
        </div>

        <div class="diff-panel">
          <div class="diff-header">
            <span class="diff-title">{{ t('admin.changelog.snapshot.after') }}</span>
            <a-button
              v-if="afterJson"
              type="text"
              size="mini"
              @click="copyJson('after')"
            >
              {{ t('admin.changelog.snapshot.copyJson') }}
            </a-button>
          </div>
          <div class="diff-content">
            <template v-if="afterJson">
              <div
                v-for="(line, index) in diffLines.after"
                :key="`after-${index}`"
                class="diff-line"
                :class="line.type"
              >
                <span class="line-number">{{ line.lineNumber }}</span>
                <pre class="line-content">{{ line.content }}</pre>
              </div>
            </template>
            <div v-else class="no-data">
              {{ t('admin.changelog.snapshot.noAfterSnapshot') }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 无快照数据 -->
    <a-empty v-else :description="t('admin.changelog.snapshot.noSnapshot')" />
  </a-drawer>
</template>

<style scoped>
.snapshot-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 差异视图 */
.diff-view {
  flex: 1;
  display: flex;
  gap: 16px;
  overflow: hidden;
}

.diff-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  overflow: hidden;
}

.diff-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--color-fill-2);
  border-bottom: 1px solid var(--color-border-2);
}

.diff-title {
  font-weight: 500;
  color: var(--color-text-1);
}

.diff-content {
  flex: 1;
  overflow: auto;
  background: var(--color-bg-2);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
}

.diff-line {
  display: flex;
  line-height: 1.6;
  min-height: 20px;
}

.diff-line.added {
  background: rgba(var(--green-6), 0.25);
  border-left: 3px solid rgb(var(--green-6));
}

.diff-line.removed {
  background: rgba(var(--red-6), 0.25);
  border-left: 3px solid rgb(var(--red-6));
}

.line-number {
  width: 40px;
  padding: 0 8px;
  text-align: right;
  color: var(--color-text-4);
  background: var(--color-fill-1);
  border-right: 1px solid var(--color-border-2);
  user-select: none;
  flex-shrink: 0;
}

.line-content {
  flex: 1;
  margin: 0;
  padding: 0 8px;
  white-space: pre;
  overflow-x: auto;
}

.no-data {
  padding: 40px;
  text-align: center;
  color: var(--color-text-3);
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
