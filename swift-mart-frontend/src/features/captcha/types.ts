/**
 * TianAI 行为验证码 /captcha/gen 返回结构
 * 详见 cloud.tianai.captcha.application.vo.ImageCaptchaVO
 */
export type CaptchaGenerateResult = {
  id: string
  data: {
    backgroundImage: string
    sliderImage: string
    backgroundImageTag?: string
    sliderImageTag?: string
    backgroundImageWidth?: number
    backgroundImageHeight?: number
    sliderImageWidth?: number
    sliderImageHeight?: number
  }
  captchaType?: string
  ts?: number
}

export type CaptchaCheckResult = {
  id: string
}

export type CaptchaTrackPayload = {
  id: string
  data: unknown
}

/**
 * 用户滑动轨迹，传递给 /captcha/check 做真人识别
 */
export type SliderTrack = {
  bgImageWidth: number
  bgImageHeight: number
  sliderImageWidth: number
  sliderImageHeight: number
  startTime: number
  endTime: number
  trackList: Array<{ x: number; y: number; t: number; type?: string }>
}