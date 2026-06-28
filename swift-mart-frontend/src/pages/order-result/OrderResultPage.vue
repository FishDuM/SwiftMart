<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import AppShell from '@/app/AppShell.vue'
import { useAuthStore } from '@/entities/user/store'
import { useCountdown } from '@/features/seckill-countdown/use-countdown'
import { serverNow } from '@/shared/api/server-clock'
import { formatPrice } from '@/shared/utils/format'

const route = useRoute()
const message = useMessage()
const auth = useAuthStore()

const orderNo = computed(() => (typeof route.query.orderNo === 'string' ? route.query.orderNo : ''))
const orderId = computed(() => (typeof route.query.orderId === 'string' ? route.query.orderId : ''))
const seckillPrice = computed(() => {
  const raw = route.query.seckillPrice
  if (typeof raw !== 'string') return null
  const num = Number(raw)
  return Number.isFinite(num) ? num : null
})
const goodsName = computed(() => (typeof route.query.goodsName === 'string' ? route.query.goodsName : ''))
const expireAt = computed(() => (typeof route.query.expireAt === 'string' ? route.query.expireAt : null))

const toMs = (s?: string | null) => {
  if (!s) return null
  const ms = new Date(s).getTime()
  return Number.isFinite(ms) ? ms : null
}

const countdownTarget = computed(() => {
  const ms = toMs(expireAt.value)
  return ms != null ? { deadlineMs: ms } : null
})

const { text, expired } = useCountdown(countdownTarget, serverNow)

const expiredTimer = ref<number | null>(null)

const closeExpiredPolling = () => {
  if (expiredTimer.value) {
    window.clearInterval(expiredTimer.value)
    expiredTimer.value = null
  }
}

const setupExpiredPolling = () => {
  closeExpiredPolling()
  if (!expireAt.value) return
  expiredTimer.value = window.setInterval(() => {
    if (expired.value) {
      closeExpiredPolling()
      message.warning('订单已过期，请重新下单')
    }
  }, 1000)
}

setupExpiredPolling()
onBeforeUnmount(closeExpiredPolling)
</script>

<template>
  <AppShell>
    <div class="result-page">
      <section class="result-card">
        <div class="result-card__icon">✓</div>
        <h1 class="result-card__title">下单成功 · 请尽快支付</h1>
        <p class="result-card__subtitle">超时未支付订单将自动关闭，库存同步释放</p>

        <div v-if="expireAt && !expired" class="result-card__countdown">
          <span>支付剩余时间</span>
          <strong>{{ text }}</strong>
        </div>
        <div
          v-else-if="expireAt && expired"
          class="result-card__countdown"
          style="background: #fff1f2; color: var(--color-danger)"
        >
          <span>订单已过期</span>
        </div>

        <div class="result-card__detail">
          <div class="result-card__detail-row">
            <span>订单号</span>
            <strong>{{ orderNo || '—' }}</strong>
          </div>
          <div class="result-card__detail-row">
            <span>订单 ID</span>
            <strong>{{ orderId || '—' }}</strong>
          </div>
          <div class="result-card__detail-row">
            <span>秒杀商品</span>
            <strong>{{ goodsName || '—' }}</strong>
          </div>
          <div class="result-card__detail-row">
            <span>实付金额</span>
            <strong>{{ seckillPrice !== null ? formatPrice(seckillPrice) : '—' }}</strong>
          </div>
          <div class="result-card__detail-row">
            <span>下单账号</span>
            <strong>{{ auth.userInfo?.nickname || '游客' }}</strong>
          </div>
        </div>

        <div class="result-card__actions">
          <RouterLink class="result-card__btn result-card__btn--ghost" to="/seckill">
            继续逛逛
          </RouterLink>
          <button class="result-card__btn result-card__btn--primary" type="button" @click="$router.push('/seckill')">
            返回秒杀会场
          </button>
        </div>
      </section>
    </div>
  </AppShell>
</template>