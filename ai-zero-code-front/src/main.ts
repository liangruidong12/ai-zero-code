import { createApp } from 'vue'
import { createPinia } from 'pinia'

// @ts-ignore Vue 单文件组件需要在 env.d.ts 中声明类型
import App from './App.vue'
import router from './router'

import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)

app.mount('#app')
