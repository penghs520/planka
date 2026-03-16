import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ArcoVue from '@arco-design/web-vue'
import '@arco-design/web-vue/dist/arco.css'

// UnoCSS
import 'virtual:uno.css'

// 主题和样式
import './styles/theme.css'
import './style.css'
import './styles/admin-table.scss'

import App from './App.vue'
import router from './router'
import i18n from './i18n'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ArcoVue)
app.use(i18n)

app.mount('#app')
