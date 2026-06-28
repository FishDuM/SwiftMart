/**
 * 鉴权相关本地存储：使用 sessionStorage 防止跨标签串号；
 * 但 token 持久化在 localStorage 让用户刷新页面后仍然登录。
 */

const TOKEN_KEY = 'swift_mart_token'
const USER_KEY = 'swift_mart_user'

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export const jsonStorage = {
  get: <T>(key = USER_KEY): T | null => {
    try {
      const value = localStorage.getItem(key)
      return value ? (JSON.parse(value) as T) : null
    } catch {
      // 解析失败时清掉坏数据
      localStorage.removeItem(key)
      return null
    }
  },
  set: <T>(value: T, key = USER_KEY) => {
    try {
      localStorage.setItem(key, JSON.stringify(value))
    } catch {
      /* 容量超限等异常静默 */
    }
  },
  clear: (key = USER_KEY) => localStorage.removeItem(key),
}