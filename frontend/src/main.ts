import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
// Global design tokens and base components (MISA Money Keeper visual language).
import './styles/theme.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
