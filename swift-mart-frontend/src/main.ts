import 'virtual:uno.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'
import App from './App.vue'
import { router } from './shared/router'
import { registerUnauthorizedHandler } from './shared/api/request'
import { useAuthStore } from './entities/user/store'
import './style.css'

const app = createApp(App)

app.use(createPinia())

// 401 / 403 → 清登录态 + 跳登录；这里在挂载 router 之前注册，
// 这样拦截器在任何请求发出之前就具备兜底能力。
registerUnauthorizedHandler(() => {
  const auth = useAuthStore()
  auth.clearSession()
  const currentPath = router.currentRoute.value.fullPath
  if (!currentPath.startsWith('/login')) {
    router.replace({ name: 'login', query: { redirect: currentPath } })
  }
})

app.use(router)

app.use(VueQueryPlugin, {
  queryClientConfig: {
    defaultOptions: {
      queries: {
        refetchOnWindowFocus: false,
        retry: 0,
        staleTime: 30_000,
      },
      mutations: {
        retry: 0,
      },
    },
  },
})

app.mount('#app')