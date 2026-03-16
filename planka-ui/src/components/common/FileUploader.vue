<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { ossApi } from '@/api/oss'
import { FileCategory, type FileDTO, type UploadProgress } from '@/types/oss'
import { useOrgStore } from '@/stores/org'

const { t } = useI18n()
const orgStore = useOrgStore()

const props = withDefaults(
  defineProps<{
    category?: FileCategory
    accept?: string
    maxSize?: number
    multiple?: boolean
    maxCount?: number
    disabled?: boolean
    showFileList?: boolean
  }>(),
  {
    category: FileCategory.ATTACHMENT,
    accept: '*/*',
    maxSize: 100,
    multiple: false,
    maxCount: 10,
    disabled: false,
    showFileList: true,
  }
)

const emit = defineEmits<{
  (e: 'success', files: FileDTO[]): void
  (e: 'error', error: Error): void
  (e: 'progress', progress: UploadProgress): void
}>()

const fileInputRef = ref<HTMLInputElement>()
const uploading = ref(false)
const uploadedFiles = ref<FileDTO[]>([])
const progress = ref<UploadProgress>({ loaded: 0, total: 0, percent: 0 })

const acceptTypes = computed(() => props.accept)

const triggerFileSelect = () => {
  if (props.disabled || uploading.value) return
  fileInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])

  if (files.length === 0) return

  if (files.length > props.maxCount) {
    Message.warning(t('oss.upload.maxCount', { count: props.maxCount }))
    return
  }

  for (const file of files) {
    if (file.size > props.maxSize * 1024 * 1024) {
      Message.warning(t('oss.upload.fileTooLarge', { size: props.maxSize }))
      return
    }
  }

  uploading.value = true
  progress.value = { loaded: 0, total: 0, percent: 0 }

  try {
    const results: FileDTO[] = []
    for (const file of files) {
      const result = await ossApi.upload(
        file,
        orgStore.currentOrgId || '',
        orgStore.currentMemberCardId || '',
        props.category,
        (p) => {
          progress.value = p
          emit('progress', p)
        }
      )
      results.push(result)
    }

    uploadedFiles.value.push(...results)
    Message.success(t('oss.upload.success'))
    emit('success', results)
  } catch (error) {
    Message.error(t('oss.upload.failed'))
    emit('error', error as Error)
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const removeFile = (index: number) => {
  uploadedFiles.value.splice(index, 1)
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

defineExpose({
  triggerFileSelect,
  uploadedFiles,
})
</script>

<template>
  <div class="file-uploader">
    <input
      ref="fileInputRef"
      type="file"
      :accept="acceptTypes"
      :multiple="multiple"
      style="display: none"
      @change="handleFileChange"
    />

    <div class="upload-trigger" :class="{ disabled, uploading }" @click="triggerFileSelect">
      <slot>
        <div class="default-trigger">
          <icon-upload v-if="!uploading" />
          <icon-loading v-else class="loading-icon" />
          <span class="trigger-text">
            {{ uploading ? t('oss.upload.uploading') : t('oss.upload.dragTip') }}
          </span>
        </div>
      </slot>
    </div>

    <div v-if="uploading" class="upload-progress">
      <a-progress :percent="progress.percent" />
    </div>

    <div v-if="showFileList && uploadedFiles.length > 0" class="file-list">
      <div v-for="(file, index) in uploadedFiles" :key="file.id" class="file-item">
        <icon-file class="file-icon" />
        <span class="file-name" :title="file.originalName">{{ file.originalName }}</span>
        <span class="file-size">{{ formatFileSize(file.size) }}</span>
        <icon-close class="remove-icon" @click="removeFile(index)" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.file-uploader {
  width: 100%;
}

.upload-trigger {
  border: 1px dashed var(--color-border-3);
  border-radius: 4px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--color-fill-1);
}

.upload-trigger:hover:not(.disabled):not(.uploading) {
  border-color: rgb(var(--primary-6));
  background: var(--color-fill-2);
}

.upload-trigger.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.upload-trigger.uploading {
  cursor: wait;
}

.default-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--color-text-3);
}

.default-trigger :deep(svg) {
  font-size: 32px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.trigger-text {
  font-size: 14px;
}

.upload-progress {
  margin-top: 12px;
}

.file-list {
  margin-top: 12px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--color-fill-1);
  border-radius: 4px;
  margin-bottom: 4px;
}

.file-icon {
  color: var(--color-text-3);
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.file-size {
  color: var(--color-text-3);
  font-size: 12px;
}

.remove-icon {
  cursor: pointer;
  color: var(--color-text-3);
  transition: color 0.2s;
}

.remove-icon:hover {
  color: rgb(var(--danger-6));
}
</style>
