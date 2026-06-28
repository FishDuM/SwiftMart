<script setup lang="ts">
import { useRouter } from 'vue-router'
import AppShell from '@/app/AppShell.vue'

const router = useRouter()
</script>

<template>
  <AppShell>
    <main class="result-page">
      <section class="result-card">
        <div class="result-card__icon">
          <slot name="icon">✓</slot>
        </div>
        <h1 class="result-card__title">
          <slot name="title">操作完成</slot>
        </h1>
        <p class="result-card__subtitle">
          <slot name="subtitle" />
        </p>

        <slot name="extra" />

        <div class="result-card__actions">
          <slot name="actions">
            <button class="result-card__btn result-card__btn--primary" type="button" @click="router.push('/seckill')">
              返回秒杀
            </button>
          </slot>
        </div>

        <slot name="footer" />
      </section>
    </main>
  </AppShell>
</template>