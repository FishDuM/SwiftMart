<script setup lang="ts">
import { useRouter } from 'vue-router'
import AppShell from '@/app/AppShell.vue'

const router = useRouter()
</script>

<template>
  <AppShell>
    <div class="result-page">
      <section class="result-card">
        <div class="result-card__icon" style="background: var(--color-info)">404</div>
        <h1 class="result-card__title">页面找不到了</h1>
        <p class="result-card__subtitle">访问的链接可能已下架，请返回秒杀专场继续浏览</p>

        <div class="result-card__actions">
          <button class="result-card__btn result-card__btn--ghost" type="button" @click="router.back()">
            返回上一页
          </button>
          <button class="result-card__btn result-card__btn--primary" type="button" @click="router.push('/seckill')">
            回到首页
          </button>
        </div>
      </section>
    </div>
  </AppShell>
</template>