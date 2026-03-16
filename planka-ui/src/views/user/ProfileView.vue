<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import type { UpdateUserRequest } from '@/types/user'
import SaveButton from '@/components/common/SaveButton.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const formRef = ref()

const formData = reactive<UpdateUserRequest>({
  nickname: '',
  avatar: '',
  phone: '',
})

const rules = computed(() => ({
  nickname: [{ maxLength: 100, message: t('common.user.nicknameMaxLength') }],
  phone: [{ maxLength: 20, message: t('common.user.phoneMaxLength') }],
}))

onMounted(async () => {
  loading.value = true
  try {
    await userStore.fetchMe()
    if (userStore.user) {
      formData.nickname = userStore.user.nickname
      formData.avatar = userStore.user.avatar || ''
      formData.phone = userStore.user.phone || ''
    }
  } finally {
    loading.value = false
  }
})

async function handleSave() {
  const errors = await formRef.value?.validate()
  if (errors) return

  saving.value = true
  try {
    await userStore.updateProfile({
      nickname: formData.nickname || undefined,
      avatar: formData.avatar || undefined,
      phone: formData.phone || undefined,
    })
    Message.success(t('common.user.profileUpdated'))
  } catch {
    // 错误已在拦截器处理
  } finally {
    saving.value = false
  }
}

function goToChangePassword() {
  router.push('/change-password')
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="profile-container">
    <a-page-header :title="t('common.user.profile')" @back="goBack">
      <template #extra>
        <SaveButton :loading="saving" :text="t('common.user.saveChanges')" @click="handleSave" />
      </template>
    </a-page-header>

    <a-spin :loading="loading" class="profile-content">
      <a-card>
        <a-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          layout="vertical"
          :style="{ maxWidth: '480px' }"
        >
          <a-form-item :label="t('common.user.email')">
            <a-input
              :model-value="userStore.user?.email"
              disabled
              :placeholder="t('common.user.emailNotEditable')"
            />
          </a-form-item>

          <a-form-item field="nickname" :label="t('common.user.nickname')">
            <a-input
              v-model="formData.nickname"
              :placeholder="t('common.user.nicknamePlaceholder')"
              :max-length="100"
              show-word-limit
            />
          </a-form-item>

          <a-form-item field="avatar" :label="t('common.user.avatarUrl')">
            <a-input
              v-model="formData.avatar"
              :placeholder="t('common.user.avatarUrlPlaceholder')"
            />
            <template #extra>
              <div v-if="formData.avatar" class="avatar-preview">
                <a-avatar :size="64" :image-url="formData.avatar">
                  <icon-user />
                </a-avatar>
              </div>
            </template>
          </a-form-item>

          <a-form-item field="phone" :label="t('common.user.phone')">
            <a-input
              v-model="formData.phone"
              :placeholder="t('common.user.phonePlaceholder')"
              :max-length="20"
            />
          </a-form-item>

          <a-divider />

          <a-form-item :label="t('common.user.password')">
            <a-button type="outline" @click="goToChangePassword">
              {{ t('common.user.changePassword') }}
            </a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </a-spin>
  </div>
</template>

<style scoped>
.profile-container {
  padding: 0 24px 24px;
}

.profile-content {
  display: block;
}

.avatar-preview {
  margin-top: 8px;
}
</style>
