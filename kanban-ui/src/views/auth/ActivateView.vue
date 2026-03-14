<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useOrgStore } from '@/stores/org'
import { Message } from '@arco-design/web-vue'
import type { ActivateRequest } from '@/types/auth'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const orgStore = useOrgStore()

const loading = ref(false)
const formData = reactive<ActivateRequest & { confirmPassword: string }>({
  email: '',
  activationCode: '',
  password: '',
  confirmPassword: '',
})

const rules = computed(() => ({
  email: [
    { required: true, message: t('auth.activate.emailRequired') },
    { type: 'email' as const, message: t('auth.activate.emailInvalid') },
  ],
  activationCode: [{ required: true, message: t('auth.activate.activationCodeRequired') }],
  password: [
    { required: true, message: t('auth.activate.passwordRequired') },
    { minLength: 6, message: t('auth.activate.passwordMinLength') },
    { maxLength: 32, message: t('auth.activate.passwordMaxLength') },
  ],
  confirmPassword: [
    { required: true, message: t('auth.activate.confirmPasswordRequired') },
    {
      validator: (value: string, callback: (error?: string) => void) => {
        if (value !== formData.password) {
          callback(t('auth.activate.passwordMismatch'))
        } else {
          callback()
        }
      },
    },
  ],
}))

onMounted(() => {
  // 从 URL 参数获取邮箱和激活码
  if (route.query.email) {
    formData.email = route.query.email as string
  }
  if (route.query.code) {
    formData.activationCode = route.query.code as string
  }
})

async function handleActivate() {
  loading.value = true
  try {
    const organizations = await userStore.activate({
      email: formData.email,
      activationCode: formData.activationCode,
      password: formData.password,
    })
    Message.success(t('auth.activate.activateSuccess'))

    // 保存用户 ID
    if (userStore.user) {
      localStorage.setItem('userId', userStore.user.id)
    }

    // 设置组织列表
    orgStore.setOrgs(organizations)

    // 根据组织数量决定跳转
    if (organizations.length === 0) {
      router.push('/select-org')
    } else if (organizations.length === 1) {
      const firstOrg = organizations[0]!
      try {
        await orgStore.switchOrganization(firstOrg.id)
        router.push('/')
      } catch {
        router.push('/select-org')
      }
    } else {
      router.push('/select-org')
    }
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="activate-container">
    <div class="activate-card">
      <div class="activate-header">
        <img src="/favicon.svg" alt="Logo" class="activate-logo" />
        <h1 class="activate-title">{{ t('auth.activate.title') }}</h1>
        <p class="activate-subtitle">{{ t('auth.activate.subtitle') }}</p>
      </div>

      <a-form :model="formData" :rules="rules" layout="vertical" @submit-success="handleActivate">
        <a-form-item field="email" :label="t('auth.activate.email')" required>
          <a-input
            v-model="formData.email"
            :placeholder="t('auth.activate.emailPlaceholder')"
            size="large"
            allow-clear
          >
            <template #prefix>
              <icon-email />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item field="activationCode" :label="t('auth.activate.activationCode')" required>
          <a-input
            v-model="formData.activationCode"
            :placeholder="t('auth.activate.activationCodePlaceholder')"
            size="large"
            allow-clear
          >
            <template #prefix>
              <icon-code />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item field="password" :label="t('auth.activate.setPassword')" required>
          <a-input-password
            v-model="formData.password"
            :placeholder="t('auth.activate.passwordPlaceholder')"
            size="large"
            allow-clear
          >
            <template #prefix>
              <icon-lock />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item field="confirmPassword" :label="t('auth.activate.confirmPassword')" required>
          <a-input-password
            v-model="formData.confirmPassword"
            :placeholder="t('auth.activate.confirmPasswordPlaceholder')"
            size="large"
            allow-clear
          >
            <template #prefix>
              <icon-lock />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            :loading="loading"
            long
            size="large"
          >
            {{ t('auth.activate.activateButton') }}
          </a-button>
        </a-form-item>
      </a-form>

      <div class="activate-footer">
        <a-typography-text type="secondary">
          {{ t('auth.activate.hasAccount') }}
          <a-link @click="goToLogin">{{ t('auth.activate.backToLogin') }}</a-link>
        </a-typography-text>
      </div>
    </div>
  </div>
</template>

<style scoped>
.activate-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.activate-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.activate-header {
  text-align: center;
  margin-bottom: 32px;
}

.activate-logo {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
}

.activate-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-1);
}

.activate-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-3);
}

.activate-footer {
  margin-top: 24px;
  text-align: center;
}
</style>
