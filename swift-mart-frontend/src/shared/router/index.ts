import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/entities/user/store'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/seckill',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/login/LoginPage.vue'),
      meta: { layout: 'blank' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/pages/register/RegisterPage.vue'),
      meta: { layout: 'blank' },
    },
    {
      path: '/seckill',
      name: 'seckill-list',
      component: () => import('@/pages/seckill-list/SeckillListPage.vue'),
    },
    {
      path: '/seckill/:activityId/:goodsId',
      name: 'seckill-detail',
      component: () => import('@/pages/seckill-detail/SeckillDetailPage.vue'),
    },
    {
      path: '/order/result',
      name: 'order-result',
      component: () => import('@/pages/order-result/OrderResultPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/cache-preheat',
      name: 'admin-cache-preheat',
      component: () => import('@/pages/admin-cache-preheat/AdminCachePreheatPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/pages/not-found/NotFoundPage.vue'),
    },
  ],
  scrollBehavior: () => ({ left: 0, top: 0 }),
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 已登录访问登录/注册页时直接回首页
  if ((to.name === 'login' || to.name === 'register') && auth.isLoggedIn) {
    return { name: 'seckill-list' }
  }
  return true
})