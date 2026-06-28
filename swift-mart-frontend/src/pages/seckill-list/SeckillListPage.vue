<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import AppShell from '@/app/AppShell.vue'
import { getSeckillGoodsList } from '@/entities/goods/api'
import { ACTIVITY_STATUS, type ActivityStatus, type SeckillGoodsSummary } from '@/entities/goods/types'
import { resolveSeckillState, stateLabel } from '@/features/seckill-countdown/state'
import CountdownBanner from '@/features/seckill-countdown/CountdownBanner.vue'
import { serverNow } from '@/shared/api/server-clock'
import { useAuthStore } from '@/entities/user/store'
import { formatPercent } from '@/shared/utils/format'
import { handleImageError } from '@/shared/utils/image'
import { normalizeError } from '@/shared/api/request'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const activityId = ref<number>(
  Number(Array.isArray(route.query.activityId) ? route.query.activityId[0] : route.query.activityId) || 1,
)

const activeFilter = ref<'ALL' | ActivityStatus>('ALL')
const refreshKey = ref(0)

const { data, isLoading, isFetching, error } = useQuery({
  queryKey: ['seckill-goods-list', activityId, refreshKey],
  queryFn: () => getSeckillGoodsList({ activityId: activityId.value }),
})

watch(error, (err) => {
  if (err) message.error(normalizeError(err).message)
})

/**
 * 把后端时间字符串解析成毫秒戳。
 * 后端序列化 LocalDateTime 不带时区信息，假定服务器与浏览器同处 Asia/Shanghai，
 * 与 Date header 的语义一致（前端拿 Date header 校准）。
 */
const toEpochMs = (time: string | undefined | null): number => {
  if (!time) return NaN
  const ms = new Date(time).getTime()
  return Number.isFinite(ms) ? ms : NaN
}

const isExpired = (item: SeckillGoodsSummary, now: number): boolean => {
  if (item.activityStatus === 2) return true
  const endMs = item.endTimeMs ?? toEpochMs(item.endTime)
  return Number.isFinite(endMs) && now >= endMs
}

/**
 * 排序：进行中 > 即将开抢 > 已结束；同状态内按 sort/endTime。
 * 同时按 serverNow 过滤掉已过期商品。
 */
const visibleList = computed<SeckillGoodsSummary[]>(() => {
  const list = data.value ?? []
  const now = serverNow.value
  return list.filter((item) => !isExpired(item, now))
})

const filtered = computed<SeckillGoodsSummary[]>(() => {
  if (activeFilter.value === 'ALL') return visibleList.value
  return visibleList.value.filter((item) => item.activityStatus === activeFilter.value)
})

const totalCount = computed(() => data.value?.length ?? 0)
const visibleCount = computed(() => visibleList.value.length)

const summary = computed(() => {
  const list = visibleList.value
  if (!list.length) return null
  const ongoing = list.filter((item) => item.activityStatus === ACTIVITY_STATUS.RUNNING).length
  const upcoming = list.filter((item) => item.activityStatus === ACTIVITY_STATUS.NOT_STARTED).length
  return { ongoing, upcoming, total: list.length }
})

const handleRefresh = () => {
  refreshKey.value += 1
}

const handleJumpDetail = (goods: SeckillGoodsSummary) => {
  router.push(`/seckill/${goods.activityId}/${goods.goodsId}`)
}

const handleApplyActivity = () => {
  router.replace({ name: 'seckill-list', query: { activityId: String(activityId.value) } })
}

const filters: Array<{ label: string; value: 'ALL' | ActivityStatus }> = [
  { label: '全部', value: 'ALL' },
  { label: '抢购中', value: ACTIVITY_STATUS.RUNNING },
  { label: '即将开抢', value: ACTIVITY_STATUS.NOT_STARTED },
]
</script>

<template>
  <AppShell>
    <div class="page-container">
      <!-- Hero Banner -->
      <section class="mall-banner">
        <div class="mall-banner__aside">
          <span class="mall-banner__badge">⚡ 限时秒杀</span>
          <h1>SwiftMart<br />限时秒杀</h1>
          <small>好货低价 · 整点开抢 · 库存有限</small>
        </div>
        <div class="mall-banner__main">
          <h2>今日秒杀 · 抢到就是赚到</h2>
          <p>
            活动 ID <strong style="color: var(--color-primary)">{{ activityId }}</strong>，
            当前共 <strong>{{ visibleCount }}</strong> 款商品在售
            <span v-if="totalCount !== visibleCount" class="muted">
              （已隐藏 {{ totalCount - visibleCount }} 款已结束商品）
            </span>
            <span v-if="summary" style="color: var(--color-warning); display: block; margin-top: 4px">
              抢购中 {{ summary.ongoing }} 款 · 即将开抢 {{ summary.upcoming }} 款
            </span>
          </p>
          <div class="mall-filter__activity">
            <span>切换活动 ID：</span>
            <input
              v-model.number="activityId"
              type="number"
              min="1"
              placeholder="活动 ID"
              @keyup.enter="handleApplyActivity"
            />
            <button type="button" @click="handleApplyActivity">确定</button>
            <button type="button" class="mall-filter__activity-refresh" @click="handleRefresh">
              {{ isFetching ? '刷新中…' : '刷新' }}
            </button>
          </div>
          <a class="mall-banner__cta" href="javascript:void 0">
            🛒 立即逛秒杀
          </a>
        </div>
      </section>

      <!-- 状态过滤 -->
      <section class="mall-filter">
        <div class="mall-filter__tabs">
          <button
            v-for="item in filters"
            :key="item.value"
            type="button"
            :class="{ 'is-active': activeFilter === item.value }"
            @click="activeFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>
        <span class="muted" style="font-size: 12px">
          {{ auth.isLoggedIn ? `已登录：${auth.userInfo?.nickname}` : '未登录：登录后可参与秒杀' }}
        </span>
      </section>

      <!-- 列表 -->
      <section>
        <div class="section-title">
          <h2>{{ activeFilter === 'ALL' ? '全部秒杀' : filters.find((f) => f.value === activeFilter)?.label }}</h2>
          <small v-if="!isLoading">共 {{ filtered.length }} 款商品</small>
        </div>

        <div v-if="isLoading" class="goods-grid-skeleton">
          <div v-for="i in 8" :key="i" class="goods-card-skeleton">
            <div class="skeleton-block" style="aspect-ratio: 1" />
            <div class="goods-card-skeleton__body">
              <div class="skeleton-block" style="height: 14px" />
              <div class="skeleton-block" style="height: 14px; width: 70%" />
              <div class="skeleton-block" style="height: 22px; width: 50%" />
              <div class="skeleton-block" style="height: 6px" />
            </div>
          </div>
        </div>

        <div v-else-if="filtered.length" class="goods-grid">
          <article v-for="goods in filtered" :key="goods.id" class="goods-card">
            <div class="goods-card__media" @click="handleJumpDetail(goods)">
              <span class="goods-card__tag">秒杀</span>
              <span class="goods-card__tag is-secondary">
                {{ stateLabel[resolveSeckillState({ goods, isLoggedIn: auth.isLoggedIn, serverNow })] }}
              </span>
              <img
                :src="goods.seckillImg"
                :alt="goods.seckillTitle"
                loading="lazy"
                @error="handleImageError"
                @click="handleJumpDetail(goods)"
              />
            </div>

            <div class="goods-card__body">
              <h3 class="goods-card__title" :title="goods.seckillTitle">{{ goods.seckillTitle }}</h3>

              <div class="goods-card__price-row">
                <span class="goods-card__price">
                  <span class="goods-card__price-currency">¥</span>{{ Math.floor(goods.seckillPrice) }}
                  <span class="goods-card__price-currency">.{{ String(goods.seckillPrice).split('.')[1] || '00' }}</span>
                </span>
                <span class="goods-card__price-original">¥{{ goods.goodsPrice }}</span>
              </div>

              <div class="goods-card__progress">
                <div class="goods-card__progress-bar">
                  <div
                    class="goods-card__progress-fill"
                    :style="{ width: `${formatPercent(goods.seckillStock, goods.seckillTotal)}%` }"
                  />
                </div>
                <div class="goods-card__progress-text">
                  <span>已抢 {{ formatPercent(goods.seckillStock, goods.seckillTotal) }}%</span>
                  <strong>仅剩 {{ goods.seckillStock }} 件</strong>
                </div>
              </div>
            </div>

            <div class="goods-card__footer">
              <CountdownBanner
                :start-at-ms="goods.beginTimeMs"
                :end-at-ms="goods.endTimeMs"
                :fallback-start="goods.beginTime"
                :fallback-end="goods.endTime"
                :full="false"
              />
              <button
                class="goods-card__action"
                :class="{ 'is-disabled': goods.seckillStock <= 0 }"
                type="button"
                @click="handleJumpDetail(goods)"
              >
                {{ goods.seckillStock > 0 ? '去抢购 ›' : '已抢光' }}
              </button>
            </div>
          </article>
        </div>

        <div
          v-else
          style="
            padding: 48px 16px;
            background: var(--color-bg-elevated);
            border-radius: var(--radius-lg);
            text-align: center;
            color: var(--color-text-tertiary);
          "
        >
          <p style="margin: 0; font-size: 15px">
            {{ totalCount > 0 && visibleCount === 0 ? '该活动下的商品均已结束，已自动隐藏' : '该筛选条件下暂无商品' }}
          </p>
          <button
            v-if="activeFilter !== 'ALL'"
            class="auth-card__btn auth-card__btn--ghost"
            style="margin-top: 12px"
            type="button"
            @click="activeFilter = 'ALL'"
          >
            查看全部
          </button>
        </div>
      </section>
    </div>
  </AppShell>
</template>

<style scoped>
.mall-filter__activity-refresh {
  background: var(--color-bg-hover) !important;
  color: var(--color-text) !important;
}
.mall-filter__activity-refresh:hover {
  background: var(--color-border) !important;
}
</style>