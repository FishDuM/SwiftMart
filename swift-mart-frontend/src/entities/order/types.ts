export type SeckillOrderStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6

export const ORDER_STATUS_LABEL: Record<number, string> = {
  0: '待支付',
  1: '待发货',
  2: '已发货',
  3: '已收货',
  4: '已退款',
  5: '已取消',
  6: '已关闭',
}

export type DoSeckillPayload = {
  activityId: number
  goodsId: number
}

export type SeckillOrder = {
  orderId: number
  orderNo: string
  goodsName: string
  goodsImg: string
  seckillPrice: number
  status: SeckillOrderStatus
  expireTime: string
  /** 服务器当前时间（毫秒），后端实现后写入 server-clock */
  localTimeMs?: number
  /** 订单过期时间 UTC 毫秒戳（可选） */
  expireTimeMs?: number
}