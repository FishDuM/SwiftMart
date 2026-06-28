export const LOGIN_TYPE = {
  PASSWORD: 1,
  VERIFY_CODE: 2,
} as const

export const VERIFY_CODE_TYPE = {
  REGISTER: 1,
  LOGIN: 2,
} as const

export type LoginType = (typeof LOGIN_TYPE)[keyof typeof LOGIN_TYPE]
export type VerifyCodeType = (typeof VERIFY_CODE_TYPE)[keyof typeof VERIFY_CODE_TYPE]

export type UserInfo = {
  id: number
  nickname: string
  avatar?: string
}

export type LoginResult = {
  token: string
  userInfo: UserInfo
}

export type LoginPayload = {
  mobile: string
  type: LoginType
  password?: string
  verifyCode?: string
}

export type RegisterPayload = {
  mobile: string
  password: string
  verifyCode: string
}

export type SendVerifyCodePayload = {
  mobile: string
  type: VerifyCodeType
  captchaId: string
}
