<script setup lang="ts">
import { computed } from 'vue'
import { useCountdown } from './use-countdown'
import { serverNow } from './use-server-now'

interface Props {
  /** 距开抢的目标时间（UTC 毫秒） */
  startAtMs?: number | null
  /** 距结束的截止时间（UTC 毫秒） */
  endAtMs?: number | null
  /**
   * 后端时间字符串回退：
   *  当 startAtMs/endAtMs 未提供时使用，按浏览器本地时区解析成毫秒（与服务器时区一致则误差为零）
   */
  fallbackStart?: string | null
  fallbackEnd?: string | null
  /**
   * 显示风格：
   *  - 'auto'（默认）：> 1 天 显示 "X 天 HH:MM:SS"；> 1 小时 显示 "HH:MM:SS"；否则 "MM:SS"
   *  - 'hms'   ：强制 hh:mm:ss（适合详情页大倒计时）
   *  - 'ms'    ：强制 mm:ss
   */
  format?: 'auto' | 'hms' | 'ms'
  /** 活动已过期时是否完全隐藏（默认 false，显示"已结束"） */
  hideOnExpired?: boolean
}

const props = withDefaults(defineProps<Props>(), { format: 'auto', hideOnExpired: false })

const toMs = (s?: string | null) => {
  if (!s) return null
  const ms = new Date(s).getTime()
  return Number.isFinite(ms) ? ms : null
}

const startMs = computed(() => props.startAtMs ?? toMs(props.fallbackStart))
const endMs = computed(() => props.endAtMs ?? toMs(props.fallbackEnd))

type Phase = 'upcoming' | 'running' | 'ended' | 'unknown'

const phase = computed<Phase>(() => {
  const now = serverNow.value
  const start = startMs.value
  const end = endMs.value
  if (start == null && end == null) return 'unknown'
  if (start != null && now < start) return 'upcoming'
  if (end != null && now >= end) return 'ended'
  if (end != null) return 'running'
  return 'unknown'
})

const target = computed(() => {
  const ph = phase.value
  if (ph === 'upcoming' && startMs.value != null) return { startAtMs: startMs.value }
  if (ph === 'running' && endMs.value != null) return { deadlineMs: endMs.value }
  return null
})

const { remainingSeconds } = useCountdown(target, serverNow)

const pad = (n: number, len = 2) => String(n).padStart(len, '0')

interface DisplayParts {
  /** 第一段（最显眼） */
  primary: string
  /** 第一段说明文案（"天"） */
  primaryUnit: string
  /** 第二段 hh:mm:ss */
  secondary?: string
}

const display = computed<DisplayParts | null>(() => {
  const total = remainingSeconds.value
  if (total <= 0) return null

  if (props.format === 'ms') {
    const minutes = Math.floor(total / 60)
    const seconds = total % 60
    return { primary: `${pad(minutes)}:${pad(seconds)}`, primaryUnit: '' }
  }
  if (props.format === 'hms') {
    const hours = Math.floor(total / 3600)
    const minutes = Math.floor((total % 3600) / 60)
    const seconds = total % 60
    return { primary: `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`, primaryUnit: '' }
  }

  // auto
  const days = Math.floor(total / 86400)
  if (days >= 1) {
    const remainAfterDay = total - days * 86400
    const hours = Math.floor(remainAfterDay / 3600)
    const minutes = Math.floor((remainAfterDay % 3600) / 60)
    const seconds = remainAfterDay % 60
    return {
      primary: String(days),
      primaryUnit: '天',
      secondary: `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`,
    }
  }
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  return { primary: `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`, primaryUnit: '' }
})

const label = computed(() => {
  switch (phase.value) {
    case 'upcoming':
      return '距开抢'
    case 'running':
      return '距结束'
    case 'ended':
      return '已结束'
    default:
      return ''
  }
})

const shouldRender = computed(() => {
  if (phase.value === 'unknown') return false
  if (phase.value === 'ended' && props.hideOnExpired) return false
  return true
})
</script>

<template>
  <div
    v-if="shouldRender"
    class="countdown-banner"
    :class="{
      'is-upcoming': phase === 'upcoming',
      'is-running': phase === 'running',
      'is-ended': phase === 'ended',
    }"
  >
    <span class="countdown-banner__label">{{ label }}</span>
    <template v-if="phase !== 'ended' && display">
      <span class="countdown-banner__primary">
        <span class="countdown-banner__digits">{{ display.primary }}</span>
        <span v-if="display.primaryUnit" class="countdown-banner__unit">{{ display.primaryUnit }}</span>
      </span>
      <span v-if="display.secondary" class="countdown-banner__secondary">
        {{ display.secondary }}
      </span>
    </template>
  </div>
</template>

<style scoped>
.countdown-banner {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--radius-md);
  background: rgba(255, 184, 0, 0.18);
  color: #c47600;
  font-size: 12px;
  font-weight: 600;
}
.countdown-banner.is-upcoming {
  background: rgba(255, 45, 58, 0.12);
  color: var(--color-primary);
}
.countdown-banner.is-running {
  background: rgba(0, 180, 83, 0.12);
  color: var(--color-success);
}
.countdown-banner.is-ended {
  background: var(--color-bg-hover);
  color: var(--color-text-tertiary);
}
.countdown-banner__primary {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
}
.countdown-banner__digits {
  font-family: var(--font-mono);
  font-weight: 700;
  letter-spacing: -0.02em;
}
.countdown-banner__unit {
  font-weight: 500;
  margin-left: 1px;
}
.countdown-banner__secondary {
  font-family: var(--font-mono);
  font-weight: 700;
  letter-spacing: -0.02em;
  opacity: 0.9;
}
</style>