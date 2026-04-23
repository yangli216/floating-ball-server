import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue2'

export default defineConfig({
  base: '/admin/',
  plugins: [vue()],
  build: {
    outDir: '../resources/static/admin',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }
          if (id.includes('element-ui')) {
            return 'element-ui'
          }
        }
      }
    }
  },
  server: {
    port: 5174
  }
})
