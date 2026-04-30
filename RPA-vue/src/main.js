import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'dayjs/locale/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { permissionDirective } from './directives/permission'
import './styles.css'

const app = createApp(App)
const pinia = createPinia()

Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  app.component(key, component)
})

app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn
})
app.directive('permission', permissionDirective)

const authStore = useAuthStore(pinia)
if (authStore.isLoggedIn) {
  authStore.loadCurrentUser(true).catch(() => {
    authStore.logout()
  })
}

app.mount('#app')
