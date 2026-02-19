<template>
  <svg
    viewBox="0 0 48 48"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    stroke="currentColor"
    class="arco-icon"
  >
    <!-- 时钟外圈 -->
    <circle cx="24" cy="24" r="18" stroke-width="4" fill="none" />
    <!-- 时针 -->
    <path d="M24 24L24 14" stroke-width="4" stroke-linecap="round" />
    <!-- 分针 -->
    <path d="M24 24L32 24" stroke-width="4" stroke-linecap="round" />
    <!-- 中心点 -->
    <circle cx="24" cy="24" r="2" fill="currentColor" />
    <!-- 底部打卡标记 -->
    <path d="M18 40L24 36L30 40" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
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
