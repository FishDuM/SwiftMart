<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import AppShell from '@/app/AppShell.vue'
import { preheatActivityCache } from '@/entities/goods/api'
import { normalizeError } from '@/shared/api/request'

const message = useMessage()

const form = reactive({
  activityId: 1,
})
const submitting = ref(false)
const result = ref<{ success: boolean; text: string } | null>(null)

const handleSubmit = async () => {
  if (!form.activityId || form.activityId <= 0) {
    message.warning('请输入合法的活动 ID')
    return
  }

  submitting.value = true
  result.value = null
  try {
    await preheatActivityCache({ activityId: form.activityId })
    result.value = { success: true, text: `活动 ${form.activityId} 缓存预热请求已提交，预计 5 秒内完成` }
    message.success(result.value.text)
  } catch (error) {
    const bizError = normalizeError(error)
    result.value = { success: false, text: bizError.message }
    message.error(bizError.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AppShell>
    <div class="admin-page">
      <section class="admin-card">
        <div class="admin-card__header">
          <div class="admin-card__icon">⚙</div>
          <div>
            <h1>秒杀活动缓存预热</h1>
            <p>将活动下的商品库存加载到 Redis，避免秒杀开始时出现缓存击穿。</p>
          </div>
        </div>

        <div class="admin-field">
          <label for="activity-id">活动 ID</label>
          <input
            id="activity-id"
            v-model.number="form.activityId"
            type="number"
            min="1"
            placeholder="例如：1"
          />
          <span class="admin-field__hint">预热前请确认该活动下已配置秒杀商品。</span>
        </div>

        <div v-if="result" class="admin-card__result" :class="{ 'is-success': result.success, 'is-error': !result.success }">
          {{ result.text }}
        </div>

        <div class="admin-card__actions">
          <button class="admin-card__btn admin-card__btn--ghost" type="button" @click="$router.push('/seckill')">
            返回会场
          </button>
          <button
            class="admin-card__btn admin-card__btn--primary"
            type="button"
            :disabled="submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '预热中…' : '提交预热' }}
          </button>
        </div>
      </section>
    </div>
  </AppShell>
</template>