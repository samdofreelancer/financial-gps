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
    // Vite >= 5.4.12 refuses requests whose Host is neither localhost nor an IP
    // (DNS-rebinding guard) and answers 403 "Blocked request". The compose stack reaches
    // this dev server through the `frontend` service name (the e2e container drives the
    // SPA at http://frontend:4173), so that name must be on the allow-list or every
    // container-driven run — `docker compose --profile e2e up` / CI — renders nothing.
    allowedHosts: ['frontend'],
    proxy: {
      '/api': process.env.API_BASE_URL || 'http://localhost:8080',
    },
  },
})
