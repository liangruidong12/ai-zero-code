import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../pages/HomeView.vue'
import UserLoginPage from '../pages/user/UserLoginPage.vue'
import UserRegisterPage from '../pages/user/UserRegisterPage.vue'
import UserManagerAdmin from '../pages/admin/UserManagerAdmin.vue'
import AppManagerAdmin from '../pages/admin/AppManagerAdmin.vue'
import AppChatPage from '../pages/app/AppChatPage.vue'
import AppEditPage from '../pages/app/AppEditPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/admin/user',
      name: '用户管理',
      component: UserManagerAdmin,
    },
    {
      path: '/admin/app',
      name: '应用管理',
      component: AppManagerAdmin,
    },
    {
      // 应用生成对话页（全屏布局，不带全局头部/底部）
      path: '/app/chat/:id',
      name: '应用对话',
      component: AppChatPage,
      meta: { fullscreen: true },
    },
    {
      // 应用信息修改页（用户只能编辑自己的应用，管理员可编辑任意应用）
      path: '/app/edit/:id',
      name: '应用编辑',
      component: AppEditPage,
    },
  ],
})

export default router
