<template>
  <svg
    viewBox="0 0 48 48"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    stroke="currentColor"
    class="arco-icon"
  >
    <!-- 顶部节点 -->
    <rect x="18" y="4" width="12" height="8" rx="2" stroke-width="4" fill="none" />
    <!-- 垂直连接线 -->
    <path d="M24 12V20" stroke-width="4" stroke-linecap="round" />
    <!-- 水平连接线 -->
    <path d="M8 20H40" stroke-width="4" stroke-linecap="round" />
    <!-- 左分支线 -->
    <path d="M8 20V28" stroke-width="4" stroke-linecap="round" />
    <!-- 中分支线 -->
    <path d="M24 20V28" stroke-width="4" stroke-linecap="round" />
    <!-- 右分支线 -->
    <path d="M40 20V28" stroke-width="4" stroke-linecap="round" />
    <!-- 左子节点 -->
    <rect x="2" y="28" width="12" height="8" rx="2" stroke-width="4" fill="none" />
    <!-- 中子节点 -->
    <rect x="18" y="28" width="12" height="8" rx="2" stroke-width="4" fill="none" />
    <!-- 右子节点 -->
    <rect x="34" y="28" width="12" height="8" rx="2" stroke-width="4" fill="none" />
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
