import { post } from '@/shared/api/request'
import type { LoginPayload, LoginResult, RegisterPayload, SendVerifyCodePayload } from './types'

export const login = (payload: LoginPayload) => post<LoginResult, LoginPayload>('/user/login', payload)

export const register = (payload: RegisterPayload) => post<void, RegisterPayload>('/user/register', payload)

export const logout = () => post<void>('/user/logout')

export const sendVerifyCode = (payload: SendVerifyCodePayload) =>
  post<void, SendVerifyCodePayload>('/user/code/send', payload)