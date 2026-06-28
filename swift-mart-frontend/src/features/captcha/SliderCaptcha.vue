<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { NSpin } from 'naive-ui'
import { checkCaptcha, generateCaptcha } from './api'
import type { SliderTrack } from './types'

const props = withDefaults(
  defineProps<{
    /** 初始生成的验证码类型 */
    type?: string
  }>(),
  { type: 'SLIDER' },
)

const emit = defineEmits<{
  /** 校验通过，返回 captchaId */
  (e: 'success', captchaId: string): void
  /** 校验失败 */
  (e: 'failed', message: string): void
}>()

interface CaptchaState {
  id: string
  backgroundImage: string
  sliderImage: string
  bgImageWidth: number
  bgImageHeight: number
  sliderImageWidth: number
  sliderImageHeight: number
}

const loading = ref(false)
const captcha = ref<CaptchaState | null>(null)
const status = ref<'idle' | 'moving' | 'checking' | 'success' | 'failed'>('idle')
const message = ref<string>('请按住滑块向右拖动完成验证')

const sliderX = ref(0)
const trackRef = ref<SliderTrack['trackList']>([])
const trackStart = ref(0)
const trackEnd = ref(0)
const dragging = ref(false)
const dragOriginX = ref(0)

const containerWidth = computed(() => captcha.value?.bgImageWidth ?? 320)
const containerHeight = computed(() => captcha.value?.bgImageHeight ?? 180)

const maxOffset = computed(() => Math.max(containerWidth.value - (captcha.value?.sliderImageWidth ?? 50), 0))

const loadCaptcha = async () => {
  loading.value = true
  status.value = 'idle'
  message.value = '请按住滑块向右拖动完成验证'
  sliderX.value = 0
  trackRef.value = []
  try {
    const data = await generateCaptcha(props.type)
    captcha.value = {
      id: data.id,
      backgroundImage: data.data.backgroundImage,
      sliderImage: data.data.sliderImage,
      bgImageWidth: data.data.backgroundImageWidth ?? 320,
      bgImageHeight: data.data.backgroundImageHeight ?? 180,
      sliderImageWidth: data.data.sliderImageWidth ?? 50,
      sliderImageHeight: data.data.sliderImageHeight ?? 50,
    }
  } catch (error) {
    message.value = error instanceof Error ? error.message : '验证码加载失败'
  } finally {
    loading.value = false
  }
}

await loadCaptcha()

const onPointerDown = (event: PointerEvent) => {
  if (status.value === 'checking' || status.value === 'success') return
  dragging.value = true
  dragOriginX.value = event.clientX - sliderX.value
  trackStart.value = Date.now()
  trackRef.value = [{ x: 0, y: 0, t: 0, type: 'down' }]
  ;(event.target as HTMLElement).setPointerCapture?.(event.pointerId)
}

const onPointerMove = (event: PointerEvent) => {
  if (!dragging.value) return
  const offset = event.clientX - dragOriginX.value
  sliderX.value = Math.max(0, Math.min(offset, maxOffset.value))
  trackRef.value.push({
    x: Math.round(sliderX.value),
    y: 0,
    t: Date.now() - trackStart.value,
    type: 'move',
  })
}

const onPointerUp = async () => {
  if (!dragging.value) return
  dragging.value = false
  if (!captcha.value) return

  trackEnd.value = Date.now()
  trackRef.value.push({ x: Math.round(sliderX.value), y: 0, t: trackEnd.value - trackStart.value, type: 'up' })

  // 滑块没拉到底就松开 → 直接判定失败
  if (sliderX.value < maxOffset.value * 0.92) {
    status.value = 'failed'
    message.value = '滑动距离不够，请拖到最右侧'
    emit('failed', message.value)
    return
  }

  status.value = 'checking'
  message.value = '正在校验…'

  try {
    await checkCaptcha({
      id: captcha.value.id,
      data: {
        bgImageWidth: captcha.value.bgImageWidth,
        bgImageHeight: captcha.value.bgImageHeight,
        sliderImageWidth: captcha.value.sliderImageWidth,
        sliderImageHeight: captcha.value.sliderImageHeight,
        startTime: trackStart.value,
        endTime: trackEnd.value,
        trackList: trackRef.value,
      } satisfies SliderTrack,
    })
    status.value = 'success'
    message.value = '验证成功'
    emit('success', captcha.value.id)
  } catch (error) {
    status.value = 'failed'
    message.value = error instanceof Error ? error.message : '校验失败，请重试'
    emit('failed', message.value)
  }
}

onBeforeUnmount(() => {
  dragging.value = false
})
</script>

<template>
  <div class="captcha">
    <div class="captcha__loading" v-if="loading">
      <NSpin size="small" />
      <span>加载验证资源…</span>
    </div>

    <template v-else-if="captcha">
      <div
        class="captcha__stage"
        :style="{ width: `${containerWidth}px`, height: `${containerHeight}px` }"
      >
        <img
          class="captcha__background"
          :src="captcha.backgroundImage"
          alt="captcha-bg"
          :width="containerWidth"
          :height="containerHeight"
        />
        <img
          class="captcha__puzzle"
          :src="captcha.sliderImage"
          alt="captcha-puzzle"
          :style="{
            width: `${captcha.sliderImageWidth}px`,
            height: `${captcha.sliderImageHeight}px`,
            transform: `translateX(${sliderX}px)`,
          }"
        />

        <div
          class="captcha__track-overlay"
          :style="{
            left: `${sliderX + captcha.sliderImageWidth / 2 - 1}px`,
          }"
        />

        <div
          class="captcha__status"
          :class="{
            'is-success': status === 'success',
            'is-failed': status === 'failed',
            'is-checking': status === 'checking',
          }"
        >
          {{ message }}
        </div>
      </div>

      <div
        class="captcha__rail"
        :style="{ width: `${containerWidth}px` }"
      >
        <div class="captcha__rail-fill" :style="{ width: `${sliderX + 24}px` }" />
        <button
          type="button"
          class="captcha__handle"
          :class="{
            'is-disabled': status === 'checking' || status === 'success',
            'is-success': status === 'success',
          }"
          :style="{ transform: `translateX(${sliderX}px)` }"
          @pointerdown="onPointerDown"
          @pointermove="onPointerMove"
          @pointerup="onPointerUp"
          @pointercancel="onPointerUp"
        >
          <span v-if="status === 'success'">✓</span>
          <span v-else>›</span>
        </button>
        <span class="captcha__rail-tip">拖动滑块完成拼图</span>
      </div>

      <div class="captcha__footer">
        <button class="captcha__refresh" type="button" @click="loadCaptcha">换一张</button>
        <span class="captcha__brand">行为验证 · TianAI</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.captcha {
  display: flex;
  flex-direction: column;
  gap: 12px;
  user-select: none;
}

.captcha__loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 24px;
  background: var(--color-bg-muted);
  border-radius: 10px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.captcha__stage {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  background: #f7f7f9;
  box-shadow: var(--shadow-xs);
}

.captcha__background {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.captcha__puzzle {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  will-change: transform;
}

.captcha__track-overlay {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: rgba(255, 45, 58, 0.45);
  pointer-events: none;
}

.captcha__status {
  position: absolute;
  inset-inline: 0;
  bottom: 0;
  padding: 6px 12px;
  font-size: 12px;
  text-align: center;
  color: rgba(255, 255, 255, 0.92);
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.55));
}
.captcha__status.is-success {
  background: linear-gradient(180deg, transparent, rgba(0, 180, 83, 0.7));
}
.captcha__status.is-failed {
  background: linear-gradient(180deg, transparent, rgba(224, 32, 32, 0.7));
}
.captcha__status.is-checking {
  background: linear-gradient(180deg, transparent, rgba(22, 119, 255, 0.7));
}

.captcha__rail {
  position: relative;
  height: 36px;
  border-radius: 999px;
  background: var(--color-bg-muted);
  overflow: hidden;
}

.captcha__rail-fill {
  position: absolute;
  inset: 0 auto 0 0;
  background: linear-gradient(90deg, rgba(255, 45, 58, 0.18), rgba(255, 45, 58, 0.4));
  transition: width 0.05s linear;
}

.captcha__rail-tip {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
  pointer-events: none;
}

.captcha__handle {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: var(--color-bg-elevated);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  display: grid;
  place-items: center;
  color: var(--color-primary);
  font-size: 18px;
  font-weight: 700;
  cursor: grab;
  transition: background 0.15s ease;
  touch-action: none;
}
.captcha__handle:hover {
  background: #fff;
}
.captcha__handle:active {
  cursor: grabbing;
}
.captcha__handle.is-disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
.captcha__handle.is-success {
  background: var(--color-success);
  color: #fff;
}

.captcha__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.captcha__refresh {
  border: 0;
  background: transparent;
  color: var(--color-primary);
  cursor: pointer;
}
.captcha__refresh:hover {
  text-decoration: underline;
}
</style>