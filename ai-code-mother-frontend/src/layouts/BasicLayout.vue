<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view v-slot="{ Component, route }">
        <keep-alive include="AppChatPage">
          <component v-if="route.meta?.keepAlive" :is="Component" />
        </keep-alive>
        <component v-if="!route.meta?.keepAlive" :is="Component" />
      </router-view>
    </a-layout-content>
    <!-- 底部版权信息 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: linear-gradient(180deg,
    #ffffff 0%,
    #f9fafb 8%,
    #f0fdfa 20%,
    #ccfbf1 34%,
    #99f6e4 48%,
    #38bdf8 62%,
    #eff6ff 82%,
    #ffffff 100%);
  position: relative;
  overflow: hidden;
}

.basic-layout::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
  opacity: 0.12;
  pointer-events: none;
  z-index: 0;
}

.main-content {
  width: 100%;
  padding: 0;
  background: transparent;
  margin: 0;
  margin-top: -64px; /* 让内容延伸到导航栏区域 */
  flex: 1;
  position: relative;
  z-index: 1;
}
</style>
