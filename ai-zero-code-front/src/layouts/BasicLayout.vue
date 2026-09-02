<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<template>
  <a-layout class="basic-layout">
    <GlobalHeader />
    <a-layout-content class="layout-content">
      <router-view />
    </a-layout-content>
    <GlobalFooter />
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 内容区 flex:1，使页脚始终固定在底部 */
.layout-content {
  flex: 1 0 auto;
  padding: 24px;
}

/* 响应式：小屏缩小内边距 */
@media (max-width: 768px) {
  .layout-content {
    padding: 12px;
  }
}
</style>
