<template>
  <svg viewBox="0 0 320 280" fill="none" xmlns="http://www.w3.org/2000/svg">
    <!-- 背景装饰圆形 -->
    <circle class="bg-circle-1" cx="160" cy="140" r="120" fill="#E8F3FF" opacity="0.6"/>
    <circle class="bg-circle-2" cx="260" cy="80" r="40" fill="#BEDAFF" opacity="0.4"/>
    <circle class="bg-circle-3" cx="60" cy="200" r="30" fill="#BEDAFF" opacity="0.4"/>

    <!-- 看板卡片组 -->
    <!-- 第一列 - To Do -->
    <g class="card-column card-column-1">
      <rect x="60" y="90" width="60" height="120" rx="6" fill="white" stroke="#E5E6EB" stroke-width="1.5"/>
      <rect x="68" y="100" width="44" height="8" rx="2" fill="#165DFF"/>
      <text x="90" y="107" font-size="6" fill="white" text-anchor="middle" font-weight="500">待办</text>

      <!-- 卡片1 - 固定 -->
      <rect x="68" y="116" width="44" height="24" rx="3" fill="#F7F8FA" stroke="#E5E6EB" stroke-width="0.5"/>
      <rect x="72" y="121" width="24" height="3" rx="1" fill="#C9CDD4"/>
      <rect x="72" y="127" width="16" height="3" rx="1" fill="#E5E6EB"/>
      <circle cx="104" cy="124" r="4" fill="#00B42A" opacity="0.2"/>

      <!-- 卡片2 - 虚线边框占位 -->
      <g class="dashed-placeholder">
        <rect x="68" y="144" width="44" height="24" rx="3" fill="none" stroke="#C9CDD4" stroke-width="1" stroke-dasharray="3 2"/>
      </g>

      <!-- 卡片3 - 会往上移动 -->
      <g class="slide-up-card">
        <rect x="68" y="172" width="44" height="24" rx="3" fill="#F7F8FA" stroke="#E5E6EB" stroke-width="0.5"/>
        <rect x="72" y="177" width="20" height="3" rx="1" fill="#C9CDD4"/>
        <rect x="72" y="183" width="32" height="3" rx="1" fill="#E5E6EB"/>
        <circle cx="104" cy="180" r="4" fill="#165DFF" opacity="0.2"/>
      </g>
    </g>

    <!-- 第二列 - In Progress -->
    <g class="card-column card-column-2">
      <rect x="130" y="90" width="60" height="120" rx="6" fill="white" stroke="#E5E6EB" stroke-width="1.5"/>
      <rect x="138" y="100" width="44" height="8" rx="2" fill="#FF7D00"/>
      <text x="160" y="107" font-size="6" fill="white" text-anchor="middle" font-weight="500">进行中</text>

      <!-- 卡片1 - 高亮状态 -->
      <rect x="138" y="116" width="44" height="28" rx="3" fill="#E8F3FF" stroke="#165DFF" stroke-width="1"/>
      <rect x="142" y="121" width="28" height="3" rx="1" fill="#165DFF"/>
      <rect x="142" y="127" width="20" height="3" rx="1" fill="#86BFFF"/>
      <rect x="142" y="133" width="32" height="3" rx="1" fill="#BEDAFF"/>

      <!-- 卡片2 - 固定 -->
      <rect x="138" y="150" width="44" height="24" rx="3" fill="#F7F8FA" stroke="#E5E6EB" stroke-width="0.5"/>
      <rect x="142" y="155" width="24" height="3" rx="1" fill="#C9CDD4"/>
      <rect x="142" y="161" width="18" height="3" rx="1" fill="#E5E6EB"/>
      <circle cx="174" cy="158" r="4" fill="#00B42A" opacity="0.2"/>

      <!-- 新到达的卡片 - 从拖拽动画结束后显示 -->
      <g class="arrived-card">
        <rect x="138" y="178" width="44" height="24" rx="3" fill="#FFF7E8" stroke="#FF7D00" stroke-width="0.5"/>
        <rect x="142" y="183" width="28" height="3" rx="1" fill="#FF7D00"/>
        <rect x="142" y="189" width="20" height="3" rx="1" fill="#FFCF8B"/>
        <circle cx="174" cy="186" r="4" fill="#FF7D00" opacity="0.2"/>
      </g>
    </g>

    <!-- 第三列 - Done -->
    <g class="card-column card-column-3">
      <rect x="200" y="90" width="60" height="120" rx="6" fill="white" stroke="#E5E6EB" stroke-width="1.5"/>
      <rect x="208" y="100" width="44" height="8" rx="2" fill="#00B42A"/>
      <text x="230" y="107" font-size="6" fill="white" text-anchor="middle" font-weight="500">已完成</text>

      <!-- 完成的卡片 -->
      <rect x="208" y="116" width="44" height="24" rx="3" fill="#E8FFEA" stroke="#00B42A" stroke-width="0.5" opacity="0.8"/>
      <rect x="212" y="121" width="28" height="3" rx="1" fill="#7BE188"/>
      <rect x="212" y="127" width="20" height="3" rx="1" fill="#AFF0B5"/>
      <circle cx="244" cy="128" r="6" fill="#00B42A"/>
      <path d="M241 128 L243 130 L247 126" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>

      <rect x="208" y="144" width="44" height="24" rx="3" fill="#E8FFEA" stroke="#00B42A" stroke-width="0.5" opacity="0.6"/>
      <rect x="212" y="149" width="24" height="3" rx="1" fill="#7BE188"/>
      <rect x="212" y="155" width="16" height="3" rx="1" fill="#AFF0B5"/>
      <circle cx="244" cy="156" r="6" fill="#00B42A" opacity="0.6"/>
      <path d="M241 156 L243 158 L247 154" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </g>

    <!-- 拖拽中的卡片 -->
    <g class="dragging-card">
      <rect x="68" y="144" width="44" height="24" rx="3" fill="#FFF7E8" stroke="#FF7D00" stroke-width="1.5" filter="url(#shadow)"/>
      <rect x="72" y="149" width="28" height="3" rx="1" fill="#FF7D00"/>
      <rect x="72" y="155" width="20" height="3" rx="1" fill="#FFCF8B"/>
      <circle cx="104" cy="152" r="4" fill="#FF7D00" opacity="0.3"/>
    </g>

    <!-- 阴影滤镜 -->
    <defs>
      <filter id="shadow" x="-50%" y="-50%" width="200%" height="200%">
        <feDropShadow dx="0" dy="4" stdDeviation="4" flood-color="#000" flood-opacity="0.15"/>
      </filter>
    </defs>

    <!-- 连接线/流动箭头 -->
    <path class="flow-line" d="M122 150 L128 150" stroke="#165DFF" stroke-width="1.5" stroke-dasharray="2 2" opacity="0.5"/>
    <path class="flow-line" d="M192 150 L198 150" stroke="#00B42A" stroke-width="1.5" stroke-dasharray="2 2" opacity="0.5"/>

    <!-- 人物图标 - 团队协作 -->
    <g class="person person-1">
      <circle cx="80" cy="240" r="12" fill="#BEDAFF"/>
      <circle cx="80" cy="236" r="5" fill="white"/>
      <ellipse cx="80" cy="248" rx="8" ry="4" fill="white"/>
    </g>

    <g class="person person-2">
      <circle cx="160" cy="240" r="14" fill="#165DFF"/>
      <circle cx="160" cy="235" r="6" fill="white"/>
      <ellipse cx="160" cy="249" rx="9" ry="5" fill="white"/>
    </g>

    <g class="person person-3">
      <circle cx="240" cy="240" r="12" fill="#BEDAFF"/>
      <circle cx="240" cy="236" r="5" fill="white"/>
      <ellipse cx="240" cy="248" rx="8" ry="4" fill="white"/>
    </g>

    <!-- 连接团队的线 -->
    <path class="team-line" d="M95 240 L145 240" stroke="#165DFF" stroke-width="1" stroke-dasharray="4 2" opacity="0.4"/>
    <path class="team-line" d="M175 240 L225 240" stroke="#165DFF" stroke-width="1" stroke-dasharray="4 2" opacity="0.4"/>

    <!-- 装饰元素 -->
    <circle class="decor-1" cx="280" cy="180" r="8" fill="#165DFF" opacity="0.15"/>
    <circle class="decor-2" cx="40" cy="120" r="6" fill="#00B42A" opacity="0.15"/>
    <rect class="decor-3" x="270" y="120" width="12" height="12" rx="2" fill="#FF7D00" opacity="0.12" transform="rotate(15 276 126)"/>
  </svg>
</template>

<script setup lang="ts">
// 登录页插图组件 - 展示看板协作场景
</script>

<style scoped>
/* 背景圆形呼吸动画 */
.bg-circle-1 {
  animation: pulse 4s ease-in-out infinite;
  transform-origin: 160px 140px;
}

.bg-circle-2 {
  animation: pulse 3s ease-in-out infinite 0.5s;
  transform-origin: 260px 80px;
}

.bg-circle-3 {
  animation: pulse 3.5s ease-in-out infinite 1s;
  transform-origin: 60px 200px;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.4;
  }
}

/* 卡片列轻微浮动 */
.card-column-1 {
  animation: float 4s ease-in-out infinite;
}

.card-column-2 {
  animation: float 4s ease-in-out infinite 0.5s;
}

.card-column-3 {
  animation: float 4s ease-in-out infinite 1s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

/* 虚线边框占位 - 拖拽过程中显示 */
.dashed-placeholder {
  animation: dashedAnim 2.5s ease-in-out infinite;
}

@keyframes dashedAnim {
  0%, 5% {
    opacity: 0;
  }
  10%, 55% {
    opacity: 1;
  }
  65%, 100% {
    opacity: 0;
  }
}

/* 第三张卡片往上滑动 - 等拖拽完成后才移动 */
.slide-up-card {
  animation: slideUp 2.5s ease-in-out infinite;
}

@keyframes slideUp {
  0%, 60% {
    transform: translateY(0);
  }
  75%, 90% {
    transform: translateY(-28px);
  }
  100% {
    transform: translateY(0);
  }
}

/* 拖拽卡片动画 - 从第一列移动到第二列 */
.dragging-card {
  animation: dragCard 2.5s cubic-bezier(0.4, 0, 0.2, 1) infinite;
}

@keyframes dragCard {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  2% {
    transform: translate(0, -1px) scale(1.01);
    opacity: 1;
  }
  4% {
    transform: translate(0, -2px) scale(1.02);
    opacity: 1;
  }
  6% {
    transform: translate(1px, -4px) scale(1.04);
    opacity: 1;
  }
  8% {
    transform: translate(2px, -6px) scale(1.05);
    opacity: 1;
  }
  10% {
    transform: translate(4px, -8px) scale(1.06);
    opacity: 1;
  }
  12% {
    transform: translate(7px, -10px) scale(1.07);
    opacity: 1;
  }
  14% {
    transform: translate(10px, -12px) scale(1.08);
    opacity: 1;
  }
  17% {
    transform: translate(15px, -14px) scale(1.08);
    opacity: 1;
  }
  20% {
    transform: translate(20px, -16px) scale(1.08);
    opacity: 1;
  }
  23% {
    transform: translate(26px, -17px) scale(1.08);
    opacity: 1;
  }
  26% {
    transform: translate(32px, -17px) scale(1.08);
    opacity: 1;
  }
  29% {
    transform: translate(38px, -16px) scale(1.08);
    opacity: 1;
  }
  32% {
    transform: translate(44px, -14px) scale(1.08);
    opacity: 1;
  }
  35% {
    transform: translate(50px, -11px) scale(1.07);
    opacity: 1;
  }
  38% {
    transform: translate(55px, -6px) scale(1.07);
    opacity: 1;
  }
  41% {
    transform: translate(60px, 0px) scale(1.06);
    opacity: 1;
  }
  44% {
    transform: translate(64px, 8px) scale(1.05);
    opacity: 1;
  }
  47% {
    transform: translate(67px, 16px) scale(1.04);
    opacity: 1;
  }
  50% {
    transform: translate(69px, 24px) scale(1.03);
    opacity: 1;
  }
  53% {
    transform: translate(70px, 30px) scale(1.02);
    opacity: 1;
  }
  56% {
    transform: translate(70px, 33px) scale(1.01);
    opacity: 1;
  }
  58% {
    transform: translate(70px, 34px) scale(1);
    opacity: 1;
  }
  65% {
    transform: translate(70px, 34px) scale(1);
    opacity: 0;
  }
  100% {
    transform: translate(0, 0) scale(1);
    opacity: 0;
  }
}

/* 到达第二列的卡片 */
.arrived-card {
  animation: arrivedAnim 2.5s ease-in-out infinite;
}

@keyframes arrivedAnim {
  0%, 60% {
    opacity: 0;
    transform: scale(0.95);
  }
  70% {
    opacity: 1;
    transform: scale(1);
  }
  85% {
    opacity: 1;
    transform: scale(1);
  }
  95%, 100% {
    opacity: 0;
    transform: scale(0.95);
  }
}

/* 人物浮动动画 */
.person-1 {
  animation: personFloat 2.5s ease-in-out infinite;
}

.person-2 {
  animation: personFloat 2.5s ease-in-out infinite 0.4s;
}

.person-3 {
  animation: personFloat 2.5s ease-in-out infinite 0.8s;
}

@keyframes personFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

/* 流动线动画 */
.flow-line {
  animation: flowDash 1.5s linear infinite;
}

@keyframes flowDash {
  to {
    stroke-dashoffset: -8;
  }
}

/* 团队连接线动画 */
.team-line {
  animation: teamDash 2s linear infinite;
}

@keyframes teamDash {
  to {
    stroke-dashoffset: -12;
  }
}

/* 装饰元素动画 */
.decor-1 {
  animation: decorFloat 4s ease-in-out infinite;
  transform-origin: 280px 180px;
}

.decor-2 {
  animation: decorFloat 3.5s ease-in-out infinite 0.5s;
  transform-origin: 40px 120px;
}

.decor-3 {
  animation: decorRotate 8s linear infinite;
  transform-origin: 276px 126px;
}

@keyframes decorFloat {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-4px) scale(1.1);
  }
}

@keyframes decorRotate {
  from {
    transform: rotate(15deg);
  }
  to {
    transform: rotate(375deg);
  }
}
</style>
