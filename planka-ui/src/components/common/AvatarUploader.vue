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
    modelValue?: string
    size?: number
    disabled?: boolean
    maxFileSize?: number
  }>(),
  {
    modelValue: '',
    size: 80,
    disabled: false,
    maxFileSize: 5,
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'success', file: FileDTO): void
  (e: 'error', error: Error): void
}>()

const fileInputRef = ref<HTMLInputElement>()
const uploading = ref(false)
const progress = ref<UploadProgress>({ loaded: 0, total: 0, percent: 0 })

const avatarUrl = computed(() => props.modelValue)

const avatarStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${props.size / 2.5}px`,
}))

const triggerFileSelect = () => {
  if (props.disabled || uploading.value) return
  fileInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) return

  if (!file.type.startsWith('image/')) {
    Message.warning(t('oss.upload.fileTypeNotAllowed'))
    return
  }

  if (file.size > props.maxFileSize * 1024 * 1024) {
    Message.warning(t('oss.upload.fileTooLarge', { size: props.maxFileSize }))
    return
  }

  uploading.value = true

  try {
    const result = await ossApi.upload(
      file,
      orgStore.currentOrgId || '',
      orgStore.currentMemberCardId || '',
      FileCategory.AVATAR,
      (p) => {
        progress.value = p
      }
    )

    emit('update:modelValue', result.url)
    emit('success', result)
    Message.success(t('oss.upload.success'))
  } catch (error) {
    Message.error(t('oss.upload.failed'))
    emit('error', error as Error)
  } finally {
    uploading.value = false
    input.value = ''
  }
}

defineExpose({
  triggerFileSelect,
})
</script>

<template>
  <div class="avatar-uploader" :class="{ disabled, uploading }">
    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleFileChange"
    />

    <div class="avatar-container" :style="avatarStyle" @click="triggerFileSelect">
      <img v-if="avatarUrl" :src="avatarUrl" alt="avatar" class="avatar-image" />
      <div v-else class="avatar-placeholder">
        <icon-user />
      </div>

      <div class="avatar-overlay">
        <template v-if="uploading">
          <icon-loading class="loading-icon" />
        </template>
        <template v-else>
          <icon-camera class="camera-icon" />
        </template>
      </div>
    </div>

    <div v-if="uploading" class="upload-progress">
      <a-progress type="circle" :percent="progress.percent" size="small" />
    </div>
  </div>
</template>

<style scoped>
.avatar-uploader {
  display: inline-block;
  position: relative;
}

.avatar-container {
  position: relative;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  background: var(--color-fill-2);
}

.avatar-uploader.disabled .avatar-container {
  cursor: not-allowed;
}

.avatar-uploader.uploading .avatar-container {
  cursor: wait;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-3);
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.avatar-uploader.uploading .avatar-overlay {
  opacity: 1;
  background: rgba(0, 0, 0, 0.6);
}

.camera-icon {
  font-size: 20px;
  color: white;
}

.loading-icon {
  font-size: 20px;
  color: white;
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

.upload-progress {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
</style>
