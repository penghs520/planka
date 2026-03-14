<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import type { ChangePasswordRequest } from '@/types/user'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref()

const formData = reactive<ChangePasswordRequest & { confirmPassword: string }>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules = computed(() => ({
  oldPassword: [{ required: true, message: t('common.user.oldPasswordRequired') }],
  newPassword: [
    { required: true, message: t('common.user.newPasswordRequired') },
    { minLength: 6, message: t('common.user.passwordMinLength') },
    { maxLength: 32, message: t('common.user.passwordMaxLength') },
  ],
  confirmPassword: [
    { required: true, message: t('common.user.confirmNewPasswordRequired') },
    {
      validator: (value: string, callback: (error?: string) => void) => {
        if (value !== formData.newPassword) {
          callback(t('common.user.passwordMismatch'))
        } else {
          callback()
        }
      },
    },
  ],
}))

async function handleSubmit() {
  const errors = await formRef.value?.validate()
  if (errors) return

  loading.value = true
  try {
    await userStore.changePassword({
      oldPassword: formData.oldPassword,
      newPassword: formData.newPassword,
    })
    Message.success(t('common.user.passwordChanged'))
    router.back()
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="change-password-container">
    <a-page-header :title="t('common.user.changePassword')" @back="goBack" />

    <a-card class="change-password-card">
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        layout="vertical"
        :style="{ maxWidth: '400px' }"
        @submit-success="handleSubmit"
      >
        <a-form-item field="oldPassword" :label="t('common.user.oldPassword')" required>
          <a-input-password
            v-model="formData.oldPassword"
            :placeholder="t('common.user.oldPasswordPlaceholder')"
            allow-clear
          />
        </a-form-item>

        <a-form-item field="newPassword" :label="t('common.user.newPassword')" required>
          <a-input-password
            v-model="formData.newPassword"
            :placeholder="t('common.user.newPasswordPlaceholder')"
            allow-clear
          />
        </a-form-item>

        <a-form-item field="confirmPassword" :label="t('common.user.confirmNewPassword')" required>
          <a-input-password
            v-model="formData.confirmPassword"
            :placeholder="t('common.user.confirmNewPasswordPlaceholder')"
            allow-clear
          />
        </a-form-item>

        <a-form-item>
          <a-space>
            <SaveButton
              html-type="submit"
              :loading="loading"
              :text="t('common.user.confirmChange')"
            />
            <CancelButton @click="goBack" />
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.change-password-container {
  padding: 0 24px 24px;
}

.change-password-card {
  max-width: 600px;
}
</style>
