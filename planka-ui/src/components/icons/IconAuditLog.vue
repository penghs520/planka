<template>
  <svg
    viewBox="0 0 48 48"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    stroke="currentColor"
    class="arco-icon"
  >
    <!-- 审计日志图标：文档 + 放大镜组合 -->
    <!-- 文档外框 -->
    <rect x="6" y="4" width="28" height="36" rx="3" stroke-width="4" fill="none" />
    <!-- 文档内横线（代表日志内容） -->
    <path d="M14 14H26" stroke-width="3" stroke-linecap="round" />
    <path d="M14 22H22" stroke-width="3" stroke-linecap="round" />
    <path d="M14 30H20" stroke-width="3" stroke-linecap="round" />
    <!-- 放大镜（代表审计/检查） -->
    <circle cx="36" cy="32" r="8" stroke-width="4" fill="none" />
    <path d="M42 38L46 42" stroke-width="4" stroke-linecap="round" />
  </svg>
</template>

<style scoped>
.arco-icon {
  width: 1em;
  height: 1em;
  display: inline-block;
  vertical-align: -0.125em;
}
</style>
