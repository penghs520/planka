<script setup lang="ts">
/**
 * 独立卡片详情页
 * 通过 /card/:cardId 路由直接访问
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import CardDetailView from './CardDetailView.vue'

const route = useRoute()
const cardId = computed(() => route.params.cardId as string)
</script>

<template>
  <div class="card-detail-page">
    <div class="page-container">
      <CardDetailView v-if="cardId" :card-id="cardId" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.card-detail-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-1);
  overflow: hidden;
}

.page-container {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  height: 100%;
  background: var(--color-bg-1);
  overflow: hidden;
}
</style>
