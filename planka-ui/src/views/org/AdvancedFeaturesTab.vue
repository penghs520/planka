<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { Message } from '@arco-design/web-vue'
import type { OrganizationDTO } from '@/types/member'
import SaveButton from '@/components/common/SaveButton.vue'

const { t } = useI18n()

const props = defineProps<{
  org: OrganizationDTO
  canEdit: boolean
}>()

const orgStore = useOrgStore()

const saving = ref(false)

const formData = reactive({
  attendanceEnabled: false,
})

watch(
  () => props.org,
  (org) => {
    if (org) {
      formData.attendanceEnabled = org.attendanceEnabled ?? false
    }
  },
  { immediate: true },
)

async function handleSave() {
  saving.value = true
  try {
    await orgStore.updateOrganization(props.org.id, {
      attendanceEnabled: formData.attendanceEnabled,
    })
    Message.success(t('admin.orgSettings.advancedFeatures.saveSuccess'))
  } catch {
    // Error handled in interceptor
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="advanced-features-tab">
    <div class="feature-section">
      <div class="section-header">
        <h3 class="section-title">{{ t('admin.orgSettings.advancedFeatures.title') }}</h3>
        <p class="section-description">
          {{ t('admin.orgSettings.advancedFeatures.description') }}
        </p>
      </div>

      <a-form :model="formData" layout="vertical" :style="{ width: '100%' }" :disabled="!canEdit">
        <a-card class="feature-card">
          <template #title>
            <div class="feature-card-title">
              <icon-calendar class="title-icon" />
              {{ t('admin.orgSettings.advancedFeatures.attendanceFeature') }}
            </div>
          </template>

          <a-form-item :label="t('admin.orgSettings.advancedFeatures.enableAttendance')">
            <a-switch v-model="formData.attendanceEnabled" />
            <template #extra>
              <span class="form-item-tip">
                {{ t('admin.orgSettings.advancedFeatures.enableAttendanceTip') }}
              </span>
            </template>
          </a-form-item>
        </a-card>

        <a-form-item v-if="canEdit" class="save-button-item">
          <SaveButton :loading="saving" :text="t('admin.orgSettings.advancedFeatures.saveButton')" @click="handleSave" />
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.advanced-features-tab {
  padding: 16px 0;
}

.feature-section {
  max-width: 800px;
}

.section-header {
  margin-bottom: 24px;
}

.section-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-1);
}

.section-description {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-2);
  line-height: 1.5;
}

.feature-card {
  margin-bottom: 24px;
}

.feature-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
}

.title-icon {
  font-size: 18px;
  color: var(--color-primary);
}

.form-item-tip {
  font-size: 13px;
  color: #86909c;
  line-height: 1.5;
  margin-left: 8px;
}

.save-button-item {
  margin-top: 16px;
}
</style>
