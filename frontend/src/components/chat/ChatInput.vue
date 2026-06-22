<script setup lang="ts">
import { ref } from 'vue'
import type { InterestMode } from '@/types'

const props = defineProps<{
  isGenerating: boolean
  isRecording: boolean
  isRecognizing: boolean
  supportsMic: boolean
  interestModes: InterestMode[]
  selectedMode: string
}>()

const emit = defineEmits<{
  (e: 'send', text: string): void
  (e: 'toggleRecording'): void
  (e: 'update:selectedMode', value: string): void
}>()

const input = ref('')

function autoResize(e: Event) {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

function handleSend() {
  const text = input.value.trim()
  if (!text || props.isGenerating) return
  emit('send', text)
  input.value = ''
}
</script>

<template>
  <div class="p-4 border-t border-emerald-100 bg-white/75 backdrop-blur-xl">
    <!-- Mobile interest buttons -->
    <div class="max-w-5xl mx-auto mb-3 flex flex-wrap gap-2 md:hidden">
      <span class="text-xs text-emerald-700 self-center">兴趣：</span>
      <button
        v-for="mode in interestModes"
        :key="mode.value + '-bottom'"
        @click="$emit('update:selectedMode', mode.value)"
        :class="[
          'px-3 py-1.5 rounded-full text-xs border',
          selectedMode === mode.value
            ? 'bg-emerald-700 text-white border-emerald-700'
            : 'bg-white text-emerald-800 border-emerald-100',
        ]"
      >
        {{ mode.icon }} {{ mode.label }}
      </button>
    </div>

    <!-- Input area -->
    <div class="max-w-5xl mx-auto relative flex items-end gap-2">
      <div class="flex items-center gap-1">
        <button
          v-if="supportsMic"
          @click="$emit('toggleRecording')"
          :class="[
            'flex-shrink-0 p-2.5 rounded-full transition-all duration-200',
            isRecording
              ? 'bg-amber-500 text-white mic-recording'
              : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100',
          ]"
          :disabled="isRecognizing"
          :title="isRecording ? '点击停止录音' : '语音输入'"
        >
          <svg v-if="!isRecording" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 18.75a6 6 0 006-6v-1.5m-6 7.5a6 6 0 01-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 01-3-3V4.5a3 3 0 116 0v8.25a3 3 0 01-3 3z" />
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" fill="currentColor" viewBox="0 0 24 24" class="w-5 h-5">
            <rect x="6" y="6" width="12" height="12" rx="2" />
          </svg>
        </button>
        <span v-if="isRecognizing" class="text-xs text-emerald-600 animate-pulse whitespace-nowrap">识别中...</span>
      </div>

      <div class="flex-1 relative">
        <textarea
          v-model="input"
          @keydown.enter.prevent="handleSend"
          placeholder="想了解路线、历史、美食或拍照点，可以直接问我..."
          rows="1"
          @input="autoResize"
          :disabled="isGenerating || isRecording"
          class="w-full pl-4 pr-12 py-3 bg-emerald-50/80 rounded-2xl focus:bg-white focus:ring-2 focus:ring-emerald-500 outline-none resize-none shadow-inner transition disabled:opacity-50 text-sm border border-emerald-100"
        ></textarea>
        <button
          @click="handleSend"
          :disabled="!input.trim() || isGenerating"
          class="absolute right-2 bottom-2 p-1.5 bg-emerald-700 text-white rounded-xl hover:bg-emerald-800 disabled:bg-gray-300 transition"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
          </svg>
        </button>
      </div>
    </div>

    <div class="text-center text-xs text-emerald-700/60 mt-2 flex items-center justify-center gap-2">
      <span>导览内容仅供游览参考，开放时间、票务与安全提示以景区公告为准</span>
      <span v-if="!supportsMic" class="text-gray-400">| 当前浏览器语音输入不可用</span>
    </div>
  </div>
</template>
