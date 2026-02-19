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
    category?: FileCategory
    accept?: string
    maxSize?: number
    disabled?: boolean
    width?: number | string
    height?: number | string
  }>(),
  {
    modelValue: '',
    category: FileCategory.DESCRIPTION_IMAGE,
    accept: 'image/*',
    maxSize: 10,
    disabled: false,
    width: 200,
    height: 200,
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

const imageUrl = computed(() => props.modelValue)

const containerStyle = computed(() => ({
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
}))

const triggerFileSelect = () => {
  if (props.disabled || uploading.value) return
  fileInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) return

  if (file.size > props.maxSize * 1024 * 1024) {
    Message.warning(t('oss.upload.fileTooLarge', { size: props.maxSize }))
    return
  }

  uploading.value = true

  try {
    const result = await ossApi.upload(
      file,
      orgStore.currentOrgId || '',
      orgStore.currentMemberCardId || '',
      props.category,
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

const removeImage = () => {
  emit('update:modelValue', '')
}

defineExpose({
  triggerFileSelect,
})
</script>

<template>
  <div class="image-uploader" :style="containerStyle">
    <input
      ref="fileInputRef"
      type="file"
      :accept="accept"
      style="display: none"
      @change="handleFileChange"
    />

    <div v-if="imageUrl" class="image-preview">
      <img :src="imageUrl" alt="preview" />
      <div class="image-overlay">
        <icon-edit class="action-icon" @click="triggerFileSelect" />
        <icon-delete class="action-icon" @click="removeImage" />
      </div>
    </div>

    <div
      v-else
      class="upload-placeholder"
      :class="{ disabled, uploading }"
      @click="triggerFileSelect"
    >
      <template v-if="uploading">
        <icon-loading class="loading-icon" />
        <span class="placeholder-text">{{ progress.percent }}%</span>
      </template>
      <template v-else>
        <icon-plus class="plus-icon" />
        <span class="placeholder-text">{{ t('oss.image.upload') }}</span>
      </template>
    </div>
  </div>
</template>

<style scoped>
.image-uploader {
  position: relative;
  border-radius: 4px;
  overflow: hidden;
}

.image-preview {
  width: 100%;
  height: 100%;
  position: relative;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-preview:hover .image-overlay {
  opacity: 1;
}

.action-icon {
  font-size: 20px;
  color: white;
  cursor: pointer;
  transition: transform 0.2s;
}

.action-icon:hover {
  transform: scale(1.2);
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  border: 1px dashed var(--color-border-3);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  background: var(--color-fill-1);
  transition: all 0.2s;
}

.upload-placeholder:hover:not(.disabled):not(.uploading) {
  border-color: rgb(var(--primary-6));
  background: var(--color-fill-2);
}

.upload-placeholder.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.upload-placeholder.uploading {
  cursor: wait;
}

.plus-icon {
  font-size: 24px;
  color: var(--color-text-3);
}

.loading-icon {
  font-size: 24px;
  color: rgb(var(--primary-6));
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

.placeholder-text {
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
