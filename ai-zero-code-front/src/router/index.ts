import { createRouter, createWebHistory } from 'vue-router'
// @ts-ignore 找不到模块的类型声明，请在项目中添加 shims-vue.d.ts 文件声明 .vue 模块类型
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      // @ts-ignore 找不到模块的类型声明，请在项目中添加 shims-vue.d.ts 文件声明 .vue 模块类型
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

export default router
