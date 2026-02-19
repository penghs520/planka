<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useOrgStore } from '@/stores/org'
import { Message } from '@arco-design/web-vue'
import type { LoginRequest } from '@/types/auth'
import type { OrganizationDTO } from '@/types/member'
import LoginIllustration from '@/components/auth/LoginIllustration.vue'
import ChangePasswordModal from '@/components/auth/ChangePasswordModal.vue'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const orgStore = useOrgStore()

const loading = ref(false)
const formData = reactive<LoginRequest>({
  email: '',
  password: '',
})
const changePasswordModalRef = ref<InstanceType<typeof ChangePasswordModal> | null>(null)

const rules = computed(() => ({
  email: [
    { required: true, message: t('auth.login.emailRequired') },
    { type: 'email' as const, message: t('auth.login.emailInvalid') },
  ],
  password: [{ required: true, message: t('auth.login.passwordRequired') }],
}))

async function handleLogin() {
  loading.value = true
  try {
    const organizations = await userStore.login(formData)
    Message.success(t('auth.login.loginSuccess'))

    // 保存用户 ID 到 localStorage（用于获取当前用户角色）
    if (userStore.user) {
      localStorage.setItem('userId', userStore.user.id)
    }

    // 检查是否需要修改密码（使用默认密码登录）
    if (userStore.requirePasswordChange) {
      changePasswordModalRef.value?.open()
      return
    }

    await proceedToApp(organizations)
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

async function proceedToApp(organizations: OrganizationDTO[]) {
  // 设置组织列表
  orgStore.setOrgs(organizations)

  // 根据组织数量决定跳转
  if (organizations.length === 0) {
    // 没有组织，跳转到组织选择页（可创建组织）
    router.push('/select-org')
  } else if (organizations.length === 1) {
    // 只有一个组织，直接切换并进入
    const firstOrg = organizations[0]!
    try {
      await orgStore.switchOrganization(firstOrg.id)
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } catch {
      // 切换失败，跳转到组织选择页
      router.push('/select-org')
    }
  } else {
    // 多个组织，跳转到组织选择页
    router.push('/select-org')
  }
}

function goToActivate() {
  router.push('/activate')
}
</script>

<template>
  <div class="login-container">
    <!-- 顶部 Logo -->
    <div class="top-bar">
      <img src="/favicon.svg" alt="Logo" class="top-logo" />
      <span class="top-title">Agilean Kanban</span>
    </div>

    <!-- 左侧：登录表单区 -->
    <div class="login-left">
      <div class="login-form-wrapper">
        <div class="login-header">
          <h1 class="login-title">{{ t('auth.login.welcome') }}</h1>
          <p class="login-subtitle">{{ t('auth.login.subtitle') }}</p>
        </div>

        <a-form :model="formData" :rules="rules" layout="vertical" @submit-success="handleLogin">
          <a-form-item field="email" :label="t('auth.login.email')" required>
            <a-input
              v-model="formData.email"
              :placeholder="t('auth.login.emailPlaceholder')"
              size="large"
              allow-clear
            />
          </a-form-item>

          <a-form-item field="password" :label="t('auth.login.password')" required>
            <a-input-password
              v-model="formData.password"
              :placeholder="t('auth.login.passwordPlaceholder')"
              size="large"
              allow-clear
            />
          </a-form-item>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              :loading="loading"
              long
              size="large"
              class="login-button"
            >
              {{ t('auth.login.loginButton') }}
            </a-button>
          </a-form-item>
        </a-form>

        <div class="login-footer">
          <span class="footer-text">{{ t('auth.login.firstLogin') }}</span>
          <a-link @click="goToActivate">{{ t('auth.login.activateAccount') }}</a-link>
        </div>
      </div>
    </div>

    <!-- 右侧：品牌展示区 -->
    <div class="login-right">
      <div class="brand-content">
        <LoginIllustration class="brand-illustration" />
        <h2 class="brand-title">{{ t('auth.login.brandTitle') }}</h2>
        <p class="brand-subtitle">{{ t('auth.login.brandSubtitle') }}</p>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <ChangePasswordModal ref="changePasswordModalRef" />
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  min-height: 100vh;
  position: relative;
}

/* 顶部 Logo 栏 */
.top-bar {
  position: absolute;
  top: 24px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 10;
}

.top-logo {
  width: 40px;
  height: 40px;
}

.top-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-1);
}

/* 左侧登录表单区 */
.login-left {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px;
}

.login-form-wrapper {
  width: 100%;
  max-width: 360px;
}

.login-header {
  margin-bottom: 40px;
}

.login-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 600;
  color: var(--color-text-1);
  letter-spacing: -0.5px;
}

.login-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-3);
}

.login-button {
  height: 44px;
  font-size: 15px;
}

.login-footer {
  margin-top: 24px;
  text-align: center;
  color: var(--color-text-3);
}

.footer-text {
  margin-right: 4px;
}

/* 右侧品牌展示区 */
.login-right {
  flex: 0 0 32%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #f2f7ff 0%, #e8f0fe 100%);
  padding: 40px;
}

.brand-content {
  text-align: center;
  max-width: 480px;
}

.brand-illustration {
  width: 320px;
  height: 280px;
  margin-bottom: 48px;
}

.brand-title {
  margin: 0 0 12px;
  font-size: 28px;
  font-weight: 600;
  color: var(--color-text-1);
}

.brand-subtitle {
  margin: 0;
  font-size: 16px;
  color: var(--color-text-3);
}

/* 响应式：小屏幕隐藏右侧 */
@media (max-width: 900px) {
  .login-right {
    display: none;
  }

  .login-left {
    flex: 1;
  }
}

/* 移除浏览器自动填充的背景色 */
.login-form-wrapper :deep(input:-webkit-autofill),
.login-form-wrapper :deep(input:-webkit-autofill:hover),
.login-form-wrapper :deep(input:-webkit-autofill:focus),
.login-form-wrapper :deep(input:-webkit-autofill:active) {
  -webkit-box-shadow: 0 0 0 30px white inset !important;
  box-shadow: 0 0 0 30px white inset !important;
  -webkit-text-fill-color: var(--color-text-1) !important;
}
</style>
