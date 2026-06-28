import { post } from '@/shared/api/request'
import type { DoSeckillPayload, SeckillOrder } from './types'

export const doSeckill = (payload: DoSeckillPayload) =>
  post<SeckillOrder, DoSeckillPayload>('/seckill/order', payload)