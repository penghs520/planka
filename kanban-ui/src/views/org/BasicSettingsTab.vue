<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { Message } from '@arco-design/web-vue'
import type { OrganizationDTO, UpdateOrganizationRequest } from '@/types/member'
import SaveButton from '@/components/common/SaveButton.vue'

const { t } = useI18n()

const props = defineProps<{
  org: OrganizationDTO
  canEdit: boolean
}>()

const orgStore = useOrgStore()

const saving = ref(false)
const formRef = ref()

const formData = reactive<UpdateOrganizationRequest>({
  name: '',
  description: '',
  logo: '',
})

const rules = computed(() => ({
  name: [
    { required: true, message: t('common.org.nameRequired') },
    { maxLength: 200, message: t('common.org.nameMaxLength') },
  ],
  description: [{ maxLength: 1000, message: t('common.org.descriptionMaxLength') }],
}))

watch(
  () => props.org,
  (org) => {
    if (org) {
      formData.name = org.name
      formData.description = org.description || ''
      formData.logo = org.logo || ''
    }
  },
  { immediate: true },
)

async function handleSave() {
  const errors = await formRef.value?.validate()
  if (errors) return

  saving.value = true
  try {
    await orgStore.updateOrganization(props.org.id, {
      name: formData.name,
      description: formData.description || undefined,
      logo: formData.logo || undefined,
    })
    Message.success(t('common.org.updated'))
  } catch {
    // Error handled in interceptor
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="basic-settings-tab">
    <a-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      layout="vertical"
      :style="{ width: '100%' }"
      :disabled="!canEdit"
    >
      <a-form-item field="name" :label="t('common.org.name')" required>
        <a-input
          v-model="formData.name"
          :placeholder="t('common.org.namePlaceholder')"
          :max-length="200"
          show-word-limit
        />
      </a-form-item>

      <a-form-item field="description" :label="t('common.org.description')">
        <a-textarea
          v-model="formData.description"
          :placeholder="t('common.org.descriptionPlaceholder')"
          :max-length="1000"
          show-word-limit
          :auto-size="{ minRows: 3, maxRows: 6 }"
        />
      </a-form-item>

      <a-form-item field="logo" :label="t('common.org.logo')">
        <a-input
          v-model="formData.logo"
          :placeholder="t('common.org.logoPlaceholder')"
        />
        <template #extra>
          <div v-if="formData.logo" class="logo-preview">
            <a-avatar :size="64" shape="square" :image-url="formData.logo">
              <icon-home />
            </a-avatar>
          </div>
        </template>
      </a-form-item>

      <a-form-item v-if="canEdit">
        <SaveButton :loading="saving" :text="t('common.user.saveChanges')" @click="handleSave" />
      </a-form-item>
    </a-form>

    <a-divider />

    <div class="org-info">
      <a-descriptions :column="1" :label-style="{ width: '120px' }">
        <a-descriptions-item :label="t('common.org.id')">
          <a-typography-text copyable>{{ org.id }}</a-typography-text>
        </a-descriptions-item>
        <a-descriptions-item :label="t('common.systemField.createdAt')">
          {{ org.createdAt }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('common.org.status')">
          <a-tag :color="org.status === 'ACTIVE' ? 'green' : 'gray'">
            {{ org.status === 'ACTIVE' ? t('common.org.statusActive') : org.status }}
          </a-tag>
        </a-descriptions-item>
      </a-descriptions>
    </div>
  </div>
</template>

<style scoped>
.basic-settings-tab {
  padding: 16px 0;
}

.form-item-tip {
  font-size: 13px;
  color: #86909c;
  line-height: 1.5;
  margin-left: 8px;
}

.logo-preview {
  margin-top: 8px;
}

.org-info {
  margin-top: 16px;
}
</style>
