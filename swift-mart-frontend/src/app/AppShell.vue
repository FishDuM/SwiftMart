<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/entities/user/store'
import { logout } from '@/entities/user/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const keyword = ref<string>(typeof route.query.keyword === 'string' ? route.query.keyword : '')

const userInitial = computed(() => {
  const nickname = auth.userInfo?.nickname
  if (!nickname) return ''
  return nickname.slice(0, 1).toUpperCase()
})

const handleSearch = async () => {
  await router.push({
    name: 'seckill-list',
    query: keyword.value ? { keyword: keyword.value } : {},
  })
}

const handleLogout = async () => {
  try {
    await logout()
  } catch {
    /* 静默 */
  } finally {
    auth.clearSession()
  }
}
</script>

<template>
  <div class="app-shell">
    <!-- 顶部条：欢迎语 + 账号操作 -->
    <header class="app-shell__topbar">
      <div class="app-shell__topbar-inner">
        <span>欢迎来到 SwiftMart 秒杀商城 · 好货低价先到先得</span>
        <nav class="app-shell__topbar-nav">
          <RouterLink to="/seckill">商城首页</RouterLink>
          <RouterLink to="/admin/cache-preheat" class="hide-on-mobile">运营工具</RouterLink>
          <template v-if="auth.isLoggedIn">
            <span class="app-shell__user">
              <span class="user-badge">
                <span class="user-badge__avatar">{{ userInitial }}</span>
                {{ auth.userInfo?.nickname }}
              </span>
            </span>
            <button class="link-button" @click="handleLogout">退出</button>
          </template>
          <template v-else>
            <RouterLink to="/login">登录</RouterLink>
            <RouterLink to="/register">免费注册</RouterLink>
          </template>
        </nav>
      </div>
    </header>

    <!-- 头部：品牌 + 搜索 + 操作 -->
    <div class="app-shell__header">
      <div class="app-shell__header-inner">
        <RouterLink to="/seckill" class="app-shell__brand">
          <span class="app-shell__brand-logo">S</span>
          <span>SwiftMart</span>
        </RouterLink>

        <form class="app-shell__search" @submit.prevent="handleSearch">
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索品牌、机型、品类等关键词"
            aria-label="搜索"
          />
          <button type="submit">搜索</button>
        </form>

        <div class="app-shell__actions">
          <span class="tag-pill">⚡ 极速下单</span>
          <span class="tag-pill">🛡 正品保障</span>
          <span class="tag-pill is-warning">⏰ 限时秒杀</span>
        </div>
      </div>
    </div>

    <!-- 主导航 -->
    <nav class="app-shell__nav">
      <div class="app-shell__nav-inner">
        <RouterLink to="/seckill">秒杀专场</RouterLink>
        <RouterLink to="/admin/cache-preheat" class="hide-on-tablet">运营后台</RouterLink>
      </div>
    </nav>

    <!-- 内容 -->
    <main class="app-shell__main">
      <slot />
    </main>

    <!-- 底部 -->
    <footer class="app-shell__footer">
      <div class="app-shell__footer-inner">
        <section>
          <h4>SwiftMart</h4>
          <p style="margin: 0; line-height: 1.7">
            高并发秒杀商城示例项目，集成 Sa-Token + Redisson + RabbitMQ，
            <br />为开发者提供完整的高性能电商解决方案。
          </p>
        </section>
        <section>
          <h4>购物指南</h4>
          <ul>
            <li><a href="javascript:void 0">购物流程</a></li>
            <li><a href="javascript:void 0">订单查询</a></li>
            <li><a href="javascript:void 0">常见问题</a></li>
          </ul>
        </section>
        <section>
          <h4>服务支持</h4>
          <ul>
            <li><a href="javascript:void 0">在线客服</a></li>
            <li><a href="javascript:void 0">售后政策</a></li>
            <li><a href="javascript:void 0">投诉建议</a></li>
          </ul>
        </section>
        <section>
          <h4>关于我们</h4>
          <ul>
            <li><a href="javascript:void 0">公司介绍</a></li>
            <li><a href="javascript:void 0">商务合作</a></li>
            <li><a href="javascript:void 0">加入我们</a></li>
          </ul>
        </section>
        <div class="app-shell__copyright">
          © {{ new Date().getFullYear() }} SwiftMart · 高并发秒杀商城 · 仅供学习演示
        </div>
      </div>
    </footer>
  </div>
</template>