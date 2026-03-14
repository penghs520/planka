<script setup lang="ts">
import CompactModal from '@/components/common/CompactModal.vue'
</script>

<template>
  <a-config-provider>
    <router-view />
    <!-- 导入 CompactModal 以加载全局样式 -->
    <CompactModal />
  </a-config-provider>
</template>

<style>
#app {
  height: 100vh;
  width: 100vw;
}
</style>
