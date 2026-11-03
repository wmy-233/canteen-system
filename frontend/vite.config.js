import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 开发环境代理后端接口，前端代码统一请求 /api 即可
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
