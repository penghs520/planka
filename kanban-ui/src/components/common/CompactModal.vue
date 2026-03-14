<script setup lang="ts">
// 此组件仅提供全局样式，不包含逻辑
</script>

<template>
  <div style="display: none"></div>
</template>

<style>
/* 全局样式 - Modal 被 portal 到 body，需要使用全局样式 */

/* 减小 Modal 外层 padding - 针对 simple 类型的 Modal */
.compact-modal.arco-modal-simple {
  padding: 12px !important;
}

/* 减小标题下方的 margin */
.compact-modal .arco-modal-header {
  margin-bottom: 8px !important;
}

.compact-modal .arco-modal-body {
  text-align: left !important;
  padding-top: 8px !important;
  padding-bottom: 16px !important;
  min-height: auto !important;
}

.compact-modal .arco-modal-footer {
  padding-top: 12px !important;
}

/* 按钮样式 - 使用蓝色主题色 */
.compact-modal .arco-modal-footer .arco-btn {
  background-color: #3370FF !important;
  border-color: #3370FF !important;
  color: #fff !important;
}

.compact-modal .arco-modal-footer .arco-btn:hover {
  background-color: #4E83FD !important;
  border-color: #4E83FD !important;
}
</style>
