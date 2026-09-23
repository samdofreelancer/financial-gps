/// <reference types="vitest/config" />
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'

// Dev server proxies /api to the Spring Boot backend so the SPA is same-origin:
// cookies (SESSION, XSRF-TOKEN) flow without any CORS setup (007 plan §Architecture).
export default defineConfig({
  plugins: [vue()],
  resolve: {
    // Unit tests live in frontend/tests/ and import source via @/ (mirror of src/).
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  test: {
    // happy-dom allows mounting SFC components in unit tests (sosach convention).
    // Unit tests are colocated in tests/ mirroring src/ — never inside src/.
    environment: 'happy-dom',
    include: ['tests/**/*.{test,spec}.{js,ts}'],
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
