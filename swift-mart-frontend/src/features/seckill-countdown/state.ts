import type { ComputedRef, Ref } from 'vue'
import type { SeckillGoodsDetail, SeckillGoodsSummary } from '@/entities/goods/types'

/**
 * 秒杀按钮的视图状态机，覆盖从商品拉取到下单全链路
 */
export type SeckillViewState =
  | 'NOT_STARTED' // 未开始（倒计时中）
  | 'RUNNING' // 抢购进行中
  | 'SOLD_OUT' // 已售罄
  | 'ENDED' // 已结束
  | 'NEED_LOGIN' // 未登录引导
  | 'SUBMITTING' // 提交中
  | 'SUCCESS' // 下单成功
  | 'FAILED' // 下单失败

export interface SeckillViewContext {
  goods?: Pick<SeckillGoodsSummary | SeckillGoodsDetail, 'activityStatus' | 'beginTime' | 'endTime' | 'seckillStock' | 'endTimeMs' | 'beginTimeMs'> | null
  isLoggedIn: boolean
  submitState?: SeckillViewState | null
  /** 当前服务器时间（毫秒），优先于客户端本地时间 */
  serverNow?: number | Ref<number> | ComputedRef<number>
}

const toNumber = (value: SeckillViewContext['serverNow']): number => {
  if (typeof value === 'number') return value
  if (value && typeof value === 'object' && 'value' in value) return (value as { value: number }).value
  return Date.now()
}

export const resolveSeckillState = (ctx: SeckillViewContext): SeckillViewState => {
  if (ctx.submitState) return ctx.submitState
  if (!ctx.isLoggedIn) return 'NEED_LOGIN'
  const goods = ctx.goods
  if (!goods) return 'NOT_STARTED'

  const now = toNumber(ctx.serverNow)

  if (goods.seckillStock <= 0) return 'SOLD_OUT'

  // 已结束：优先用 endTimeMs（毫秒戳）做服务器时间比较，回退到 activityStatus
  const endTimeMs = goods.endTimeMs ?? (goods.endTime ? new Date(goods.endTime).getTime() : NaN)
  if (goods.activityStatus === 2 || (Number.isFinite(endTimeMs) && now >= endTimeMs)) return 'ENDED'

  // 未开始：用 beginTimeMs 比较
  const beginTimeMs = goods.beginTimeMs ?? (goods.beginTime ? new Date(goods.beginTime).getTime() : NaN)
  if (goods.activityStatus === 0 || (Number.isFinite(beginTimeMs) && now < beginTimeMs)) return 'NOT_STARTED'

  return 'RUNNING'
}

export const stateLabel: Record<SeckillViewState, string> = {
  NOT_STARTED: '即将开抢',
  RUNNING: '立即抢购',
  SOLD_OUT: '已抢光',
  ENDED: '活动已结束',
  NEED_LOGIN: '登录后抢购',
  SUBMITTING: '提交中…',
  SUCCESS: '下单成功',
  FAILED: '下单失败，请重试',
}

export const isActionable = (state: SeckillViewState): boolean =>
  state === 'RUNNING' || state === 'NEED_LOGIN' || state === 'SUBMITTING'