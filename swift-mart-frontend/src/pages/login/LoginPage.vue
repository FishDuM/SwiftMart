<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import AppShell from '@/app/AppShell.vue'
import SliderCaptcha from '@/features/captcha/SliderCaptcha.vue'
import { login, sendVerifyCode } from '@/entities/user/api'
import { useAuthStore } from '@/entities/user/store'
import { LOGIN_TYPE, VERIFY_CODE_TYPE, type LoginType } from '@/entities/user/types'
import { isValidMobile } from '@/shared/utils/validate'
import { normalizeError } from '@/shared/api/request'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const loginType = ref<LoginType>(LOGIN_TYPE.PASSWORD)
const submitting = ref(false)
const sending = ref(false)

const form = reactive({
  mobile: '',
  password: '',
  verifyCode: '',
  captchaId: '',
})

const isPasswordLogin = computed(() => loginType.value === LOGIN_TYPE.PASSWORD)

// 后端只校验非空，前端也只校验非空
const validateBeforeSubmit = (): string | null => {
  if (!isValidMobile(form.mobile)) return '请输入正确的手机号'
  if (isPasswordLogin.value) {
    if (!form.password?.trim()) return '请输入登录密码'
  } else {
    if (!form.verifyCode?.trim()) return '请输入短信验证码'
  }
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
  if (!isValidMobile(form.mobile)) {
    message.warning('请输入正确的手机号')
    return
  }
  if (!form.captchaId) {
    message.warning('请先完成行为验证码校验')
    return
  }

  sending.value = true
  try {
    await sendVerifyCode({
      mobile: form.mobile,
      type: VERIFY_CODE_TYPE.LOGIN,
      captchaId: form.captchaId,
    })
    message.success('登录验证码已发送，请注意查收短信')
  } catch (error) {
    message.error(normalizeError(error).message)
  } finally {
    sending.value = false
  }
}

const handleSubmit = async () => {
  const errorMessage = validateBeforeSubmit()
  if (errorMessage) {
    message.warning(errorMessage)
    return
  }

  submitting.value = true
  try {
    const session = await login(
      isPasswordLogin.value
        ? {
            mobile: form.mobile,
            type: LOGIN_TYPE.PASSWORD,
            password: form.password,
          }
        : {
            mobile: form.mobile,
            type: LOGIN_TYPE.VERIFY_CODE,
            verifyCode: form.verifyCode,
          },
    )
    auth.setSession(session)
    message.success(`欢迎回来，${session.userInfo.nickname}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/seckill'
    await router.replace(redirect)
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
            <h1>SwiftMart<br />极速秒杀商城</h1>
            <p>新人首单 9 折 · 30 天价保 · 7 天无理由退换</p>
          </div>
          <div class="auth-card__features">
            <div class="auth-card__feature">⚡ 高并发引擎 10 万 QPS</div>
            <div class="auth-card__feature">🛒 主流秒杀方案完整落地</div>
            <div class="auth-card__feature">🔒 Sa-Token 全链路鉴权</div>
          </div>
        </aside>

        <div class="auth-card__form">
          <h2 class="auth-card__title">账号登录</h2>
          <p class="auth-card__subtitle">登录后可参与限时秒杀活动</p>

          <div class="auth-card__tabs" role="tablist">
            <button
              type="button"
              :class="{ 'is-active': isPasswordLogin }"
              role="tab"
              @click="loginType = LOGIN_TYPE.PASSWORD"
            >
              密码登录
            </button>
            <button
              type="button"
              :class="{ 'is-active': !isPasswordLogin }"
              role="tab"
              @click="loginType = LOGIN_TYPE.VERIFY_CODE"
            >
              短信验证码登录
            </button>
          </div>

          <div class="auth-card__field">
            <label for="login-mobile">手机号</label>
            <input
              id="login-mobile"
              v-model="form.mobile"
              class="auth-card__input"
              type="tel"
              inputmode="numeric"
              maxlength="11"
              placeholder="请输入 11 位手机号"
              autocomplete="tel"
            />
          </div>

          <template v-if="isPasswordLogin">
            <div class="auth-card__field">
              <label for="login-password">登录密码</label>
              <input
                id="login-password"
                v-model="form.password"
                class="auth-card__input"
                type="password"
                placeholder="请输入登录密码"
                autocomplete="current-password"
              />
            </div>
          </template>

          <template v-else>
            <div class="auth-card__field">
              <label>行为验证</label>
              <SliderCaptcha
                v-if="loginType === LOGIN_TYPE.VERIFY_CODE"
                @success="handleCaptchaSuccess"
                @failed="handleCaptchaFailed"
              />
              <small v-if="form.captchaId" class="tag-pill is-success" style="align-self: flex-start">
                ✓ 已通过验证
              </small>
            </div>

            <div class="auth-card__field">
              <label for="login-verify-code">短信验证码</label>
              <div class="auth-card__row">
                <input
                  id="login-verify-code"
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
          </template>

          <button
            class="auth-card__btn auth-card__submit"
            type="button"
            :disabled="submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '登录中…' : isPasswordLogin ? '立即登录' : '验证码登录' }}
          </button>

          <div class="auth-card__footer">
            <span>登录即代表同意 <a href="javascript:void 0">用户协议</a> 与 <a href="javascript:void 0">隐私政策</a></span>
            <RouterLink to="/register">没有账号？立即注册</RouterLink>
          </div>
        </div>
      </section>
    </main>
  </AppShell>
</template>