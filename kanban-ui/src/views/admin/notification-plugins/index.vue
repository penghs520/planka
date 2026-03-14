<template>
  <div class="notification-plugins-page">
    <!-- 顶部操作栏 -->
    <div class="page-header">
      <h2>{{ t('admin.plugin.title') }}</h2>
      <a-button type="primary" @click="handleUpload">
        <template #icon><icon-upload /></template>
        {{ t('admin.plugin.upload') }}
      </a-button>
    </div>

    <!-- 插件列表 -->
    <a-table
      :columns="columns"
      :data="pluginList"
      :loading="loading"
      :pagination="false"
    >
      <template #pluginState="{ record }">
        <a-tag :color="getStateColor(record.pluginState)">
          {{ getStateText(record.pluginState) }}
        </a-tag>
      </template>

      <template #actions="{ record }">
        <a-space>
          <a-button
            v-if="record.pluginState === 'STOPPED'"
            size="small"
            @click="handleStart(record)"
          >
            {{ t('admin.plugin.start') }}
          </a-button>
          <a-button
            v-else
            size="small"
            @click="handleStop(record)"
          >
            {{ t('admin.plugin.stop') }}
          </a-button>
          <a-button
            size="small"
            status="danger"
            @click="handleDelete(record)"
          >
            {{ t('admin.plugin.delete') }}
          </a-button>
        </a-space>
      </template>
    </a-table>

    <!-- 上传对话框 -->
    <a-modal
      v-model:visible="uploadVisible"
      :title="t('admin.plugin.upload')"
      @ok="handleUploadConfirm"
    >
      <a-upload
        :custom-request="customUpload"
        :file-list="fileList"
        @change="handleFileChange"
      >
        <template #upload-button>
          <a-button>
            <icon-upload />
            {{ t('admin.plugin.selectFile') }}
          </a-button>
        </template>
      </a-upload>
      <a-alert type="info" style="margin-top: 16px">
        {{ t('admin.plugin.uploadTip') }}
      </a-alert>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import { pluginApi } from '@/api/plugin'
import type { PluginInfo } from '@/api/plugin'

const { t } = useI18n()

const loading = ref(false)
const pluginList = ref<PluginInfo[]>([])
const uploadVisible = ref(false)
const fileList = ref([])

const columns = [
  { title: t('admin.plugin.id'), dataIndex: 'pluginId' },
  { title: t('admin.plugin.name'), dataIndex: 'pluginName' },
  { title: t('admin.plugin.version'), dataIndex: 'version' },
  { title: t('admin.plugin.provider'), dataIndex: 'provider' },
  { title: t('admin.plugin.state'), slotName: 'pluginState' },
  { title: t('common.action'), slotName: 'actions', width: 200 }
]

const loadPlugins = async () => {
  loading.value = true
  try {
    const res = await pluginApi.list()
    pluginList.value = res.data
  } catch {
    Message.error(t('admin.plugin.loadError'))
  } finally {
    loading.value = false
  }
}

const handleUpload = () => {
  uploadVisible.value = true
  fileList.value = []
}

const handleFileChange = () => {
  // File change handler
}

const handleUploadConfirm = () => {
  uploadVisible.value = false
}

const customUpload = async (option: any) => {
  const { onSuccess, onError, fileItem } = option
  try {
    await pluginApi.upload(fileItem.file)
    onSuccess()
    Message.success(t('admin.plugin.uploadSuccess'))
    uploadVisible.value = false
    loadPlugins()
  } catch (error) {
    onError(error)
    Message.error(t('admin.plugin.uploadError'))
  }
}

const handleStart = async (record: PluginInfo) => {
  try {
    await pluginApi.start(record.pluginId)
    Message.success(t('admin.plugin.startSuccess'))
    loadPlugins()
  } catch {
    Message.error(t('admin.plugin.startError'))
  }
}

const handleStop = async (record: PluginInfo) => {
  try {
    await pluginApi.stop(record.pluginId)
    Message.success(t('admin.plugin.stopSuccess'))
    loadPlugins()
  } catch {
    Message.error(t('admin.plugin.stopError'))
  }
}

const handleDelete = async (record: PluginInfo) => {
  Modal.confirm({
    title: t('admin.plugin.deleteConfirm'),
    content: t('admin.plugin.deleteConfirmContent', { name: record.pluginName }),
    onOk: async () => {
      try {
        await pluginApi.delete(record.pluginId)
        Message.success(t('admin.plugin.deleteSuccess'))
        loadPlugins()
      } catch {
        Message.error(t('admin.plugin.deleteError'))
      }
    }
  })
}

const getStateColor = (state: string) => {
  return state === 'STARTED' ? 'green' : 'gray'
}

const getStateText = (state: string) => {
  return state === 'STARTED' ? t('admin.plugin.stateStarted') : t('admin.plugin.stateStopped')
}

onMounted(() => {
  loadPlugins()
})
</script>

<style scoped lang="scss">
.notification-plugins-page {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
}
</style>
