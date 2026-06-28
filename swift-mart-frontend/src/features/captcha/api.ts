import { request } from '@/shared/api/request'
import type { CaptchaCheckResult, CaptchaGenerateResult, CaptchaTrackPayload } from './types'

/**
 * 生成行为验证码（默认 SLIDER 滑块）
 * 返参不走 ApiResponse 包装，是 TianAI SDK 自己的格式。
 */
export const generateCaptcha = async (type = 'SLIDER') => {
  const { data } = await request.post<CaptchaGenerateResult>('/captcha/gen', undefined, {
    params: { type },
  })
  return data
}

export const checkCaptcha = async (payload: CaptchaTrackPayload) => {
  const { data } = await request.post<CaptchaCheckResult>('/captcha/check', payload)
  return data
}