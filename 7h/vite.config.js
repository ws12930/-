import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: './',
  server: {
    port: 5173,
    // 开发期把 /api 请求转发到 Spring Boot（application-dev.properties 端口 8081）
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 上传的图片/附件同样由后端 /uploads/** 提供，不代理的话会被 SPA fallback 到 index.html
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
