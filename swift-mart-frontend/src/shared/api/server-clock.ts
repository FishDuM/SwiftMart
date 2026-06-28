/**
 * 全局 server clock：单一权威源。
 *
 * 设计：
 *  - 不依赖业务字段，后端只需要在 HTTP 响应头里带上标准的 `Date: ...`（任何 Web 框架默认都会发）。
 *  - axios 响应拦截器在每次响应后调用 `setServerClockFromHeaders(headers)`：
 *      serverTimeMs = Date header 解析出来的 UTC 毫秒
 *      clientReceivedAt = Date.now()（客户端收到响应的本地时间）
 *  - 之后任意时刻的"服务器当前时间" = serverTimeMs + (Date.now() - clientReceivedAt)
 *  - 即便服务器时钟与客户端时钟存在偏差，倒计时也以服务器时间为准。
 *  - 后端没发 Date header（或解析失败）时，全部退化为 Date.now()，不影响功能。
 *  - 1Hz 心跳让所有依赖 serverNow 的倒计时/状态机每秒刷新一次。
 */
import { computed, ref } from 'vue'

const serverTimeMs = ref<number | null>(null)
const clientReceivedAt = ref<number | null>(null)
const tick = ref(0)

let timer: ReturnType<typeof setInterval> | null = null

const ensureTimer = () => {
  if (timer !== null) return
  if (typeof window === 'undefined') return
  timer = setInterval(() => {
    tick.value += 1
  }, 1000)
}

const stopTimer = () => {
  if (timer !== null) {
    clearInterval(timer)
    timer = null
  }
}

/**
 * 直接写入服务器时间锚点（毫秒）。
 * 传入 null / undefined / 非数 表示清除。
 */
export const setServerClock = (ms: number | null | undefined): void => {
  if (typeof ms === 'number' && Number.isFinite(ms)) {
    serverTimeMs.value = ms
    clientReceivedAt.value = Date.now()
    ensureTimer()
  }
}

/**
 * 从 axios response headers 提取 Date 头，写入 server clock。
 * axios 会自动把 headers key 转成小写。
 */
export const setServerClockFromHeaders = (headers: unknown): void => {
  if (!headers || typeof headers !== 'object') return
  const h = headers as Record<string, unknown>
  const raw = h.date ?? h.Date
  if (typeof raw !== 'string') return
  const ms = new Date(raw).getTime()
  setServerClock(ms)
}

/**
 * 响应式"服务器当前时间"（毫秒）。
 * 后端没传过 Date 头时退化为客户端 Date.now()。
 */
export const serverNow = computed(() => {
  void tick.value // 心跳驱动刷新
  if (serverTimeMs.value === null || clientReceivedAt.value === null) {
    return Date.now()
  }
  return serverTimeMs.value + (Date.now() - clientReceivedAt.value)
})

/** 是否已经收到过服务器时间校准 */
export const hasServerClock = computed(() => serverTimeMs.value !== null)

/** 测试/调试用：清除校准，回到本地时间 */
export const __resetServerClock = () => {
  stopTimer()
  serverTimeMs.value = null
  clientReceivedAt.value = null
  tick.value = 0
}