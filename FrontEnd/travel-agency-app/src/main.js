import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router/index.js'
import { tenantConfig } from './config/tenantConfig.js'

document.documentElement.setAttribute('data-tenant-theme', tenantConfig.themeName)
document.documentElement.setAttribute('data-tenant-key', tenantConfig.key)

createApp(App).use(router).mount('#app')
