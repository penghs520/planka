<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { useUserStore } from '@/stores/user'
import Sidebar from './Sidebar.vue'
import ChangePasswordModal from '@/components/auth/ChangePasswordModal.vue'
import OrgSwitchDetector from '@/components/common/OrgSwitchDetector.vue'

const route = useRoute()
const collapsed = ref(false)
const orgStore = useOrgStore()
const userStore = useUserStore()
const changePasswordModalRef = ref<InstanceType<typeof ChangePasswordModal> | null>(null)

// 根据路由元信息判断是否隐藏菜单
const hideMenu = computed(() => route.meta.hideMenu === true)

function handleCollapse(value: boolean) {
  collapsed.value = value
}

// 监听是否需要修改密码
watch(() => userStore.requirePasswordChange, (requireChange) => {
  if (requireChange) {
    changePasswordModalRef.value?.open()
  }
}, { immediate: true })

onMounted(() => {
  if (orgStore.currentOrgId) {
    orgStore.fetchCurrentOrgRole()
  }
})
</script>

<template>
  <div class="admin-layout">
    <Sidebar v-if="!hideMenu" :collapsed="collapsed" @update:collapsed="handleCollapse" />
    <main class="admin-main">
      <router-view />
    </main>
  </div>

  <!-- 修改密码弹窗 -->
  <ChangePasswordModal ref="changePasswordModalRef" />

  <!-- 组织切换检测组件 -->
  <OrgSwitchDetector v-if="orgStore.currentOrgId" />
</template>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background-color: var(--color-bg-1);
}

.admin-main {
  flex: 1;
  overflow: auto;
  background-color: var(--color-bg-1);
}
</style>
