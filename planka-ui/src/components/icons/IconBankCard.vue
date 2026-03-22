<template>
  <svg
    viewBox="0 0 48 48"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    stroke="currentColor"
    class="arco-icon"
  >
    <!-- 银行卡外框 -->
    <rect x="6" y="10" width="36" height="28" rx="4" stroke-width="4" fill="none" />
    <!-- 磁条带 -->
    <rect x="10" y="15" width="28" height="5" rx="1" fill="currentColor" />
    <!-- 芯片 -->
    <rect x="12" y="26" width="10" height="8" rx="2" stroke-width="3" fill="none" />
    <!-- 卡号示意线 -->
    <path d="M26 28H36" stroke-width="3" stroke-linecap="round" />
    <path d="M26 32H34" stroke-width="3" stroke-linecap="round" />
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
