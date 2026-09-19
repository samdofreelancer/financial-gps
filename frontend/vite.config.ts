/// <reference types="vitest/config" />
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// Dev server proxies /api to the Spring Boot backend so the SPA is same-origin:
// cookies (SESSION, XSRF-TOKEN) flow without any CORS setup (007 plan §Architecture).
export default defineConfig({
  plugins: [vue()],
  test: {
    // happy-dom allows mounting SFC components in unit tests (sosach convention).
    environment: 'happy-dom',
    include: ['src/**/*.{test,spec}.{js,ts}'],
  },
  server: {
    host: '0.0.0.0',
    port: 5174,
    strictPort: true,
    proxy: {
      '/api': process.env.API_BASE_URL || 'http://localhost:8080',
    },
  },
})
