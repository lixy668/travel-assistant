import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5175,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:3200',
        changeOrigin: true,
        // 去掉 /api 前缀：/api/admin/login -> /admin/login
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
