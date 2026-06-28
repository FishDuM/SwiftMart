<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useMutation, useQuery } from '@tanstack/vue-query'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import AppShell from '@/app/AppShell.vue'
import { getSeckillGoodsDetail } from '@/entities/goods/api'
import { doSeckill } from '@/entities/order/api'
import { useAuthStore } from '@/entities/user/store'
import { resolveSeckillState, stateLabel, isActionable, type SeckillViewState } from '@/features/seckill-countdown/state'
import { useCountdown } from '@/features/seckill-countdown/use-countdown'
import { serverNow } from '@/shared/api/server-clock'
import { formatPercent, formatPrice } from '@/shared/utils/format'
import { handleImageError } from '@/shared/utils/image'
import { normalizeError } from '@/shared/api/request'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const activityId = Number(route.params.activityId)
const goodsId = Number(route.params.goodsId)

const submitState = ref<SeckillViewState | null>(null)
const activeImageIndex = ref(0)

const safeImgs = computed<string[]>(() => goods.value?.goodsImgs ?? [])
const currentImage = computed(() => safeImgs.value[activeImageIndex.value] ?? safeImgs.value[0] ?? null)

const { data: goods, isLoading } = useQuery({
  queryKey: ['seckill-goods-detail', activityId, goodsId],
  queryFn: () => getSeckillGoodsDetail({ activityId, goodsId }),
})

watch(
  goods,
  (next) => {
    activeImageIndex.value = 0
    if (!next?.goodsImgs?.length) return
    activeImageIndex.value = Math.min(activeImageIndex.value, next.goodsImgs.length - 1)
  },
  { immediate: true },
)

const toMs = (s?: string | null) => {
  if (!s) return null
  const ms = new Date(s).getTime()
  return Number.isFinite(ms) ? ms : null
}

/**
 * 倒计时目标：未开始 → 距 begin；进行中 / 已结束 → 距 end。
 * 优先用毫秒戳，回退到字符串解析。
 */
const countdownTarget = computed(() => {
  if (!goods.value) return null
  const startMs = goods.value.beginTimeMs ?? toMs(goods.value.beginTime)
  const endMs = goods.value.endTimeMs ?? toMs(goods.value.endTime)
  const now = serverNow.value

  if (goods.value.activityStatus === 0 || (startMs != null && now < startMs)) {
    return startMs != null ? { startAtMs: startMs } : null
  }
  return endMs != null ? { deadlineMs: endMs } : null
})

const countdown = useCountdown(countdownTarget, serverNow)

const viewState = computed<SeckillViewState>(() =>
  resolveSeckillState({
    goods: goods.value,
    isLoggedIn: auth.isLoggedIn,
    submitState: submitState.value,
    serverNow,
  }),
)

const stockPercent = computed(() =>
  goods.value ? formatPercent(goods.value.seckillStock, goods.value.seckillTotal) : 0,
)

const mutation = useMutation({
  mutationFn: () => doSeckill({ activityId, goodsId }),
  onMutate: () => {
    submitState.value = 'SUBMITTING'
  },
  onSuccess: async (order) => {
    submitState.value = 'SUCCESS'
    message.success(`下单成功，订单号 ${order.orderNo}`)
    await router.replace({
      name: 'order-result',
      query: {
        orderNo: order.orderNo,
        orderId: String(order.orderId),
        seckillPrice: String(order.seckillPrice),
        goodsName: order.goodsName,
        expireAt: order.expireTime,
      },
    })
  },
  onError: (error) => {
    submitState.value = 'FAILED'
    message.error(normalizeError(error).message)
  },
})

const handleSubmit = async () => {
  if (!auth.isLoggedIn) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (viewState.value === 'RUNNING') {
    mutation.mutate()
  }
}

const handlePrevImage = () => {
  const length = safeImgs.value.length
  if (length <= 1) return
  activeImageIndex.value = (activeImageIndex.value - 1 + length) % length
}

const handleNextImage = () => {
  const length = safeImgs.value.length
  if (length <= 1) return
  activeImageIndex.value = (activeImageIndex.value + 1) % length
}
</script>

<template>
  <AppShell>
    <div class="page-container">
      <div v-if="isLoading && !goods" class="detail-page">
        <div class="detail-page__gallery-col">
          <section class="detail-gallery">
            <div class="detail-gallery__main skeleton-block" />
            <div class="detail-gallery__thumbs">
              <div v-for="i in 4" :key="i" class="skeleton-block" style="aspect-ratio: 1" />
            </div>
          </section>
        </div>
        <div class="detail-page__right">
          <section class="detail-panel">
            <div class="skeleton-block" style="height: 24px; width: 60%" />
            <div class="skeleton-block" style="height: 16px; width: 80%" />
            <div class="skeleton-block" style="height: 100px" />
            <div class="skeleton-block" style="height: 60px" />
          </section>
        </div>
      </div>

      <div v-else-if="goods" class="detail-page">
        <!-- 左列：画廊（sticky 容器） -->
        <div class="detail-page__gallery-col">
          <section class="detail-gallery">
            <div class="detail-gallery__main">
              <img
                v-if="currentImage"
                :src="currentImage"
                :alt="goods.goodsName"
                @error="handleImageError"
              />
              <div v-else class="detail-gallery__placeholder">暂无图片</div>
              <div v-if="safeImgs.length > 1" class="detail-gallery__nav">
                <button type="button" aria-label="上一张" @click="handlePrevImage">‹</button>
                <button type="button" aria-label="下一张" @click="handleNextImage">›</button>
              </div>
            </div>
            <div v-if="safeImgs.length > 1" class="detail-gallery__thumbs">
              <button
                v-for="(img, index) in safeImgs"
                :key="img"
                type="button"
                :class="{ 'is-active': index === activeImageIndex }"
                @click="activeImageIndex = index"
              >
                <img :src="img" :alt="`thumb-${index}`" @error="handleImageError" />
              </button>
            </div>
          </section>
        </div>

        <!-- 右列：面板 + 商品详情 -->
        <div class="detail-page__right">
          <section class="detail-panel">
            <div class="detail-panel__brand">
              <span class="detail-panel__brand-dot" />
              SwiftMart 自营 · 限时秒杀 · 假一赔十
            </div>

            <h1 class="detail-panel__title">{{ goods.goodsName }}</h1>
            <p class="detail-panel__sub">
              活动 ID {{ goods.activityId }} · 库存有限，售完即恢复原价
            </p>

            <div class="detail-panel__price-card">
              <div>
                <div class="detail-panel__price-label">
                  <strong>限时秒杀</strong>
                  <span>距结束 / 距开始</span>
                </div>
                <div class="detail-panel__price-row">
                  <span class="detail-panel__price-currency">¥</span>
                  <span class="detail-panel__price">{{ formatPrice(goods.seckillPrice).replace('¥', '') }}</span>
                  <span class="detail-panel__price-original">¥{{ goods.goodsPrice }}</span>
                </div>
              </div>
              <div class="detail-panel__countdown">
                <span class="detail-panel__countdown-label">
                  {{ viewState === 'NOT_STARTED' ? '距开抢' : '距结束' }}
                </span>
                <span class="detail-panel__countdown-digits">
                  <span>{{ countdown.text.value.slice(0, 2) }}</span>
                  <em>:</em>
                  <span>{{ countdown.text.value.slice(3, 5) }}</span>
                  <em>:</em>
                  <span>{{ countdown.text.value.slice(6, 8) }}</span>
                </span>
              </div>
            </div>

            <dl class="detail-panel__meta">
              <dt>活动状态</dt>
              <dd>
                <span class="tag-pill" :class="{ 'is-warning': viewState === 'RUNNING' }">
                  {{ stateLabel[viewState] }}
                </span>
                <span class="muted" style="font-size: 12px">
                  {{ goods.beginTime }} ~ {{ goods.endTime }}
                </span>
              </dd>

              <dt>剩余库存</dt>
              <dd>
                <strong>{{ goods.seckillStock }}</strong>
                <span class="muted">/ {{ goods.seckillTotal }} 件</span>
              </dd>

              <dt>发货</dt>
              <dd>
                <span class="tag-pill is-muted">付款后 48 小时内发货</span>
              </dd>
            </dl>

            <div class="detail-panel__progress">
              <div class="detail-panel__progress-text">
                <span>已抢 {{ stockPercent }}%</span>
                <strong>仅剩 {{ goods.seckillStock }} 件</strong>
              </div>
              <div class="detail-panel__progress-bar">
                <div class="detail-panel__progress-fill" :style="{ width: `${stockPercent}%` }" />
              </div>
            </div>

            <div class="detail-panel__services">
              <span class="detail-panel__service">✓ 极速发货</span>
              <span class="detail-panel__service">✓ 7 天无理由</span>
              <span class="detail-panel__service">✓ 30 天价保</span>
              <span class="detail-panel__service">✓ 官方正品</span>
              <span class="detail-panel__service">✓ 顺丰包邮</span>
            </div>

            <div class="detail-panel__actions">
              <button
                type="button"
                class="detail-panel__btn detail-panel__btn--ghost"
                @click="router.push('/seckill')"
              >
                返回会场
              </button>
              <button
                type="button"
                class="detail-panel__btn detail-panel__btn--primary"
                :disabled="!isActionable(viewState)"
                @click="handleSubmit"
              >
                <span v-if="viewState === 'SUBMITTING'">提交中…</span>
                <span v-else-if="viewState === 'NEED_LOGIN'">登录后立即抢购</span>
                <span v-else>{{ stateLabel[viewState] }}</span>
              </button>
            </div>

            <p class="detail-panel__terms">
              提交订单表示同意 <a href="javascript:void 0" style="color: var(--color-primary)">《SwiftMart 秒杀服务协议》</a>
            </p>
          </section>

          <section class="detail-content">
            <div class="section-title">
              <h2>商品介绍</h2>
              <small>图文详情由商家提供</small>
            </div>
            <div class="detail-html" v-html="goods.goodsDetail" />
          </section>
        </div>
      </div>

      <div v-else style="padding: 48px; text-align: center; color: var(--color-text-tertiary)">
        商品信息加载失败，
        <a href="javascript:void 0" style="color: var(--color-primary)" @click="router.replace('/seckill')">返回会场</a>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.detail-panel__price-card :deep(.detail-panel__price) {
  font-size: 40px;
}
</style>