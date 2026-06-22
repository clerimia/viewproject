<script setup lang="ts">
import type { ChatMessage as ChatMsg } from '@/types'
import { renderMarkdown } from '@/composables/useMarkdown'
import ReferencesPanel from './ReferencesPanel.vue'
import FeedbackButtons from './FeedbackButtons.vue'
import LoadingDots from '@/components/common/LoadingDots.vue'

defineProps<{
  msg: ChatMsg
  isSpeaking: boolean
}>()

defineEmits<{
  (e: 'speak', content: string): void
}>()
</script>

<template>
  <!-- User message -->
  <div v-if="msg.role === 'user'" class="flex gap-4 flex-row-reverse">
    <div
      class="w-9 h-9 rounded-2xl bg-amber-100 flex-shrink-0 flex items-center justify-center text-amber-700 text-sm border border-amber-200"
    >
      我
    </div>
    <div
      class="bg-emerald-700 text-white p-3.5 rounded-2xl rounded-tr-sm text-sm max-w-[82%] shadow-sm whitespace-pre-wrap"
    >
      {{ msg.content }}
    </div>
  </div>

  <!-- Assistant message -->
  <div v-else class="flex gap-4">
    <div
      class="w-9 h-9 rounded-2xl bg-emerald-100 flex-shrink-0 flex items-center justify-center text-emerald-700 text-sm border border-emerald-200"
    >
      导
    </div>
    <div class="flex-1 space-y-2 max-w-[92%]">
      <!-- References -->
      <ReferencesPanel :references="msg.references" />

      <!-- Agent steps -->
      <div v-if="msg.steps?.length" class="space-y-2 mb-2">
        <div
          v-for="(step, sIdx) in msg.steps"
          :key="sIdx"
          class="text-xs rounded border border-emerald-100 overflow-hidden bg-emerald-50"
        >
          <div v-if="step.type === 'THINKING'" class="px-3 py-2 flex items-center gap-2 text-emerald-700">
            <span class="animate-pulse">🧭</span> 正在规划第 {{ step.step }} 步...
          </div>
          <div v-if="step.type === 'EXECUTING'" class="px-3 py-2 bg-amber-50 border-l-4 border-amber-500 text-amber-700">
            <div class="flex items-center gap-2 font-semibold">
              <span>🛠️ 调用导览工具:</span>
              <span class="font-mono bg-white px-1 rounded border border-amber-200">{{ step.tool }}</span>
            </div>
          </div>
          <div v-if="step.type === 'ERROR'" class="px-3 py-2 bg-red-50 text-red-600 border-l-4 border-red-500">
            ⚠️ {{ step.content }}
          </div>
        </div>
      </div>

      <!-- Content + TTS -->
      <div class="flex items-end gap-1">
        <div
          v-if="msg.content"
          class="bg-white/95 border border-emerald-100 p-4 rounded-2xl rounded-tl-sm text-sm text-gray-800 shadow-sm prose max-w-none flex-1"
        >
          <div v-html="renderMarkdown(msg.content)"></div>
          <div class="text-[10px] text-red-500 mt-1">raw: {{ msg.content.substring(0, 50) }}</div>
        </div>
        <button
          v-if="msg.content"
          @click="$emit('speak', msg.content)"
          :disabled="isSpeaking || msg.isTyping"
          :class="[
            'flex-shrink-0 p-2 rounded-full transition-all duration-200',
            isSpeaking
              ? 'bg-emerald-100 text-emerald-700 tts-playing'
              : 'text-gray-400 hover:text-emerald-700 hover:bg-emerald-50',
          ]"
          :title="isSpeaking ? '正在播放...' : '朗读此消息'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.114 5.636a9 9 0 010 12.728M16.463 8.288a5.25 5.25 0 010 7.424M6.75 8.25l4.72-4.72a.75.75 0 011.28.53v15.88a.75.75 0 01-1.28.53l-4.72-4.72H4.51c-.88 0-1.704-.507-1.938-1.354A9.01 9.01 0 012.25 12c0-.83.112-1.633.322-2.396C2.806 8.756 3.63 8.25 4.51 8.25H6.75z" />
          </svg>
        </button>
      </div>

      <!-- Feedback -->
      <FeedbackButtons :msg="msg" />

      <!-- Typing indicator -->
      <div v-if="msg.isTyping && !msg.content" class="text-emerald-700 text-xs ml-1 flex items-center gap-1">
        <LoadingDots />
        <span class="ml-1">正在检索景区资料...</span>
      </div>
    </div>
  </div>
</template>
