/** 后端 ActivityStatusEnum：0=未开始 1=进行中 2=已结束 */
export type ActivityStatus = 0 | 1 | 2

export const ACTIVITY_STATUS = {
  NOT_STARTED: 0,
  RUNNING: 1,
  ENDED: 2,
} as const satisfies Record<string, ActivityStatus>

export const ACTIVITY_STATUS_LABEL: Record<ActivityStatus, string> = {
  0: '即将开始',
  1: '抢购进行中',
  2: '活动已结束',
}

/** 秒杀商品列表项 */
export type SeckillGoodsSummary = {
  id: number
  goodsId: number
  activityId: number
  goodsPrice: number
  seckillTitle: string
  seckillImg: string
  seckillPrice: number
  seckillTotal: number
  seckillStock: number
  activityStatus: ActivityStatus
  beginTime: string
  endTime: string
  /**
   * 服务器当前时间（毫秒），后端尚未实现时为 undefined，前端会退化为本地时间。
   * 一旦后端在响应中带上，前端会立即用作"现在"基准。
   */
  localTimeMs?: number
  /** 活动开始时间 UTC 毫秒戳（可选，后端如未实现则按 beginTime 字符串解析） */
  beginTimeMs?: number
  /** 活动结束时间 UTC 毫秒戳（可选） */
  endTimeMs?: number
}

/** 秒杀商品详情 = 列表项 + 商品名 + 详情 + 图集 */
export type SeckillGoodsDetail = SeckillGoodsSummary & {
  goodsName: string
  goodsImgs: string[]
  goodsDetail: string
}

export type SeckillGoodsListPayload = {
  activityId: number
}

export type SeckillGoodsDetailPayload = {
  activityId: number
  goodsId: number
}

export type PreheatPayload = {
  activityId: number
}