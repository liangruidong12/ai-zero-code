import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
      },
      '/app': {
        target: 'http://localhost:8123',
        changeOrigin: true,
        // 浏览器导航请求（Accept: text/html）是 Vue Router 前端路由，不转发给后端
        bypass(req) {
          if (req.headers.accept?.includes('text/html')) return req.url
        },
      },
      '/user': {
        target: 'http://localhost:8123',
        changeOrigin: true,
        bypass(req) {
          if (req.headers.accept?.includes('text/html')) return req.url
        },
      },
      '/chatHistory': {
        target: 'http://localhost:8123',
        changeOrigin: true,
      },
    },
  },
})
