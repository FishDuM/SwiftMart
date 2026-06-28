<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import AppShell from '@/app/AppShell.vue'
import SliderCaptcha from '@/features/captcha/SliderCaptcha.vue'
import { register, sendVerifyCode } from '@/entities/user/api'
import { VERIFY_CODE_TYPE } from '@/entities/user/types'
import { isValidMobile } from '@/shared/utils/validate'
import { normalizeError } from '@/shared/api/request'

const router = useRouter()
const message = useMessage()

const submitting = ref(false)
const sending = ref(false)

const form = reactive({
  mobile: '',
  password: '',
  verifyCode: '',
  captchaId: '',
})

const validateBeforeSend = (): string | null => {
  if (!isValidMobile(form.mobile)) return '请输入正确的手机号'
  if (!form.captchaId) return '请先完成行为验证码校验'
  return null
}

// 后端只校验非空，前端保持一致
const validateBeforeRegister = (): string | null => {
  if (!isValidMobile(form.mobile)) return '请输入正确的手机号'
  if (!form.password?.trim()) return '请输入登录密码'
  if (!form.verifyCode?.trim()) return '请输入短信验证码'
  return null
}

const handleCaptchaSuccess = (captchaId: string) => {
  form.captchaId = captchaId
  message.success('行为验证已通过')
}

const handleCaptchaFailed = (msg: string) => {
  form.captchaId = ''
  message.error(msg)
}

const handleSendCode = async () => {
  const errorMessage = validateBeforeSend()
  if (errorMessage) {
    message.warning(errorMessage)
    return
  }
  if (sending.value) return

  sending.value = true
  try {
    await sendVerifyCode({
      mobile: form.mobile,
      type: VERIFY_CODE_TYPE.REGISTER,
      captchaId: form.captchaId,
    })
    // 后端有 60s 频率限制，超出时直接抛 VERIFY_CODE_SEND_TOO_FREQUENT
    // 由 message.error 自然提示，不需要前端额外加倒计时
    message.success('注册验证码已发送，请注意查收短信')
  } catch (error) {
    message.error(normalizeError(error).message)
  } finally {
    sending.value = false
  }
}

const handleSubmit = async () => {
  const errorMessage = validateBeforeRegister()
  if (errorMessage) {
    message.warning(errorMessage)
    return
  }

  submitting.value = true
  try {
    await register({
      mobile: form.mobile,
      password: form.password,
      verifyCode: form.verifyCode,
    })
    message.success('注册成功，请登录')
    await router.replace({ name: 'login' })
  } catch (error) {
    message.error(normalizeError(error).message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AppShell>
    <main class="auth-page">
      <section class="auth-card">
        <aside class="auth-card__brand">
          <div>
            <h1>注册 SwiftMart<br />开启秒杀之旅</h1>
            <p>完成注册即可参与限时秒杀，新人专享 9 折优惠。</p>
          </div>
          <div class="auth-card__features">
            <div class="auth-card__feature">🎁 注册即得新人礼包</div>
            <div class="auth-card__feature">⚡ 提前 30 秒提醒开抢</div>
            <div class="auth-card__feature">🔐 一键登录跨端同步</div>
          </div>
        </aside>

        <div class="auth-card__form">
          <h2 class="auth-card__title">免费注册</h2>
          <p class="auth-card__subtitle">仅需 30 秒，开启你的秒杀体验</p>

          <div class="auth-card__field">
            <label for="register-mobile">手机号</label>
            <input
              id="register-mobile"
              v-model="form.mobile"
              class="auth-card__input"
              type="tel"
              inputmode="numeric"
              maxlength="11"
              placeholder="请输入 11 位手机号"
            />
          </div>

          <div class="auth-card__field">
            <label for="register-password">登录密码</label>
            <input
              id="register-password"
              v-model="form.password"
              class="auth-card__input"
              type="password"
              placeholder="请输入登录密码"
            />
          </div>

          <div class="auth-card__field">
            <label>行为验证</label>
            <SliderCaptcha @success="handleCaptchaSuccess" @failed="handleCaptchaFailed" />
            <small v-if="form.captchaId" class="tag-pill is-success" style="align-self: flex-start">
              ✓ 已通过验证
            </small>
          </div>

          <div class="auth-card__field">
            <label for="register-verify-code">短信验证码</label>
            <div class="auth-card__row">
              <input
                id="register-verify-code"
                v-model="form.verifyCode"
                class="auth-card__input"
                type="text"
                inputmode="numeric"
                placeholder="请输入收到的短信验证码"
              />
              <button
                class="auth-card__btn auth-card__btn--ghost"
                type="button"
                :disabled="!form.captchaId || sending"
                @click="handleSendCode"
              >
                {{ sending ? '发送中…' : '获取验证码' }}
              </button>
            </div>
          </div>

          <button
            class="auth-card__btn auth-card__submit"
            type="button"
            :disabled="submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '注册中…' : '注册并登录' }}
          </button>

          <div class="auth-card__footer">
            <span>已有账号？</span>
            <RouterLink to="/login">立即登录</RouterLink>
          </div>
        </div>
      </section>
    </main>
  </AppShell>
</template>