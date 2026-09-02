<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import logo from '@/assets/logo.svg'

// 菜单配置：在此处增删改菜单项（key 为路由 path）
interface MenuItem {
  key: string
  label: string
}
const menus: MenuItem[] = [
  { key: '/', label: '主页' },
  { key: '/about', label: '关于' },
]

const router = useRouter()
const route = useRoute()
const selectedKeys = ref<string[]>([route.path])

// 监听路由变化，更新菜单选中项
watch(
  () => route.path,
  (newPath) => {
    selectedKeys.value = [newPath]
  },
)

const handleMenuClick = ({ key }: { key: string }) => {
  router.push(key)
}

const handleLogin = () => {
  // TODO: 暂时用按钮替代，后续接入登录流程
  router.push('/user/login')
}
</script>

<template>
  <a-layout-header class="global-header">
    <div class="header-left">
      <div class="logo-area">
        <img :src="logo" alt="logo" class="logo" />
        <span class="title">AI 在线编程</span>
      </div>
      <a-menu
        class="header-menu"
        mode="horizontal"
        :selected-keys="selectedKeys"
        @click="handleMenuClick"
      >
        <a-menu-item v-for="item in menus" :key="item.key">
          {{ item.label }}
        </a-menu-item>
      </a-menu>
    </div>
    <div class="header-right">
      <a-button type="primary" @click="handleLogin">登录</a-button>
    </div>
  </a-layout-header>
</template>

<style scoped>
.global-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  line-height: 64px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.logo-area {
  display: flex;
  align-items: center;
  margin-right: 24px;
  cursor: pointer;
}

.logo {
  height: 36px;
  width: 36px;
  margin-right: 12px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
}

.header-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
  background: transparent;
  line-height: 62px;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 响应式：小屏隐藏标题、紧凑间距 */
@media (max-width: 768px) {
  .global-header {
    padding: 0 12px;
  }
  .title {
    display: none;
  }
  .logo-area {
    margin-right: 12px;
  }
}
</style>
