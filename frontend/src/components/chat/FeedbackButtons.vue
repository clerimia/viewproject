<script setup lang="ts">
import type { ChatMessage } from '@/types'
import { submitSatisfaction } from '@/api/dashboard'
import { ref } from 'vue'

const props = defineProps<{ msg: ChatMessage }>()

const feedbackOptions = [
  { score: 5, label: '满意' },
  { score: 3, label: '一般' },
  { score: 1, label: '不满意' },
]

const feedbackMessage = ref('')

async function submit(score: number) {
  if (!props.msg.interactionLogId || props.msg.feedbackSubmitting) return
  props.msg.feedbackSubmitting = true
  feedbackMessage.value = ''
  try {
    const data = await submitSatisfaction(props.msg.interactionLogId, score)
    if (!data.success) throw new Error(data.message || '评价失败')
    props.msg.satisfaction = score
    feedbackMessage.value = '感谢反馈'
  } catch (e: any) {
    feedbackMessage.value = e.message
  } finally {
    props.msg.feedbackSubmitting = false
  }
}
</script>

<template>
  <div
    v-if="msg.content && !msg.isTyping && msg.interactionLogId"
    class="flex flex-wrap items-center gap-2 text-xs text-gray-500 ml-1"
  >
    <span>本次讲解是否有帮助？</span>
    <button
      v-for="option in feedbackOptions"
      :key="option.score"
      @click="submit(option.score)"
      :disabled="msg.feedbackSubmitting || msg.satisfaction === option.score"
      :class="[
        'px-2.5 py-1 rounded-full border transition',
        msg.satisfaction === option.score
          ? 'bg-emerald-700 text-white border-emerald-700'
          : 'bg-white text-emerald-700 border-emerald-100 hover:bg-emerald-50',
      ]"
    >
      {{ option.label }}
    </button>
    <span v-if="feedbackMessage" class="text-emerald-600">{{ feedbackMessage }}</span>
  </div>
</template>
