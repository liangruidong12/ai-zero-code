<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserOutlined } from '@ant-design/icons-vue'
import logo from '@/assets/logo.svg'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { message } from 'ant-design-vue'

// 当前登录用户（来自全局 store）
const loginUser = computed(() => useLoginUserStore().loginUser)
// 是否已登录：以 id 是否存在判断
const isLogin = computed(() => !!loginUser.value.id)

// 菜单配置：在此处增删改菜单项（key 为路由 path）
interface MenuItem {
  key: string
  label: string
  requireAdmin?: boolean
}
const allMenus: MenuItem[] = [
  { key: '/', label: '主页' },
  { key: '/admin/app', label: '应用管理', requireAdmin: true },
  { key: '/admin/chatHistory', label: '对话管理', requireAdmin: true },
  { key: '/admin/user', label: '用户管理', requireAdmin: true },
]
// 仅在管理员登录时展示需要管理员权限的菜单
const menus = computed<MenuItem[]>(() => {
  const isAdmin = loginUser.value.userRole === 'admin'
  return allMenus.filter((m) => !m.requireAdmin || isAdmin)
})

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

// 退出登录
const handleLogout = async () => {
  await userLogout()
  useLoginUserStore().setLoginUser({ userName: '未登录' })
  message.success('退出登录成功')
  //router.push('/user/login')
}

const handleUserMenuClick = ({ key }: { key: string }) => {
  if (key === 'logout') {
    handleLogout()
  }
}
</script>

<template>
  <a-layout-header class="global-header">
    <div class="header-left">
      <div class="logo-area">
        <img :src="logo" alt="logo" class="logo" />
        <span class="title">AI 在线编程</span>
      </div>
      <a-menu class="header-menu" mode="horizontal" :selected-keys="selectedKeys" @click="handleMenuClick">
        <a-menu-item v-for="item in menus" :key="item.key">
          {{ item.label }}
        </a-menu-item>
      </a-menu>
    </div>
    <div class="header-right">
      <!-- 已登录：展示用户头像 + 用户名 -->
      <a-dropdown v-if="isLogin">
        <div class="user-entry">
          <a-avatar :src="loginUser.userAvatar" class="user-avatar">
            <template #icon>
              <UserOutlined />
            </template>
          </a-avatar>
          <span class="user-name">{{ loginUser.userName ?? '匿名用户' }}</span>
        </div>
        <template #overlay>
          <a-menu @click="handleUserMenuClick">
            <a-menu-item key="logout">退出登录</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
      <!-- 未登录：展示登录按钮 -->
      <a-button v-else type="primary" @click="handleLogin">登录</a-button>
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

/* 用户信息入口 */
.user-entry {
  display: flex;
  align-items: center;
  line-height: normal;
  cursor: pointer;
}

.user-avatar {
  background-color: #1677ff;
}

.user-name {
  margin-left: 8px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
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
