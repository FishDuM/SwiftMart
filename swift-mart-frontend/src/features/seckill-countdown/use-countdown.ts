import { computed, onBeforeUnmount, ref, watch, type ComputedRef, type Ref } from 'vue'

export interface CountdownTarget {
  /** 倒计时指向的截止时刻（UTC 毫秒） */
  deadlineMs?: number | null
  /** 倒计时指向的开始时刻（UTC 毫秒） */
  startAtMs?: number | null
  /** 是否使用短格式 mm:ss */
  short?: boolean
}

export interface CountdownResult {
  /** 剩余秒数（已 clamp 到 0） */
  remainingSeconds: ComputedRef<number>
  /** 是否到期 */
  expired: ComputedRef<boolean>
  /** 是否尚未开始 */
  notStarted: ComputedRef<boolean>
  /** 展示文案 */
  text: ComputedRef<string>
  /** 重新触发 */
  refresh: () => void
}

/**
 * 通用倒计时 hook（基于服务器时间基准）：
 *  - 接受 deadlineMs / startAtMs（毫秒戳）和 serverNow 响应式引用
 *  - 1Hz 心跳驱动剩余秒数刷新
 *  - 自动在 unmount 清理
 */
export const useCountdown = (
  target: Ref<CountdownTarget | null | undefined>,
  serverNow: ComputedRef<number>,
): CountdownResult => {
  const tick = ref(0)
  let timer: number | null = null

  const ensureTimer = () => {
    if (timer === null) {
      timer = window.setInterval(() => {
        tick.value += 1
      }, 1000)
    }
  }

  ensureTimer()

  onBeforeUnmount(() => {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  })

  const remainingSeconds = computed(() => {
    void tick.value
    const value = target.value
    if (!value) return 0

    const now = serverNow.value
    if (value.deadlineMs != null) {
      return Math.max(Math.floor((value.deadlineMs - now) / 1000), 0)
    }
    if (value.startAtMs != null) {
      return Math.max(Math.floor((value.startAtMs - now) / 1000), 0)
    }
    return 0
  })

  const expired = computed(() => {
    const value = target.value
    if (value?.deadlineMs == null) return false
    void tick.value
    return serverNow.value >= value.deadlineMs
  })

  const notStarted = computed(() => {
    const value = target.value
    if (value?.startAtMs == null) return false
    void tick.value
    return serverNow.value < value.startAtMs
  })

  const text = computed(() => {
    const total = remainingSeconds.value
    if (total <= 0) return target.value?.short ? '00:00' : '00:00:00'
    if (target.value?.short) {
      const minutes = Math.floor(total / 60)
      const seconds = total % 60
      return [minutes, seconds].map((n) => String(n).padStart(2, '0')).join(':')
    }
    const hours = Math.floor(total / 3600)
    const minutes = Math.floor((total % 3600) / 60)
    const seconds = total % 60
    return [hours, minutes, seconds].map((n) => String(n).padStart(2, '0')).join(':')
  })

  watch(target, () => {
    tick.value += 1
  })

  return {
    remainingSeconds,
    expired,
    notStarted,
    text,
    refresh: () => {
      tick.value += 1
    },
  }
}