import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { jsonStorage, tokenStorage } from '@/shared/storage/auth-storage'
import type { LoginResult, UserInfo } from './types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(tokenStorage.get())
  const userInfo = ref<UserInfo | null>(jsonStorage.get<UserInfo>())

  const isLoggedIn = computed(() => Boolean(token.value))

  const setSession = (session: LoginResult) => {
    token.value = session.token
    userInfo.value = session.userInfo
    tokenStorage.set(session.token)
    jsonStorage.set(session.userInfo)
  }

  const clearSession = () => {
    token.value = null
    userInfo.value = null
    tokenStorage.clear()
    jsonStorage.clear()
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setSession,
    clearSession,
  }
})