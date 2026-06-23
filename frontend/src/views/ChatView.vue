<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDigitalHumanStore } from '@/stores/digitalHuman'
import { useSSE } from '@/composables/useSSE'
import { useSpeech } from '@/composables/useSpeech'
import { useLipSync } from '@/composables/useLipSync'
import type { ChatMessage, InterestMode } from '@/types'

import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import ChatMessageComponent from '@/components/chat/ChatMessage.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import AvatarCard from '@/components/digitalHuman/AvatarCard.vue'
import LoadingDots from '@/components/common/LoadingDots.vue'

const router = useRouter()
const auth = useAuthStore()
const dhStore = useDigitalHumanStore()
const { isGenerating, sendChatMessage } = useSSE()

const messages = ref<ChatMessage[]>([])
const chatContainer = ref<HTMLElement | null>(null)
const isConnected = ref(true)
const voiceReplyEnabled = ref(true)

const interestModes: InterestMode[] = [
  { value: 'all', label: '综合', icon: '✨', desc: '全面讲解与推荐' },
  { value: 'history', label: '历史文化', icon: '🏯', desc: '古迹、传说、建筑' },
  { value: 'nature', label: '自然风光', icon: '🌲', desc: '山水、四季、观景' },
  { value: 'food', label: '美食特产', icon: '🍜', desc: '小吃、伴手礼' },
]
const selectedInterestMode = ref('all')

const activeInterestLabel = computed(
  () => interestModes.find((m) => m.value === selectedInterestMode.value)?.label || '综合'
)

const guideName = computed(() => dhStore.config?.name || '云隐山 AI 导览员')
const guideWelcome = computed(
  () =>
    dhStore.config?.welcomeMessage ||
    dhStore.config?.greetingMessage ||
    '你好，我是云隐山 AI 导览员。你可以问我景区历史、自然风光、美食特产、游览路线和服务须知。'
)

const hasSpeakingAudio = computed(() => Object.values(speech.isSpeaking.value).some(Boolean))
const avatarStatus = computed(() => {
  if (hasSpeakingAudio.value) return 'speaking' as const
  if (speech.isRecording.value) return 'listening' as const
  if (speech.isRecognizing.value) return 'thinking' as const
  if (isGenerating.value) {
    const lastAssistant = [...messages.value].reverse().find((m) => m.role === 'assistant')
    return lastAssistant?.content ? 'speaking' as const : 'thinking' as const
  }
  return 'idle' as const
})
const avatarStatusText = computed(
  () =>
    ({
      idle: '待机中 · 随时为你导览',
      listening: '聆听中 · 请说出你的问题',
      thinking: '检索中 · 正在查询景区资料',
      speaking: '讲解中 · 正在为你说明',
    })[avatarStatus.value] || '待机中'
)

const speech = useSpeech(() => dhStore.config)
const lipSync = useLipSync()
const pausedSpeech = ref<Record<number, boolean>>({})
const audioByMessage = new Map<number, HTMLAudioElement>()
const audioUrlByMessage = new Map<number, string>()

function finalAnswerOnly(content: string) {
  return (content || '')
    .replace(/<think>[\s\S]*?<\/think>/gi, '')
    .replace(/<\/?think>/gi, '')
    .replace(/[#*_~`>|]{2,}/g, '')
    .replace(/(?:^|\n)\s*[-*+]\s+/g, '$1')
    .replace(/(?:^|\n)\s*\d+[.、]\s+/g, '$1')
    .replace(/\[([^\]]*)\]\([^)]+\)/g, '$1')
    .replace(/!\[([^\]]*)\]\([^)]+\)/g, '$1')
    .replace(/```[\s\S]*?```/g, '')
    .replace(/`([^`]+)`/g, '$1')
    .trim()
}

// Enhanced speak with lip-sync
async function speakWithLipSync(content: string, index: number) {
  const existingAudio = audioByMessage.get(index)
  if (existingAudio) {
    if (existingAudio.paused) {
      speech.isSpeaking.value[index] = true
      pausedSpeech.value[index] = false
      await existingAudio.play()
    } else {
      existingAudio.pause()
      lipSync.stopLipSync()
      speech.isSpeaking.value[index] = false
      pausedSpeech.value[index] = true
    }
    return
  }

  const speakText = finalAnswerOnly(content)
  if (!speakText) return

  speech.isSpeaking.value[index] = true
  pausedSpeech.value[index] = false
  let objectUrl = ''
  try {
    const { synthesizeSpeech } = await import('@/api/speech')
    const dh = dhStore.config
    const rate = (() => {
      const numeric = Number(dh?.speechSpeed)
      if (!Number.isFinite(numeric) || numeric <= 0) return '+0%'
      const percent = Math.round((numeric - 1) * 100)
      return percent >= 0 ? `+${percent}%` : `${percent}%`
    })()
    const pitch = (() => {
      const numeric = Number(dh?.speechPitch ?? dh?.pitch ?? 1)
      if (!Number.isFinite(numeric) || numeric <= 0) return '+0Hz'
      const percent = Math.round((numeric - 1) * 100)
      return percent >= 0 ? `+${percent}Hz` : `${percent}Hz`
    })()
    const volume = (() => {
      const numeric = Number(dh?.speechVolume ?? 1)
      if (!Number.isFinite(numeric) || numeric <= 0) return '+0%'
      const percent = Math.round((numeric - 1) * 100)
      return percent >= 0 ? `+${percent}%` : `${percent}%`
    })()
    const blob = await synthesizeSpeech(speakText, dh?.voiceName, rate, pitch, volume)
    objectUrl = URL.createObjectURL(blob)
    const audio = new Audio(objectUrl)
    audioByMessage.set(index, audio)
    audioUrlByMessage.set(index, objectUrl)

    audio.onplay = () => {
      speech.isSpeaking.value[index] = true
      pausedSpeech.value[index] = false
      lipSync.startLipSync(audio)
    }

    audio.onpause = () => {
      if (!audio.ended) {
        speech.isSpeaking.value[index] = false
        pausedSpeech.value[index] = true
        lipSync.stopLipSync()
      }
    }

    const cleanup = () => {
      lipSync.stopLipSync()
      speech.isSpeaking.value[index] = false
      pausedSpeech.value[index] = false
      audioByMessage.delete(index)
      const url = audioUrlByMessage.get(index)
      audioUrlByMessage.delete(index)
      if (url) URL.revokeObjectURL(url)
    }
    audio.onended = cleanup
    audio.onerror = cleanup
    await audio.play()
  } catch (e) {
    console.warn('TTS error', e)
    lipSync.stopLipSync()
    speech.isSpeaking.value[index] = false
    pausedSpeech.value[index] = false
    audioByMessage.delete(index)
    audioUrlByMessage.delete(index)
    if (objectUrl) URL.revokeObjectURL(objectUrl)
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

async function handleSend(text: string, options: { speakReply?: boolean } = {}) {
  messages.value.push({ role: 'user', content: text } as ChatMessage)

  const aiMsg = reactive<ChatMessage>({
    role: 'assistant',
    content: '',
    rawContent: '',
    thinkingContent: '',
    references: [],
    interactionLogId: null,
    satisfaction: null,
    feedbackSubmitting: false,
    feedbackMessage: '',
    steps: [],
    isTyping: true,
  })
  messages.value.push(aiMsg)
  const aiIndex = messages.value.length - 1
  scrollToBottom()

  await sendChatMessage(text, selectedInterestMode.value, aiMsg, scrollToBottom)

  const shouldSpeakReply = options.speakReply ?? voiceReplyEnabled.value
  if (shouldSpeakReply && aiMsg.content.trim()) {
    await speakWithLipSync(aiMsg.content, aiIndex)
  }
}

function handleSpeak(content: string, index: number) {
  speakWithLipSync(content, index)
}

function handleToggleRecording() {
  speech.toggleRecording((text) => {
    const recognizedText = text.trim()
    if (!recognizedText) return
    handleSend(recognizedText, { speakReply: voiceReplyEnabled.value })
  })
}

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}

// Scroll to bottom when messages change
watch(() => messages.value.length, scrollToBottom)

onMounted(() => {
  dhStore.load()
  speech.checkMicSupport()
})
</script>

<template>
  <div class="h-full flex p-3 gap-3 overflow-hidden">
    <!-- Sidebar -->
    <AppSidebar
      :config="dhStore.config"
      :guide-name="guideName"
      :guide-welcome="guideWelcome"
      :avatar-status="avatarStatus"
      :avatar-status-text="avatarStatusText"
      :interest-modes="interestModes"
      :selected-mode="selectedInterestMode"
      :user-id="auth.userId"
      :role="auth.role"
      :mouth-level="lipSync.mouthLevel.value"
      @update:selected-mode="selectedInterestMode = $event"
      @logout="logout"
    />

    <!-- Main chat area -->
    <div class="flex-1 flex flex-col paper-card rounded-[1.75rem] overflow-hidden border border-white/70 relative">
      <AppHeader :interest-label="activeInterestLabel" :is-connected="isConnected" />

      <div class="px-4 md:px-6 py-2 bg-white/60 border-b border-emerald-100 flex items-center justify-end gap-2 text-xs text-emerald-800">
        <span>数字人语音回答</span>
        <button
          type="button"
          @click="voiceReplyEnabled = !voiceReplyEnabled"
          :class="[
            'relative w-11 h-6 rounded-full transition-colors',
            voiceReplyEnabled ? 'bg-emerald-600' : 'bg-gray-300',
          ]"
          :title="voiceReplyEnabled ? '语音提问后自动朗读回答' : '只显示文字，可手动点击喇叭朗读'"
        >
          <span
            :class="[
              'absolute top-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform',
              voiceReplyEnabled ? 'translate-x-5 left-0.5' : 'translate-x-0 left-0.5',
            ]"
          ></span>
        </button>
        <span class="text-emerald-700/60">{{ voiceReplyEnabled ? '开启' : '关闭' }}</span>
      </div>

      <!-- Mobile interest buttons -->
      <div class="flex md:hidden px-4 py-3 gap-2 overflow-x-auto bg-emerald-50/80 border-b border-emerald-100">
        <button
          v-for="mode in interestModes"
          :key="mode.value"
          @click="selectedInterestMode = mode.value"
          :class="[
            'px-3 py-2 rounded-full text-xs whitespace-nowrap border',
            selectedInterestMode === mode.value
              ? 'bg-emerald-700 text-white border-emerald-700'
              : 'bg-white text-emerald-800 border-emerald-100',
          ]"
        >
          {{ mode.icon }} {{ mode.label }}
        </button>
      </div>

      <!-- Messages -->
      <div class="flex-1 overflow-y-auto p-4 md:p-6 space-y-6 scroll-smooth" ref="chatContainer">
        <!-- Welcome card -->
        <div class="max-w-5xl mx-auto scenic-stage rounded-[2rem] p-5 md:p-6 border border-white/70 shadow-sm">
          <div class="relative z-10 flex flex-col md:flex-row gap-5 items-center md:items-start">
            <AvatarCard :status="avatarStatus" :status-text="avatarStatusText" :config="dhStore.config" size="large" :mouth-level="lipSync.mouthLevel.value" />
            <div class="flex-1 text-center md:text-left">
              <div class="flex flex-wrap items-center justify-center md:justify-start gap-2">
                <h2 class="display-font text-2xl md:text-3xl font-bold text-emerald-950">{{ guideName }}</h2>
                <span class="px-2.5 py-1 rounded-full bg-emerald-700 text-white text-xs">{{ avatarStatusText }}</span>
              </div>
              <p class="mt-3 text-sm md:text-base leading-relaxed text-emerald-900/80">{{ guideWelcome }}</p>
              <div class="mt-4 flex flex-wrap justify-center md:justify-start gap-2 text-xs text-emerald-800">
                <span class="px-3 py-1 rounded-full bg-white/70 border border-emerald-100">🏯 历史讲解</span>
                <span class="px-3 py-1 rounded-full bg-white/70 border border-emerald-100">🌲 自然风光</span>
                <span class="px-3 py-1 rounded-full bg-white/70 border border-emerald-100">🍜 美食特产</span>
                <span class="px-3 py-1 rounded-full bg-white/70 border border-emerald-100">🗺️ 路线建议</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Chat messages -->
        <div v-for="(msg, index) in messages" :key="index" class="max-w-5xl mx-auto w-full">
          <ChatMessageComponent
            :msg="msg"
            :is-speaking="!!speech.isSpeaking.value[index]"
            :is-paused="!!pausedSpeech[index]"
            @speak="(content: string) => handleSpeak(content, index)"
          />
        </div>
      </div>

      <!-- Input -->
      <ChatInput
        :is-generating="isGenerating"
        :is-recording="speech.isRecording.value"
        :is-recognizing="speech.isRecognizing.value"
        :supports-mic="speech.supportsMic.value"
        :interest-modes="interestModes"
        :selected-mode="selectedInterestMode"
        @send="handleSend"
        @toggle-recording="handleToggleRecording"
        @update:selected-mode="selectedInterestMode = $event"
      />
    </div>
  </div>
</template>
