import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址：默认指向本机局域网 IP，可用环境变量 BLOG_HOST 覆盖
//   set BLOG_HOST=192.168.68.100 && npm run dev
const BACKEND = `http://${process.env.BLOG_HOST || '192.168.68.77'}:8081`

export default defineConfig({
  plugins: [vue()],
  base: './',
  server: {
    port: 5173,
    // 监听所有网卡：手机 / 同网段电脑可用 http://192.168.68.77:5173 打开开发版
    host: '0.0.0.0',
    // 开发期把 /api 请求转发到 Spring Boot（端口 8081）
    proxy: {
      '/api': {
        target: BACKEND,
        changeOrigin: true
      },
      // 上传的图片/附件同样由后端 /uploads/** 提供，不代理的话会被 SPA fallback 到 index.html
      '/uploads': {
        target: BACKEND,
        changeOrigin: true
      }
    }
  }
})
