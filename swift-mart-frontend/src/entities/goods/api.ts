import { post } from '@/shared/api/request'
import type {
  PreheatPayload,
  SeckillGoodsDetail,
  SeckillGoodsDetailPayload,
  SeckillGoodsListPayload,
  SeckillGoodsSummary,
} from './types'

export const getSeckillGoodsList = (payload: SeckillGoodsListPayload) =>
  post<SeckillGoodsSummary[], SeckillGoodsListPayload>('/seckill/goods/list', payload)

export const getSeckillGoodsDetail = (payload: SeckillGoodsDetailPayload) =>
  post<SeckillGoodsDetail, SeckillGoodsDetailPayload>('/seckill/goods/detail', payload)

export const preheatActivityCache = (payload: PreheatPayload) =>
  post<unknown, PreheatPayload>('/admin/seckill/goods/cache/preheat', payload)