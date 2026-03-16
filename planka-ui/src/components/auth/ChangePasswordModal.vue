<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'

const { t } = useI18n()
const userStore = useUserStore()

const visible = ref(false)
const loading = ref(false)
const formRef = ref()

const formData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules = computed(() => ({
  oldPassword: [{ required: true, message: t('auth.changePassword.oldPasswordRequired') }],
  newPassword: [
    { required: true, message: t('auth.changePassword.newPasswordRequired') },
    { minLength: 6, message: t('auth.changePassword.newPasswordMinLength') },
    { maxLength: 32, message: t('auth.changePassword.newPasswordMaxLength') },
  ],
  confirmPassword: [
    { required: true, message: t('auth.changePassword.confirmPasswordRequired') },
    {
      validator: (value: string, cb: (error?: string) => void) => {
        if (value !== formData.newPassword) {
          cb(t('auth.changePassword.passwordMismatch'))
        } else {
          cb()
        }
      },
    },
  ],
}))

function open() {
  visible.value = true
  // 重置表单
  formData.oldPassword = ''
  formData.newPassword = ''
  formData.confirmPassword = ''
}

function close() {
  visible.value = false
}

async function handleSubmit() {
  const errors = await formRef.value?.validate()
  if (errors) return

  loading.value = true
  try {
    await userStore.changePassword({
      oldPassword: formData.oldPassword,
      newPassword: formData.newPassword,
    })
    userStore.clearRequirePasswordChange()
    Message.success(t('auth.changePassword.changeSuccess'))
    close()
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  // 用户取消修改密码，执行登出
  userStore.logout()
  close()
}

defineExpose({
  open,
  close,
})
</script>

<template>
  <a-modal
    v-model:visible="visible"
    :title="t('auth.changePassword.title')"
    :mask-closable="false"
    :esc-to-close="false"
    :closable="false"
    :footer="false"
    width="400px"
  >
    <div class="change-password-content">
      <a-alert type="warning" class="mb-4">
        {{ t('auth.changePassword.subtitle') }}
      </a-alert>

      <a-form ref="formRef" :model="formData" :rules="rules" layout="vertical">
        <a-form-item field="oldPassword" :label="t('auth.changePassword.oldPassword')" required>
          <a-input-password
            v-model="formData.oldPassword"
            :placeholder="t('auth.changePassword.oldPasswordPlaceholder')"
            allow-clear
          />
        </a-form-item>

        <a-form-item field="newPassword" :label="t('auth.changePassword.newPassword')" required>
          <a-input-password
            v-model="formData.newPassword"
            :placeholder="t('auth.changePassword.newPasswordPlaceholder')"
            allow-clear
          />
        </a-form-item>

        <a-form-item field="confirmPassword" :label="t('auth.changePassword.confirmPassword')" required>
          <a-input-password
            v-model="formData.confirmPassword"
            :placeholder="t('auth.changePassword.confirmPasswordPlaceholder')"
            allow-clear
          />
        </a-form-item>

        <div class="form-actions">
          <CancelButton @click="handleCancel" />
          <SaveButton :loading="loading" :text="t('auth.changePassword.submit')" @click="handleSubmit" />
        </div>
      </a-form>
    </div>
  </a-modal>
</template>

<style scoped>
.change-password-content {
  padding: 8px 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

/* 弹窗圆角 */
:deep(.arco-modal) {
  border-radius: var(--radius-xl, 12px);
}

:deep(.arco-modal-header) {
  border-radius: var(--radius-xl, 12px) var(--radius-xl, 12px) 0 0;
}

:deep(.arco-modal-body) {
  border-radius: 0 0 var(--radius-xl, 12px) var(--radius-xl, 12px);
}

/* 输入框圆角 */
:deep(.arco-input-wrapper),
:deep(.arco-input-password) {
  border-radius: var(--radius-md, 6px);
}
</style>
