<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { useUserStore } from '@/stores/user'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import ChangePasswordModal from '@/components/auth/ChangePasswordModal.vue'

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
  <a-layout class="layout">
    <Sidebar v-if="!hideMenu" :collapsed="collapsed" @update:collapsed="handleCollapse" />
    <a-layout>
      <Header v-if="!hideMenu" :collapsed="collapsed" @toggle-collapse="collapsed = !collapsed" />
      <a-layout-content class="layout-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>

  <!-- 修改密码弹窗 -->
  <ChangePasswordModal ref="changePasswordModalRef" />
</template>

<style scoped>
.layout {
  height: 100vh;
  overflow: hidden;
}

.layout-content {
  padding: 0;
  background-color: var(--color-bg-1);
  overflow: auto;
}
</style>
