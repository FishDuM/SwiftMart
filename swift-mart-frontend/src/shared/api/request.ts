import axios, { AxiosError, type AxiosInstance } from 'axios'
import { tokenStorage } from '@/shared/storage/auth-storage'
import { setServerClockFromHeaders } from './server-clock'
import { BizError, type ApiResponse } from './types'

export interface RequestExtra {
  /** 是否静默吞掉 401（默认会跳登录） */
  silent401?: boolean
  /** 业务成功时是否跳过 message 提示 */
  silentSuccess?: boolean
}

let onUnauthorized: (() => void) | null = null

/**
 * 注册全局 401 处理器（由 main.ts 在挂载 Vue 之前注册，
 * 避免在 request 中直接持有 router / store）。
 */
export const registerUnauthorizedHandler = (handler: () => void) => {
  onUnauthorized = handler
}

export const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15_000,
})

request.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    // 任意一次成功响应，都用 HTTP Date 头校准服务器时间基准
    setServerClockFromHeaders(response.headers)
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    // 错误响应也带 Date 头，照样校准（401/403 也会刷新时间）
    if (error.response?.headers) {
      setServerClockFromHeaders(error.response.headers)
    }
    // Sa-Token 鉴权失败（401/403）→ 视为登录态失效
    if (error.response?.status === 401 || error.response?.status === 403) {
      tokenStorage.clear()
      onUnauthorized?.()
    }
    return Promise.reject(error)
  },
)

/**
 * 将后端统一返参解包为业务数据。
 * 注意：响应拦截器已经处理了 HTTP 层的鉴权失败，这里只关心业务 success=false。
 */
export const unwrap = <T>(response: ApiResponse<T>): T => {
  if (!response?.success) {
    throw new BizError(response?.message || '请求失败', response?.errorCode)
  }
  return response.data
}

export const post = async <TResponse, TPayload = unknown>(
  url: string,
  payload?: TPayload,
): Promise<TResponse> => {
  const { data } = await request.post<ApiResponse<TResponse>>(url, payload)
  return unwrap(data)
}

export const get = async <TResponse>(url: string, params?: Record<string, unknown>): Promise<TResponse> => {
  const { data } = await request.get<ApiResponse<TResponse>>(url, { params })
  return unwrap(data)
}

/**
 * 把任意抛错归一化为 BizError，方便上层直接 message 展示。
 */
export const normalizeError = (error: unknown): BizError => {
  if (error instanceof BizError) return error
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiResponse<unknown> | undefined
    if (data && !data.success) {
      return new BizError(data.message || error.message, data.errorCode)
    }
    return new BizError(error.message || '网络请求异常')
  }
  if (error instanceof Error) return new BizError(error.message)
  return new BizError('未知错误')
}