<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()

// 全屏页面（如应用对话页）不展示全局头部和底部
const isFullscreen = computed(() => route.meta.fullscreen === true)
</script>

<template>
  <!-- 全屏布局：仅渲染页面内容 -->
  <router-view v-if="isFullscreen" />
  <!-- 默认布局：全局头部 + 内容区 + 全局底部 -->
  <a-layout v-else class="basic-layout">
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
